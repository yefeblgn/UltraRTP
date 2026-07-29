package com.yefeblgn.ultrartp.teleport;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bölge başına önceden hesaplanmış güvenli konum deposu.
 * <p>
 * Sunucu boştayken arka planda doldurulur; oyuncu /rtp yazdığında ışınlanma
 * chunk üretimi beklemeden anında gerçekleşir.
 */
public final class LocationCache {

    private final UltraRTP plugin;

    private final Map<String, Deque<Location>> cache = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> inFlight = new ConcurrentHashMap<>();

    private BukkitTask task;

    public LocationCache(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.config().cacheEnabled()) return;

        long interval = plugin.config().cacheRefillSeconds() * 20L;
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::refill, 200L, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        clear();
        start();
    }

    private void refill() {
        if (!plugin.config().cacheEnabled()) return;
        if (plugin.config().cachePauseWhenEmpty() && Bukkit.getOnlinePlayers().isEmpty()) return;

        int target = plugin.config().cacheSize();
        int perCycle = plugin.config().cacheMaxPerCycle();

        for (Region region : plugin.config().regions()) {
            if (!region.enabled() || !region.cacheable()) continue;
            if (Bukkit.getWorld(region.worldName()) == null) continue;

            Deque<Location> queue = cache.computeIfAbsent(region.id(), id -> new ConcurrentLinkedDeque<>());
            AtomicInteger pending = inFlight.computeIfAbsent(region.id(), id -> new AtomicInteger());

            int missing = target - queue.size() - pending.get();
            if (missing <= 0) continue;

            int toGenerate = Math.min(perCycle, missing);
            for (int i = 0; i < toGenerate; i++) {
                pending.incrementAndGet();
                plugin.finder().find(region, null).whenComplete((location, throwable) -> {
                    pending.decrementAndGet();
                    if (location != null && queue.size() < plugin.config().cacheSize()) {
                        queue.add(location);
                    }
                });
            }
        }
    }

    /**
     * Depodan bir konum alır. Depo boşsa {@code null} döner.
     */
    public Location poll(Region region) {
        if (region == null || !plugin.config().cacheEnabled() || !region.cacheable()) return null;
        Deque<Location> queue = cache.get(region.id());
        if (queue == null) return null;

        Location location = queue.poll();
        if (location != null && location.getWorld() == null) {
            return null;
        }
        return location;
    }

    public int size(String regionId) {
        if (regionId == null) return 0;
        Deque<Location> queue = cache.get(regionId.toLowerCase(java.util.Locale.ROOT));
        return queue == null ? 0 : queue.size();
    }

    public int totalSize() {
        int total = 0;
        for (Deque<Location> queue : cache.values()) total += queue.size();
        return total;
    }

    public void clear() {
        cache.clear();
        inFlight.clear();
    }

    /**
     * Depoyu hemen doldurmayı dener (admin komutu / panel için).
     */
    public void forceRefill() {
        refill();
    }

    public String describe() {
        if (cache.isEmpty()) return "0";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Deque<Location>> entry : cache.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getKey()).append('=').append(entry.getValue().size());
        }
        return sb.toString();
    }
}
