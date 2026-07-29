package com.yefeblgn.ultrartp.model;

import com.yefeblgn.ultrartp.util.Registries;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Bir efekt adımının (warmup, arrival, ...) parçacık + ses tanımı.
 */
public record EffectSet(
        boolean particleEnabled,
        Particle particle,
        int count,
        boolean spiral,
        double radius,
        double offsetX,
        double offsetY,
        double offsetZ,
        double extra,
        Color color,
        float size,

        boolean soundEnabled,
        String soundKey,
        float volume,
        float pitch
) {

    public static EffectSet empty() {
        return new EffectSet(false, Particle.PORTAL, 0, false, 1.0D, 0, 0, 0, 0,
                Color.WHITE, 1.0F, false, null, 1.0F, 1.0F);
    }

    public static EffectSet load(ConfigurationSection section) {
        if (section == null) return empty();

        ConfigurationSection p = section.getConfigurationSection("particle");
        ConfigurationSection s = section.getConfigurationSection("sound");

        boolean particleEnabled = p != null && p.getBoolean("enabled", false);
        Particle particle = p == null ? Particle.PORTAL : Registries.particle(p.getString("type"), Particle.PORTAL);
        int count = p == null ? 0 : Math.max(0, p.getInt("count", 20));
        boolean spiral = p != null && p.getBoolean("spiral", false);
        double radius = p == null ? 1.0D : p.getDouble("radius", 1.0D);
        double offX = p == null ? 0.0D : p.getDouble("offset-x", 0.0D);
        double offY = p == null ? 0.0D : p.getDouble("offset-y", 0.0D);
        double offZ = p == null ? 0.0D : p.getDouble("offset-z", 0.0D);
        double extra = p == null ? 0.0D : p.getDouble("extra", 0.0D);
        Color color = p == null ? Color.WHITE : Registries.color(p.getString("color"), Color.WHITE);
        float size = p == null ? 1.0F : (float) p.getDouble("size", 1.0D);

        boolean soundEnabled = s != null && s.getBoolean("enabled", false);
        String soundKey = s == null ? null : Registries.soundKey(s.getString("type"));
        float volume = s == null ? 1.0F : (float) s.getDouble("volume", 1.0D);
        float pitch = s == null ? 1.0F : (float) s.getDouble("pitch", 1.0D);

        return new EffectSet(particleEnabled, particle, count, spiral, radius, offX, offY, offZ, extra,
                color, size, soundEnabled, soundKey, volume, pitch);
    }
}
