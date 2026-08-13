package com.yefeblgn.ultrartp.zone;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.api.RTPZoneEnterEvent;
import com.yefeblgn.ultrartp.api.RTPZoneLeaveEvent;
import com.yefeblgn.ultrartp.api.RTPZonePreTeleportEvent;
import com.yefeblgn.ultrartp.api.RTPZoneTeleportEvent;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.model.Zone;
import com.yefeblgn.ultrartp.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * /rtpzone bölgelerini yükler, kaydeder, oyuncu giriş/çıkışlarını ve geri sayımlarını işletir.
 * <p>
 * Her saniye çalışan tek bir görev tüm bölgeleri tarar. Bir bölgede oyuncu
 * varken sayaç işler; bölge boşalırsa sayaç sıfırlanır.
 */
public final class ZoneManager {

    /** Kaç saniye kala hedef konumlar önceden hazırlanmaya başlansın. */
    private static final int PREPARE_AT = 5;
    /** Bir turda önceden hazırlanacak azami konum sayısı. */
    private static final int PREPARE_LIMIT = 12;

    private final UltraRTP plugin;
    private final File file;

    private final Map<String, Zone> zones = new LinkedHashMap<>();
    private final Map<UUID, ZoneSelection> selections = new ConcurrentHashMap<>();
    private final Map<String, Integer> counters = new ConcurrentHashMap<>();
    private final Map<String, Deque<Location>> prepared = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> playersInZone = new ConcurrentHashMap<>();

    private BukkitTask task;

