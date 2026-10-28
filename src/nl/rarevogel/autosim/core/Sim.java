package nl.rarevogel.autosim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Physical acceleration model. Each time step:
 *   drive force  = engine torque(rpm) x gear x final drive x efficiency / wheel radius
 *                  (limited by tyre grip on the driven wheels, with weight transfer)
 *   resistance   = rolling resistance + air drag (+ slope)
 *   acceleration = (drive - resistance) / (mass x factor for rotating parts)
 * Starts with a slipping clutch (or torque converter), shifts at the point where the
 * next gear gives more pull, and loses drive for the duration of each shift.
 */
public final class Sim {
    public static final double G = 9.81, RHO = 1.2;
    /** Model constants (calibrated once against published 0-100 times, the same for every car). */
    public static double CLUTCH_TIME = 1.0, SHIFT_MANUAL = 0.40, SHIFT_AUTO = 0.25, SHIFT_DCT = 0.10, INERTIA = 0.0010, MU = 1.1;

    // settings
    public double crr = 0.013;          // rolling resistance
    public double mu;                   // tyre grip, dry asphalt (depends on the tyre era)
    public double hOverL = 0.20;        // centre of gravity height / wheelbase
    public double extraMass = 75;       // driver
    public double slopeDeg = 0;
    public double windKmh = 0;          // headwind +, tailwind -
    public boolean shiftAtRedline = false;
    public double limitKmh = 0;         // electronic limiter, 0 = none

    public final Car car;
    final double r, mass;
    public double launchRpm;
    public double[] shiftRpm;

    // state
    public double t, v, x, a, rpm;
    public int gear;                    // 0-based
    public boolean shifting, slipping, wheelspin;
    double shiftLeft;
    boolean gripLimited;
    public double peakKmh;

    public Sim(Car car) {
        this.car = car;
        r = car.wheelRadius();
        mass = car.mass + extraMass;
        shiftRpm = new double[car.gears.length];
        limitKmh = car.vmaxLimited ? car.pubVmax : 0;
        mu = tyreGrip(car.year);
    }

    public double efficiency() {
        double e = car.drive == Car.FWD ? 0.92 : car.drive == Car.AWD ? 0.87 : 0.90;
        return car.automatic ? e - 0.05 : e;
    }

    /**
     * Dry grip by tyre generation, around the calibrated value for 1985-1999 radials: narrow cross-plies of the
     * fifties and sixties grip far less, modern ultra-high-performance tyres clearly more.
     */
    public static double tyreGrip(int year) {
        double off = year < 1960 ? -0.30 : year < 1970 ? -0.25 : year < 1980 ? -0.15 : year < 1985 ? -0.05 : year < 2000 ? 0 : year < 2010 ? 0.10 : 0.20;
        return MU + off;
    }

    /** Launch: a dual-clutch box with launch control engages faster than a driver with a clutch pedal. */
    double clutchTime() { return car.dct ? CLUTCH_TIME * 0.55 : CLUTCH_TIME; }

    public double shiftTime() { return car.dct ? SHIFT_DCT : car.automatic ? SHIFT_AUTO : SHIFT_MANUAL; }

    double stallRpm() {
        // modern automatics hold the engine at a higher speed at launch (launch control / tighter converters)
        if (car.year >= 2005) return Math.max(2200, Math.min(3200, car.torqueRpm + 500));
        return Math.min(2200, car.torqueRpm * 0.8 + 600);
    }

    /** Wheel force from the engine at engine speed n in gear i (no grip limit). */
    public double wheelForce(int i, double n) {
        return car.torque.at(n) * car.gears[i] * car.diff * efficiency() / r;
    }

    public double resistance(double vms) {
        double vr = vms + windKmh / 3.6;
        double slope = Math.toRadians(slopeDeg);
        return crr * mass * G * Math.cos(slope) + 0.5 * RHO * car.cw * car.frontalArea() * vr * Math.abs(vr) + mass * G * Math.sin(slope);
    }

    /** Engine speed that belongs to a road speed in gear i. */
    public double rpmFor(double vms, int i) { return vms * 60 / (2 * Math.PI * r) * car.gears[i] * car.diff; }

