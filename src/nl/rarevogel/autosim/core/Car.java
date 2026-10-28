package nl.rarevogel.autosim.core;

import java.util.ArrayList;
import java.util.List;

/**
 * One car: the gearing from the original 1993 CARS file, plus the engine and body data
 * needed for a physical acceleration model.
 */
public final class Car {
    public static final int FWD = 0, RWD = 1, AWD = 2;

    // --- from the original database ---
    public String name;
    public double[] gears;
    public double reverse, diff;
    public int maxRpm, rim, tyreWidth, tyreAspect;

    // --- added for the physical model ---
    public double kw, powerRpm, nm, torqueRpm;   // peak power and torque
    public boolean turbo, automatic;
    public int cylinders;
    public double mass;            // kerb weight, kg
    public double cw, width, height;  // drag coefficient, body width and height in m
    public int drive;
    public double drivenShare;     // static share of the weight on the driven axle (1 for AWD)
    public double pub0100, pubVmax; // published figures, to check the model
    public boolean vmaxLimited;    // top speed electronically limited
    public String source;          // where the engine figures come from
    public boolean estimated;      // engine figures partly estimated

    public int year;               // model year (first year of this version)
    public boolean dct;            // dual-clutch or automated sequential gearbox: fast shifts, no torque converter
    public boolean rotary;         // Wankel engine: 'cylinders' holds the number of rotors

    public Torque torque;

    public int decade() { return year / 10 * 10; }

    /** Firing pulses per crankshaft revolution x 2, as the engine sound expects from a 4-stroke 'cylinder count'. */
    public int soundCylinders() { return rotary ? cylinders * 2 : cylinders; }

    public String engineText() { return rotary ? cylinders + " rotoren (wankel)" : cylinders + " cilinders"; }

    public String gearboxText() { return gears.length + (automatic ? " automaat" : dct ? " DCT" : " versn."); }

    /** Wheel circumference in mm, exactly as the original program computed it. */
    public double circumference() { return (25.4 * rim + tyreWidth * tyreAspect / 100.0 * 2) * Math.PI; }

    public double wheelRadius() { return circumference() / 1000 / (2 * Math.PI); }

    /** Road speed in km/h at a given engine speed in a gear (original formula). */
    public double kmh(double rpm, double ratio) { return rpm / ratio / diff * circumference() / 1e6 * 60; }

    /** Engine speed at a road speed in a gear. */
    public double rpmAt(double kmh, double ratio) { return kmh * ratio * diff / (circumference() * 0.00006); }

    public double frontalArea() { return 0.84 * width * height; }

    public String tyre() { return tyreWidth + "/" + tyreAspect + " R" + rim; }

    // ------------------------------------------------------------------ data

    static Car parse(String line) {
        Car c = new Car();
        c.name = line.substring(0, 30).trim();
        String[] p = line.substring(30).trim().split("\\s+");
        int n = Integer.parseInt(p[0]);
        c.gears = new double[n];
        for (int i = 0; i < n; i++) c.gears[i] = Double.parseDouble(p[1 + i]);
        c.reverse = Double.parseDouble(p[1 + n]);
        c.diff = Double.parseDouble(p[2 + n]);
        c.maxRpm = Integer.parseInt(p[3 + n]);
        c.rim = Integer.parseInt(p[4 + n]);
        c.tyreWidth = Integer.parseInt(p[5 + n]);
        c.tyreAspect = Integer.parseInt(p[6 + n]);
        return c;
    }

    Car engine(double kw, double powerRpm, double nm, double torqueRpm, boolean turbo, int cyl) {
        this.kw = kw; this.powerRpm = powerRpm; this.nm = nm; this.torqueRpm = torqueRpm; this.turbo = turbo; this.cylinders = cyl;
        return this;
    }

    Car body(double mass, double cw, double width, double height, int drive, double drivenShare, boolean automatic) {
        this.mass = mass; this.cw = cw; this.width = width; this.height = height; this.drive = drive;
        this.drivenShare = drivenShare; this.automatic = automatic;
        return this;
    }

