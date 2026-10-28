package nl.rarevogel.autosim.core;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
/** App icon: a rev counter with the needle in the red zone. */
public class Icon {
    public static void main(String[] a) throws Exception {
        int S = 1024;
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(S / 100.0, S / 100.0);
        Shape badge = new RoundRectangle2D.Double(2, 2, 96, 96, 30, 30);
        g.setPaint(new GradientPaint(0, 0, new Color(0x2A3038), 0, 100, new Color(0x0A0C0F)));
        g.fill(badge);
        g.setClip(badge);
        g.setPaint(new RadialGradientPaint(50, 20, 60, new float[]{0, 1}, new Color[]{new Color(0x55FF8A1F, true), new Color(0x00FF8A1F, true)}));
        g.fill(badge);
        double cx = 50, cy = 56, r = 34;
        g.setColor(new Color(0x14181D));
        g.fill(new Ellipse2D.Double(cx - r - 3, cy - r - 3, 2 * r + 6, 2 * r + 6));
        g.setColor(new Color(0x40FFFFFF, true));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new Ellipse2D.Double(cx - r - 3, cy - r - 3, 2 * r + 6, 2 * r + 6));
        // red zone
        g.setColor(new Color(0xE5483B));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.draw(new Arc2D.Double(cx - r + 4, cy - r + 4, 2 * r - 8, 2 * r - 8, -45, 50, Arc2D.OPEN));
        // ticks 0..8
        for (int i = 0; i <= 8; i++) {
            double ang = Math.toRadians(135 + i * 270 / 8.0);
            double ca = Math.cos(ang), sa = Math.sin(ang);
            g.setColor(i >= 7 ? new Color(0xE5483B) : new Color(0xECEFF3));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx + ca * r * 0.72, cy + sa * r * 0.72, cx + ca * r * 0.9, cy + sa * r * 0.9));
        }
        // needle near the red line
        double ang = Math.toRadians(135 + 6.6 * 270 / 8.0);
        g.setColor(new Color(0xFF8A1F));
        g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - Math.cos(ang) * 6, cy - Math.sin(ang) * 6, cx + Math.cos(ang) * r * 0.86, cy + Math.sin(ang) * r * 0.86));
        g.setColor(new Color(0x3A414A));
        g.fill(new Ellipse2D.Double(cx - 5, cy - 5, 10, 10));
        // speed lines
        g.setColor(new Color(0xFF8A1F));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(12, 86, 30, 86));
        g.draw(new Line2D.Double(8, 91, 24, 91));
        g.setClip(null);
        g.setColor(new Color(0xFF8A1F));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new RoundRectangle2D.Double(3.1, 3.1, 93.8, 93.8, 28, 28));
        g.dispose();
        ImageIO.write(img, "png", new File(a[0]));
    }
}