    /** Rotating parts: wheels always; engine and gearbox only when the clutch is engaged. */
    double massFactor(int i) {
        if (slipping || shifting) return 1.04;
        double it = car.gears[i] * car.diff;
        return 1.04 + INERTIA * it * it;
    }

    /** Most force the tyres can transfer at acceleration a (weight shifts to the rear). */
    double grip(double acc) {
        if (car.drive == Car.AWD) return mu * mass * G;
        double transfer = mass * acc * hOverL;
        double load = mass * G * car.drivenShare + (car.drive == Car.RWD ? transfer : -transfer);
        return mu * Math.max(0, load);
    }

    /** Computes shift points: shift as soon as the next gear pulls harder (or at the limiter). */
    public void planShifts() {
        int n = car.gears.length;
        for (int i = 0; i < n - 1; i++) {
            double best = car.maxRpm - 100;
            if (!shiftAtRedline) {
                for (double rp = car.torqueRpm; rp < car.maxRpm - 100; rp += 25) {
                    double next = rp * car.gears[i + 1] / car.gears[i];
                    if (wheelForce(i + 1, next) >= wheelForce(i, rp)) { best = rp; break; }
                }
            }
            shiftRpm[i] = best;
        }
        shiftRpm[n - 1] = car.maxRpm;
    }

    public void reset() {
        t = v = x = a = 0; gear = 0; shifting = false; slipping = true; shiftLeft = 0; peakKmh = 0;
        rpm = car.automatic ? stallRpm() : launchRpm;
    }

    /** One time step of dt seconds. */
    public void step(double dt) {
        double force = 0;
        wheelspin = false;
        gripLimited = false;
        double nWheel = rpmFor(v, gear);
        if (shifting && shiftLeft <= 0) shifting = false;     // shift complete: drive again from this step on
        if (shifting) {
            shiftLeft -= dt;
            rpm += (rpmFor(v, gear) - rpm) * Math.min(1, dt * 12);   // revs drop to the new gear
        } else {
            double engineTorque, ratioMult = 1;
            if (car.automatic) {
                // torque converter: at standstill the engine runs at the stall speed; as the car picks up
                // speed the engine speed rises smoothly towards the turbine speed (no plateau), and the
                // torque multiplication falls from 2.0 at stall to 1.0 at the coupling point (speed ratio 0.85)
                double ns = stallRpm(), nt = nWheel;
                rpm = nt * 1.03 + ns * Math.exp(-(nt / ns) * (nt / ns));
                double sr = nt / rpm;
                ratioMult = sr < 0.85 ? 2.0 - sr / 0.85 : 1.0;
                slipping = sr < 0.85;
                engineTorque = car.torque.at(rpm) * ratioMult;
            } else if (slipping && nWheel < launchRpm && t < clutchTime()) {
                // slipping clutch: the revs are pulled down to the wheel speed while it engages, and meet it when
                // the clutch is fully engaged; from then on the revs follow the road speed
                rpm = launchRpm - (launchRpm - Math.max(nWheel, 0)) * (t / clutchTime());
                engineTorque = car.torque.at(rpm);
            } else {
                slipping = false;
                rpm = Math.max(nWheel, 900);
                engineTorque = car.torque.at(rpm);
            }
            force = engineTorque * car.gears[gear] * car.diff * efficiency() / r;
            if (limitKmh > 0 && v * 3.6 >= limitKmh) force = Math.min(force, resistance(v));
            // grip limit. The engine force first has to spin up the engine and gearbox (mass factor); what reaches
            // the road accelerates the car and the wheels. The tyres can pass on at most 'grip' (acceleration feeds
            // back into the weight transfer). When they cannot, the car accelerates at the grip limit and the surplus
            // engine torque only spins the wheels.
            double accEngine = (force - resistance(v)) / (mass * massFactor(gear));
            double tyreForce = mass * 1.04 * accEngine + resistance(v);
            double gmax = grip(Math.max(0, accEngine));
            gripLimited = tyreForce > gmax;
            if (gripLimited) { wheelspin = true; force = gmax; }
            // shift?
            if (gear < car.gears.length - 1 && rpm >= shiftRpm[gear] && !slipping) {
                gear++;
                shifting = true;
                shiftLeft = shiftTime();
                force = 0;
            }
        }
        a = (force - resistance(v)) / (mass * (gripLimited && !shifting ? 1.04 : massFactor(gear)));
        v = Math.max(0, v + a * dt);
        x += v * dt;
        t += dt;
        if (v * 3.6 > peakKmh) peakKmh = v * 3.6;
    }

