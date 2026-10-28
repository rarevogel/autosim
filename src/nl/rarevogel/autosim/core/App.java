package nl.rarevogel.autosim.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/** The whole app, drawn on a canvas: screens, layout and touch handling. */
public final class App {
    public interface Host {
        void quit();
        /** Engine sound: rpm, load 0..1, on/off. */
        void engine(double rpm, double load, int cylinders, boolean on);
        /** Brand logo for a car (an image for Gfx.drawLayer), or null when there is none in assets/logos. */
        Object logo(String carName);
        /** True when the logos are the monochrome version (orange on black) instead of colour. */
        boolean logosMono();
        /** Text of an asset file (UTF-8), or null when it does not exist. */
        String readAsset(String path);
        /** Image from the assets for Gfx.drawLayer (cached by the host), or null. */
        Object image(String path);
    }

    // colours
    static final int BG0 = 0xFF161A20, BG1 = 0xFF0A0C0F;
    static final int P0 = 0xFF2A3038, P1 = 0xFF1C2027, EDGE = 0x22FFFFFF;
    static final int TEXT = 0xFFECEFF3, MUTED = 0xFF9AA3AE, FAINT = 0xFF5E6670;
    static final int ACCENT = 0xFFFF8A1F, RED = 0xFFE5483B, BLUE = 0xFF4DA3FF, GREEN = 0xFF52C67A, YELLOW = 0xFFFFD34A;
    static final int[] SERIES = {0xFFFF8A1F, 0xFF4DA3FF};

    public static final int GARAGE = 0, CAR = 1, SIM = 2, PICK = 3, COMPARE = 4, PHYS = 5, INFO = 6, BRANDS = 7, BRAND = 8;

    final Host host;
    public int screen = GARAGE;
    final ArrayList<Integer> stack = new ArrayList<>();
    final float[] scroll = new float[10];
    public Car car, other;
    int sortMode;
    public float time;

    // simulation screen
    public Sim live;
    public OldSim old;
    public boolean oldMode, running, sound = true, redlineShift, wet;
    public float speed = 1;
    final List<double[]> trace = new ArrayList<>();   // t, km/h, rpm, gear, x
    double traceNext;
    public Sim.Result result;
    final HashMap<Car, Sim.Result> results = new HashMap<>();
    final HashMap<Car, Sim> sims = new HashMap<>();

    // physics calculators
    public int physTab;
    final double[] drag = {100, 0, 1.80, 1.45, 0.32, 0.015, 1200, 0};   // km/h, wind, b, h, cw, roll, kg, slope deg
    final double[] brake = {100, 1.0, 0.80};                           // km/h, reaction s, friction
    final double[] corner = {80, 0.80};                                // radius m, friction

    // layout
    int W, H;
    float u, ox, cw;
    Object bg;
    int bgKey;

    // hits
    final ArrayList<float[]> hitR = new ArrayList<>();
    final ArrayList<Runnable> hitA = new ArrayList<>();
    final ArrayList<Slider> sliders = new ArrayList<>();
    float offY, clipTop, clipBottom;

    // touch
    float downX, downY, lastY, vel;
    boolean pressed, dragging;
    float scrollTop, scrollBottom;
    Slider active;

    public App(Host host) {
        this.host = host;
        car = Car.ALL.get(0);
    }

    // ================================================================ data helpers

    public Sim simFor(Car c) {
        Sim s = sims.get(c);
        if (s == null) {
            s = new Sim(c);
            s.shiftAtRedline = redlineShift;
            if (wet) s.mu *= 0.64;   // wet road: about 0.7 on 1990s tyres
            s.optimiseLaunch();
            sims.put(c, s);
        }
        return s;
    }

    public Sim.Result resultFor(Car c) {
        Sim.Result r = results.get(c);
        if (r == null) { r = simFor(c).run(90); results.put(c, r); }
        return r;
    }

    void settingsChanged() {
        sims.clear(); results.clear(); sortedCache.remove(3);
        if (screen == SIM) startRun();
    }

    static String f1(double v) { return String.format(java.util.Locale.GERMANY, "%.1f", v); }
    static String f0(double v) { return String.format(java.util.Locale.GERMANY, "%.0f", v); }
    static String f2(double v) { return String.format(java.util.Locale.GERMANY, "%.2f", v); }
    static String f3(double v) { return String.format(java.util.Locale.GERMANY, "%.3f", v); }
    static String pk(double kw) { return f0(kw * 1.35962); }

    static final java.util.HashSet<String> UPPER = new java.util.HashSet<>(java.util.Arrays.asList(
            "BMW", "VW", "GTS", "SZ", "CRX", "NSX", "ZX", "VTEC", "SE", "II", "TR", "SL", "DS", "MGA", "GTO", "GTI", "GT", "R/T",
            "SS", "SEL", "GTB", "BB", "HF", "RS", "FD", "WRX", "VI", "GT-R", "LFA", "AMG", "FSI", "SLR", "STO", "GR", "SRT", "DCT"));

