package com.yefeblgn.ultrartp.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * config.yml içindeki bir ışınlanma bölgesi.
 * <p>
 * {@code null} / -1 olan alanlar genel ayarları devralır.
 */
public final class Region {

    public enum Shape {
        SQUARE,
        CIRCLE
    }

    public enum CenterMode {
        FIXED,
        WORLD_SPAWN,
        PLAYER
    }

    private final String id;
    private final boolean enabled;
    private final String displayName;
    private final String icon;
    /** Bedrock form butonundaki gorsel: pack yolu ya da http adresi. */
    private final String bedrockIcon;
    private final int slot;
    private final List<String> lore;

    private final String worldName;
    private final Shape shape;
    private final CenterMode centerMode;
    private final double centerX;
    private final double centerZ;
    private final int minRadius;
    private final int maxRadius;

    private final String permission;
    private final double cost;
    private final int cooldown;

    private final Boolean allowWater;
    private final Boolean avoidUnderRoof;
    private final Integer minY;
    private final Integer maxY;
    private final Set<String> blockedBiomes;

    private Region(String id, ConfigurationSection section) {
        this.id = id;
        this.enabled = section.getBoolean("enabled", true);
        this.displayName = section.getString("display-name", "<white>" + id);
        this.icon = section.getString("icon", "GRASS_BLOCK");
        this.bedrockIcon = section.getString("bedrock-icon", section.getString("bedrock-ikon"));
        this.slot = section.getInt("slot", -1);
        this.lore = new ArrayList<>(section.getStringList("lore"));

        this.worldName = section.getString("world", "world");
        this.shape = parseEnum(section.getString("shape", "SQUARE"), Shape.class, Shape.SQUARE);
        this.centerMode = parseEnum(section.getString("center", "WORLD_SPAWN"), CenterMode.class, CenterMode.WORLD_SPAWN);
        this.centerX = section.getDouble("center-x", 0.0D);
        this.centerZ = section.getDouble("center-z", 0.0D);

        int min = Math.max(0, section.getInt("min-radius", 500));
        int max = Math.max(min + 1, section.getInt("max-radius", 5000));
        this.minRadius = min;
        this.maxRadius = max;

        String perm = section.getString("permission", "");
        this.permission = (perm == null || perm.isBlank()) ? null : perm.trim();

        this.cost = section.getDouble("cost", -1.0D);
        this.cooldown = section.getInt("cooldown", -1);

        this.allowWater = section.contains("allow-water") ? section.getBoolean("allow-water") : null;
        this.avoidUnderRoof = section.contains("avoid-under-roof") ? section.getBoolean("avoid-under-roof") : null;
        this.minY = section.contains("min-y") ? section.getInt("min-y") : null;
        this.maxY = section.contains("max-y") ? section.getInt("max-y") : null;

        Set<String> biomes = new HashSet<>();
        for (String biome : section.getStringList("blocked-biomes")) {
            biomes.add(normalizeBiome(biome));
        }
        this.blockedBiomes = biomes;
    }

    public static Region load(String id, ConfigurationSection section) {
        if (section == null) return null;
        return new Region(id, section);
    }

    /**
     * config.yml'de karşılığı olmayan, bellekte üretilen bölge.
     * {@link com.yefeblgn.ultrartp.model.Zone} gibi kendi hedefini belirleyen
     * sistemler {@link com.yefeblgn.ultrartp.teleport.LocationFinder} ile
     * çalışabilmek için buna ihtiyaç duyar.
     */
    public static Region virtual(String id, String displayName, String world,
                                 Shape shape, int minRadius, int maxRadius) {
        ConfigurationSection section = new MemoryConfiguration().createSection(id);
        section.set("display-name", displayName);
        section.set("world", world);
        section.set("shape", (shape == null ? Shape.SQUARE : shape).name());
        section.set("center", CenterMode.WORLD_SPAWN.name());
        section.set("min-radius", minRadius);
        section.set("max-radius", maxRadius);
        return new Region(id, section);
    }

    public static String normalizeBiome(String raw) {
        if (raw == null) return "";
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("minecraft:")) value = value.substring(10);
        return value;
    }

    private static <T extends Enum<T>> T parseEnum(String value, Class<T> type, T fallback) {
        if (value == null) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    public String id() {
        return id;
    }

    public boolean enabled() {
        return enabled;
    }

    public String displayName() {
        return displayName;
    }

    public String icon() {
        return icon;
    }

    public Material iconFallback() {
        return Material.GRASS_BLOCK;
    }

    /** Bedrock form butonu gorseli; tanimli degilse null. */
    public String bedrockIcon() {
        return bedrockIcon;
    }

    public int slot() {
        return slot;
    }

    public List<String> lore() {
        return lore;
    }

    public String worldName() {
        return worldName;
    }

    public Shape shape() {
        return shape;
    }

    public CenterMode centerMode() {
        return centerMode;
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public int minRadius() {
        return minRadius;
    }

    public int maxRadius() {
        return maxRadius;
    }

    public String permission() {
        return permission;
    }

    public double cost() {
        return cost;
    }

    public int cooldown() {
        return cooldown;
    }

    public Boolean allowWater() {
        return allowWater;
    }

    public Boolean avoidUnderRoof() {
        return avoidUnderRoof;
    }

    public Integer minY() {
        return minY;
    }

    public Integer maxY() {
        return maxY;
    }

    public Set<String> blockedBiomes() {
        return blockedBiomes;
    }

    /** Merkezi oyuncuya göre değiştiği için önbelleğe alınamaz. */
    public boolean cacheable() {
        return centerMode != CenterMode.PLAYER;
    }

    public String configPath() {
        return "regions." + id;
    }
}