    // ------------------------------------------------------------------ full runs

    public static final class Result {
        public final List<double[]> trace = new ArrayList<>();   // t, km/h, rpm, gear, metres
        public double t50 = -1, t80 = -1, t100 = -1, t120 = -1, t160 = -1, t200 = -1;
        public double t400 = -1, v400, t1000 = -1, v1000;
        public double vmax, launchRpm;
        public double[] shiftRpm, shiftKmh;
    }

    /** Runs from standstill for up to maxT seconds, recording milestones. */
    public Result run(double maxT) {
        Result res = new Result();
        reset();
        double dt = 0.002, next = 0;
        while (t < maxT) {
            double vk0 = v * 3.6, x0 = x;
            step(dt);
            double vk = v * 3.6;
            if (res.t50 < 0 && vk >= 50) res.t50 = t;
            if (res.t80 < 0 && vk >= 80) res.t80 = t;
            if (res.t100 < 0 && vk >= 100) res.t100 = t;
            if (res.t120 < 0 && vk >= 120) res.t120 = t;
            if (res.t160 < 0 && vk >= 160) res.t160 = t;
            if (res.t200 < 0 && vk >= 200) res.t200 = t;
            if (res.t400 < 0 && x >= 400) { res.t400 = t; res.v400 = vk; }
            if (res.t1000 < 0 && x >= 1000) { res.t1000 = t; res.v1000 = vk; }
            if (t >= next) { res.trace.add(new double[]{t, vk, rpm, gear + 1, x}); next += 0.05; }
            if (t > 20 && Math.abs(vk - vk0) < 1e-6 && x - x0 < 1) break;
        }
        res.vmax = topSpeed();
        res.launchRpm = launchRpm;
        res.shiftRpm = shiftRpm.clone();
        res.shiftKmh = new double[shiftRpm.length];
        for (int i = 0; i < shiftRpm.length; i++) res.shiftKmh[i] = car.kmh(shiftRpm[i], car.gears[i]);
        return res;
    }

    /** Top speed: highest speed in any gear where the drive force still equals the resistance. */
    public double topSpeed() {
        double best = 0;
        for (int i = 0; i < car.gears.length; i++) {
            for (double vk = 5; vk < 450; vk += 0.25) {
                double n = rpmFor(vk / 3.6, i) * (car.automatic ? 1.03 : 1);   // converter slip, as in step()
                if (n > car.maxRpm) break;
                if (wheelForce(i, n) >= resistance(vk / 3.6)) best = Math.max(best, vk);
            }
        }
        if (limitKmh > 0) best = Math.min(best, limitKmh);
        return best;
    }

    /** Picks the launch rpm with the best 0-100 time, like a test driver would. */
    public void optimiseLaunch() {
        planShifts();
        if (car.automatic) { launchRpm = stallRpm(); return; }
        double bestT = 1e9, bestR = 2500;
        for (double lr = 1500; lr <= car.maxRpm - 500; lr += 250) {
            launchRpm = lr;
            Result res = quickTo100();
            if (res.t100 > 0 && res.t100 < bestT) { bestT = res.t100; bestR = lr; }
        }
        launchRpm = bestR;
    }

    Result quickTo100() {
        Result res = new Result();
        reset();
        double dt = 0.004;
        while (t < 60) {
            step(dt);
            if (v * 3.6 >= 100) { res.t100 = t; break; }
        }
        return res;
    }

    /** Convenience: a fully prepared simulation for a car. */
    public static Sim forCar(Car c) {
        Sim s = new Sim(c);
        s.optimiseLaunch();
        return s;
    }
}
