package nl.rarevogel.autosim.core;

/** Minimal drawing surface shared by the Android canvas and the Java2D test renderer. */
public interface Gfx {
    int SANS = 0, BOLD = 1, TITLE = 2, DIGIT = 3, TABULAR = 4;   // TABULAR: DIGIT with fixed-width figures

    void save();
    void restore();
    void translate(float x, float y);
    void rotate(float deg);
    void clip(float x, float y, float w, float h);

    void color(int argb);
    void linear(float x0, float y0, float x1, float y1, int c0, int c1);
    void radial(float cx, float cy, float r, int c0, int c1);

    void fillRect(float x, float y, float w, float h);
    void fillRoundRect(float x, float y, float w, float h, float r);
    void strokeRoundRect(float x, float y, float w, float h, float r, float width);
    void fillCircle(float cx, float cy, float r);
    void strokeCircle(float cx, float cy, float r, float w);
    void arc(float cx, float cy, float r, float startDeg, float sweepDeg, float w);
    void fillPoly(float[] xy);
    void strokePoly(float[] xy, boolean closed, float w);
    void line(float x0, float y0, float x1, float y1, float w);

    /** align: -1 left, 0 centre, 1 right; y is the baseline. */
    void text(String s, float x, float y, float size, int align, int font);
    float textWidth(String s, float size, int font);

    Object layer(int w, int h, Painter p);
    void drawLayer(Object layer, float x, float y, float w, float h);

    interface Painter { void paint(Gfx g); }
}
