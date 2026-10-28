package nl.rarevogel.autosim;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;

import nl.rarevogel.autosim.core.Gfx;

/** Gfx on an Android Canvas. */
final class AndroidGfx implements Gfx {
    Canvas c;
    final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint bmp = new Paint(Paint.FILTER_BITMAP_FLAG);
    final Path path = new Path();
    final RectF rect = new RectF();
    final Typeface sans, bold, title, digit, tabular;

    AndroidGfx(Typeface title, Typeface digit, Typeface tabular) {
        this.title = title;
        this.digit = digit;
        this.tabular = tabular;
        sans = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void save() { c.save(); }
    public void restore() { c.restore(); }
    public void translate(float x, float y) { c.translate(x, y); }
    public void rotate(float deg) { c.rotate(deg); }
    public void clip(float x, float y, float w, float h) { c.clipRect(x, y, x + w, y + h); }

    public void color(int argb) {
        fill.setShader(null); stroke.setShader(null);
        fill.setColor(argb); stroke.setColor(argb);
    }

    // with a shader the paint alpha still applies, so make it opaque
    public void linear(float x0, float y0, float x1, float y1, int c0, int c1) {
        if (x0 == x1 && y0 == y1) y1 += 0.01f;
        Shader s = new LinearGradient(x0, y0, x1, y1, c0, c1, Shader.TileMode.CLAMP);
        fill.setColor(0xFF000000); stroke.setColor(0xFF000000);
        fill.setShader(s); stroke.setShader(s);
    }

    public void radial(float cx, float cy, float r, int c0, int c1) {
        Shader s = new RadialGradient(cx, cy, Math.max(0.01f, r), c0, c1, Shader.TileMode.CLAMP);
        fill.setColor(0xFF000000); stroke.setColor(0xFF000000);
        fill.setShader(s); stroke.setShader(s);
    }

    public void fillRect(float x, float y, float w, float h) { c.drawRect(x, y, x + w, y + h, fill); }
    public void fillRoundRect(float x, float y, float w, float h, float r) { rect.set(x, y, x + w, y + h); c.drawRoundRect(rect, r, r, fill); }
    public void strokeRoundRect(float x, float y, float w, float h, float r, float lw) {
        stroke.setStrokeWidth(lw); rect.set(x, y, x + w, y + h); c.drawRoundRect(rect, r, r, stroke);
    }
    public void fillCircle(float cx, float cy, float r) { c.drawCircle(cx, cy, r, fill); }
    public void strokeCircle(float cx, float cy, float r, float w) { stroke.setStrokeWidth(w); c.drawCircle(cx, cy, r, stroke); }
    public void arc(float cx, float cy, float r, float start, float sweep, float w) {
        stroke.setStrokeWidth(w); rect.set(cx - r, cy - r, cx + r, cy + r); c.drawArc(rect, start, sweep, false, stroke);
    }

    void build(float[] xy, boolean closed) {
        path.rewind();
        path.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) path.lineTo(xy[i], xy[i + 1]);
        if (closed) path.close();
    }

    public void fillPoly(float[] xy) { build(xy, true); c.drawPath(path, fill); }
    public void strokePoly(float[] xy, boolean closed, float w) { build(xy, closed); stroke.setStrokeWidth(w); c.drawPath(path, stroke); }
    public void line(float x0, float y0, float x1, float y1, float w) { stroke.setStrokeWidth(w); c.drawLine(x0, y0, x1, y1, stroke); }

    Typeface face(int f) { return f == TABULAR ? tabular : f == DIGIT ? digit : f == TITLE ? title : f == BOLD ? bold : sans; }

    public float textWidth(String s, float size, int f) {
        fill.setTypeface(face(f));
        fill.setTextSize(size);
        return fill.measureText(s);
    }

    public void text(String s, float x, float y, float size, int align, int f) {
        fill.setTypeface(face(f));
        fill.setTextSize(size);
        fill.setTextAlign(align < 0 ? Paint.Align.LEFT : align == 0 ? Paint.Align.CENTER : Paint.Align.RIGHT);
        c.drawText(s, x, y, fill);
    }

    public Object layer(int w, int h, Painter p) {
        Bitmap b = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(title, digit, tabular);
        g.c = new Canvas(b);
        p.paint(g);
        return b;
    }

    public void drawLayer(Object layer, float x, float y, float w, float h) {
        rect.set(x, y, x + w, y + h);
        c.drawBitmap((Bitmap) layer, null, rect, bmp);
    }
}
