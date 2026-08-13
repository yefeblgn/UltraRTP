package com.yefeblgn.ultrartp.model;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;

/**
 * /rtpzone ile çubuk kullanılarak seçilen kare bölge.
 * <p>
 * İçinde duran oyuncular {@link #interval()} saniyede bir
 * {@link #targetWorld()} dünyasında rastgele bir konuma ışınlanır.
 */
public final class Zone {

    private final String id;

    private boolean enabled;
    private String displayName;

    /** Bölgenin çizildiği dünya. */
    private String worldName;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    /** true -> Y sınırları yok sayılır, bölge gökten zemine kadar geçerlidir. */
    private boolean fullHeight;

    /** Işınlanmanın yapılacağı dünya. */
    private String targetWorld;
    private int interval;
    private int minRadius;
    private int maxRadius;
    private Region.Shape shape;

    private Region cachedRegion;

    private Zone(String id) {
        this.id = id;
    }

    public static Zone create(String id, Location first, Location second, String targetWorld, int interval) {
        Zone zone = new Zone(id.toLowerCase(Locale.ROOT));
        zone.enabled = true;
        zone.displayName = "<gradient:#00D4FF:#7B2FF7><bold>" + id + "</bold></gradient>";
        zone.targetWorld = targetWorld;
        zone.interval = Math.max(1, interval);
        zone.minRadius = 500;
        zone.maxRadius = 5000;
        zone.shape = Region.Shape.SQUARE;
        zone.fullHeight = true;
        zone.applySelection(first, second);
        return zone;
    }

    public static Zone load(String id, ConfigurationSection section) {
        if (section == null) return null;

        Zone zone = new Zone(id.toLowerCase(Locale.ROOT));
        zone.enabled = section.getBoolean("enabled", true);
        zone.displayName = section.getString("display-name", "<white>" + id);

        zone.worldName = section.getString("world", "world");
        zone.minX = section.getInt("min-x");
        zone.minY = section.getInt("min-y");
        zone.minZ = section.getInt("min-z");
        zone.maxX = section.getInt("max-x");
        zone.maxY = section.getInt("max-y");
        zone.maxZ = section.getInt("max-z");
        zone.fullHeight = section.getBoolean("full-height", true);

        zone.targetWorld = section.getString("target-world", zone.worldName);
        zone.interval = Math.max(1, section.getInt("interval", 60));
        zone.minRadius = Math.max(0, section.getInt("min-radius", 500));
        zone.maxRadius = Math.max(zone.minRadius + 1, section.getInt("max-radius", 5000));
        zone.shape = parseShape(section.getString("shape", "SQUARE"));
        return zone;
    }

    public void save(ConfigurationSection section) {
        section.set("enabled", enabled);
        section.set("display-name", displayName);
        section.set("world", worldName);
        section.set("min-x", minX);
        section.set("min-y", minY);
        section.set("min-z", minZ);
        section.set("max-x", maxX);
        section.set("max-y", maxY);
        section.set("max-z", maxZ);
        section.set("full-height", fullHeight);
        section.set("target-world", targetWorld);
        section.set("interval", interval);
        section.set("min-radius", minRadius);
        section.set("max-radius", maxRadius);
        section.set("shape", shape.name());
    }

    private static Region.Shape parseShape(String value) {
        if (value == null) return Region.Shape.SQUARE;
        try {
            return Region.Shape.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Region.Shape.SQUARE;
        }
    }

    /** İki köşeyi kare (kutu) hâline getirir. */
    public void applySelection(Location first, Location second) {
        this.worldName = first.getWorld() == null ? "world" : first.getWorld().getName();
        this.minX = Math.min(first.getBlockX(), second.getBlockX());
        this.maxX = Math.max(first.getBlockX(), second.getBlockX());
        this.minY = Math.min(first.getBlockY(), second.getBlockY());
        this.maxY = Math.max(first.getBlockY(), second.getBlockY());
        this.minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        this.maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
    }

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().getName().equalsIgnoreCase(worldName)) return false;

        int x = location.getBlockX();
        int z = location.getBlockZ();
        if (x < minX || x > maxX || z < minZ || z > maxZ) return false;

        if (fullHeight) return true;
        int y = location.getBlockY();
        return y >= minY && y <= maxY;
    }

    /** Işınlanma hedefini tanımlayan sanal bölge (önbelleklenir). */
    public Region region() {
        if (cachedRegion == null) {
            cachedRegion = Region.virtual("zone_" + id, displayName, targetWorld, shape, minRadius, maxRadius);
        }
        return cachedRegion;
    }

    private void invalidate() {
        this.cachedRegion = null;
    }

    public long area() {
        return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    // ------------------------------------------------------------ erişimciler

    public String id() {
        return id;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String displayName() {
        return displayName;
    }

    public void displayName(String displayName) {
        this.displayName = displayName;
        invalidate();
    }

    public String worldName() {
        return worldName;
    }

    public String targetWorld() {
        return targetWorld;
    }

    public void targetWorld(String targetWorld) {
        this.targetWorld = targetWorld;
        invalidate();
    }

    public int interval() {
        return interval;
    }

    public void interval(int interval) {
        this.interval = Math.max(1, interval);
    }

    public int minRadius() {
        return minRadius;
    }

    public void minRadius(int minRadius) {
        this.minRadius = Math.max(0, minRadius);
        if (this.maxRadius <= this.minRadius) this.maxRadius = this.minRadius + 1;
        invalidate();
    }

    public int maxRadius() {
        return maxRadius;
    }

    public void maxRadius(int maxRadius) {
        this.maxRadius = Math.max(this.minRadius + 1, maxRadius);
        invalidate();
    }

    public Region.Shape shape() {
        return shape;
    }

    public void shape(Region.Shape shape) {
        this.shape = shape == null ? Region.Shape.SQUARE : shape;
        invalidate();
    }

    public boolean fullHeight() {
        return fullHeight;
    }

    public void fullHeight(boolean fullHeight) {
        this.fullHeight = fullHeight;
    }

    public int minX() {
        return minX;
    }

    public int minY() {
        return minY;
    }

    public int minZ() {
        return minZ;
    }

    public int maxX() {
        return maxX;
    }

    public int maxY() {
        return maxY;
    }

    public int maxZ() {
        return maxZ;
    }

    public String describeBounds() {
        return minX + "," + minZ + " ➜ " + maxX + "," + maxZ;
    }
}
