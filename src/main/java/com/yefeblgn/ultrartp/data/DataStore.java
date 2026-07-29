package com.yefeblgn.ultrartp.data;

import com.yefeblgn.ultrartp.UltraRTP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bekleme süreleri, istatistikler ve "geri dön" konumları için kalıcı depolama.
 * Tüm veri {@code plugins/UltraRTP/data.yml} içinde tutulur.
 */
public final class DataStore {

    public record BackEntry(Location location, long time) {
    }

    private final UltraRTP plugin;
    private final File file;

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> teleportCounts = new ConcurrentHashMap<>();
    private final Map<UUID, BackEntry> backLocations = new ConcurrentHashMap<>();

    private volatile boolean dirty;

    public DataStore(UltraRTP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ------------------------------------------------------------ yükle/kaydet

    public void load() {
        cooldowns.clear();
        teleportCounts.clear();
        backLocations.clear();

        if (!file.exists()) return;

        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
        long now = System.currentTimeMillis();

        if (plugin.config().cooldownPersist()) {
            ConfigurationSection section = data.getConfigurationSection("cooldowns");
            if (section != null) {
                for (String rawId : section.getKeys(false)) {
                    UUID uuid = parseUuid(rawId);
                    if (uuid == null) continue;
                    ConfigurationSection entries = section.getConfigurationSection(rawId);
                    if (entries == null) continue;
                    Map<String, Long> map = new ConcurrentHashMap<>();
                    for (String key : entries.getKeys(false)) {
                        long expiry = entries.getLong(key);
                        if (expiry > now) map.put(key, expiry);
                    }
                    if (!map.isEmpty()) cooldowns.put(uuid, map);
                }
            }
        }

        ConfigurationSection stats = data.getConfigurationSection("stats");
        if (stats != null) {
            for (String rawId : stats.getKeys(false)) {
                UUID uuid = parseUuid(rawId);
                if (uuid != null) teleportCounts.put(uuid, stats.getInt(rawId));
            }
        }

        ConfigurationSection back = data.getConfigurationSection("back");
        if (back != null) {
            for (String rawId : back.getKeys(false)) {
                UUID uuid = parseUuid(rawId);
                if (uuid == null) continue;
                ConfigurationSection entry = back.getConfigurationSection(rawId);
                if (entry == null) continue;
                World world = Bukkit.getWorld(entry.getString("world", ""));
                if (world == null) continue;
                Location location = new Location(world,
                        entry.getDouble("x"), entry.getDouble("y"), entry.getDouble("z"),
                        (float) entry.getDouble("yaw"), (float) entry.getDouble("pitch"));
                backLocations.put(uuid, new BackEntry(location, entry.getLong("time", now)));
            }
        }
    }

    public void save() {
        YamlConfiguration data = new YamlConfiguration();
        long now = System.currentTimeMillis();

        if (plugin.config().cooldownPersist()) {
            for (Map.Entry<UUID, Map<String, Long>> entry : cooldowns.entrySet()) {
                for (Map.Entry<String, Long> inner : entry.getValue().entrySet()) {
                    if (inner.getValue() > now) {
                        data.set("cooldowns." + entry.getKey() + "." + inner.getKey(), inner.getValue());
                    }
                }
            }
        }

        for (Map.Entry<UUID, Integer> entry : teleportCounts.entrySet()) {
            if (entry.getValue() > 0) {
                data.set("stats." + entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<UUID, BackEntry> entry : backLocations.entrySet()) {
            Location location = entry.getValue().location();
            if (location == null || location.getWorld() == null) continue;
            String path = "back." + entry.getKey() + ".";
            data.set(path + "world", location.getWorld().getName());
            data.set(path + "x", location.getX());
            data.set(path + "y", location.getY());
            data.set(path + "z", location.getZ());
            data.set(path + "yaw", location.getYaw());
            data.set(path + "pitch", location.getPitch());
            data.set(path + "time", entry.getValue().time());
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Veri klasörü oluşturulamadı.");
            }
            data.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().severe("data.yml kaydedilemedi: " + ex.getMessage());
        }
    }

    public void saveIfDirty() {
        if (dirty) save();
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    // ------------------------------------------------------------ cooldown

    /** Kalan süre (saniye). 0 = hazır. */
    public long cooldownRemaining(UUID uuid, String key) {
        Map<String, Long> map = cooldowns.get(uuid);
        if (map == null) return 0L;
        Long expiry = map.get(key);
        if (expiry == null) return 0L;
        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0L) {
            map.remove(key);
            return 0L;
        }
        return (remaining + 999L) / 1000L;
    }

    public void setCooldown(UUID uuid, String key, int seconds) {
        if (seconds <= 0) return;
        cooldowns.computeIfAbsent(uuid, id -> new ConcurrentHashMap<>())
                .put(key, System.currentTimeMillis() + seconds * 1000L);
        dirty = true;
    }

    public void clearCooldowns(UUID uuid) {
        cooldowns.remove(uuid);
        dirty = true;
    }

    public int clearAllCooldowns() {
        int size = cooldowns.size();
        cooldowns.clear();
        dirty = true;
        return size;
    }

    // ------------------------------------------------------------ istatistik

    public int teleportCount(UUID uuid) {
        return teleportCounts.getOrDefault(uuid, 0);
    }

    public void incrementTeleports(UUID uuid) {
        teleportCounts.merge(uuid, 1, Integer::sum);
        dirty = true;
    }

    public int totalTeleports() {
        int total = 0;
        for (int value : teleportCounts.values()) total += value;
        return total;
    }

    public Map<UUID, Integer> allStats() {
        return new HashMap<>(teleportCounts);
    }

    // ------------------------------------------------------------ geri dön

    public void setBack(UUID uuid, Location location) {
        if (location == null) return;
        backLocations.put(uuid, new BackEntry(location.clone(), System.currentTimeMillis()));
        dirty = true;
    }

    /** Süresi dolmuşsa {@code null} döner ve kaydı temizler. */
    public BackEntry back(UUID uuid) {
        BackEntry entry = backLocations.get(uuid);
        if (entry == null) return null;

        int expire = plugin.config().backExpireSeconds();
        if (expire > 0 && System.currentTimeMillis() - entry.time() > expire * 1000L) {
            backLocations.remove(uuid);
            return null;
        }
        return entry;
    }

    public void clearBack(UUID uuid) {
        backLocations.remove(uuid);
        dirty = true;
    }
}