    public ZoneManager(UltraRTP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "zones.yml");
    }

    // ------------------------------------------------------------ yükle/kaydet

    public void load() {
        zones.clear();
        counters.clear();
        prepared.clear();
        playersInZone.clear();

        if (!file.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("zones");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            Zone zone = Zone.load(key, root.getConfigurationSection(key));
            if (zone != null) zones.put(zone.id(), zone);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of(
                "UltraRTP - /rtpzone bölgeleri",
                "Bu dosya komutlarla yönetilir, elle düzenledikten sonra /rtpadmin reload gerekir."));

        for (Zone zone : zones.values()) {
            zone.save(yaml.createSection("zones." + zone.id()));
        }

        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Veri klasörü oluşturulamadı.");
            }
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("zones.yml kaydedilemedi: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------ görev

    public void start() {
        stop();
        this.task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        counters.clear();
        prepared.clear();
        playersInZone.clear();
        start();
    }

    private void tick() {
        for (Zone zone : new ArrayList<>(zones.values())) {
            if (!zone.enabled()) {
                counters.remove(zone.id());
                playersInZone.remove(zone.id());
                continue;
            }

            World world = Bukkit.getWorld(zone.worldName());
            if (world == null) continue;

            List<Player> rawInside = playersInside(zone, world);
            Set<UUID> currentSet = playersInZone.computeIfAbsent(zone.id(), id -> ConcurrentHashMap.newKeySet());
            Set<UUID> rawUuids = new HashSet<>();
            for (Player p : rawInside) {
                rawUuids.add(p.getUniqueId());
            }

            // Çıkan oyuncular
            List<UUID> leftUuids = new ArrayList<>();
            for (UUID uuid : currentSet) {
                if (!rawUuids.contains(uuid)) {
                    leftUuids.add(uuid);
                }
            }
            for (UUID uuid : leftUuids) {
                currentSet.remove(uuid);
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    Bukkit.getPluginManager().callEvent(new RTPZoneLeaveEvent(player, zone));
                }
            }

            // Giren ve kalan aktif oyuncular
            List<Player> activeInside = new ArrayList<>();
            for (Player player : rawInside) {
                if (!currentSet.contains(player.getUniqueId())) {
                    RTPZoneEnterEvent enterEvent = new RTPZoneEnterEvent(player, zone);
                    Bukkit.getPluginManager().callEvent(enterEvent);
                    if (!enterEvent.isCancelled()) {
                        currentSet.add(player.getUniqueId());
                        activeInside.add(player);
                    }
                } else {
                    activeInside.add(player);
                }
            }

            if (activeInside.isEmpty()) {
                counters.remove(zone.id());
                prepared.remove(zone.id());
                continue;
            }

            int left = counters.getOrDefault(zone.id(), zone.interval()) - 1;
            if (left > 0) {
                counters.put(zone.id(), left);
                countdown(zone, activeInside, left);
                continue;
            }

            counters.put(zone.id(), zone.interval());
            launch(zone, activeInside);
        }
    }

    /** Son saniyelerde action bar + uyarı efektleri. */
    private void countdown(Zone zone, List<Player> players, int left) {
        List<Integer> warnSeconds = plugin.config().zoneWarnSeconds();

        int barFrom = PREPARE_AT;
        for (int second : warnSeconds) {
            barFrom = Math.max(barFrom, second);
        }
        if (left > barFrom) return;

        boolean warn = warnSeconds.contains(left);
        for (Player player : players) {
            plugin.messages().actionBar(player, "zone.countdown-actionbar",
                    Text.parsed("zone", zone.displayName()),
                    Text.num("time", left));
            if (warn) {
                plugin.effects().zoneWarning(player, left);
                plugin.messages().send(player, "zone.warning", Text.num("time", left));
            }
        }

        if (left == PREPARE_AT) {
            prepare(zone, players.size());
        }
    }

    /** Geri sayım bittiğinde bölgedeki herkesi ışınlar. */
    private void launch(Zone zone, List<Player> players) {
        if (Bukkit.getWorld(zone.targetWorld()) == null) {
            plugin.getLogger().warning("Bölge '" + zone.id() + "': hedef dünya yüklü değil -> " + zone.targetWorld());
            for (Player player : players) {
                plugin.messages().send(player, "region.world-missing", Text.of("value", zone.targetWorld()));
            }
            return;
        }

        Region region = zone.region();
        Deque<Location> queue = prepared.computeIfAbsent(zone.id(), id -> new ConcurrentLinkedDeque<>());

        for (Player player : players) {
            Location ready = queue.poll();
            if (usable(ready, region)) {
                fire(player, zone, ready);
                continue;
            }

            plugin.finder().find(region, player).whenComplete((location, throwable) -> {
                runSync(() -> {
                    if (!player.isOnline()) return;
                    if (throwable != null || location == null) {
                        plugin.messages().send(player, "zone.failed");
                        return;
                    }
                    fire(player, zone, location);
                });
            });
        }
        queue.clear();
    }

    /**
     * Hazırlanmış konum hâlâ kullanılabilir mi.
     */
    private boolean usable(Location location, Region region) {
        if (location == null || location.getWorld() == null) return false;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return true;
        return plugin.safety().isStillSafe(location, region);
    }

    private void fire(Player player, Zone zone, Location target) {
        if (!player.isOnline()) return;

        RTPZonePreTeleportEvent preEvent = new RTPZonePreTeleportEvent(player, zone, target);
        Bukkit.getPluginManager().callEvent(preEvent);
        if (preEvent.isCancelled()) return;

        Location finalTarget = preEvent.getTargetLocation();
        Location from = player.getLocation().clone();

        plugin.effects().blast(from);
        plugin.messages().send(player, "zone.teleported");
        plugin.teleports().zoneTeleport(player, zone.region(), finalTarget);

        Bukkit.getPluginManager().callEvent(new RTPZoneTeleportEvent(player, zone, from, finalTarget));
    }

    /** Işınlanmadan birkaç saniye önce hedef konumları arka planda hazırlar. */
    private void prepare(Zone zone, int players) {
        if (Bukkit.getWorld(zone.targetWorld()) == null) return;

        Region region = zone.region();
        Deque<Location> queue = prepared.computeIfAbsent(zone.id(), id -> new ConcurrentLinkedDeque<>());

        int needed = Math.min(players, PREPARE_LIMIT) - queue.size();
        for (int i = 0; i < needed; i++) {
            plugin.finder().find(region, null).whenComplete((location, throwable) -> {
                if (location != null && location.getWorld() != null) queue.add(location);
            });
        }
    }

    public List<Player> playersInside(Zone zone, World world) {
        return playersInside(zone, world, false);
    }

    public List<Player> playersInside(Zone zone, World world, boolean ignoreBypass) {
        List<Player> list = new ArrayList<>();
        if (zone == null || world == null) return list;

        for (Player player : world.getPlayers()) {
            if (player.isDead()) continue;
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            if (!ignoreBypass && !player.isOp() && player.hasPermission("ultrartp.zone.bypass")) continue;
            if (zone.contains(player.getLocation())) list.add(player);
        }
        return list;
    }

    /** Sayacı beklemeden bölgeyi hemen tetikler (test / admin). */
    public int trigger(Zone zone) {
        World world = Bukkit.getWorld(zone.worldName());
        if (world == null) return 0;

        List<Player> inside = playersInside(zone, world, true);
        if (inside.isEmpty()) return 0;

        counters.put(zone.id(), zone.interval());
        launch(zone, inside);
        return inside.size();
    }

    /** Bölgenin bir sonraki ışınlanmasına kalan saniye (-1 = boş / durmuş). */
    public int remaining(Zone zone) {
        Integer value = counters.get(zone.id());
        return value == null ? -1 : value;
    }

    /** Oyuncu sunucudan ayrıldığında bölge durumunu temizler. */
    public void handleQuit(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        for (Map.Entry<String, Set<UUID>> entry : playersInZone.entrySet()) {
            if (entry.getValue().remove(uuid)) {
                Zone zone = zones.get(entry.getKey());
                if (zone != null) {
                    Bukkit.getPluginManager().callEvent(new RTPZoneLeaveEvent(player, zone));
                }
            }
        }
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    // ------------------------------------------------------------ bölge yönetimi

    public Collection<Zone> zones() {
        return zones.values();
    }

    public Zone zone(String id) {
        return id == null ? null : zones.get(id.toLowerCase(Locale.ROOT));
    }

    public void add(Zone zone) {
        zones.put(zone.id(), zone);
        save();
    }

    public boolean remove(String id) {
        String key = id == null ? null : id.toLowerCase(Locale.ROOT);
        if (key == null || zones.remove(key) == null) return false;
        counters.remove(key);
        prepared.remove(key);
        playersInZone.remove(key);
        save();
        return true;
    }

    /** Bölge ayarı değiştikten sonra sayaç ve hazır konumları tazeler. */
    public void update(Zone zone) {
        counters.remove(zone.id());
        prepared.remove(zone.id());
        playersInZone.remove(zone.id());
        save();
    }

    // ------------------------------------------------------------ seçim

    public ZoneSelection selection(UUID uuid) {
        return selections.computeIfAbsent(uuid, id -> new ZoneSelection());
    }

    public ZoneSelection peekSelection(UUID uuid) {
        return selections.get(uuid);
    }

    public void clearSelection(UUID uuid) {
        selections.remove(uuid);
    }
}
