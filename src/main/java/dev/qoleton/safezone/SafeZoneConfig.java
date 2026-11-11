package dev.qoleton.safezone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Stored in config/hudsafezone.json. Margins are the fraction of the screen cut from EACH side. */
public final class SafeZoneConfig {
    public static final double MAX_H = 0.25; // up to 50% of the width removed
    public static final double MAX_V = 0.20; // up to 40% of the height removed

    public double horizontal = 0.0;
    public double vertical = 0.0;
    public boolean hud = true;
    public boolean menus = true;
    public MenuBackground menuBackground = MenuBackground.FULL;

    /** What happens to the dark/blurred background behind menus that are squeezed into the zone. */
    public enum MenuBackground {
        FULL("Full screen"), FRAME("Frame only"), OFF("Off");

        public final String label;

        MenuBackground(String label) {
            this.label = label;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SafeZoneConfig instance = new SafeZoneConfig();

    public static SafeZoneConfig get() {
        return instance;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve("hudsafezone.json");
    }

    public static void load() {
        Path p = path();
        // Carry over settings from builds made before the rename (config/safezone.json).
        Path old = FMLPaths.CONFIGDIR.get().resolve("safezone.json");
        if (!Files.exists(p) && Files.exists(old)) {
            try {
                Files.move(old, p);
            } catch (IOException e) {
                SafeZone.LOGGER.warn("Could not migrate {}", old, e);
            }
        }
        if (Files.exists(p)) {
            try (Reader r = Files.newBufferedReader(p)) {
                SafeZoneConfig c = GSON.fromJson(r, SafeZoneConfig.class);
                if (c != null) {
                    instance = c;
                }
            } catch (Exception e) {
                SafeZone.LOGGER.warn("Could not read {}, using defaults", p, e);
            }
        }
        instance.clamp();
    }

    public static void save() {
        instance.clamp();
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            SafeZone.LOGGER.warn("Could not save hudsafezone.json", e);
        }
    }

    public void clamp() {
        horizontal = Math.max(0, Math.min(MAX_H, horizontal));
        vertical = Math.max(0, Math.min(MAX_V, vertical));
        if (menuBackground == null) {
            menuBackground = MenuBackground.FULL;
        }
    }

    public boolean hasZone() {
        return horizontal > 0.0001 || vertical > 0.0001;
    }

    public boolean hudActive() {
        return hud && hasZone();
    }

    public boolean menusActive() {
        return menus && hasZone();
    }
}
