package nl.rarevogel.autosim.core;
public class Validate {
    public static void main(String[] a) {
        double se = 0, sv = 0; int n = 0, nv = 0;
        System.out.printf("%-30s %4s %6s %6s %6s | %5s %5s %5s | %5s %s%n", "auto", "jaar", "0-100", "fabr.", "afw%", "vmax", "fabr.", "afw%", "start", "400m");
        for (Car c : Car.ALL) {
            Sim s = Sim.forCar(c);
            Sim.Result r = s.run(90);
            double e = c.pub0100 > 0 ? (r.t100 - c.pub0100) / c.pub0100 * 100 : 0, ev = (r.vmax - c.pubVmax) / c.pubVmax * 100;
            if (c.pub0100 > 0) { se += e * e; n++; }
            if (!c.vmaxLimited) { sv += ev * ev; nv++; }
            System.out.printf("%-30s %4d %6.1f %6.1f %+5.0f%% | %5.0f %5.0f %+4.0f%% | %5.0f %5.1f%s%n", c.name, c.year, r.t100, c.pub0100, e, r.vmax, c.pubVmax, ev, r.launchRpm, r.t400, c.estimated ? "  (deels geschat)" : "");
        }
        System.out.printf("RMS afwijking 0-100: %.1f%%   topsnelheid: %.1f%%%n", Math.sqrt(se / n), Math.sqrt(sv / nv));
    }
}
