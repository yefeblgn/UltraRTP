package com.yefeblgn.ultrartp.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.util.Locale;

/**
 * 1.21.x'te bazı türler enum olmaktan çıkıp Registry'ye taşındı.
 * Bu sınıf config'teki metinleri sürümden bağımsız şekilde çözümler.
 */
public final class Registries {

    private Registries() {
    }

    public static Material material(String name, Material fallback) {
        if (name == null || name.isBlank()) return fallback;
        Material material = Material.matchMaterial(name.trim());
        if (material == null || material.isAir()) return fallback;
        return material;
    }

    public static Particle particle(String name, Particle fallback) {
        if (name == null || name.isBlank()) return fallback;
        String normalized = name.trim().toUpperCase(Locale.ROOT).replace('.', '_');
        int colon = normalized.indexOf(':');
        if (colon >= 0) normalized = normalized.substring(colon + 1);
        try {
            return Particle.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            try {
                NamespacedKey key = NamespacedKey.minecraft(normalized.toLowerCase(Locale.ROOT));
                Particle fromRegistry = Registry.PARTICLE_TYPE.get(key);
                if (fromRegistry != null) return fromRegistry;
            } catch (Throwable ignored2) {
                // yoksay
            }
            return fallback;
        }
    }

    /**
     * Ses anahtarını "minecraft:entity.enderman.teleport" biçimine çevirir.
     * Hem eski enum yazımı (ENTITY_ENDERMAN_TELEPORT) hem de yeni anahtar yazımı desteklenir.
     */
    public static String soundKey(String input) {
        if (input == null || input.isBlank()) return null;
        String value = input.trim();

        if (value.contains(":")) {
            return value.toLowerCase(Locale.ROOT);
        }
        if (value.contains(".")) {
            return "minecraft:" + value.toLowerCase(Locale.ROOT);
        }

        // Enum tarzı yazım -> registry taraması
        String target = value.toUpperCase(Locale.ROOT);
        try {
            for (Sound sound : Registry.SOUNDS) {
                NamespacedKey key = Registry.SOUNDS.getKey(sound);
                if (key == null) continue;
                if (key.getKey().replace('.', '_').toUpperCase(Locale.ROOT).equals(target)) {
                    return key.toString();
                }
            }
        } catch (Throwable ignored) {
            // Registry erişilemezse aşağıdaki basit dönüşüme düşer
        }
        return "minecraft:" + value.toLowerCase(Locale.ROOT).replace('_', '.');
    }

    public static Color color(String hex, Color fallback) {
        if (hex == null || hex.isBlank()) return fallback;
        String value = hex.trim();
        if (value.startsWith("#")) value = value.substring(1);
        try {
            int rgb = Integer.parseInt(value, 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
