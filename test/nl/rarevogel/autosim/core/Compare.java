package nl.rarevogel.autosim.core;
import java.util.*;
public class Compare {
    static double[] ranks(double[] x) { Integer[] i = new Integer[x.length]; for (int k = 0; k < x.length; k++) i[k] = k; Arrays.sort(i, (p, q) -> Double.compare(x[p], x[q])); double[] r = new double[x.length]; for (int k = 0; k < x.length; k++) r[i[k]] = k; return r; }
    static double corr(double[] a, double[] b) { int n = a.length; double ma = 0, mb = 0; for (int k = 0; k < n; k++) { ma += a[k]; mb += b[k]; } ma /= n; mb /= n; double s = 0, sa = 0, sb = 0; for (int k = 0; k < n; k++) { s += (a[k]-ma)*(b[k]-mb); sa += (a[k]-ma)*(a[k]-ma); sb += (b[k]-mb)*(b[k]-mb); } return s / Math.sqrt(sa * sb); }
    public static void main(String[] a) {
        int n = Car.ALL.size(); double[] pub = new double[n], old = new double[n], neu = new double[n];
        for (int k = 0; k < n; k++) { Car c = Car.ALL.get(k); pub[k] = c.pub0100; old[k] = OldSim.stepsTo100(c); neu[k] = Sim.forCar(c).run(60).t100; }
        System.out.printf("Pearson (tijden): oud %.2f nieuw %.2f%n", corr(old, pub), corr(neu, pub)); System.out.printf("Rangcorrelatie met fabrieksopgave 0-100:  oud algoritme %.2f   nieuw model %.2f%n", corr(ranks(old), ranks(pub)), corr(ranks(neu), ranks(pub)));
    }
}
