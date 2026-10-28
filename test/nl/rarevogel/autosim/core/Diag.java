package nl.rarevogel.autosim.core;
public class Diag {
    public static void main(String[] a) {
        for (Car c : Car.ALL) if (c.name.contains(a[0])) {
            Sim s = Sim.forCar(c);
            System.out.println(c.name + " launch " + s.launchRpm + " shifts " + java.util.Arrays.toString(s.shiftRpm));
            for (double n = 1000; n <= c.maxRpm; n += 500) System.out.printf("  %5.0f rpm %5.0f Nm %5.0f kW%n", n, c.torque.at(n), c.torque.kw(n));
            s.reset(); double dt = 0.002; double spin = 0, slip = 0, shift = 0;
            while (s.v * 3.6 < 100 && s.t < 60) {
                s.step(dt);
                if (s.wheelspin) spin += dt; if (s.slipping) slip += dt; if (s.shifting) shift += dt;
                if (Math.abs(s.t * 10 - Math.round(s.t * 10)) < 0.0011) System.out.printf("  t=%.1f v=%.0f rpm=%.0f g=%d a=%.2f %s%s%n", s.t, s.v * 3.6, s.rpm, s.gear + 1, s.a, s.wheelspin ? "spin " : "", s.slipping ? "slip" : "");
            }
            System.out.printf("t100 %.2f  spin %.2f  clutch %.2f  shift %.2f massfactor1 %.2f%n", s.t, spin, slip, shift, s.massFactor(0));
        }
    }
}
