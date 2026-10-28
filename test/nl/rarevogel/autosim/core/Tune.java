package nl.rarevogel.autosim.core;
/** Grid search for the model constants over all cars with a published 0-100 time. */
public class Tune {
    public static void main(String[] a) {
        double[] cts = {0.6, 0.8, 1.0, 1.2}, shs = {0.3, 0.4}, ins = {0.001, 0.0015}, mus = {1.0, 1.1, 1.2};
        for (double ct : cts) for (double sh : shs) for (double in : ins) for (double mu : mus) {
            Sim.CLUTCH_TIME = ct; Sim.SHIFT_MANUAL = sh; Sim.INERTIA = in; Sim.MU = mu;
            double se = 0, bias = 0, seO = 0; int n = 0, nO = 0, k = 0;
            for (Car c : Car.ALL) {
                k++;
                if (c.pub0100 <= 0) continue;
                Sim s = Sim.forCar(c);
                Sim.Result r = s.quickTo100();
                double e = (r.t100 - c.pub0100) / c.pub0100 * 100;
                se += e * e; bias += e; n++;
                if (k <= 28) { seO += e * e; nO++; }
            }
            System.out.printf("clutch %.1f shift %.2f inertia %.4f mu %.2f  -> RMS %.1f%%  bias %+.1f%%  (oorspr. 28: %.1f%%)%n", ct, sh, in, mu, Math.sqrt(se / n), bias / n, Math.sqrt(seO / nO));
        }
    }
}
