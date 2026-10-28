package nl.rarevogel.autosim.core;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;

/** Java2D Gfx for desktop screenshots. Rune characters always use the rune font. */
public class J2dGfx implements Gfx {
    static Font sans, bold, title, rune, tab;
    static {
        try {
            title = Font.createFont(Font.TRUETYPE_FONT, new File("assets/orbitron-bold.ttf"));
            rune = Font.createFont(Font.TRUETYPE_FONT, new File("assets/orbitron.ttf"));
            tab = Font.createFont(Font.TRUETYPE_FONT, new File("assets/autosim-digits.ttf"));
            sans = new Font("DejaVu Sans", Font.PLAIN, 10);
            bold = new Font("DejaVu Sans", Font.BOLD, 10);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    Graphics2D g; Paint paint = Color.BLACK;
    final ArrayDeque<AffineTransform> stack = new ArrayDeque<>();
    final ArrayDeque<Shape> clips = new ArrayDeque<>();
    public J2dGfx(Graphics2D g) {
        this.g = g;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }
    static Color c(int argb) { return new Color(argb, true); }
    public void save() { stack.push(g.getTransform()); clips.push(g.getClip() == null ? new Rectangle(-100000, -100000, 200000, 200000) : g.getClip()); }
    public void restore() { g.setTransform(stack.pop()); Shape s = clips.pop(); g.setClip(s); }
    public void translate(float x, float y) { g.translate(x, y); }
    public void rotate(float deg) { g.rotate(Math.toRadians(deg)); }
    public void clip(float x, float y, float w, float h) { g.clip(new Rectangle2D.Float(x, y, w, h)); }
    public void color(int argb) { paint = c(argb); }
    public void linear(float x0, float y0, float x1, float y1, int c0, int c1) { if (x0 == x1 && y0 == y1) y1 += 0.01f; paint = new GradientPaint(x0, y0, c(c0), x1, y1, c(c1)); }
    public void radial(float cx, float cy, float r, int c0, int c1) { paint = new RadialGradientPaint(cx, cy, Math.max(0.01f, r), new float[]{0, 1}, new Color[]{c(c0), c(c1)}); }
    void fill(Shape s) { g.setPaint(paint); g.fill(s); }
    void stroke(Shape s, float w) { g.setPaint(paint); g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g.draw(s); }
    public void fillRect(float x, float y, float w, float h) { fill(new Rectangle2D.Float(x, y, w, h)); }
    public void fillRoundRect(float x, float y, float w, float h, float r) { fill(new RoundRectangle2D.Float(x, y, w, h, 2 * r, 2 * r)); }
    public void strokeRoundRect(float x, float y, float w, float h, float r, float lw) { stroke(new RoundRectangle2D.Float(x, y, w, h, 2 * r, 2 * r), lw); }
    public void fillCircle(float cx, float cy, float r) { fill(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r)); }
    public void strokeCircle(float cx, float cy, float r, float w) { stroke(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r), w); }
    public void arc(float cx, float cy, float r, float start, float sweep, float w) {
        g.setPaint(paint); g.setStroke(new BasicStroke(w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Float(cx - r, cy - r, 2 * r, 2 * r, -start, -sweep, Arc2D.OPEN));
    }
    Path2D path(float[] xy, boolean closed) { Path2D.Float p = new Path2D.Float(); p.moveTo(xy[0], xy[1]); for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]); if (closed) p.closePath(); return p; }
    public void fillPoly(float[] xy) { fill(path(xy, true)); }
    public void strokePoly(float[] xy, boolean closed, float w) { stroke(path(xy, closed), w); }
    public void line(float x0, float y0, float x1, float y1, float w) { stroke(new Line2D.Float(x0, y0, x1, y1), w); }
    static boolean isRune(char c) { return false; }
    Font font(int f, float size) { Font b = f == TABULAR ? tab : f == DIGIT ? rune : f == TITLE ? title : f == BOLD ? bold : sans; return b.deriveFont(size); }
    float runW(String s, Font f) { return (float) f.getStringBounds(s, g.getFontRenderContext()).getWidth(); }
    public float textWidth(String s, float size, int f) {
        float w = 0; int i = 0;
        while (i < s.length()) { int j = i; boolean r = isRune(s.charAt(i)); while (j < s.length() && isRune(s.charAt(j)) == r) j++; w += runW(s.substring(i, j), font(f, size)); i = j; }
        return w;
    }
    public void text(String s, float x, float y, float size, int align, int f) {
        float w = textWidth(s, size, f);
        float cx = x + (align < 0 ? 0 : align == 0 ? -w / 2 : -w);
        g.setPaint(paint);
        int i = 0;
        while (i < s.length()) {
            int j = i; boolean r = isRune(s.charAt(i));
            while (j < s.length() && isRune(s.charAt(j)) == r) j++;
            String part = s.substring(i, j); Font fo = font(f, size);
            g.setFont(fo); g.drawString(part, cx, y); cx += runW(part, fo); i = j;
        }
    }
    public Object layer(int w, int h, Painter p) { BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); Graphics2D g2 = img.createGraphics(); p.paint(new J2dGfx(g2)); g2.dispose(); return img; }
    public void drawLayer(Object layer, float x, float y, float w, float h) {
        BufferedImage img = (BufferedImage) layer; AffineTransform at = new AffineTransform(); at.translate(x, y); at.scale(w / img.getWidth(), h / img.getHeight());
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); g.drawImage(img, at, null);
    }
}
