package nl.rarevogel.autosim.core;

import static nl.rarevogel.autosim.core.Car.AWD;
import static nl.rarevogel.autosim.core.Car.FWD;
import static nl.rarevogel.autosim.core.Car.RWD;

/**
 * Iconic cars per decade, added in version 1.2. Data line format as in the original CARS file:
 * number of gears, ratios, reverse, final drive, max rpm, rim (inch), tyre width, aspect ratio.
 * Old cross-ply sizes are given as the metric size with the same rolling diameter.
 *
 * Power, torque, weight and the published performance come from reference works; the gear ratios
 * were compiled from reference knowledge and have not all been checked per car, so every car here
 * is marked as partly estimated.
 */
final class CarsMore {
    static final String SRC = "naslagwerken";

    static Car a(String name, String data) { return Car.add(String.format("%-30s", name) + data); }

    static void load() {
        // ------------------------------------------------------------------ 1950s
        a("MERCEDES-BENZ 300 SL", "4 3.34 1.97 1.39 1 2.73 3.64 6400 15 175 86").year(1954)
                .engine(158, 5800, 274, 4600, false, 6).body(1310, 0.38, 1.79, 1.30, RWD, 0.48, false).published(9.3, 235, false, SRC, true);
        a("VOLKSWAGEN KEVER 1200", "4 3.6 2.07 1.25 0.8 3.88 4.43 4000 15 145 91").year(1955)
                .engine(22, 3400, 76, 2000, false, 4).body(730, 0.48, 1.54, 1.50, RWD, 0.60, false).published(40, 110, false, SRC, true);
        a("FIAT 500 NUOVA", "4 3.7 2.067 1.3 0.875 5.14 5.125 4500 12 125 86").year(1957)
                .engine(10, 4000, 24, 2500, false, 2).body(470, 0.38, 1.32, 1.33, RWD, 0.62, false).published(0, 85, false, SRC, true);
        a("CITROEN DS 19", "4 3.15 1.75 1.13 0.79 3.15 3.875 5000 15 165 80").year(1955)
                .engine(50, 4500, 128, 3000, false, 4).body(1235, 0.38, 1.79, 1.47, FWD, 0.65, false).published(21, 140, false, SRC, true);
        a("JAGUAR XK120", "4 3.375 1.98 1.365 1 3.375 3.64 5500 16 155 93").year(1951)
                .engine(112, 5000, 264, 2500, false, 6).body(1310, 0.40, 1.57, 1.33, RWD, 0.48, false).published(10.0, 193, false, SRC, true);
        a("PORSCHE 356 A 1600", "4 3.091 1.765 1.13 0.815 3.86 4.43 5000 15 145 91").year(1956)
                .engine(44, 4500, 110, 2800, false, 4).body(850, 0.40, 1.67, 1.31, RWD, 0.58, false).published(14.5, 160, false, SRC, true);
        a("CHEVROLET CORVETTE 1957", "4 2.2 1.66 1.31 1 2.26 3.7 6200 15 175 86").year(1957)
                .engine(175, 6200, 393, 4400, false, 8).body(1370, 0.45, 1.80, 1.30, RWD, 0.48, false).published(6.2, 212, false, SRC, true);
        a("FORD THUNDERBIRD 1955", "3 2.4 1.47 1 2 3.31 4800 15 175 86").year(1955)
                .engine(140, 4400, 386, 2500, false, 8).body(1500, 0.48, 1.79, 1.32, RWD, 0.46, true).published(10.5, 180, false, SRC, true);
        a("CHEVROLET BEL AIR 1957", "2 1.82 1 1.82 3.36 5200 14 190 88").year(1957)
                .engine(155, 4800, 400, 3000, false, 8).body(1590, 0.50, 1.88, 1.52, RWD, 0.46, true).published(10.0, 175, false, SRC, true);
        a("MINI 850", "4 3.628 2.172 1.412 1 3.628 3.765 6000 10 145 93").year(1959)
                .engine(25, 5500, 59, 2900, false, 4).body(617, 0.48, 1.41, 1.35, FWD, 0.62, false).published(30, 116, false, SRC, true);
        a("AUSTIN-HEALEY 100", "4 2.25 1.42 1 0.78 2.25 4.125 4800 15 155 90").year(1953)
                .engine(67, 4000, 197, 2000, false, 4).body(980, 0.43, 1.54, 1.24, RWD, 0.48, false).published(11.0, 166, false, SRC, true);
        a("BMW 507", "4 3.387 2.073 1.364 1 3.678 3.42 6000 16 155 93").year(1956)
                .engine(110, 5000, 236, 4000, false, 8).body(1330, 0.45, 1.65, 1.26, RWD, 0.48, false).published(11.0, 200, false, SRC, true);
        a("ALFA ROMEO GIULIETTA SPRINT", "4 3.258 1.985 1.357 1 3.252 4.556 6500 15 155 82").year(1954)
                .engine(48, 6000, 108, 4500, false, 4).body(880, 0.42, 1.54, 1.32, RWD, 0.48, false).published(15.0, 165, false, SRC, true);
        a("MG MGA 1500", "4 3.64 2.21 1.37 1 4.76 4.3 6000 15 145 91").year(1955)
                .engine(50, 5500, 105, 3500, false, 4).body(890, 0.42, 1.47, 1.27, RWD, 0.48, false).published(15.6, 158, false, SRC, true);

        // ------------------------------------------------------------------ 1960s
        a("JAGUAR E-TYPE 3.8", "4 3.377 1.86 1.283 1 3.377 3.07 5800 15 185 80").year(1961)
                .engine(165, 5500, 353, 4000, false, 6).body(1220, 0.44, 1.66, 1.22, RWD, 0.50, false).published(7.1, 241, false, SRC, true);
        a("FORD MUSTANG 289", "4 2.78 1.93 1.36 1 2.78 3.0 5500 14 185 84").year(1965)
                .engine(155, 4800, 412, 3200, false, 8).body(1300, 0.45, 1.73, 1.30, RWD, 0.46, false).published(8.5, 190, false, SRC, true);
        a("PORSCHE 911 2.0", "5 3.091 1.889 1.318 1.04 0.857 3.127 4.428 6800 15 165 82").year(1964)
                .engine(96, 6100, 174, 4200, false, 6).body(1080, 0.38, 1.61, 1.32, RWD, 0.59, false).published(9.1, 210, false, SRC, true);
        a("LAMBORGHINI MIURA P400", "5 2.52 1.735 1.225 1 0.815 2.52 4.08 7500 15 210 70").year(1966)
                .engine(257, 7000, 368, 5100, false, 12).body(1180, 0.40, 1.76, 1.05, RWD, 0.58, false).published(6.7, 280, false, SRC, true);
        a("FERRARI 250 GTO", "5 2.536 1.77 1.256 1 0.796 2.53 4.25 7500 15 185 85").year(1962)
                .engine(221, 7500, 294, 5500, false, 12).body(950, 0.35, 1.60, 1.21, RWD, 0.50, false).published(6.1, 280, false, SRC, true);
        a("ASTON MARTIN DB5", "5 2.9 1.78 1.25 1 0.834 3.0 3.77 5750 15 175 86").year(1963)
                .engine(180, 5500, 390, 4500, false, 6).body(1500, 0.36, 1.68, 1.34, RWD, 0.50, false).published(8.1, 233, false, SRC, true);
        a("SHELBY COBRA 427", "4 2.32 1.69 1.29 1 2.32 3.54 6500 15 235 68").year(1965)
                .engine(300, 6000, 650, 3700, false, 8).body(1150, 0.50, 1.73, 1.24, RWD, 0.48, false).published(4.3, 265, false, SRC, true);
        a("FORD GT40", "5 2.42 1.47 1.09 0.958 0.846 2.42 3.77 7000 15 235 60").year(1966)
                .engine(228, 6000, 447, 4200, false, 8).body(1000, 0.35, 1.78, 1.03, RWD, 0.58, false).published(5.3, 265, false, SRC, true);
        a("PONTIAC GTO 1964", "4 2.56 1.91 1.48 1 2.64 3.55 5600 14 190 88").year(1964)
                .engine(235, 4900, 576, 3600, false, 8).body(1600, 0.48, 1.87, 1.38, RWD, 0.45, false).published(7.0, 190, false, SRC, true);
        a("CHEVROLET CAMARO Z/28", "4 2.2 1.64 1.27 1 2.27 3.73 7000 15 205 70").year(1969)
                .engine(240, 6300, 440, 4200, false, 8).body(1500, 0.42, 1.88, 1.30, RWD, 0.46, false).published(6.9, 210, false, SRC, true);
        a("DODGE CHARGER R/T 440", "3 2.45 1.45 1 2.2 3.23 5200 14 215 70").year(1968)
                .engine(250, 4600, 650, 3200, false, 8).body(1700, 0.50, 1.95, 1.35, RWD, 0.45, true).published(7.0, 200, false, SRC, true);
        a("MINI COOPER S 1275", "4 3.2 1.916 1.357 1 3.2 3.765 7200 10 145 80").year(1964)
                .engine(57, 5800, 108, 3000, false, 4).body(650, 0.46, 1.41, 1.35, FWD, 0.62, false).published(11.5, 157, false, SRC, true);
        a("ALFA ROMEO SPIDER DUETTO", "5 3.304 1.988 1.355 1 0.791 3.01 4.556 6500 15 155 82").year(1966)
                .engine(80, 6000, 142, 2800, false, 4).body(990, 0.39, 1.63, 1.29, RWD, 0.48, false).published(11.3, 185, false, SRC, true);
        a("MERCEDES-BENZ 280 SL", "4 4.05 2.23 1.4 1 3.58 3.69 6200 14 185 80").year(1968)
                .engine(125, 5750, 240, 4500, false, 6).body(1360, 0.42, 1.76, 1.32, RWD, 0.48, false).published(9.3, 200, false, SRC, true);
        a("LOTUS ELAN", "4 2.51 1.64 1.23 1 2.51 3.9 6500 13 145 80").year(1964)
                .engine(78, 5500, 147, 4000, false, 4).body(640, 0.35, 1.42, 1.16, RWD, 0.48, false).published(8.0, 185, false, SRC, true);

        // ------------------------------------------------------------------ 1970s
        a("LAMBORGHINI COUNTACH LP400", "5 2.256 1.769 1.31 0.99 0.775 2.134 4.083 8000 14 215 70").year(1974)
                .engine(276, 8000, 361, 5000, false, 12).body(1300, 0.42, 1.89, 1.07, RWD, 0.58, false).published(5.9, 290, false, SRC, true);
        a("FERRARI 365 GT4 BB", "5 2.583 1.743 1.258 0.968 0.787 2.21 3.214 7700 15 215 70").year(1973)
                .engine(254, 7200, 431, 3900, false, 12).body(1320, 0.36, 1.80, 1.12, RWD, 0.56, false).published(6.5, 280, false, SRC, true);
        a("PORSCHE 911 TURBO 3.0", "4 2.25 1.304 0.893 0.656 2.438 4.0 6950 15 225 50").year(1975)
                .engine(191, 5500, 343, 4000, true, 6).body(1140, 0.40, 1.78, 1.31, RWD, 0.61, false).published(5.5, 250, false, SRC, true);
        a("BMW 2002 TURBO", "4 3.764 2.02 1.32 1 4.1 3.36 6500 13 185 70").year(1973)
                .engine(125, 5800, 240, 4000, true, 4).body(1080, 0.43, 1.62, 1.41, RWD, 0.48, false).published(7.0, 211, false, SRC, true);
        a("VOLKSWAGEN GOLF GTI", "4 3.45 2.12 1.44 1.13 3.17 3.89 6600 13 175 70").year(1976)
                .engine(81, 6100, 140, 5000, false, 4).body(810, 0.42, 1.61, 1.39, FWD, 0.62, false).published(9.0, 182, false, SRC, true);
        a("DATSUN 240Z", "4 3.549 2.197 1.42 1 3.164 3.364 7000 14 175 82").year(1970)
                .engine(105, 5600, 198, 4400, false, 6).body(1050, 0.39, 1.63, 1.28, RWD, 0.50, false).published(8.0, 200, false, SRC, true);
        a("LANCIA STRATOS HF", "5 2.923 2.062 1.524 1.156 0.917 2.611 3.71 7800 14 205 70").year(1973)
                .engine(140, 7400, 226, 4000, false, 6).body(980, 0.38, 1.75, 1.11, RWD, 0.58, false).published(6.8, 230, false, SRC, true);
        a("PLYMOUTH HEMI CUDA", "4 2.44 1.77 1.34 1 2.36 3.54 6300 15 235 60").year(1970)
                .engine(290, 5000, 664, 4000, false, 8).body(1700, 0.48, 1.90, 1.30, RWD, 0.45, false).published(6.0, 225, false, SRC, true);
        a("CHEVROLET CHEVELLE SS 454", "3 2.48 1.48 1 2.08 3.77 6000 14 225 70").year(1970)
                .engine(300, 5600, 678, 3600, false, 8).body(1700, 0.48, 1.92, 1.35, RWD, 0.45, true).published(6.4, 210, false, SRC, true);
        a("MERCEDES-BENZ 450 SEL 6.9", "3 2.31 1.46 1 1.84 2.65 5800 14 215 70").year(1975)
                .engine(210, 4250, 549, 3000, false, 8).body(1935, 0.40, 1.87, 1.42, RWD, 0.48, true).published(7.4, 225, false, SRC, true);
        a("FERRARI 308 GTB", "5 3.419 2.353 1.693 1.244 0.919 3.248 3.706 7700 14 205 70").year(1975)
                .engine(188, 7700, 284, 5000, false, 8).body(1150, 0.34, 1.72, 1.12, RWD, 0.58, false).published(6.7, 252, false, SRC, true);
        a("BMW M1", "5 2.42 1.61 1.14 0.846 0.704 2.86 4.22 7000 16 225 50").year(1978)
                .engine(204, 6500, 330, 5000, false, 6).body(1300, 0.36, 1.82, 1.14, RWD, 0.58, false).published(5.6, 262, false, SRC, true);
        a("LOTUS ESPRIT S1", "5 2.92 1.94 1.32 0.97 0.76 3.46 4.375 6500 14 205 60").year(1976)
                .engine(118, 6200, 190, 4900, false, 4).body(1000, 0.34, 1.86, 1.11, RWD, 0.58, false).published(8.4, 200, false, SRC, true);
        a("ALPINE A110 1600 S", "5 3.61 2.33 1.61 1.21 0.97 3.08 3.556 7000 13 185 70").year(1971)
                .engine(101, 6000, 144, 5000, false, 4).body(785, 0.37, 1.52, 1.13, RWD, 0.62, false).published(7.0, 210, false, SRC, true);
        a("SAAB 99 TURBO", "4 3.24 1.84 1.2 0.89 3.15 3.89 6000 15 195 60").year(1978)
                .engine(107, 5000, 235, 3000, true, 4).body(1210, 0.37, 1.69, 1.42, FWD, 0.62, false).published(8.9, 197, false, SRC, true);

        // ------------------------------------------------------------------ 1980s
        a("AUDI QUATTRO", "5 3.6 2.125 1.36 0.966 0.763 3.5 3.889 6900 15 205 60").year(1980)
                .engine(147, 5500, 285, 3500, true, 5).body(1290, 0.43, 1.72, 1.34, AWD, 1, false).published(7.1, 220, false, SRC, true);
        a("LANCIA DELTA HF INTEGRALE", "5 3.5 2.235 1.522 1.152 0.925 3.545 3.111 6500 15 195 55").year(1988)
                .engine(136, 5300, 304, 3500, true, 4).body(1200, 0.37, 1.70, 1.38, AWD, 1, false).published(6.6, 215, false, SRC, true);
        a("BMW M3 E30", "5 3.83 2.2 1.4 1 0.81 3.46 4.1 7250 15 205 55").year(1986)
                .engine(147, 6750, 240, 4750, false, 4).body(1200, 0.33, 1.68, 1.37, RWD, 0.48, false).published(6.7, 235, false, SRC, true);
        a("PORSCHE 959", "5 2.15 1.56 1.21 0.97 0.75 2.86 4.125 7600 17 255 40").year(1987)
                .engine(331, 6500, 500, 5000, true, 6).body(1450, 0.31, 1.84, 1.28, AWD, 1, false).published(3.7, 317, false, SRC, true);
        a("FORD SIERRA RS COSWORTH", "5 2.95 1.94 1.34 1 0.8 2.76 3.64 6500 15 205 50").year(1986)
                .engine(150, 6000, 276, 4500, true, 4).body(1220, 0.33, 1.73, 1.38, RWD, 0.47, false).published(6.2, 242, false, SRC, true);
        a("PEUGEOT 205 GTI 1.9", "5 2.92 1.85 1.36 1.07 0.86 3.33 3.69 6300 15 185 55").year(1986)
                .engine(96, 6000, 161, 4750, false, 4).body(910, 0.34, 1.57, 1.35, FWD, 0.63, false).published(7.8, 206, false, SRC, true);
        a("TOYOTA COROLLA GT AE86", "5 3.587 2.022 1.384 1 0.861 3.484 4.3 7600 14 185 60").year(1983)
                .engine(91, 6600, 142, 5800, false, 4).body(940, 0.38, 1.63, 1.34, RWD, 0.47, false).published(8.7, 196, false, SRC, true);
        a("DELOREAN DMC-12", "5 3.36 2.06 1.38 1.06 0.82 3.18 3.44 5800 15 235 60").year(1981)
                .engine(97, 5500, 207, 2750, false, 6).body(1230, 0.36, 1.86, 1.14, RWD, 0.65, false).published(10.5, 177, false, SRC, true);

        // ------------------------------------------------------------------ 1990s
        a("MCLAREN F1", "6 3.23 2.19 1.71 1.39 1.16 0.93 2.43 2.37 7500 17 315 45").year(1993)
                .engine(461, 7400, 650, 5600, false, 12).body(1140, 0.32, 1.82, 1.14, RWD, 0.58, false).published(3.2, 386, false, SRC, true);
        a("TOYOTA SUPRA TWIN TURBO", "6 3.827 2.36 1.685 1.312 1 0.793 3.28 3.133 6800 17 255 40").year(1993)
                .engine(239, 5600, 441, 4000, true, 6).body(1570, 0.32, 1.81, 1.28, RWD, 0.47, false).published(4.9, 250, true, SRC, true);
        a("MAZDA MX-5 1.6", "5 3.136 1.888 1.33 1 0.814 3.758 4.3 7200 14 185 60").year(1989)
                .engine(85, 6500, 136, 5500, false, 4).body(960, 0.38, 1.68, 1.23, RWD, 0.48, false).published(8.7, 197, false, SRC, true);
        a("SUBARU IMPREZA TURBO", "5 3.454 1.947 1.366 0.972 0.738 3.333 3.9 7000 15 205 55").year(1994)
                .engine(155, 6000, 290, 4000, true, 4).body(1235, 0.35, 1.69, 1.41, AWD, 1, false).published(5.8, 230, false, SRC, true);
        a("MITSUBISHI LANCER EVO VI", "5 2.785 1.95 1.407 1.031 0.761 3.416 4.529 7500 17 225 45").year(1999)
                .engine(206, 6500, 373, 3000, true, 4).body(1360, 0.35, 1.77, 1.41, AWD, 1, false).published(4.6, 240, false, SRC, true);
        a("NISSAN SKYLINE GT-R R34", "6 3.827 2.36 1.685 1.312 1 0.793 3.28 3.545 8000 18 245 40").year(1999)
                .engine(206, 6800, 392, 4400, true, 6).body(1560, 0.34, 1.79, 1.36, AWD, 1, false).published(4.9, 250, true, SRC, true);
        a("BMW M3 3.2 E36", "6 4.2 2.49 1.66 1.24 1 0.83 3.85 3.23 7600 17 245 40").year(1995)
                .engine(236, 7400, 350, 3250, false, 6).body(1460, 0.32, 1.71, 1.33, RWD, 0.48, false).published(5.5, 250, true, SRC, true);
        a("FERRARI F355 BERLINETTA", "6 3.076 2.12 1.572 1.25 1.03 0.873 2.67 4.3 8500 18 265 40").year(1994)
                .engine(280, 8250, 363, 6000, false, 8).body(1350, 0.33, 1.90, 1.17, RWD, 0.58, false).published(4.7, 295, false, SRC, true);
        a("DODGE VIPER RT/10", "6 2.66 1.78 1.3 1 0.74 0.5 2.9 3.07 6000 17 335 35").year(1992)
                .engine(298, 4600, 630, 3600, false, 10).body(1490, 0.50, 1.92, 1.12, RWD, 0.48, false).published(4.6, 266, false, SRC, true);
        a("MAZDA RX-7 FD", "5 3.483 2.015 1.391 1 0.719 3.288 4.1 8000 16 225 50").year(1992)
                .engine(176, 6500, 294, 5000, true, 2).rotary().body(1310, 0.31, 1.76, 1.23, RWD, 0.50, false).published(5.3, 250, true, SRC, true);
        a("PORSCHE 911 CARRERA 993", "6 3.82 2.2 1.52 1.22 1.02 0.82 3.55 3.444 6700 17 245 40").year(1994)
                .engine(200, 6100, 330, 5000, false, 6).body(1370, 0.33, 1.74, 1.30, RWD, 0.61, false).published(5.6, 270, false, SRC, true);
        a("RENAULT CLIO WILLIAMS", "5 3.09 1.84 1.32 1.03 0.86 3.55 4.07 6500 15 185 55").year(1993)
                .engine(110, 6100, 175, 4500, false, 4).body(990, 0.35, 1.64, 1.37, FWD, 0.63, false).published(7.8, 215, false, SRC, true);
        a("HONDA S2000", "6 3.133 2.045 1.481 1.161 0.971 0.811 2.8 4.1 9000 16 225 50").year(1999)
                .engine(177, 8300, 208, 7500, false, 4).body(1260, 0.33, 1.75, 1.28, RWD, 0.50, false).published(6.2, 240, false, SRC, true);

        // ------------------------------------------------------------------ 2000s
        a("BUGATTI VEYRON 16.4", "7 3.183 2.256 1.731 1.373 1.111 0.903 0.751 3.0 2.86 6400 20 365 30").year(2005)
                .engine(736, 6000, 1250, 2200, true, 16).body(1888, 0.36, 2.00, 1.16, AWD, 1, false).dct().published(2.5, 407, false, SRC, true);
        a("PORSCHE CARRERA GT", "6 2.83 2.05 1.6 1.29 1.03 0.77 3.0 4.44 8400 20 335 30").year(2004)
                .engine(450, 8000, 590, 5750, false, 10).body(1380, 0.40, 1.92, 1.17, RWD, 0.58, false).published(3.9, 330, false, SRC, true);
        a("FERRARI ENZO", "6 3.15 2.18 1.63 1.29 1.03 0.84 3.0 3.8 8200 19 345 35").year(2002)
                .engine(485, 7800, 657, 5500, false, 12).body(1365, 0.36, 2.04, 1.15, RWD, 0.56, false).dct().published(3.65, 350, false, SRC, true);
        a("FORD GT", "6 2.61 1.71 1.23 0.94 0.77 0.63 2.9 3.36 6500 19 315 40").year(2005)
                .engine(410, 6500, 678, 3750, true, 8).body(1520, 0.35, 1.95, 1.12, RWD, 0.57, false).published(3.8, 330, false, SRC, true);
        a("BMW M3 E46", "6 4.23 2.53 1.67 1.23 1 0.83 3.75 3.62 8000 18 255 40").year(2000)
                .engine(252, 7900, 365, 4900, false, 6).body(1570, 0.32, 1.78, 1.37, RWD, 0.49, false).published(5.2, 250, true, SRC, true);
        a("BMW M5 E60", "7 3.99 2.65 1.81 1.39 1.16 1 0.83 3.68 3.62 8250 19 285 35").year(2005)
                .engine(373, 7750, 520, 6100, false, 10).body(1830, 0.31, 1.85, 1.47, RWD, 0.48, false).dct().published(4.7, 250, true, SRC, true);
        a("AUDI R8 4.2 FSI", "6 3.13 2.04 1.57 1.24 1.03 0.84 3.0 3.91 8250 19 295 30").year(2007)
                .engine(309, 7800, 430, 4500, false, 8).body(1560, 0.34, 1.90, 1.25, AWD, 1, false).published(4.6, 301, false, SRC, true);
        a("NISSAN GT-R R35", "6 4.056 2.301 1.595 1.248 1.001 0.796 3.383 3.7 7000 20 285 35").year(2007)
                .engine(357, 6400, 588, 3200, true, 6).body(1740, 0.27, 1.90, 1.37, AWD, 1, false).dct().published(3.6, 310, false, SRC, true);
        a("LAMBORGHINI GALLARDO", "6 3.091 2.105 1.565 1.241 1.065 0.939 2.9 3.41 8000 19 295 30").year(2003)
                .engine(368, 7800, 510, 4500, false, 10).body(1520, 0.33, 1.90, 1.17, AWD, 1, false).published(4.2, 309, false, SRC, true);
        a("MERCEDES-BENZ SLR MCLAREN", "5 3.59 2.19 1.41 1 0.83 3.16 2.82 6500 18 295 35").year(2004)
                .engine(460, 6500, 780, 3250, true, 8).body(1768, 0.37, 1.91, 1.26, RWD, 0.48, true).published(3.8, 334, false, SRC, true);
        a("PORSCHE 911 GT3 997", "6 3.82 2.15 1.56 1.21 1 0.85 2.86 3.44 8400 19 305 30").year(2006)
                .engine(305, 7600, 405, 5500, false, 6).body(1395, 0.30, 1.81, 1.28, RWD, 0.62, false).published(4.3, 310, false, SRC, true);
        a("VOLKSWAGEN GOLF GTI MK5", "6 3.36 2.09 1.47 1.1 0.92 0.77 3.0 3.65 6500 17 225 45").year(2004)
                .engine(147, 5100, 280, 1800, true, 4).body(1336, 0.32, 1.76, 1.47, FWD, 0.62, false).published(7.2, 235, false, SRC, true);
        a("CHEVROLET CORVETTE Z06 C6", "6 2.97 2.07 1.43 1 0.71 0.57 3.28 3.42 7100 19 325 30").year(2006)
                .engine(377, 6300, 637, 4800, false, 8).body(1420, 0.34, 1.93, 1.24, RWD, 0.50, false).published(3.9, 318, false, SRC, true);
        a("FERRARI F430", "6 3.287 2.158 1.609 1.269 1.034 0.848 2.9 4.3 8500 19 285 35").year(2004)
                .engine(360, 8500, 465, 5250, false, 8).body(1450, 0.34, 1.92, 1.21, RWD, 0.57, false).published(4.0, 315, false, SRC, true);

        // ------------------------------------------------------------------ 2010s
        a("LEXUS LFA", "6 3.231 2.188 1.645 1.296 1.065 0.879 3.0 4.1 9000 20 305 30").year(2010)
                .engine(412, 8700, 480, 6800, false, 10).body(1480, 0.31, 1.90, 1.22, RWD, 0.48, false).dct().published(3.7, 325, false, SRC, true);
        a("FERRARI 488 GTB", "7 3.08 2.19 1.63 1.29 1.03 0.84 0.69 2.9 4.58 8000 20 305 30").year(2015)
                .engine(492, 8000, 760, 3000, true, 8).body(1475, 0.32, 1.95, 1.21, RWD, 0.58, false).dct().published(3.0, 330, false, SRC, true);
        a("MCLAREN MP4-12C", "7 3.226 2.188 1.645 1.296 1.065 0.865 0.684 2.9 3.31 8500 20 305 30").year(2011)
                .engine(441, 7000, 600, 3000, true, 8).body(1434, 0.33, 1.91, 1.20, RWD, 0.58, false).dct().published(3.1, 333, false, SRC, true);
        a("LAMBORGHINI AVENTADOR", "7 3.909 2.438 1.81 1.458 1.185 0.967 0.844 2.9 2.867 8500 20 335 30").year(2011)
                .engine(515, 8250, 690, 5500, false, 12).body(1575, 0.33, 2.03, 1.14, AWD, 1, false).dct().published(2.9, 350, false, SRC, true);
        a("PORSCHE 911 GT3 RS 991", "7 3.75 2.38 1.72 1.34 1.11 0.96 0.84 3.42 3.97 8800 21 325 30").year(2015)
                .engine(368, 8250, 460, 6250, false, 6).body(1420, 0.34, 1.88, 1.30, RWD, 0.62, false).dct().published(3.3, 310, false, SRC, true);
        a("MERCEDES-AMG GT S", "7 3.4 2.19 1.63 1.29 1.03 0.84 0.63 2.9 3.67 7200 20 295 30").year(2015)
                .engine(375, 6250, 650, 1750, true, 8).body(1570, 0.36, 1.94, 1.29, RWD, 0.53, false).dct().published(3.8, 310, false, SRC, true);
        a("BMW M4 F82", "7 4.806 2.593 1.701 1.277 1 0.844 0.671 3.9 3.46 7600 19 275 35").year(2014)
                .engine(317, 5500, 550, 1850, true, 6).body(1572, 0.34, 1.87, 1.38, RWD, 0.48, false).dct().published(4.1, 250, true, SRC, true);
        a("FORD FOCUS RS MK3", "6 3.23 2.19 1.52 1.08 0.86 0.69 3.5 4.06 6800 19 235 35").year(2016)
                .engine(257, 6000, 440, 2000, true, 4).body(1600, 0.35, 1.82, 1.47, AWD, 1, false).published(4.7, 266, false, SRC, true);
        a("HONDA CIVIC TYPE R FK8", "6 3.625 2.115 1.529 1.125 0.911 0.734 3.6 4.111 7000 20 245 30").year(2017)
                .engine(235, 6500, 400, 2500, true, 4).body(1380, 0.33, 1.88, 1.43, FWD, 0.62, false).published(5.8, 272, false, SRC, true);
        a("VOLKSWAGEN GOLF R MK7", "6 3.462 2.05 1.3 0.902 0.659 0.545 3.0 4.77 6500 18 225 40").year(2014)
                .engine(221, 5500, 380, 1800, true, 4).body(1476, 0.33, 1.79, 1.44, AWD, 1, false).dct().published(4.9, 250, true, SRC, true);
        a("TOYOTA GT86", "6 3.626 2.188 1.541 1.213 1 0.767 3.437 4.1 7400 17 215 45").year(2012)
                .engine(147, 7000, 205, 6400, false, 4).body(1240, 0.30, 1.78, 1.29, RWD, 0.47, false).published(7.6, 226, false, SRC, true);
        a("FORD MUSTANG GT", "6 3.66 2.43 1.69 1.32 1 0.65 3.53 3.55 7000 19 275 40").year(2015)
                .engine(310, 6500, 530, 4250, false, 8).body(1720, 0.35, 1.92, 1.38, RWD, 0.46, false).published(4.8, 250, true, SRC, true);
        a("DODGE CHALLENGER SRT HELLCAT", "8 4.71 3.14 2.1 1.67 1.29 1 0.84 0.67 3.3 2.62 6200 20 275 40").year(2015)
                .engine(527, 6000, 881, 4000, true, 8).body(2000, 0.38, 1.92, 1.45, RWD, 0.43, true).published(3.9, 320, false, SRC, true);
        a("BUGATTI CHIRON", "7 3.17 2.17 1.66 1.32 1.07 0.87 0.71 3.0 3.0 6700 21 355 25").year(2016)
                .engine(1103, 6700, 1600, 2000, true, 16).body(1995, 0.38, 2.04, 1.21, AWD, 1, false).dct().published(2.4, 420, true, SRC, true);
        a("ALFA ROMEO GIULIA QUADRIFOGLIO", "8 5 3.2 2.14 1.72 1.31 1 0.82 0.64 3.46 3.09 7000 19 285 30").year(2016)
                .engine(375, 6500, 600, 2500, true, 6).body(1580, 0.32, 1.87, 1.43, RWD, 0.50, true).published(3.9, 307, false, SRC, true);

        // ------------------------------------------------------------------ 2020s
        a("PORSCHE 911 GT3 992", "7 3.75 2.38 1.72 1.34 1.11 0.96 0.84 3.42 4.47 9000 21 315 30").year(2021)
                .engine(375, 8400, 470, 6100, false, 6).body(1435, 0.36, 1.85, 1.28, RWD, 0.62, false).dct().published(3.4, 318, false, SRC, true);
        a("CHEVROLET CORVETTE C8", "8 2.91 1.76 1.22 0.97 0.81 0.67 0.56 0.45 2.9 5.17 6500 20 305 30").year(2020)
                .engine(369, 6450, 637, 5150, false, 8).body(1530, 0.35, 1.93, 1.23, RWD, 0.60, false).dct().published(3.0, 312, false, SRC, true);
        a("TOYOTA GR YARIS", "6 3.538 2.238 1.535 1.162 0.928 0.777 3.5 4.058 7000 18 225 40").year(2020)
                .engine(192, 6500, 360, 3000, true, 3).body(1280, 0.34, 1.81, 1.46, AWD, 1, false).published(5.5, 230, true, SRC, true);
        a("HONDA CIVIC TYPE R FL5", "6 3.625 2.115 1.529 1.125 0.911 0.734 3.6 4.111 7000 19 265 30").year(2022)
                .engine(242, 6500, 420, 2600, true, 4).body(1430, 0.33, 1.89, 1.41, FWD, 0.62, false).published(5.4, 275, false, SRC, true);
        a("BMW M3 COMPETITION G80", "8 5 3.2 2.14 1.72 1.31 1 0.82 0.64 3.46 3.15 7200 20 285 30").year(2021)
                .engine(375, 6250, 650, 2750, true, 6).body(1730, 0.32, 1.90, 1.43, RWD, 0.48, true).published(3.9, 250, true, SRC, true);
        a("FORD MUSTANG DARK HORSE", "6 3.24 2.39 1.68 1.27 1 0.63 3.4 3.73 7500 19 305 30").year(2024)
                .engine(334, 7250, 540, 4900, false, 8).body(1800, 0.37, 1.92, 1.40, RWD, 0.46, false).published(4.4, 250, true, SRC, true);
        a("LAMBORGHINI HURACAN STO", "7 3.133 2.588 1.958 1.522 1.244 1.026 0.88 2.9 3.04 8500 20 325 30").year(2021)
                .engine(470, 8000, 565, 6500, false, 10).body(1450, 0.40, 1.95, 1.22, RWD, 0.59, false).dct().published(3.0, 310, false, SRC, true);
        a("FERRARI 812 COMPETIZIONE", "7 3.08 2.19 1.63 1.29 1.03 0.84 0.69 2.9 4.39 9500 20 315 35").year(2021)
                .engine(610, 9250, 692, 7000, false, 12).body(1600, 0.37, 1.97, 1.28, RWD, 0.53, false).dct().published(2.85, 340, false, SRC, true);
        a("MCLAREN 765LT", "7 3.98 2.61 1.91 1.48 1.16 0.91 0.69 3.0 3.54 8500 20 305 30").year(2020)
                .engine(563, 7500, 800, 5500, true, 8).body(1339, 0.37, 1.93, 1.19, RWD, 0.58, false).dct().published(2.8, 330, false, SRC, true);
        a("TOYOTA GR86", "6 3.626 2.188 1.541 1.213 1 0.767 3.437 4.3 7500 18 215 40").year(2021)
                .engine(172, 7000, 250, 3700, false, 4).body(1275, 0.30, 1.78, 1.31, RWD, 0.47, false).published(6.3, 226, false, SRC, true);
        a("PORSCHE 718 CAYMAN GT4 RS", "7 3.91 2.29 1.58 1.18 0.94 0.79 0.62 3.55 4.92 9000 20 295 30").year(2022)
                .engine(368, 8400, 450, 6750, false, 6).body(1415, 0.34, 1.82, 1.27, RWD, 0.56, false).dct().published(3.4, 315, false, SRC, true);
        a("ASTON MARTIN VANTAGE", "8 5 3.2 2.14 1.72 1.31 1 0.82 0.64 3.46 2.93 7000 21 325 30").year(2024)
                .engine(489, 6000, 800, 3000, true, 8).body(1745, 0.35, 1.98, 1.27, RWD, 0.50, true).published(3.5, 325, false, SRC, true);
    }
}
