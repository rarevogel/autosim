package nl.rarevogel.autosim;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import nl.rarevogel.autosim.core.App;

public class MainActivity extends Activity implements App.Host {
    AppView view;
    App app;
    final EngineSound engine = new EngineSound();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setStatusBarColor(0xFF111418);
        getWindow().setNavigationBarColor(0xFF0A0C0F);
        app = new App(this);
        Typeface title = Typeface.createFromAsset(getAssets(), "orbitron-bold.ttf");
        Typeface digit = Typeface.createFromAsset(getAssets(), "orbitron.ttf");
        Typeface tabular = Typeface.createFromAsset(getAssets(), "autosim-digits.ttf");
        view = new AppView(this, new AndroidGfx(title, digit, tabular));
        setContentView(view);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        super.onResume();
        engine.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        engine.on = false;
        engine.stop();
    }

    @Override
    public void onBackPressed() {
        if (!app.back()) super.onBackPressed();
    }

    // ---- App.Host
    public void quit() { finish(); }

    // ---- brand logos (optional): assets/logos/logos.json maps car names to PNG files, made by tools/logos.py
    final java.util.HashMap<String, String> logoFiles = new java.util.HashMap<>();
    final java.util.HashMap<String, android.graphics.Bitmap> logoCache = new java.util.HashMap<>();
    boolean logosRead, logosMono;

    public boolean logosMono() { logo(""); return logosMono; }

    public Object logo(String carName) {
        if (!logosRead) {
            logosRead = true;
            try {
                java.io.InputStream in = getAssets().open("logos/logos.json");
                java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
                byte[] b = new byte[8192];
                for (int n; (n = in.read(b)) > 0; ) buf.write(b, 0, n);
                in.close();
                org.json.JSONObject json = new org.json.JSONObject(buf.toString("UTF-8"));
                logosMono = "mono".equals(json.optString("stijl", "kleur"));
                org.json.JSONObject cars = json.getJSONObject("cars");
                java.util.Iterator<String> it = cars.keys();
                while (it.hasNext()) {
                    String k = it.next(), f = cars.getString(k);
                    logoFiles.put(k, logosMono ? f.substring(0, f.length() - 4) + "-mono.png" : f);
                }
            } catch (Exception ignored) { }   // no logos installed: the header just shows the name
        }
        String file = logoFiles.get(carName);
        if (file == null) return null;
        if (!logoCache.containsKey(file)) {
            android.graphics.Bitmap bm = null;
            try {
                java.io.InputStream in = getAssets().open("logos/" + file);
                bm = android.graphics.BitmapFactory.decodeStream(in);
                in.close();
            } catch (Exception ignored) { }
            logoCache.put(file, bm);
        }
        return logoCache.get(file);
    }

    public String readAsset(String path) {
        try {
            java.io.InputStream in = getAssets().open(path);
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192];
            for (int n; (n = in.read(b)) > 0; ) buf.write(b, 0, n);
            in.close();
            return buf.toString("UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /** Images for the brand gallery: decoded as RGB_565 (they have no transparency) and kept in a small LRU cache. */
    final android.util.LruCache<String, android.graphics.Bitmap> images = new android.util.LruCache<String, android.graphics.Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String k, android.graphics.Bitmap b) { return b.getByteCount(); }
    };

    public Object image(String path) {
        android.graphics.Bitmap bm = images.get(path);
        if (bm == null) {
            try {
                java.io.InputStream in = getAssets().open(path);
                android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                o.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
                bm = android.graphics.BitmapFactory.decodeStream(in, null, o);
                in.close();
            } catch (Exception ignored) { }
            if (bm != null) images.put(path, bm);
        }
        return bm;
    }

    public void engine(double rpm, double load, int cylinders, boolean on) {
        engine.rpm = rpm;
        engine.load = load;
        engine.cylinders = cylinders;
        engine.on = on;
    }

    final class AppView extends View {
        final AndroidGfx gfx;
        long last;
        final Runnable tick = new Runnable() { public void run() { invalidate(); } };

        AppView(Context ctx, AndroidGfx gfx) {
            super(ctx);
            this.gfx = gfx;
        }

        @Override
        protected void onDraw(Canvas c) {
            long now = System.nanoTime();
            float dt = last == 0 ? 0.016f : Math.min(0.1f, (now - last) / 1e9f);
            last = now;
            gfx.c = c;
            app.frame(gfx, getWidth(), getHeight(), dt);
            removeCallbacks(tick);
            if (app.busy()) postInvalidateOnAnimation();
            else { last = 0; postDelayed(tick, 1000); }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            invalidate();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: app.down(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_MOVE: app.move(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_UP: app.up(e.getX(), e.getY()); break;
                case MotionEvent.ACTION_CANCEL: app.up(-1000, -1000); break;
                default: break;
            }
            return true;
        }
    }
}