    /** Display name: capitalised words, but type codes (anything with a digit, and known abbreviations) stay as they are. */
    public static String title(String n) {
        StringBuilder b = new StringBuilder();
        for (String w : n.split(" ")) {
            if (w.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            String r;
            if (w.equals("CHEVROLETTE")) r = "Chevrolet";
            else if (w.equals("MCLAREN")) r = "McLaren";
            else if (w.equals("4X4")) r = "4x4";
            else if (w.matches("MK\\d+")) r = "Mk" + w.substring(2);
            else if (UPPER.contains(w) || w.matches(".*\\d.*")) r = w;
            else {
                StringBuilder t = new StringBuilder();
                boolean up = true;
                for (char c : w.toLowerCase().toCharArray()) { t.append(up ? Character.toUpperCase(c) : c); up = c == '-' || c == '/' || c == '.'; }
                r = t.toString();
            }
            b.append(r);
        }
        return b.toString();
    }

    // ================================================================ navigation

    void go(int s) { stack.add(screen); screen = s; scroll[s] = 0; }

    public boolean back() {
        if (stack.isEmpty()) return false;
        if (screen == SIM) stopRun();
        screen = stack.remove(stack.size() - 1);
        return true;
    }

    void openSim() { go(SIM); startRun(); }

    void startRun() {
        trace.clear();
        traceNext = 0;
        if (oldMode) { old = new OldSim(car); live = null; result = null; }
        else { result = resultFor(car); live = simFor(car); live.reset(); old = null; }
        running = true;
    }

    void stopRun() { running = false; host.engine(0, 0, car.soundCylinders(), false); }

    // ================================================================ input

    public boolean busy() { return pressed || running || Math.abs(vel) > 5; }

    public void down(float x, float y) {
        downX = x; downY = y; lastY = y; pressed = true; dragging = false; vel = 0;
        active = null;
        for (Slider s : sliders) if (s.contains(x, y)) { active = s; s.set(x); }
    }

    public void move(float x, float y) {
        if (!pressed) return;
        if (active != null) { active.set(x); return; }
        if (!dragging && Math.abs(y - downY) > 10 * u && downY >= scrollTop && downY <= scrollBottom) dragging = true;
        if (dragging) {
            float dy = y - lastY;
            scroll[screen] -= dy;
            vel = vel * 0.6f - dy * 0.4f * 60;
        }
        lastY = y;
    }

    public void up(float x, float y) {
        // swipe left/right through the brand logos
        if (screen == BRAND && pressed && !dragging && active == null
                && Math.abs(x - downX) > 60 * u && Math.abs(x - downX) > 2 * Math.abs(y - downY)) {
            stepBrand(x < downX ? 1 : -1);
            pressed = false;
            return;
        }
        if (active == null && pressed && !dragging) tap(x, y);
        active = null;
        pressed = false;
        if (!dragging) vel = 0;
        dragging = false;
    }

    void tap(float x, float y) {
        for (int i = hitR.size() - 1; i >= 0; i--) {
            float[] r = hitR.get(i);
            if (x >= r[0] && x <= r[0] + r[2] && y >= r[1] && y <= r[1] + r[3] && y >= r[4] && y <= r[5]) { hitA.get(i).run(); return; }
        }
    }

    void hit(float x, float y, float w, float h, Runnable a) {
        hitR.add(new float[]{x, y + offY, w, h, clipTop, clipBottom});
        hitA.add(a);
    }

    boolean isPressed(float x, float y, float w, float h) {
        if (!pressed || dragging || active != null) return false;
        float yy = y + offY;
        return downX >= x && downX <= x + w && downY >= yy && downY <= yy + h && downY >= clipTop && downY <= clipBottom;
    }

    /** A horizontal slider for one value of an array. */
    final class Slider {
        float x, y, w, h;
        double[] arr; int idx; double min, max, step;
        boolean contains(float px, float py) { return px >= x - 10 * u && px <= x + w + 10 * u && py >= y && py <= y + h && py >= clipTop && py <= clipBottom; }
        void set(float px) {
            double f = Math.max(0, Math.min(1, (px - x) / w));
            double v = min + f * (max - min);
            arr[idx] = Math.round(v / step) * step;
        }
    }

    // ================================================================ frame

    public void frame(Gfx g, int W, int H, float dt) {
        this.W = W; this.H = H;
        time += dt;
        cw = Math.min(W, H * 0.62f);
        u = cw / 400f;
        ox = (W - cw) / 2f;
        hitR.clear(); hitA.clear(); sliders.clear();
        offY = 0; clipTop = 0; clipBottom = H;
        scrollTop = 0; scrollBottom = 0;
        if (!pressed && Math.abs(vel) > 5) { scroll[screen] += vel * dt; vel *= (float) Math.pow(0.05, dt); }

        if (screen == SIM) advance(dt);
        background(g);
        switch (screen) {
            case GARAGE: garage(g, false); break;
            case PICK: garage(g, true); break;
            case CAR: carScreen(g); break;
            case SIM: simScreen(g); break;
            case COMPARE: compare(g); break;
            case PHYS: phys(g); break;
            case BRANDS: brands(g); break;
            case BRAND: brand(g); break;
            default: info(g); break;
        }
    }

    float X(float ux) { return ox + ux * u; }

    void background(Gfx g) {
        int key = W * 10000 + H;
        if (bg == null || bgKey != key) {
            bgKey = key;
            final int fw = W, fh = H;
            bg = g.layer(Math.max(1, W / 2), Math.max(1, H / 2), new Gfx.Painter() {
                public void paint(Gfx gg) {
                    float w = fw / 2f, h = fh / 2f;
                    gg.linear(0, 0, 0, h, BG0, BG1);
                    gg.fillRect(0, 0, w, h);
                    // a faint grid, as in the 1993 program
                    gg.color(0x0CFFFFFF);
                    float step = w / 20f;
                    for (float x = 0; x < w; x += step) gg.line(x, 0, x, h, 1);
                    for (float y = 0; y < h; y += step) gg.line(0, y, w, y, 1);
                    gg.radial(w * 0.5f, 0, w, 0x30FF8A1F, 0x00000000);
                    gg.fillRect(0, 0, w, h);
                }
            });
        }
        g.drawLayer(bg, 0, 0, W, H);
    }

    void panel(Gfx g, float x, float y, float w, float h, float r) {
        g.color(0x55000000);
        g.fillRoundRect(x + 1.5f * u, y + 3 * u, w, h, r);
        g.linear(0, y, 0, y + h, P0, P1);
        g.fillRoundRect(x, y, w, h, r);
        g.color(EDGE);
        g.strokeRoundRect(x + 0.5f * u, y + 0.5f * u, w - u, h - u, r, 1.2f * u);
    }

    static final int PRIMARY = 0, PANEL = 1, GHOST = 2, ON = 3;

    void button(Gfx g, float x, float y, float w, float h, String label, int kind, Runnable a) {
        boolean pr = isPressed(x, y, w, h);
        float r = Math.min(h * 0.3f, 14 * u);
        if (kind == PRIMARY) {
            g.color(0x66000000); g.fillRoundRect(x + 1.5f * u, y + 3 * u, w, h, r);
            g.linear(0, y, 0, y + h, pr ? 0xFFD06A10 : 0xFFFF9A33, pr ? 0xFF8A3E05 : 0xFFD9650A);
            g.fillRoundRect(x, y, w, h, r);
        } else if (kind == PANEL) {
            panel(g, x, y, w, h, r);
            if (pr) { g.color(0x30000000); g.fillRoundRect(x, y, w, h, r); }
        } else if (kind == ON) {
            g.color(pr ? 0x55FF8A1F : 0x40FF8A1F); g.fillRoundRect(x, y, w, h, r);
            g.color(ACCENT); g.strokeRoundRect(x, y, w, h, r, 1.4f * u);
        } else {
            g.color(pr ? 0x30FFFFFF : 0x14FFFFFF); g.fillRoundRect(x, y, w, h, r);
            g.color(0x40FFFFFF); g.strokeRoundRect(x, y, w, h, r, 1.2f * u);
        }
        if (label != null) {
            float fs = Math.min(h * 0.38f, 17 * u);
            while (g.textWidth(label, fs, Gfx.BOLD) > w - 12 * u && fs > 9 * u) fs *= 0.92f;
            g.color(kind == PRIMARY ? 0xFF1A0D00 : TEXT);
            g.text(label, x + w / 2, y + h / 2 + fs * 0.36f, fs, 0, Gfx.BOLD);
        }
        if (a != null) hit(x, y, w, h, a);
    }

    float header(Gfx g, String title, String sub) { return header(g, title, sub, null); }

    /** Height of the header of a car's screens: double height when there is a logo to show. */
    float headerHeight(Car c) { return host.logo(c.name) != null ? 128 * u : 64 * u; }

    float header(Gfx g, String title, String sub, Object logo) {
        float h = logo != null ? 128 * u : 64 * u;
        g.linear(0, 0, 0, h, 0xF2111418, 0xE6111418);
        g.fillRect(0, 0, W, h);
        g.color(0x66FF8A1F);
        g.fillRect(0, h - 1.5f * u, W, 1.5f * u);
        float bs = 40 * u, bx = X(10), by = logo != null ? (h - bs) / 2 : 12 * u;
        g.color(isPressed(bx, by, bs, bs) ? 0x33FFFFFF : 0x14FFFFFF);
        g.fillCircle(bx + bs / 2, by + bs / 2, bs / 2);
        g.color(TEXT);
        float cx = bx + bs / 2, cy = by + bs / 2;
        g.strokePoly(new float[]{cx + 4 * u, cy - 9 * u, cx - 5 * u, cy, cx + 4 * u, cy + 9 * u}, false, 3 * u);
        hit(bx - 6 * u, by - 8 * u, bs + 12 * u, bs + 16 * u, new Runnable() { public void run() { back(); } });
        float ts = 18 * u, maxW = X(390) - X(62);
        if (logo == null) {
            if (g.textWidth(title, ts, Gfx.TITLE) > maxW) ts *= maxW / g.textWidth(title, ts, Gfx.TITLE);
            g.color(TEXT);
            g.text(title, X(62), sub == null ? 39 * u : 31 * u, ts, -1, Gfx.TITLE);
            if (sub != null) { g.color(MUTED); g.text(sub, X(62), 51 * u, 12.5f * u, -1, Gfx.SANS); }
            return h;
        }
        // tall header: large logo tile on the right, the name (on one or two lines) and the subtitle left of it.
        // Colour logos sit on a light tile so that dark logos stay visible; monochrome ones on black with a thin edge.
        float ls = 104 * u, lx = X(390) - ls, ly = (h - ls) / 2;
        boolean mono = host.logosMono();
        g.color(mono ? 0xFF000000 : 0xFFF2F3F5);
        g.fillRoundRect(lx, ly, ls, ls, 16 * u);
        if (mono) { g.color(0x40FFFFFF); g.strokeRoundRect(lx, ly, ls, ls, 16 * u, 1.2f * u); }
        g.drawLayer(logo, lx + 6 * u, ly + 6 * u, ls - 12 * u, ls - 12 * u);
        maxW = lx - 12 * u - X(62);
        // one line if it fits at a readable size, otherwise split at the space nearest the middle
        String[] lines = {title};
        if (g.textWidth(title, ts, Gfx.TITLE) > maxW) {
            int best = -1;
            for (int k = title.indexOf(' '); k > 0; k = title.indexOf(' ', k + 1))
                if (best < 0 || Math.abs(k - title.length() / 2) < Math.abs(best - title.length() / 2)) best = k;
            if (best > 0) lines = new String[]{title.substring(0, best), title.substring(best + 1)};
        }
        for (String l : lines) if (g.textWidth(l, ts, Gfx.TITLE) > maxW) ts *= maxW / g.textWidth(l, ts, Gfx.TITLE);
        float lh = ts * 1.3f, block = lines.length * lh + (sub != null ? 22 * u : 0);
        float ty = (h - block) / 2 + ts;
        g.color(TEXT);
        for (String l : lines) { g.text(l, X(62), ty, ts, -1, Gfx.TITLE); ty += lh; }
        if (sub != null) {
            float ss = 12.5f * u;
            if (g.textWidth(sub, ss, Gfx.SANS) > maxW) ss *= maxW / g.textWidth(sub, ss, Gfx.SANS);
            g.color(MUTED);
            g.text(sub, X(62), ty - lh + ts * 0.3f + 20 * u, ss, -1, Gfx.SANS);
        }
        return h;
    }


    void beginScroll(Gfx g, float top, float bottom) {
        scrollTop = top; scrollBottom = bottom;
        clipTop = top; clipBottom = bottom;
        g.save();
        g.clip(0, top, W, bottom - top);
        offY = top - scroll[screen];
        g.translate(0, offY);
    }

    void endScroll(Gfx g, float contentH) {
        g.restore();
        float max = Math.max(0, contentH - (scrollBottom - scrollTop));
        if (scroll[screen] < 0) { scroll[screen] = 0; if (!pressed) vel = 0; }
        if (scroll[screen] > max) { scroll[screen] = max; if (!pressed) vel = 0; }
        offY = 0; clipTop = 0; clipBottom = H;
    }

    List<String> wrap(Gfx g, String s, float size, int font, float maxW) {
        List<String> out = new ArrayList<>();
        for (String para : s.split("\n")) {
            String line = "";
            for (String w : para.split(" ")) {
                String t = line.isEmpty() ? w : line + " " + w;
                if (g.textWidth(t, size, font) > maxW && !line.isEmpty()) { out.add(line); line = w; } else line = t;
            }
            out.add(line);
        }
        return out;
    }

    float para(Gfx g, String s, float x, float y, float size, int font, int col, float maxW) {
        g.color(col);
        float lh = size * 1.4f;
        List<String> lines = wrap(g, s, size, font, maxW);
        for (int i = 0; i < lines.size(); i++) g.text(lines.get(i), x, y + size + i * lh, size, -1, font);
        return lines.size() * lh;
    }

    void kv(Gfx g, float x, float y, float w, String k, String v) {
        g.color(MUTED); g.text(k, x, y, 13.5f * u, -1, Gfx.SANS);
        g.color(TEXT); g.text(v, x + w, y, 13.5f * u, 1, Gfx.BOLD);
    }

    void section(Gfx g, String s, float y) {
        g.color(ACCENT);
        g.text(s.toUpperCase(), X(20), y, 13 * u, -1, Gfx.TITLE);
    }

    // ================================================================ GARAGE

    final java.util.HashSet<Integer> openDecades = new java.util.HashSet<>();
    final java.util.HashMap<Integer, List<Car>> sortedCache = new java.util.HashMap<>();
    static final java.util.HashMap<Car, String> TITLES = new java.util.HashMap<>();

    static String titleOf(Car c) {
        String t = TITLES.get(c);
        if (t == null) { t = title(c.name); TITLES.put(c, t); }
        return t;
    }

    /** The garage list in a sort order, computed once (the 0-100 order needs a simulation per car). */
    List<Car> sorted(int mode) {
        List<Car> list = sortedCache.get(mode);
        if (list != null) return list;
        list = new ArrayList<>(Car.ALL);
        if (mode == 0) Collections.sort(list, new Comparator<Car>() { public int compare(Car a, Car b) { return a.year != b.year ? a.year - b.year : titleOf(a).compareTo(titleOf(b)); } });
        else if (mode == 1) Collections.sort(list, new Comparator<Car>() { public int compare(Car a, Car b) { return titleOf(a).compareTo(titleOf(b)); } });
        else if (mode == 2) Collections.sort(list, new Comparator<Car>() { public int compare(Car a, Car b) { return Double.compare(b.kw, a.kw); } });
        else Collections.sort(list, new Comparator<Car>() { public int compare(Car a, Car b) { return Double.compare(t100Key(a), t100Key(b)); } });
        sortedCache.put(mode, list);
        return list;
    }

    void carRow(Gfx g, final Car c, float y, float ch, final boolean pick) {
        button(g, X(20), y, 360 * u, ch, null, PANEL, new Runnable() {
            public void run() {
                if (pick) { other = c; screen = COMPARE; scroll[COMPARE] = 0; }
                else { car = c; go(CAR); }
            }
        });
        g.color(TEXT);
        String n = titleOf(c);
        float fs = 16 * u;
        if (g.textWidth(n, fs, Gfx.BOLD) > 250 * u) fs *= 250 * u / g.textWidth(n, fs, Gfx.BOLD);
        g.text(n, X(36), y + 30 * u, fs, -1, Gfx.BOLD);
        g.color(MUTED);
        g.text(c.year + " · " + pk(c.kw) + " pk · " + f0(c.mass) + " kg · " + c.gearboxText(), X(36), y + 54 * u, 12.5f * u, -1, Gfx.SANS);
        String right = sortMode == 3 ? t(resultFor(c).t100) : sortMode == 2 ? f0(c.kw) + " kW" : c.pub0100 > 0 ? f1(c.pub0100) + " s" : "-";
        g.color(ACCENT);
        g.text(right, X(366), y + 32 * u, 16 * u, 1, Gfx.DIGIT);
        g.color(FAINT);
        g.text(sortMode == 3 ? "simulatie" : sortMode == 2 ? "" : "0-100 fabr.", X(366), y + 52 * u, 11 * u, 1, Gfx.SANS);
    }

    void garage(Gfx g, final boolean pick) {
        float top = 0;
        if (pick) top = header(g, "Vergelijk met...", title(car.name));
        float bottom = pick ? H : H - 78 * u;
        beginScroll(g, pick ? 64 * u : 0, bottom);
        float y = 0;
        if (!pick) {
            g.color(ACCENT);
            g.text("AUTOSIM", W / 2f, 66 * u, 40 * u, 0, Gfx.TITLE);
            g.color(MUTED);
            g.text("optrekken, schakelen en natuurkunde", W / 2f, 92 * u, 14 * u, 0, Gfx.SANS);
            y = 112 * u;
        } else y = 14 * u;
        // sort chips
        String[] sorts = {"Decennium", "Naam", "Vermogen", "0-100"};
        for (int i = 0; i < 4; i++) {
            final int k = i;
            button(g, X(20 + i * 91), y, 87 * u, 38 * u, sorts[i], sortMode == i ? ON : GHOST, new Runnable() { public void run() { sortMode = k; } });
        }
        y += 52 * u;
        List<Car> list = sorted(sortMode);
        float viewTop = scroll[screen] - 100 * u, viewBottom = scroll[screen] + (bottom - (pick ? 64 * u : 0)) + 100 * u;
        int decade = -1;
        boolean open = true;
        for (final Car c : list) {
            if (pick && c == car) continue;
            if (sortMode == 0 && c.decade() != decade) {
                // collapsible decade header
                decade = c.decade();
                final int d = decade;
                open = openDecades.contains(d);
                float hh = 54 * u;
                if (y + hh > viewTop && y < viewBottom) {
                    int n = 0;
                    for (Car o : list) if (o.decade() == d && !(pick && o == car)) n++;
                    button(g, X(20), y, 360 * u, hh, null, open ? ON : GHOST, new Runnable() {
                        public void run() { if (!openDecades.remove(d)) openDecades.add(d); }
                    });
                    g.color(ACCENT);
                    g.text("JAREN " + (d % 100 == 0 ? "00" : String.valueOf(d % 100)), X(38), y + 34 * u, 16 * u, -1, Gfx.TITLE);
                    g.color(MUTED);
                    g.text(d + "-" + (d + 9) + "  ·  " + n + " auto's", X(334), y + 33 * u, 12.5f * u, 1, Gfx.SANS);
                    // chevron: pointing right when closed, down when open
                    float cx = X(356), cy = y + hh / 2, k = 6 * u;
                    g.color(TEXT);
                    g.strokePoly(open ? new float[]{cx - k, cy - k / 2, cx, cy + k / 2, cx + k, cy - k / 2}
                            : new float[]{cx - k / 2, cy - k, cx + k / 2, cy, cx - k / 2, cy + k}, false, 2 * u);
                }
                y += hh + 8 * u;
            }
            if (!open) continue;
            float ch = 74 * u;
            if (y + ch > viewTop && y < viewBottom) carRow(g, c, y, ch, pick);
            y += ch + 8 * u;
        }
        endScroll(g, y + 16 * u);
        if (!pick) {
            g.linear(0, H - 78 * u, 0, H, 0xE0111418, 0xFF0A0C0F);
            g.fillRect(0, H - 78 * u, W, 78 * u);
            button(g, X(20), H - 64 * u, 114 * u, 50 * u, "Natuurkunde", PANEL, new Runnable() { public void run() { go(PHYS); } });
            button(g, X(143), H - 64 * u, 114 * u, 50 * u, "Merken", PANEL, new Runnable() { public void run() { go(BRANDS); } });
            button(g, X(266), H - 64 * u, 114 * u, 50 * u, "Over het model", PANEL, new Runnable() { public void run() { go(INFO); } });
        }
    }

    // ================================================================ CAR

    void carScreen(Gfx g) {
        final Car c = car;
        float top = headerHeight(c);
        beginScroll(g, top, H - 84 * u);
        float y = 14 * u;
        // key figures
        panel(g, X(20), y, 360 * u, 96 * u, 14 * u);
        String[][] big = {{pk(c.kw), "pk"}, {f0(c.nm), "Nm"}, {f0(c.mass), "kg"}};
        for (int i = 0; i < 3; i++) {
            float cx = X(80 + i * 120);
            g.color(TEXT); g.text(big[i][0], cx, y + 48 * u, 26 * u, 0, Gfx.DIGIT);
            g.color(MUTED); g.text(big[i][1], cx, y + 72 * u, 12.5f * u, 0, Gfx.SANS);
        }
        y += 112 * u;
        kv(g, X(24), y, 352 * u, "Vermogen", f0(c.kw) + " kW bij " + f0(c.powerRpm) + " tpm"); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Koppel", f0(c.nm) + " Nm bij " + f0(c.torqueRpm) + " tpm"); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Motor", c.engineText() + (c.turbo ? ", turbo/compressor" : "")); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Aandrijving", (c.drive == Car.FWD ? "voorwielen" : c.drive == Car.AWD ? "vierwiel" : "achterwielen") + (c.automatic ? ", automaat" : c.dct ? ", DCT" : "")); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Banden", c.tyre() + "  (omtrek " + f0(c.circumference()) + " mm)"); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Luchtweerstand", "cw " + f2(c.cw) + " × " + f2(c.frontalArea()) + " m²"); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Fabriek: 0-100 / top", (c.pub0100 > 0 ? f1(c.pub0100) + " s" : "-") + " / " + f0(c.pubVmax) + " km/u"); y += 14 * u;
        g.color(FAINT);
        g.text("bron: " + c.source + (c.estimated ? " · deels geschat" : ""), X(24), y + 10 * u, 11 * u, -1, Gfx.SANS);
        y += 30 * u;

        // gears table (theoretical data, as in the 1993 program)
        section(g, "Versnellingen", y + 6 * u);
        y += 18 * u;
        Sim s = simFor(c);
        panel(g, X(20), y, 360 * u, 34 * u + (c.gears.length + 1) * 24 * u, 14 * u);
        float ty = y + 24 * u;
        g.color(MUTED);
        g.text("versn.", X(34), ty, 12 * u, -1, Gfx.SANS);
        g.text("overbr.", X(140), ty, 12 * u, 1, Gfx.SANS);
        g.text("max km/u", X(250), ty, 12 * u, 1, Gfx.SANS);
        g.text("schakelt bij", X(366), ty, 12 * u, 1, Gfx.SANS);
        for (int i = 0; i < c.gears.length; i++) {
            ty += 24 * u;
            g.color(TEXT);
            g.text((i + 1) + "e", X(34), ty, 14 * u, -1, Gfx.BOLD);
            g.text(f3(c.gears[i]), X(140), ty, 14 * u, 1, Gfx.DIGIT);
            g.color(ACCENT);
            g.text(f0(c.kmh(c.maxRpm, c.gears[i])), X(250), ty, 14 * u, 1, Gfx.DIGIT);
            g.color(MUTED);
            g.text(i < c.gears.length - 1 ? f0(s.shiftRpm[i]) + " tpm" : "-", X(366), ty, 12.5f * u, 1, Gfx.SANS);
        }
        ty += 24 * u;
        g.color(TEXT);
        g.text("R", X(34), ty, 14 * u, -1, Gfx.BOLD);
        g.text(f3(c.reverse), X(140), ty, 14 * u, 1, Gfx.DIGIT);
        g.color(ACCENT);
        g.text(f0(c.kmh(c.maxRpm, c.reverse)), X(250), ty, 14 * u, 1, Gfx.DIGIT);
        y = ty + 22 * u;
        kv(g, X(24), y, 352 * u, "Differentieel", f3(c.diff)); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Max. toerental", c.maxRpm + " tpm"); y += 22 * u;
        kv(g, X(24), y, 352 * u, "Hoogste versn. bij 1000 tpm", f1(c.kmh(1000, c.gears[c.gears.length - 1])) + " km/u"); y += 26 * u;

        // shift diagram
        section(g, "Schakelschema", y + 6 * u);
        y += 18 * u;
        y += shiftDiagram(g, c, s, X(20), y, 360 * u, 230 * u) + 16 * u;

        // torque and power
        section(g, "Koppel en vermogen", y + 6 * u);
        y += 18 * u;
        y += curve(g, c, X(20), y, 360 * u, 150 * u, false) + 10 * u;
        y += curve(g, c, X(20), y, 360 * u, 150 * u, true) + 6 * u;
        y += para(g, "De kromme is berekend uit het maximale koppel en vermogen met hun toerentallen; echte koppeltabellen van deze auto's zijn zelden gepubliceerd.",
                X(20), y, 12 * u, Gfx.SANS, FAINT, 360 * u) + 20 * u;
        endScroll(g, y);

        g.linear(0, H - 84 * u, 0, H, 0xE0111418, 0xFF0A0C0F);
        g.fillRect(0, H - 84 * u, W, 84 * u);
        button(g, X(20), H - 70 * u, 230 * u, 56 * u, "Optrekken", PRIMARY, new Runnable() { public void run() { openSim(); } });
        button(g, X(262), H - 70 * u, 118 * u, 56 * u, "Vergelijk", PANEL, new Runnable() { public void run() { go(PICK); } });
        header(g, title(c.name), c.gears.length + " versnellingen · " + c.tyre(), host.logo(c.name));
    }

    /** Speed against revs per gear, with the shift path (the 1993 'schakeldiagram'). */
    float shiftDiagram(Gfx g, Car c, Sim s, float x, float y, float w, float h) {
        panel(g, x, y, w, h, 14 * u);
        float px = x + 44 * u, py = y + 30 * u, pw = w - 58 * u, ph = h - 62 * u;
        double vTop = 0;
        for (double gr : c.gears) vTop = Math.max(vTop, c.kmh(c.maxRpm, gr));
        vTop = Math.min(vTop, 400);
        double vStep = vTop > 250 ? 100 : 50;
        double vMax = Math.ceil(vTop / vStep) * vStep;
        g.color(0x22FFFFFF);
        for (double v = 0; v <= vMax; v += vStep) {
            float yy = py + ph - (float) (v / vMax) * ph;
            g.line(px, yy, px + pw, yy, u);
            g.color(MUTED); g.text(f0(v), px - 6 * u, yy + 4 * u, 11 * u, 1, Gfx.SANS); g.color(0x22FFFFFF);
        }
        for (int r = 1000; r <= c.maxRpm; r += 1000) {
            float xx = px + (float) r / c.maxRpm * pw;
            g.line(xx, py, xx, py + ph, u);
            g.color(MUTED); g.text("" + r / 1000, xx, py + ph + 15 * u, 11 * u, 0, Gfx.SANS); g.color(0x22FFFFFF);
        }
        g.color(MUTED);
        g.text("x1000 tpm", px + pw, py + ph + 30 * u, 11 * u, 1, Gfx.SANS);
        g.text("km/u", x + 12 * u, y + 18 * u, 11 * u, -1, Gfx.SANS);
        for (int i = 0; i < c.gears.length; i++) {
            float x1 = px + pw, y1 = py + ph - (float) (Math.min(c.kmh(c.maxRpm, c.gears[i]), vMax) / vMax) * ph;
            g.color(0x55FFFFFF);
            g.line(px, py + ph, x1, y1, 1.4f * u);
            g.color(MUTED);
            g.text("" + (i + 1), x1 + 4 * u, y1 + 4 * u, 11 * u, -1, Gfx.BOLD);
        }
        // the path a driver follows: up to the shift point, then down to the next gear
        float[] path = new float[c.gears.length * 4 + 2];
        int k = 0;
        path[k++] = px; path[k++] = py + ph;
        for (int i = 0; i < c.gears.length; i++) {
            double rs = i < c.gears.length - 1 ? s.shiftRpm[i] : c.maxRpm;
            double v = Math.min(c.kmh(rs, c.gears[i]), vMax);
            path[k++] = px + (float) (rs / c.maxRpm) * pw; path[k++] = py + ph - (float) (v / vMax) * ph;
            if (i < c.gears.length - 1) {
                double rn = c.rpmAt(v, c.gears[i + 1]);
                path[k++] = px + (float) (rn / c.maxRpm) * pw; path[k++] = py + ph - (float) (v / vMax) * ph;
            }
        }
        float[] pp = new float[k];
        System.arraycopy(path, 0, pp, 0, k);
        g.color(ACCENT);
        g.strokePoly(pp, false, 2.6f * u);
        return h;
    }

    float curve(Gfx g, Car c, float x, float y, float w, float h, boolean power) {
        panel(g, x, y, w, h, 14 * u);
        float px = x + 44 * u, py = y + 26 * u, pw = w - 58 * u, ph = h - 52 * u;
        double max = 0;
        for (double n = 800; n <= c.maxRpm; n += 50) max = Math.max(max, power ? c.torque.kw(n) : c.torque.at(n));
        double step = max > 400 ? 200 : max > 200 ? 100 : max > 80 ? 50 : 20;
        double top = Math.ceil(max / step) * step;
        g.color(power ? BLUE : ACCENT);
        g.text(power ? "Vermogen (kW)" : "Koppel (Nm)", x + 14 * u, y + 17 * u, 12.5f * u, -1, Gfx.BOLD);
        g.color(0x22FFFFFF);
        for (double v = 0; v <= top; v += step) {
            float yy = py + ph - (float) (v / top) * ph;
            g.line(px, yy, px + pw, yy, u);
            g.color(MUTED); g.text(f0(v), px - 6 * u, yy + 4 * u, 10.5f * u, 1, Gfx.SANS); g.color(0x22FFFFFF);
        }
        for (int r = 1000; r <= c.maxRpm; r += 1000) {
            float xx = px + (float) r / c.maxRpm * pw;
            g.color(MUTED); g.text("" + r / 1000, xx, py + ph + 14 * u, 10.5f * u, 0, Gfx.SANS);
        }
        int n = 60;
        float[] pts = new float[(n + 1) * 2];
        for (int i = 0; i <= n; i++) {
            double r = 800 + (c.maxRpm - 800) * i / (double) n;
            double v = power ? c.torque.kw(r) : c.torque.at(r);
            pts[2 * i] = px + (float) (r / c.maxRpm) * pw;
            pts[2 * i + 1] = py + ph - (float) (v / top) * ph;
        }
        g.color(power ? BLUE : ACCENT);
        g.strokePoly(pts, false, 2.4f * u);
        // the published peak
        double pr = power ? c.powerRpm : c.torqueRpm, pv = power ? c.kw : c.nm;
        float mx = px + (float) (pr / c.maxRpm) * pw, my = py + ph - (float) (pv / top) * ph;
        g.color(BG1); g.fillCircle(mx, my, 5 * u);
        g.color(TEXT); g.strokeCircle(mx, my, 4.5f * u, 2 * u);
        return h;
    }

    // ================================================================ SIM

    void advance(float dt) {
        if (!running) return;
        if (oldMode) {
            int steps = Math.max(1, Math.round(dt * 100 * speed));
            for (int i = 0; i < steps && !old.done; i++) {
                old.step();
                if (old.steps % 2 == 0) trace.add(new double[]{old.steps, old.v, old.tt, old.gear + 1, 0});
            }
            host.engine(old.tt, 1, car.soundCylinders(), sound && !old.done);
            if (old.done) stopRun();
            return;
        }
        double target = live.t + dt * speed;
        while (live.t < target) {
            live.step(0.002);
            if (live.t >= traceNext) { trace.add(new double[]{live.t, live.v * 3.6, live.rpm, live.gear + 1, live.x}); traceNext += 0.05; }
        }
        // keep going until the car is practically at its top speed (or hardly accelerates any more)
        double vTop = result != null ? result.vmax : 0;
        boolean done = live.v * 3.6 >= 0.995 * vTop || (live.t > 20 && !live.shifting && !live.slipping && live.a < 0.02) || live.t > 180;
        host.engine(live.rpm, live.shifting ? 0.1 : 1, car.soundCylinders(), sound && !done);
        if (done) stopRun();
    }

    void simScreen(Gfx g) {
        final Car c = car;
        float top = headerHeight(c);
        beginScroll(g, top, H);
        float y = 10 * u;
        // gauges
        double rpm = oldMode ? (old == null ? 0 : old.tt) : live == null ? 0 : live.rpm;
        double kmh = oldMode ? (old == null ? 0 : old.v) : live == null ? 0 : live.v * 3.6;
        int gearNo = oldMode ? (old == null ? 1 : old.gear + 1) : live == null ? 1 : live.gear + 1;
        double vScale = Math.max(160, Math.ceil((oldMode ? 1.1 * c.kmh(c.maxRpm, c.gears[c.gears.length - 1]) : resultFor(c).vmax * 1.08) / 40) * 40);
        vScale = Math.min(vScale, 480);
        gauge(g, X(102), y + 96 * u, 86 * u, rpm / 1000, Math.ceil(c.maxRpm / 1000.0) + 1, c.maxRpm / 1000.0, "x1000 tpm", 1);
        gauge(g, X(298), y + 96 * u, 86 * u, kmh, vScale, 0, "km/u", vScale > 300 ? 50 : 20);
        // gear and readouts
        g.color(0x66000000);
        g.fillRoundRect(X(176), y + 150 * u, 48 * u, 50 * u, 10 * u);
        g.color(ACCENT);
        g.text("" + gearNo, X(200), y + 188 * u, 32 * u, 0, Gfx.TITLE);
        g.color(TEXT);
        g.text(f0(kmh), X(298), y + 160 * u, 22 * u, 0, Gfx.TABULAR);
        g.text(f0(rpm), X(102), y + 160 * u, 22 * u, 0, Gfx.TABULAR);
        g.color(MUTED);
        // fixed anchors (time right-aligned, distance left-aligned) so the readout does not shift while counting
        if (oldMode) g.text((old == null ? "0" : old.steps) + " stappen", W / 2f, y + 222 * u, 14 * u, 0, Gfx.DIGIT);
        else {
            g.text(f2(live == null ? 0 : live.t) + " s", W / 2f - 10 * u, y + 222 * u, 14 * u, 1, Gfx.TABULAR);
            g.text(f0(live == null ? 0 : live.x) + " m", W / 2f + 10 * u, y + 222 * u, 14 * u, -1, Gfx.TABULAR);
        }
        if (!oldMode && live != null && (live.wheelspin || live.slipping || live.shifting)) {
            g.color(live.wheelspin ? RED : YELLOW);
            g.text(live.shifting ? "schakelen" : live.wheelspin ? "wielspin" : "koppeling slipt", W / 2f, y + 242 * u, 12 * u, 0, Gfx.BOLD);
        }
        y += 256 * u;

        // speed over time
        y += timeChart(g, X(20), y, 360 * u, 190 * u, trace, null, oldMode) + 12 * u;

        // controls
        button(g, X(20), y, 174 * u, 50 * u, running ? "Stop" : "Opnieuw", PRIMARY, new Runnable() { public void run() { if (running) stopRun(); else startRun(); } });
        button(g, X(206), y, 84 * u, 50 * u, "x" + (int) speed, GHOST, new Runnable() { public void run() { speed = speed == 1 ? 2 : speed == 2 ? 4 : 1; } });
        button(g, X(296), y, 84 * u, 50 * u, sound ? "geluid" : "stil", sound ? ON : GHOST, new Runnable() { public void run() { sound = !sound; if (!sound) host.engine(0, 0, car.soundCylinders(), false); } });
        y += 62 * u;
        button(g, X(20), y, 174 * u, 44 * u, redlineShift ? "Schakelen: max. tpm" : "Schakelen: optimaal", GHOST, new Runnable() { public void run() { redlineShift = !redlineShift; settingsChanged(); } });
        button(g, X(206), y, 174 * u, 44 * u, wet ? "Wegdek: nat" : "Wegdek: droog", GHOST, new Runnable() { public void run() { wet = !wet; settingsChanged(); } });
        y += 54 * u;
        button(g, X(20), y, 360 * u, 44 * u, oldMode ? "Terug naar het nieuwe model" : "Bekijk het oude algoritme (1993)", oldMode ? ON : GHOST, new Runnable() { public void run() { oldMode = !oldMode; startRun(); } });
        y += 58 * u;

        if (oldMode) {
            y += para(g, "Zo rekende het programma uit 1993: het toerental stijgt elke stap met een vaste hoeveelheid maal de overbrenging in het kwadraat, langs een sinusvormige 'koppelkromme'. Massa, vermogen en luchtweerstand spelen geen rol en er is geen echte tijd: alleen stappen. De snelheid per toerental klopt wel.",
                    X(20), y, 13 * u, Gfx.SANS, MUTED, 360 * u) + 20 * u;
        } else {
            results(g, c, resultFor(c), y);
            y += 330 * u;
        }
        endScroll(g, y);
        header(g, title(c.name), oldMode ? "oud algoritme (1993)" : "optrekken vanuit stilstand", host.logo(c.name));
    }

    void results(Gfx g, Car c, Sim.Result r, float y) {
        section(g, "Resultaat", y + 6 * u);
        y += 18 * u;
        panel(g, X(20), y, 360 * u, 300 * u, 14 * u);
        float ty = y + 28 * u;
        String[][] rows = {
                {"0-50 km/u", t(r.t50)}, {"0-100 km/u", t(r.t100) + (c.pub0100 > 0 ? "   (fabriek " + f1(c.pub0100) + ")" : "")}, {"0-160 km/u", t(r.t160)},
                {"0-200 km/u", t(r.t200)}, {"400 m", r.t400 < 0 ? "-" : t(r.t400) + " · " + f0(r.v400) + " km/u"},
                {"1000 m", r.t1000 < 0 ? "-" : t(r.t1000) + " · " + f0(r.v1000) + " km/u"},
                {"Topsnelheid", f0(r.vmax) + " km/u   (fabriek " + f0(c.pubVmax) + ")"},
                {"Wegrijden bij", f0(r.launchRpm) + " tpm"}};
        for (String[] row : rows) {
            kv(g, X(34), ty, 332 * u, row[0], row[1]);
            ty += 31 * u;
        }
        g.color(FAINT);
        g.text("met bestuurder (+75 kg), " + (wet ? "nat" : "droog") + " wegdek, windstil", X(34), ty, 11 * u, -1, Gfx.SANS);
    }

    static String t(double s) { return s < 0 ? "-" : f1(s) + " s"; }

    double t100Key(Car c) { double v = resultFor(c).t100; return v < 0 ? 1e9 : v; }

    void gauge(Gfx g, float cx, float cy, float r, double value, double max, double red, String unit, double step) {
        g.radial(cx, cy, r * 1.1f, 0xFF262B33, 0xFF101317);
        g.fillCircle(cx, cy, r);
        g.color(0x40FFFFFF);
        g.strokeCircle(cx, cy, r, 2 * u);
        float a0 = 135, sweep = 270;
        if (red > 0) {
            g.color(0xAAE5483B);
            g.arc(cx, cy, r * 0.86f, a0 + (float) (red / max) * sweep, (float) ((max - red) / max) * sweep, 7 * u);
        }
        for (double v = 0; v <= max + 1e-6; v += step) {
            double ang = Math.toRadians(a0 + v / max * sweep);
            float ca = (float) Math.cos(ang), sa = (float) Math.sin(ang);
            g.color(TEXT);
            g.line(cx + ca * r * 0.78f, cy + sa * r * 0.78f, cx + ca * r * 0.92f, cy + sa * r * 0.92f, 2 * u);
            boolean label = step >= 20 ? Math.round(v) % (step * 2) == 0 : true;
            if (label) {
                g.color(MUTED);
                g.text(f0(v), cx + ca * r * 0.62f, cy + sa * r * 0.62f + 4 * u, 10.5f * u, 0, Gfx.SANS);
            }
        }
        g.color(MUTED);
        g.text(unit, cx, cy + r * 0.42f, 10 * u, 0, Gfx.SANS);
        double ang = Math.toRadians(a0 + Math.min(value, max) / max * sweep);
        g.color(ACCENT);
        g.line(cx, cy, cx + (float) Math.cos(ang) * r * 0.86f, cy + (float) Math.sin(ang) * r * 0.86f, 3.2f * u);
        g.color(0xFF3A414A);
        g.fillCircle(cx, cy, 7 * u);
    }

    /** Speed against time; optional second trace for comparisons. */
    float timeChart(Gfx g, float x, float y, float w, float h, List<double[]> a, List<double[]> b, boolean steps) {
        panel(g, x, y, w, h, 14 * u);
        float px = x + 42 * u, py = y + 30 * u, pw = w - 56 * u, ph = h - 60 * u;
        double tMax = 10, vMax = 100;
        for (List<double[]> l : new List[]{a, b}) if (l != null) for (double[] p : l) { tMax = Math.max(tMax, p[0]); vMax = Math.max(vMax, p[1]); }
        double tStep = steps ? (tMax > 1000 ? 500 : 100) : tMax > 150 ? 30 : tMax > 80 ? 20 : tMax > 40 ? 10 : 5;
        tMax = Math.ceil(tMax / tStep) * tStep;
        double vStep = vMax > 250 ? 100 : 50;
        vMax = Math.ceil(vMax / vStep) * vStep;
        g.color(0x22FFFFFF);
        for (double v = 0; v <= vMax; v += vStep) {
            float yy = py + ph - (float) (v / vMax) * ph;
            g.line(px, yy, px + pw, yy, u);
            g.color(MUTED); g.text(f0(v), px - 6 * u, yy + 4 * u, 10.5f * u, 1, Gfx.SANS); g.color(0x22FFFFFF);
        }
        for (double t = 0; t <= tMax; t += tStep) {
            float xx = px + (float) (t / tMax) * pw;
            g.color(MUTED); g.text(f0(t), xx, py + ph + 14 * u, 10.5f * u, 0, Gfx.SANS);
        }
        g.color(MUTED);
        g.text(steps ? "stappen" : "seconden", px + pw, py + ph + 28 * u, 10.5f * u, 1, Gfx.SANS);
        g.text("km/u", x + 12 * u, y + 18 * u, 10.5f * u, -1, Gfx.SANS);
        // 100 km/h line
        if (vMax >= 100) {
            float yy = py + ph - (float) (100 / vMax) * ph;
            g.color(0x40FFD34A);
            g.line(px, yy, px + pw, yy, 1.2f * u);
        }
        List<double[]>[] ls = new List[]{b, a};
        int[] cols = {SERIES[1], SERIES[0]};
        for (int s = 0; s < 2; s++) {
            List<double[]> l = ls[s];
            if (l == null || l.size() < 2) continue;
            int n = l.size(), stride = Math.max(1, n / 300);
            float[] pts = new float[((n - 1) / stride + 1) * 2];
            int k = 0;
            for (int i = 0; i < n; i += stride) {
                double[] p = l.get(i);
                pts[k++] = px + (float) (p[0] / tMax) * pw;
                pts[k++] = py + ph - (float) (p[1] / vMax) * ph;
            }
            float[] q = new float[k];
            System.arraycopy(pts, 0, q, 0, k);
            g.color(cols[s]);
            g.strokePoly(q, false, 2.4f * u);
        }
        return h;
    }

    // ================================================================ COMPARE

    // ================================================================ BRAND LOGOS

    /** One brand from assets/merken/merken.json (made by tools/alle_logos.py). */
    static final class Brand {
        final String name, file, source;
        Brand(String name, String file, String source) { this.name = name; this.file = file; this.source = source; }
    }

    List<Brand> brandList;
    int brandIndex;

    List<Brand> brandList() {
        if (brandList == null) {
            brandList = new ArrayList<>();
            String json = host.readAsset("merken/merken.json");
            if (json != null) {
                // small, fixed format: {"naam": "...", "bestand": "...", "bron": "..."} per brand
                java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                        "\\{\\s*\"naam\":\\s*\"((?:[^\"\\\\]|\\\\.)*)\",\\s*\"bestand\":\\s*\"([^\"]*)\",\\s*\"bron\":\\s*\"([^\"]*)\"\\s*\\}").matcher(json);
                while (m.find()) brandList.add(new Brand(unescape(m.group(1)), m.group(2), m.group(3)));
            }
        }
        return brandList;
    }

    static String unescape(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                if (n == 'u' && i + 4 < s.length()) { b.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16)); i += 4; }
                else b.append(n == 'n' ? '\n' : n);
            } else b.append(c);
        }
        return b.toString();
    }

    void stepBrand(int d) {
        int n = brandList().size();
        if (n > 0) brandIndex = (brandIndex + d + n) % n;
    }

    /** Grid of all styled brand logos, three per row. */
    void brands(Gfx g) {
        final List<Brand> list = brandList();
        float top = header(g, "Merklogo's", list.isEmpty() ? "nog geen logo's" : list.size() + " merken, oranje op zwart");
        beginScroll(g, top, H);
        float y = 14 * u;
        if (list.isEmpty()) {
            y += para(g, "Er zijn nog geen merklogo's in de app. Draai op je computer python3 tools/alle_logos.py "
                    + "en bouw de app daarna opnieuw met ./build.sh; zie de README.", X(20), y, 14 * u, Gfx.SANS, MUTED, 360 * u);
            endScroll(g, y + 20 * u);
            header(g, "Merklogo's", "nog geen logo's");
            return;
        }
        float tile = 112 * u, gap = 12 * u, rowH = tile + 34 * u;
        float viewTop = scroll[screen] - rowH, viewBottom = scroll[screen] + (H - top) + rowH;
        for (int i = 0; i < list.size(); i++) {
            float x = X(20) + (i % 3) * (tile + gap), ty = y + (i / 3) * rowH;
            if (ty + rowH < viewTop || ty > viewBottom) continue;
            final int k = i;
            Brand b = list.get(i);
            boolean pr = isPressed(x, ty, tile, tile);
            g.color(pr ? 0xFF2A2F36 : 0xFF000000);
            g.fillRoundRect(x, ty, tile, tile, 14 * u);
            Object img = host.image("merken/" + b.file);
            if (img != null) g.drawLayer(img, x + 6 * u, ty + 6 * u, tile - 12 * u, tile - 12 * u);
            g.color(0x30FFFFFF);
            g.strokeRoundRect(x, ty, tile, tile, 14 * u, 1 * u);
            float fs = 12 * u;
            if (g.textWidth(b.name, fs, Gfx.SANS) > tile) fs *= tile / g.textWidth(b.name, fs, Gfx.SANS);
            g.color(TEXT);
            g.text(b.name, x + tile / 2, ty + tile + 18 * u, fs, 0, Gfx.SANS);
            hit(x, ty, tile, tile + 24 * u, new Runnable() { public void run() { brandIndex = k; go(BRAND); } });
        }
        y += ((list.size() + 2) / 3) * rowH;
        endScroll(g, y + 10 * u);
        header(g, "Merklogo's", list.size() + " merken, oranje op zwart");
    }

    /** One logo, large, with the brand name; browse with the arrows or by swiping. */
    void brand(Gfx g) {
        List<Brand> list = brandList();
        if (list.isEmpty()) { back(); return; }
        Brand b = list.get(brandIndex);
        float top = header(g, "Merklogo's", (brandIndex + 1) + " van " + list.size());
        float size = Math.min(320 * u, H - top - 230 * u), x = W / 2f - size / 2, y = top + 40 * u;
        g.color(0xFF000000);
        g.fillRoundRect(x, y, size, size, 28 * u);
        g.color(0x30FFFFFF);
        g.strokeRoundRect(x, y, size, size, 28 * u, 1.2f * u);
        Object img = host.image("merken/" + b.file);
        if (img != null) g.drawLayer(img, x + 12 * u, y + 12 * u, size - 24 * u, size - 24 * u);
        float ty = y + size + 52 * u, fs = 26 * u;
        if (g.textWidth(b.name, fs, Gfx.TITLE) > 360 * u) fs *= 360 * u / g.textWidth(b.name, fs, Gfx.TITLE);
        g.color(TEXT);
        g.text(b.name, W / 2f, ty, fs, 0, Gfx.TITLE);
        g.color(FAINT);
        g.text("bron: " + b.source, W / 2f, ty + 26 * u, 12 * u, 0, Gfx.SANS);
        float by = H - 78 * u;
        button(g, X(20), by, 174 * u, 56 * u, "Vorige", PANEL, new Runnable() { public void run() { stepBrand(-1); } });
        button(g, X(206), by, 174 * u, 56 * u, "Volgende", PANEL, new Runnable() { public void run() { stepBrand(1); } });
    }

    void compare(Gfx g) {
        final Car a = car, b = other;
        float top = 64 * u;
        beginScroll(g, top, H);
        float y = 14 * u;
        Sim.Result ra = resultFor(a), rb = resultFor(b);
        // legend
        for (int i = 0; i < 2; i++) {
            Car c = i == 0 ? a : b;
            g.color(SERIES[i]);
            g.fillRoundRect(X(24), y + 6 * u + i * 24 * u, 22 * u, 6 * u, 3 * u);
            g.color(TEXT);
            g.text(title(c.name), X(54), y + 13 * u + i * 24 * u, 14 * u, -1, Gfx.BOLD);
        }
        y += 54 * u;
        double lim = Math.min(60, Math.max(ra.t1000, rb.t1000) + 2);
        y += timeChart(g, X(20), y, 360 * u, 220 * u, upTo(ra.trace, lim), upTo(rb.trace, lim), false) + 14 * u;
        panel(g, X(20), y, 360 * u, 330 * u, 14 * u);
        float ty = y + 28 * u;
        g.color(SERIES[0]); g.text("A", X(250), ty, 13 * u, 1, Gfx.TITLE);
        g.color(SERIES[1]); g.text("B", X(352), ty, 13 * u, 1, Gfx.TITLE);
        String[] lab = {"0-100 km/u", "0-160 km/u", "0-200 km/u", "400 m", "1000 m", "Topsnelheid", "Vermogen", "Gewicht", "kg per pk"};
        String[][] val = {
                {t(ra.t100), t(rb.t100)}, {t(ra.t160), t(rb.t160)}, {t(ra.t200), t(rb.t200)}, {t(ra.t400), t(rb.t400)}, {t(ra.t1000), t(rb.t1000)},
                {f0(ra.vmax) + " km/u", f0(rb.vmax) + " km/u"}, {pk(a.kw) + " pk", pk(b.kw) + " pk"}, {f0(a.mass) + " kg", f0(b.mass) + " kg"},
                {f1(a.mass / (a.kw * 1.35962)), f1(b.mass / (b.kw * 1.35962))}};
        for (int i = 0; i < lab.length; i++) {
            ty += 31 * u;
            g.color(MUTED); g.text(lab[i], X(34), ty, 13.5f * u, -1, Gfx.SANS);
            g.color(TEXT);
            g.text(val[i][0], X(250), ty, 13.5f * u, 1, Gfx.BOLD);
            g.text(val[i][1], X(352), ty, 13.5f * u, 1, Gfx.BOLD);
        }
        y += 346 * u;
        button(g, X(20), y, 360 * u, 48 * u, "Andere auto kiezen", GHOST, new Runnable() { public void run() { screen = PICK; scroll[PICK] = 0; } });
        y += 64 * u;
        endScroll(g, y);
        header(g, "Vergelijken", "simulatie, zelfde omstandigheden");
    }

    static List<double[]> upTo(List<double[]> l, double tMax) {
        List<double[]> r = new ArrayList<>();
        for (double[] p : l) if (p[0] <= tMax) r.add(p);
        return r;
    }

    // ================================================================ PHYSICS (the 1993 'natuurkundige gegevens')

    void slider(Gfx g, float y, String label, String value, double[] arr, int idx, double min, double max, double step) {
        g.color(MUTED); g.text(label, X(24), y, 13 * u, -1, Gfx.SANS);
        g.color(TEXT); g.text(value, X(376), y, 13.5f * u, 1, Gfx.BOLD);
        Slider s = new Slider();
        s.x = X(30); s.y = y + 6 * u + offY; s.w = 340 * u; s.h = 30 * u;
        s.arr = arr; s.idx = idx; s.min = min; s.max = max; s.step = step;
        if (s.y + s.h > clipTop && s.y < clipBottom) sliders.add(s);
        float ty = y + 21 * u, f = (float) ((arr[idx] - min) / (max - min));
        g.color(0x30FFFFFF); g.fillRoundRect(X(30), ty - 3 * u, 340 * u, 6 * u, 3 * u);
        g.color(ACCENT); g.fillRoundRect(X(30), ty - 3 * u, 340 * u * f, 6 * u, 3 * u);
        g.color(TEXT); g.fillCircle(X(30) + 340 * u * f, ty, 10 * u);
    }

    void phys(Gfx g) {
        float top = 64 * u;
        beginScroll(g, top, H);
        float y = 14 * u;
        String[] tabs = {"Rijweerstand", "Remweg", "Bochtsnelheid"};
        for (int i = 0; i < 3; i++) {
            final int k = i;
            button(g, X(20 + i * 122), y, 114 * u, 40 * u, tabs[i], physTab == i ? ON : GHOST, new Runnable() { public void run() { physTab = k; } });
        }
        y += 68 * u;
        if (physTab == 0) {
            final Car c = car;
            button(g, X(20), y, 360 * u, 40 * u, "Waarden van de " + title(c.name) + " overnemen", GHOST, new Runnable() {
                public void run() { drag[2] = c.width; drag[3] = c.height; drag[4] = c.cw; drag[6] = c.mass + 75; }
            });
            y += 54 * u;
            slider(g, y, "Rijsnelheid", f0(drag[0]) + " km/u", drag, 0, 0, 300, 5); y += 50 * u;
            slider(g, y, "Wind (+ tegen, - mee)", f0(drag[1]) + " km/u", drag, 1, -50, 50, 5); y += 50 * u;
            slider(g, y, "Breedte", f2(drag[2]) + " m", drag, 2, 1.4, 2.1, 0.01); y += 50 * u;
            slider(g, y, "Hoogte", f2(drag[3]) + " m", drag, 3, 1.0, 1.9, 0.01); y += 50 * u;
            slider(g, y, "cw-waarde", f2(drag[4]), drag, 4, 0.20, 0.60, 0.01); y += 50 * u;
            slider(g, y, "Rolweerstand", f3(drag[5]), drag, 5, 0.008, 0.030, 0.001); y += 50 * u;
            slider(g, y, "Massa", f0(drag[6]) + " kg", drag, 6, 500, 2600, 10); y += 50 * u;
            slider(g, y, "Helling", f1(drag[7]) + "°", drag, 7, -10, 15, 0.5); y += 56 * u;
            double v = drag[0] / 3.6, vw = drag[1] / 3.6, area = 0.84 * drag[2] * drag[3];
            double fl = 0.5 * Sim.RHO * drag[4] * area * (v + vw) * Math.abs(v + vw);
            double fr = drag[5] * drag[6] * Sim.G * Math.cos(Math.toRadians(drag[7]));
            double fh = drag[6] * Sim.G * Math.sin(Math.toRadians(drag[7]));
            double fw = (fl + fr + fh) / 0.85;
            panel(g, X(20), y, 360 * u, 190 * u, 14 * u);
            float ty = y + 30 * u;
            double[][] vals = {{fl}, {fr}, {fh}, {fw}};
            String[] names = {"Luchtweerstand", "Rolweerstand", "Hellingweerstand", "Aan de motor"};
            for (int i = 0; i < 4; i++) {
                double f = vals[i][0], p = f * v / 1000;
                g.color(i == 3 ? ACCENT : MUTED); g.text(names[i], X(34), ty, 13.5f * u, -1, i == 3 ? Gfx.BOLD : Gfx.SANS);
                g.color(TEXT); g.text(f0(f) + " N", X(250), ty, 13.5f * u, 1, Gfx.BOLD);
                g.text(f1(p) + " kW · " + pk(p) + " pk", X(366), ty, 12.5f * u, 1, Gfx.SANS);
                ty += 38 * u;
            }
            g.color(FAINT);
            g.text("motor = som ÷ 0,85 (rendement aandrijving); frontaal opp. " + f2(area) + " m²", X(34), ty - 6 * u, 11 * u, -1, Gfx.SANS);
            y += 206 * u;
        } else if (physTab == 1) {
            slider(g, y, "Rijsnelheid", f0(brake[0]) + " km/u", brake, 0, 10, 250, 5); y += 50 * u;
            slider(g, y, "Reactietijd", f1(brake[1]) + " s", brake, 1, 0.3, 2.5, 0.1); y += 50 * u;
            slider(g, y, "Wrijvingscoëfficiënt", f2(brake[2]), brake, 2, 0.1, 1.0, 0.05); y += 56 * u;
            y = surfaces(g, y, brake, 2);
            double v = brake[0] / 3.6, a = brake[2] * Sim.G, re = v * brake[1], rw = v * v / (2 * a);
            panel(g, X(20), y, 360 * u, 160 * u, 14 * u);
            float ty = y + 32 * u;
            kv(g, X(34), ty, 332 * u, "Reactieweg", f1(re) + " m"); ty += 32 * u;
            kv(g, X(34), ty, 332 * u, "Remweg", f1(rw) + " m"); ty += 32 * u;
            g.color(ACCENT); g.text("Totale stopafstand", X(34), ty, 14 * u, -1, Gfx.BOLD);
            g.text(f1(re + rw) + " m", X(366), ty, 16 * u, 1, Gfx.DIGIT); ty += 32 * u;
            kv(g, X(34), ty, 332 * u, "Vertraging", f2(a) + " m/s²");
            y += 176 * u;
        } else {
            slider(g, y, "Straal van de bocht", f0(corner[0]) + " m", corner, 0, 10, 500, 5); y += 50 * u;
            slider(g, y, "Wrijvingscoëfficiënt", f2(corner[1]), corner, 1, 0.1, 1.0, 0.05); y += 56 * u;
            y = surfaces(g, y, corner, 1);
            double v = Math.sqrt(corner[1] * Sim.G * corner[0]);
            panel(g, X(20), y, 360 * u, 120 * u, 14 * u);
            g.color(MUTED);
            g.text("Snelheid waarbij de auto gaat slippen", X(34), y + 30 * u, 13 * u, -1, Gfx.SANS);
            g.color(ACCENT);
            g.text(f0(v * 3.6) + " km/u", X(34), y + 72 * u, 30 * u, -1, Gfx.DIGIT);
            g.color(MUTED);
            g.text(f1(v) + " m/s  ·  v = √(µ · g · r)", X(34), y + 100 * u, 12.5f * u, -1, Gfx.SANS);
            y += 136 * u;
        }
        endScroll(g, y + 20 * u);
        header(g, "Natuurkunde", tabs[physTab]);
    }

    static final String[][] SURF = {{"Beton, droog", "0.85", "0.95"}, {"Asfalt, droog", "0.80", "0.90"}, {"Zandweg", "0.50", "0.50"},
            {"Nat, 1 mm water", "0.55", "0.40"}, {"Nat, 2 mm water", "0.45", "0.30"}, {"IJs", "0.10", "0.10"}};

    /** The friction table from the 1993 program; tap a value to use it. */
    float surfaces(Gfx g, float y, final double[] arr, final int idx) {
        panel(g, X(20), y, 360 * u, 40 * u + SURF.length * 34 * u, 14 * u);
        g.color(MUTED);
        g.text("wegdek", X(34), y + 26 * u, 12 * u, -1, Gfx.SANS);
        g.text("nieuwe band", X(270), y + 26 * u, 12 * u, 1, Gfx.SANS);
        g.text("oude band", X(366), y + 26 * u, 12 * u, 1, Gfx.SANS);
        float ty = y + 40 * u;
        for (String[] s : SURF) {
            g.color(TEXT); g.text(s[0], X(34), ty + 22 * u, 13.5f * u, -1, Gfx.SANS);
            for (int k = 1; k <= 2; k++) {
                final double val = Double.parseDouble(s[k]);
                boolean on = Math.abs(arr[idx] - val) < 0.001;
                button(g, X(k == 1 ? 214 : 310), ty + 2 * u, 58 * u, 28 * u, s[k].replace('.', ','), on ? ON : GHOST, new Runnable() { public void run() { arr[idx] = val; } });
            }
            ty += 34 * u;
        }
        return ty + 16 * u;
    }

    // ================================================================ INFO

    static final String[][] INFO_TEXT = {
            {"Wat er klopte", "De formules voor de snelheid per versnelling waren goed: toerental gedeeld door de overbrenging en het differentieel, maal de omtrek van het wiel. Ook de omtrek zelf (velgmaat plus twee keer de bandhoogte) en het schakelschema kloppen. Die berekeningen zitten ongewijzigd in deze app."},
            {"Wat er niet klopte", "Het optrekken was geen natuurkunde. Het toerental steeg per stap met een vaste hoeveelheid maal de overbrenging in het kwadraat, langs een sinusvorm. Massa, vermogen, koppel en luchtweerstand deden niet mee, en er was geen echte tijd. Daardoor kon een Citroën 2CV sneller lijken dan een Corvette, en reikte de topsnelheid tot het maximale toerental in de hoogste versnelling: 507 km/u voor de Corvette."},
            {"Het nieuwe model", "Elke 2 milliseconden rekent de app de trekkracht uit: het koppel bij het huidige toerental maal de overbrenging en het differentieel, gedeeld door de straal van het wiel. Daar gaan rolweerstand en luchtweerstand (½ · ρ · cw · A · v²) af. De versnelling is de overgebleven kracht gedeeld door de massa, plus een toeslag voor de draaiende delen van motor en aandrijving."},
            {"Wegrijden en schakelen", "Bij het wegrijden slipt de koppeling 1,5 seconde; de app zoekt het wegrijtoerental met de snelste tijd, zoals een testrijder doet. De banden kunnen niet meer kracht kwijt dan de grip toelaat; bij optrekken schuift gewicht naar achteren, wat achterwielaandrijving helpt. Er wordt geschakeld zodra de volgende versnelling meer trekkracht geeft, en elke schakeling kost 0,4 seconde zonder aandrijving. Automaten hebben een koppelomvormer die bij lage snelheid het koppel vergroot."},
            {"Koppelkrommes", "Volledige koppeltabellen van deze auto's zijn zelden gepubliceerd. De app bouwt de kromme uit het maximale koppel en vermogen met hun toerentallen, met een vlakker verloop voor turbo- en compressormotoren. Die gegevens komen uit fabrieksopgaven en naslagwerken zoals Encycarpedia en Wikipedia; bij een paar auto's (Bentley, Pantera GTS, Donkervoort, Thunderbird) zijn ze deels geschat."},
            {"Hoe goed is het?", "Vier constanten (koppelingstijd, schakeltijd, draaiende massa en bandengrip) zijn één keer afgesteld voor alle auto's samen. Daarna wijkt de berekende 0-100 km/u gemiddeld ongeveer 8% af van de fabrieksopgave, en de topsnelheid ongeveer 6%. Fabriekscijfers zijn zelf ook vaak wat optimistisch."},
            {"Lettertype", "Orbitron, vrij te gebruiken onder de SIL Open Font License."},
    };

    void info(Gfx g) {
        float top = 64 * u;
        beginScroll(g, top, H);
        float y = 8 * u;
        for (String[] s : INFO_TEXT) {
            g.color(ACCENT);
            g.text(s[0], X(20), y + 24 * u, 15 * u, -1, Gfx.TITLE);
            y += 34 * u;
            y += para(g, s[1], X(20), y, 14 * u, Gfx.SANS, TEXT, 360 * u) + 14 * u;
        }
        endScroll(g, y + 20 * u);
        header(g, "Over het model", "oud en nieuw vergeleken");
    }
}
