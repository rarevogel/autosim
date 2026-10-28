package nl.rarevogel.autosim.core;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
public class Shot {
    public static void main(String[] a) throws Exception {
        String out = a[0]; int W = Integer.parseInt(a[1]), H = Integer.parseInt(a[2]);
        String[] p = a[3].split(":");
        App app = new App(new App.Host() { public Object logo(String n) { return TestLogos.get(n); } public boolean logosMono() { return TestLogos.mono(); } public String readAsset(String p) { return TestLogos.text(p); } public Object image(String p) { return TestLogos.image(p); } public void quit() {} public void engine(double r, double l, int c, boolean on) {} });
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = img.createGraphics();
        J2dGfx g = new J2dGfx(g2);
        float k = W / 400f;
        switch (p[0]) {
            case "garage": if (p.length > 1) app.sortMode = Integer.parseInt(p[1]); for (int i = 2; i < p.length; i++) app.openDecades.add(Integer.parseInt(p[i])); break;
            case "car": app.car = Car.ALL.get(Integer.parseInt(p[1])); app.go(App.CAR); if (p.length > 2) app.scroll[App.CAR] = Float.parseFloat(p[2]) * k; break;
            case "sim": {
                app.car = Car.ALL.get(Integer.parseInt(p[1])); app.go(App.CAR);
                if (p.length > 3) app.oldMode = true;
                app.openSim();
                float t = Float.parseFloat(p[2]);
                for (float s = 0; s < t; s += 0.05f) { app.frame(g, W, H, 0.05f); g2.setTransform(new java.awt.geom.AffineTransform()); g2.setClip(null); }
                if (p.length > 4) app.scroll[App.SIM] = Float.parseFloat(p[4]) * k;
                break;
            }
            case "compare": app.car = Car.ALL.get(Integer.parseInt(p[1])); app.other = Car.ALL.get(Integer.parseInt(p[2])); app.go(App.CAR); app.go(App.COMPARE); break;
            case "phys": app.go(App.PHYS); app.physTab = Integer.parseInt(p[1]); break;
            case "info": app.go(App.INFO); break;
            case "merken": app.go(App.BRANDS); if (p.length > 1) app.scroll[App.BRANDS] = Float.parseFloat(p[1]) * k; break;
            case "merk": app.go(App.BRANDS); app.brandIndex = Integer.parseInt(p[1]); app.go(App.BRAND); break;
        }
        app.frame(g, W, H, 0f);
        g2.setTransform(new java.awt.geom.AffineTransform()); g2.setClip(null);
        app.frame(g, W, H, 0f);
        ImageIO.write(img, "png", new File(out));
    }
}
