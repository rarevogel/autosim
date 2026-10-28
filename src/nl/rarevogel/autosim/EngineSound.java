package nl.rarevogel.autosim;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Synthesised engine sound: the firing frequency (rpm / 60 x cylinders / 2) with a few
 * harmonics, a deep crankshaft rumble and exhaust pulses of filtered noise.
 */
final class EngineSound implements Runnable {
    static final int RATE = 22050;
    volatile double rpm = 800, load;
    volatile int cylinders = 4;
    volatile boolean on, alive = true;
    Thread thread;

    void start() {
        alive = true;
        thread = new Thread(this, "engine");
        thread.start();
    }

    void stop() {
        alive = false;
        if (thread != null) try { thread.join(300); } catch (InterruptedException ignored) { }
    }

    @SuppressWarnings("deprecation")
    public void run() {
        int min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, RATE, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 2048 * 2), AudioTrack.MODE_STREAM);
        short[] buf = new short[512];
        double phase = 0, crank = 0, curRpm = 800, vol = 0, lp = 0, lp2 = 0;
        java.util.Random rnd = new java.util.Random(1);
        boolean playing = false;
        while (alive) {
            if (!on && vol < 0.001) {
                if (playing) { track.pause(); track.flush(); playing = false; }
                try { Thread.sleep(40); } catch (InterruptedException e) { break; }
                continue;
            }
            if (!playing) { track.play(); playing = true; }
            double target = Math.max(700, rpm), l = Math.max(0, Math.min(1, load));
            double tv = on ? 0.35 + 0.5 * l : 0;
            for (int i = 0; i < buf.length; i++) {
                curRpm += (target - curRpm) * 0.002;
                vol += (tv - vol) * 0.0015;
                double fire = curRpm / 60 * cylinders / 2.0;
                phase += 2 * Math.PI * fire / RATE;
                crank += 2 * Math.PI * (curRpm / 60) / RATE;
                if (phase > 2 * Math.PI) phase -= 2 * Math.PI;
                if (crank > 2 * Math.PI) crank -= 2 * Math.PI;
                double pulse = Math.pow(0.5 + 0.5 * Math.sin(phase), 6);
                double noise = rnd.nextDouble() * 2 - 1;
                lp += (noise - lp) * 0.25;
                double s = 0.45 * Math.sin(phase) + 0.22 * Math.sin(2 * phase + 0.3) + 0.12 * Math.sin(3 * phase + 1.1)
                        + 0.25 * Math.sin(crank) + (0.5 + 0.6 * l) * pulse * lp;
                lp2 += (s - lp2) * 0.35;
                double v = Math.max(-1, Math.min(1, lp2 * vol * 0.45));
                buf[i] = (short) (v * 32000);
            }
            track.write(buf, 0, buf.length);
        }
        try { track.stop(); } catch (Exception ignored) { }
        track.release();
    }
}
