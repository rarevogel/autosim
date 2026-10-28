package nl.rarevogel.autosim.core;
/** Finds launches where the speed rises while the engine speed stays (nearly) the same. */
public class Plateau {
    public static void main(String[] a) {
        for (Car c : Car.ALL) {
            Sim s = Sim.forCar(c);
            s.reset();
            double t0 = 0, v0 = 0, r0 = -1, worst = 0, worstV = 0;
            while (s.t < 8 && s.gear == 0) {
                s.step(0.002);
                if (r0 < 0 || Math.abs(s.rpm - r0) > 0.03 * r0) { t0 = s.t; v0 = s.v; r0 = s.rpm; }
                else if ((s.v - v0) * 3.6 > worstV) { worstV = (s.v - v0) * 3.6; worst = s.t - t0; }
            }
            if (worstV > 5) System.out.printf("%-30s %4d  %4.1f s vlak toerental, snelheid +%3.0f km/u%s%n", c.name, c.year, worst, worstV, c.automatic ? "  (automaat)" : c.dct ? "  (DCT)" : "");
        }
    }
}
