package nl.rarevogel.autosim.core;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/** Desktop counterpart of MainActivity.logo(): reads assets/logos/logos.json if it exists. */
final class TestLogos {
    static HashMap<String, Object> cache;
    static boolean mono;

    static boolean mono() { get(""); return mono; }

    static Object get(String car) {
        if (cache == null) {
            cache = new HashMap<>();
            try {
                String json = new String(Files.readAllBytes(new File("assets/logos/logos.json").toPath()), "UTF-8");
                mono = json.matches("(?s).*\"stijl\"\\s*:\\s*\"mono\".*");
                int i = json.indexOf("\"cars\""), j = json.indexOf("}", i);
                Matcher m = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"").matcher(json.substring(i, j));
                while (m.find()) cache.put(m.group(1), ImageIO.read(new File("assets/logos/" + (mono ? m.group(2).replace(".png", "-mono.png") : m.group(2)))));
            } catch (Exception ignored) { }
        }
        return cache.get(car);
    }

    static final HashMap<String, Object> images = new HashMap<>();

    static String text(String path) {
        try { return new String(Files.readAllBytes(new File("assets/" + path).toPath()), "UTF-8"); } catch (Exception e) { return null; }
    }

    static Object image(String path) {
        if (!images.containsKey(path)) {
            Object img = null;
            try { img = ImageIO.read(new File("assets/" + path)); } catch (Exception ignored) { }
            images.put(path, img);
        }
        return images.get(path);
    }
}
