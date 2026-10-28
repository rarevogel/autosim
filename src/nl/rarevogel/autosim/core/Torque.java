package nl.rarevogel.autosim.core;

/**
 * Full-load torque curve built from the published peak torque and peak power,
 * smoothed with a monotone cubic (Fritsch-Carlson). Normally aspirated engines get a
 * rounded curve; turbo and supercharged engines a low-rev dip and a flat plateau.
 */
public final class Torque {
    public final double[] rpm, nm;
    final double[] slope;
    public final double maxRpm;

    Torque(double[] rpm, double[] nm, double maxRpm) {
        this.rpm = rpm; this.nm = nm; this.maxRpm = maxRpm;
        slope = pchip(rpm, nm);
    }

    public static Torque build(Car c) {
        double tMax = c.nm, nT = c.torqueRpm, nP = c.powerRpm, nR = c.maxRpm;
        double tP = c.kw * 1000 / (2 * Math.PI * nP / 60);
        if (tP > tMax * 0.99) tP = tMax * 0.99;
        double tR = nR > nP ? Math.min(tP, tP * nP / nR * 0.92) : tP;
        java.util.ArrayList<double[]> p = new java.util.ArrayList<>();
        if (c.turbo) {
            p.add(new double[]{800, 0.40 * tMax});
            p.add(new double[]{Math.max(1200, nT * 0.7), 0.80 * tMax});
            p.add(new double[]{nT, tMax});
            // the plateau lasts until full torque would exceed the peak power (modern turbos), at least 40% of the way
            double nCap = c.kw * 1000 / (tMax * 2 * Math.PI / 60) * 0.97;
            double nT2 = Math.max(nT + 0.4 * (nP - nT), Math.min(nCap, nP - 300));
            if (nT2 > nT + 200 && nT2 < nP - 200) p.add(new double[]{nT2, 0.98 * tMax});
        } else {
            p.add(new double[]{800, 0.62 * tMax});
            p.add(new double[]{Math.max(1300, nT * 0.5 + 400), 0.86 * tMax});
            p.add(new double[]{nT, tMax});
        }
        if (nP > nT + 100 && nP < nR - 50) p.add(new double[]{nP, tP});
        p.add(new double[]{nR, nR <= nP + 50 ? tP : tR});
        // keep strictly increasing rpm
        java.util.ArrayList<double[]> q = new java.util.ArrayList<>();
        for (double[] x : p) if (q.isEmpty() || x[0] > q.get(q.size() - 1)[0] + 50) q.add(x);
        double[] r = new double[q.size()], t = new double[q.size()];
        for (int i = 0; i < q.size(); i++) { r[i] = q.get(i)[0]; t[i] = q.get(i)[1]; }
        return new Torque(r, t, nR);
    }

    /** Full-load torque in Nm; zero above the rev limit. */
    public double at(double n) {
        if (n > maxRpm) return 0;
        if (n <= rpm[0]) return nm[0];
        int i = 0;
        while (i < rpm.length - 2 && n > rpm[i + 1]) i++;
        double h = rpm[i + 1] - rpm[i], s = (n - rpm[i]) / h;
        double h00 = (1 + 2 * s) * (1 - s) * (1 - s), h10 = s * (1 - s) * (1 - s), h01 = s * s * (3 - 2 * s), h11 = s * s * (s - 1);
        return h00 * nm[i] + h10 * h * slope[i] + h01 * nm[i + 1] + h11 * h * slope[i + 1];
    }

    /** Power in kW at an engine speed. */
    public double kw(double n) { return at(n) * n * 2 * Math.PI / 60 / 1000; }

    static double[] pchip(double[] x, double[] y) {
        int n = x.length;
        double[] d = new double[n], m = new double[n - 1];
        for (int i = 0; i < n - 1; i++) m[i] = (y[i + 1] - y[i]) / (x[i + 1] - x[i]);
        d[0] = m[0];
        d[n - 1] = m[n - 2];
        for (int i = 1; i < n - 1; i++) {
            if (m[i - 1] * m[i] <= 0) d[i] = 0;
            else {
                double w1 = 2 * (x[i + 1] - x[i]) + (x[i] - x[i - 1]), w2 = (x[i + 1] - x[i]) + 2 * (x[i] - x[i - 1]);
                d[i] = (w1 + w2) / (w1 / m[i - 1] + w2 / m[i]);
            }
        }
        return d;
    }
}