    Car year(int y) { year = y; return this; }

    Car dct() { dct = true; return this; }

    Car rotary() { rotary = true; return this; }

    Car published(double t0100, double vmax, boolean limited, String source, boolean estimated) {
        this.pub0100 = t0100; this.pubVmax = vmax; this.vmaxLimited = limited; this.source = source; this.estimated = estimated;
        this.torque = Torque.build(this);
        return this;
    }

    public static final List<Car> ALL = new ArrayList<>();

    static void setYear(String name, int y) { for (Car c : ALL) if (c.name.equals(name)) c.year = y; }

    static Car add(String line) { Car c = parse(line); ALL.add(c); return c; }

    static final String ENC = "Encycarpedia", WIKI = "Wikipedia", FAB = "fabrieksopgave";

    static {
        add("ALFA ROMEO SZ                 5 2.875 1.72 1.226 0.946 0.78 3 3.909 6400 16 225 50")
                .engine(154, 6200, 245, 4500, false, 6).body(1260, 0.30, 1.73, 1.31, RWD, 0.50, false).published(7.0, 245, false, WIKI, false);
        add("ASTON MARTIN LAGONDA          3 2.45 1.45 1 2.2 3.06 5300 16 255 60")
                .engine(216, 5500, 435, 3000, false, 8).body(2064, 0.40, 1.816, 1.32, RWD, 0.48, true).published(9.1, 230, false, ENC, false);
        add("AUDI 80 C                     4 3.455 1.7 1.065 0.703 3.167 4.111 5250 13 175 70")
                .engine(55, 4500, 140, 2500, false, 4).body(1020, 0.29, 1.695, 1.397, FWD, 0.61, false).published(13.7, 167, false, ENC + " (Audi 80 1.8, 75 pk)", false);
        add("BENTLEY MULSANNE TURBO R      3 2.48 1.48 1 2 2.69 5500 15 255 65")
                .engine(221, 4000, 660, 2250, true, 8).body(2450, 0.37, 1.887, 1.485, RWD, 0.48, true).published(7.0, 230, false, WIKI + " (Rolls-Royce gaf geen officiële cijfers)", true);
        add("BMW 850I                      6 4.25 2.53 1.68 1.23 1 0.83 3.89 2.93 6700 16 235 50")
                .engine(221, 5200, 450, 4100, false, 12).body(1790, 0.29, 1.855, 1.34, RWD, 0.47, false).published(6.8, 250, true, ENC, false);
        add("BMW 325I                      5 4.23 2.519 1.665 1.222 1 4.039 3.15 6300 15 205 60")
                .engine(125, 5800, 222, 4300, false, 6).body(1210, 0.36, 1.645, 1.38, RWD, 0.47, false).published(8.2, 218, false, FAB, false);
        add("BMW ALPINA B10 BI-TURBO       5 4.02 2.32 1.4 1 0.81 3.74 3.15 6500 17 265 40")
                .engine(265, 6000, 520, 4000, true, 6).body(1695, 0.32, 1.75, 1.417, RWD, 0.48, false).published(5.9, 290, false, ENC, false);
        add("CHEVROLETTE CORVETTE ZR1      6 2.68 1.8 1.29 1 0.75 0.5 2.5 3.54 7300 17 315 35")
                .engine(280, 5800, 502, 4800, false, 8).body(1588, 0.33, 1.86, 1.18, RWD, 0.49, false).published(4.9, 290, false, "GM Authority, " + FAB, false);
        add("CITROEN 2CV                   4 5.203 2.657 1.786 1.316 5.203 4.125 6000 13 135 70")
                .engine(21, 5750, 39, 3000, false, 2).body(585, 0.51, 1.48, 1.60, FWD, 0.62, false).published(33.5, 115, false, "Ultimatespecs (2CV6 Special)", false);
        add("DE TOMASO PANTERA GTS         5 2.23 1.47 1.04 0.85 0.71 2.87 4.22 6000 15 205 55")
                .engine(221, 6000, 450, 4000, false, 8).body(1420, 0.40, 1.83, 1.10, RWD, 0.58, false).published(6.0, 240, false, "geschat (Ford 351 Cleveland)", true);
        add("DONKERVOORT D10               5 3.36 1.81 1.26 1 0.82 3.66 3.62 5500 15 225 50")
                .engine(130, 5700, 260, 3500, true, 4).body(650, 0.60, 1.60, 1.05, RWD, 0.50, false).published(4.85, 240, false, "Donkervoort; koppel geschat", true);
        add("FERRARI F40                   5 3.7 2.3 1.64 1.28 1.02 3.5 2.73 7750 17 335 35")
                .engine(352, 7000, 577, 4000, true, 8).body(1254, 0.34, 1.98, 1.13, RWD, 0.58, false).published(4.1, 324, false, FAB, false);
        add("FERRARI TESTAROSSA            5 3.14 2.014 1.526 1.167 0.875 2.532 3.214 7200 16 255 50")
                .engine(287, 6300, 490, 4500, false, 12).body(1506, 0.36, 1.976, 1.13, RWD, 0.60, false).published(5.8, 290, false, FAB, false);
        add("FERRARI 512 TR                5 2.916 1.882 1.421 1.087 0.815 2.428 3.452 7400 18 295 35")
                .engine(315, 6750, 491, 5500, false, 12).body(1473, 0.36, 1.976, 1.135, RWD, 0.59, false).published(4.8, 314, false, FAB, false);
        add("FORD THUNDERBIRD              4 2.4 1.47 1 0.67 2 2.73 5200 15 215 70")
                .engine(104, 3800, 292, 2400, false, 6).body(1604, 0.31, 1.84, 1.33, RWD, 0.43, true).published(11.8, 190, false, WIKI + " (3.8 V6); prestaties geschat", true);
        add("HONDA CIVIC CRX VTEC          5 3.25 2.052 1.416 1.103 0.87 3 4.133 8200 14 195 60")
                .engine(110, 7600, 144, 7100, false, 4).body(1010, 0.33, 1.675, 1.27, FWD, 0.62, false).published(8.1, 222, false, ENC, false);
        add("HONDA NSX                     5 3.071 1.727 1.23 0.967 0.771 3.186 4.062 8000 16 225 50")
                .engine(201, 7300, 285, 5400, false, 6).body(1365, 0.32, 1.81, 1.17, RWD, 0.58, false).published(5.7, 270, false, FAB, false);
        add("LAMBORGHINI DIABLO            5 2.31 1.52 1.12 0.88 0.68 2.12 3.83 7600 17 335 35")
                .engine(362, 7000, 580, 5200, false, 12).body(1576, 0.31, 2.04, 1.105, RWD, 0.59, false).published(4.5, 325, false, FAB, false);
        add("LOTUS ESPRIT TURBO SE         5 3.36 2.05 1.38 1.03 0.82 3.15 3.88 7000 16 245 50")
                .engine(197, 6500, 354, 4250, true, 4).body(1268, 0.35, 1.86, 1.15, RWD, 0.57, false).published(5.2, 262, false, FAB, false);
        add("MASERATI SHAMAL               6 4.22 2.688 1.71 1.265 1 0.848 3.62 3.36 6700 16 245 45")
                .engine(240, 6000, 432, 2800, true, 8).body(1420, 0.36, 1.85, 1.30, RWD, 0.48, false).published(5.3, 270, false, FAB, false);
        add("MERCEDES E 2.5-16             5 4.08 2.52 1.77 1.26 1 4.16 3.07 7800 15 205 55")
                .engine(143, 6750, 235, 5000, false, 4).body(1360, 0.32, 1.706, 1.36, RWD, 0.47, false).published(7.5, 230, false, FAB + " (190 E 2.5-16 met kat)", false);
        add("NISSAN 300 ZX TWIN TURBO      5 3.214 1.925 1.302 1 0.752 3.369 3.917 6600 16 245 45")
                .engine(208, 6400, 388, 3600, true, 6).body(1570, 0.31, 1.79, 1.25, RWD, 0.46, false).published(5.6, 250, true, FAB, false);
        add("OPEL CALIBRA TURBO 4X4        6 3.57 2.16 1.45 1.1 0.89 0.74 3.32 3.72 6750 16 205 50")
                .engine(150, 5600, 280, 2400, true, 4).body(1395, 0.29, 1.69, 1.32, AWD, 1.0, false).published(6.8, 245, false, FAB, false);
        add("PANTERA GT5                   5 2.23 1.47 1.04 0.85 0.71 2.87 4.22 6200 17 335 35")
                .engine(261, 6000, 452, 3800, false, 8).body(1474, 0.44, 1.98, 1.10, RWD, 0.58, false).published(5.7, 221, false, "Supercars.net", false);
        add("PORSCHE 911 CARRERA 2         5 3.5 2.059 1.407 1.086 0.868 2.857 3.444 6200 16 225 50")
                .engine(184, 6100, 310, 4800, false, 6).body(1350, 0.32, 1.652, 1.31, RWD, 0.61, false).published(5.7, 260, false, FAB, false);
        add("PORSCHE 968                   6 3.182 2 1.435 1.111 0.912 0.778 3.455 3.778 6800 16 225 50")
                .engine(177, 6200, 305, 4100, false, 4).body(1370, 0.34, 1.735, 1.275, RWD, 0.50, false).published(6.5, 252, false, FAB, false);
        add("RENAULT 21 GTS                5 3.095 1.845 1.321 0.967 0.758 3.546 4.092 6200 13 175 70")
                .engine(69, 5250, 140, 3000, false, 4).body(1050, 0.31, 1.726, 1.40, FWD, 0.62, false).published(10.7, 185, false, ENC, false);
        add("VW CORRADO G60                5 3.78 2.11 1.34 0.97 0.8 3.12 3.45 7000 15 185 55")
                .engine(118, 5600, 225, 4000, true, 4).body(1210, 0.32, 1.674, 1.32, FWD, 0.62, false).published(8.3, 225, false, FAB, false);

        // model years of the original 28 cars
        setYear("ALFA ROMEO SZ", 1989);
        setYear("ASTON MARTIN LAGONDA", 1987);
        setYear("AUDI 80 C", 1986);
        setYear("BENTLEY MULSANNE TURBO R", 1985);
        setYear("BMW 850I", 1990);
        setYear("BMW 325I", 1986);
        setYear("BMW ALPINA B10 BI-TURBO", 1989);
        setYear("CHEVROLETTE CORVETTE ZR1", 1990);
        setYear("CITROEN 2CV", 1981);
        setYear("DE TOMASO PANTERA GTS", 1984);
        setYear("DONKERVOORT D10", 1988);
        setYear("FERRARI F40", 1987);
        setYear("FERRARI TESTAROSSA", 1984);
        setYear("FERRARI 512 TR", 1992);
        setYear("FORD THUNDERBIRD", 1989);
        setYear("HONDA CIVIC CRX VTEC", 1989);
        setYear("HONDA NSX", 1990);
        setYear("LAMBORGHINI DIABLO", 1990);
        setYear("LOTUS ESPRIT TURBO SE", 1989);
        setYear("MASERATI SHAMAL", 1990);
        setYear("MERCEDES E 2.5-16", 1988);
        setYear("NISSAN 300 ZX TWIN TURBO", 1989);
        setYear("OPEL CALIBRA TURBO 4X4", 1992);
        setYear("PANTERA GT5", 1980);
        setYear("PORSCHE 911 CARRERA 2", 1989);
        setYear("PORSCHE 968", 1991);
        setYear("RENAULT 21 GTS", 1986);
        setYear("VW CORRADO G60", 1988);
        CarsMore.load();
    }
}
