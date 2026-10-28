package nl.rarevogel.autosim.core;
import java.awt.image.BufferedImage;
import java.util.*;
public class Monkey {
    public static void main(String[] a) {
        App app = new App(new App.Host() { public Object logo(String n) { return TestLogos.get(n); } public boolean logosMono() { return TestLogos.mono(); } public String readAsset(String p) { return TestLogos.text(p); } public Object image(String p) { return TestLogos.image(p); } public void quit() {} public void engine(double r, double l, int c, boolean on) {} });
        int W = 720, H = 1520;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        J2dGfx g = new J2dGfx(g2);
        Random r = new Random(3);
        int[] seen = new int[10];
        for (int i = 0; i < Integer.parseInt(a[0]); i++) {
            g2.setTransform(new java.awt.geom.AffineTransform()); g2.setClip(null);
            app.frame(g, W, H, 0.05f);
            seen[app.screen]++;
            int act = r.nextInt(100);
            if (act < 75) { float x = r.nextFloat() * W, y = r.nextFloat() * H; app.down(x, y); app.up(x, y); }
            else if (act < 92) { float x = r.nextFloat() * W, y = r.nextFloat() * H, dx = (r.nextFloat() - 0.5f) * W, dy = (r.nextFloat() - 0.5f) * H; app.down(x, y); for (int k = 1; k <= 5; k++) app.move(x + dx * k / 5, y + dy * k / 5); app.up(x + dx, y + dy); }
            else if (!app.back()) app.screen = App.GARAGE;
        }
        System.out.println("ok " + Arrays.toString(seen));
    }
}
