package nl.rarevogel.autosim.core;

/**
 * The acceleration loop of the original 1993 program, kept for comparison.
 * Each loop step the revs rise by ttt x ratio^2, where ttt follows a sine "torque curve";
 * there is no mass, power, air drag or real time in it.
 */
public final class OldSim {
    public final Car car;
    public int tt = 800, ttt = 7, gear = 0, steps = 0;
    public double v;
    public boolean done;
    final double omtrek;

    public OldSim(Car c) { car = c; omtrek = c.circumference(); }

    public void step() {
        if (done) return;
        double ratio = car.gears[gear];
        tt = (int) Math.round(tt + ttt * ratio * ratio);
        ttt = (int) Math.round(12 * Math.sin(1.15 * tt * (Math.PI / (car.maxRpm + 2000))));
        v = Math.round(tt / ratio / car.diff * omtrek / 1000000 * 60);
        steps++;
        if (tt > car.maxRpm) {
            if (gear == car.gears.length - 1) done = true;
            else {
                gear++;
                tt = (int) Math.round(v * car.gears[gear] * car.diff / (omtrek * 0.00006));
            }
        }
    }

    /** Loop steps until 100 km/h, or -1. */
    public static int stepsTo100(Car c) {
        OldSim o = new OldSim(c);
        while (!o.done && o.steps < 100000) { o.step(); if (o.v >= 100) return o.steps; }
        return -1;
    }
}
