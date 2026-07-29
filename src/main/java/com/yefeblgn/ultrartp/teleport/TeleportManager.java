package com.yefeblgn.ultrartp.teleport;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.api.RTPPreTeleportEvent;
import com.yefeblgn.ultrartp.api.RTPTeleportEvent;
import com.yefeblgn.ultrartp.data.DataStore;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Formatter;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Işınlanma akışının tamamını yönetir:
 * kontroller → bekleme (warmup) → ücret → konum arama → ışınlanma → efektler.
 */
public final class TeleportManager {

    private static final String SHARED_KEY = "*";

    private final UltraRTP plugin;

    private final Map<UUID, WarmupSession> warmups = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> invulnerability = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> searching = new ConcurrentHashMap<>();
    private final AtomicInteger activeSearches = new AtomicInteger();

    public TeleportManager(UltraRTP plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------- genel API

    /**
     * Oyuncunun ışınlanma isteği. Tüm kontroller burada yapılır.
     */
    public void request(Player player, Region region) {
        if (player == null) return;

        if (region == null) {
            plugin.messages().send(player, "region.none-available");
            return;
        }
        if (!region.enabled()) {
            plugin.messages().send(player, "region.disabled");
            return;
        }
        if (!hasAccess(player, region)) {
            plugin.messages().send(player, "region.no-permission");
            return;
        }

        World world = Bukkit.getWorld(region.worldName());
        if (world == null) {
            plugin.messages().send(player, "region.world-missing", Text.of("value", region.worldName()));
            return;
        }

        if (isBusy(player.getUniqueId())) {
            plugin.messages().send(player, "search.already");
            return;
        }

        long remaining = remainingCooldown(player, region);
        if (remaining > 0) {
            plugin.messages().send(player, "cooldown.active", Text.of("time", Formatter.time(remaining)));
            return;
        }

        double cost = effectiveCost(player, region);
        if (cost > 0 && !plugin.economy().has(player, cost)) {
            plugin.messages().send(player, "cost.insufficient",
                    Text.of("cost", plugin.economy().format(cost)),
                    Text.of("balance", plugin.economy().format(plugin.economy().balance(player))));
            return;
        }

        RTPPreTeleportEvent event = new RTPPreTeleportEvent(player, region);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        int warmupSeconds = effectiveWarmup(player);
        if (warmupSeconds <= 0) {
            execute(player, region, cost);
        } else {
            startWarmup(player, region, cost, warmupSeconds);
        }
    }

    /**
     * Kontrolleri atlayan yönetici ışınlanması.
     */
    public void forceTeleport(Player player, Region region) {
        if (player == null || region == null) return;
        execute(player, region, 0.0D);
    }

    public boolean isBusy(UUID uuid) {
        return warmups.containsKey(uuid) || searching.containsKey(uuid);
    }

    public boolean isWarmingUp(UUID uuid) {
        return warmups.containsKey(uuid);
    }

    public int warmupRemaining(UUID uuid) {
        WarmupSession session = warmups.get(uuid);
        return session == null ? 0 : session.remainingSeconds();
    }

    public WarmupSession session(UUID uuid) {
        return warmups.get(uuid);
    }

    // ------------------------------------------------------------- warmup

    private void startWarmup(Player player, Region region, double cost, int seconds) {
        WarmupSession session = new WarmupSession(
                player.getUniqueId(), region, player.getLocation().clone(), seconds * 20);
        warmups.put(player.getUniqueId(), session);

        plugin.messages().send(player, "warmup.started", Text.of("time", Formatter.time(seconds)));

        String mode = plugin.config().countdownMode();
        if (usesBossBar(mode)) {
            BossBar bar = BossBar.bossBar(
                    plugin.messages().get(player, "warmup.bossbar", Text.num("time", seconds)),
                    1.0F, bossBarColor(), bossBarOverlay());
            session.bossBar(bar);
            player.showBossBar(bar);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickWarmup(player, session, cost), 0L, 1L);
        session.task(task);
    }

    private void tickWarmup(Player player, WarmupSession session, double cost) {
        if (!player.isOnline()) {
            cancel(player, null);
            return;
        }

        session.tick();
        int tick = session.elapsedTicks();
        int remaining = session.remainingSeconds();
        String mode = plugin.config().countdownMode();

        if (tick % 2 == 0) {
            plugin.effects().warmupSpiral(player, tick);
        }
        if (tick % 20 == 0) {
            plugin.effects().sound("warmup", player.getLocation());

            if (usesTitle(mode)) {
                player.showTitle(Title.title(
                        plugin.messages().get(player, "warmup.countdown-title", Text.num("time", remaining)),
                        plugin.messages().get(player, "warmup.countdown-subtitle"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(200))));
            }
        }
        if (tick % 4 == 0 && usesActionBar(mode)) {
            plugin.messages().actionBar(player, "warmup.countdown-actionbar",
                    Text.of("bar", Formatter.progressBar(session.progress(), 20, "|", "|")),
                    Text.num("time", remaining));
        }
        if (session.bossBar() != null) {
            BossBar bar = session.bossBar();
            bar.progress((float) Math.max(0.0D, 1.0D - session.progress()));
            bar.name(plugin.messages().get(player, "warmup.bossbar", Text.num("time", remaining)));
        }

        if (session.finished()) {
            clearSession(player, session);
            execute(player, session.region(), cost);
        }
    }

    /**
     * Devam eden ışınlanmayı iptal eder.
     *
     * @param messagePath gösterilecek mesaj (null olabilir)
     */
    public boolean cancel(Player player, String messagePath) {
        if (player == null) return false;
        WarmupSession session = warmups.get(player.getUniqueId());
        if (session == null) return false;

        clearSession(player, session);
        plugin.effects().play("cancel", player);
        if (messagePath != null) {
            plugin.messages().send(player, messagePath);
        }
        if (usesTitle(plugin.config().countdownMode())) {
            player.clearTitle();
        }
        return true;
    }

    public void cancelAll() {
        for (UUID uuid : new ArrayList<>(warmups.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            WarmupSession session = warmups.get(uuid);
            if (session != null) {
                if (session.task() != null) session.task().cancel();
                if (player != null && session.bossBar() != null) player.hideBossBar(session.bossBar());
            }
            warmups.remove(uuid);
        }
        for (BukkitTask task : invulnerability.values()) {
            task.cancel();
        }
        invulnerability.clear();
        searching.clear();
        activeSearches.set(0);
    }

    private void clearSession(Player player, WarmupSession session) {
        warmups.remove(session.playerId());
        if (session.task() != null) session.task().cancel();
        if (session.bossBar() != null && player != null) player.hideBossBar(session.bossBar());
    }

    // ------------------------------------------------------------- yürütme

    private void execute(Player player, Region region, double cost) {
        UUID uuid = player.getUniqueId();

        if (activeSearches.get() >= plugin.config().maxConcurrentSearches()) {
            plugin.messages().send(player, "search.busy");
            return;
        }

        boolean charged = false;
        if (cost > 0) {
            if (!plugin.economy().isAvailable()) {
                plugin.messages().send(player, "cost.no-economy");
            } else if (!plugin.economy().withdraw(player, cost)) {
                plugin.messages().send(player, "cost.insufficient",
                        Text.of("cost", plugin.economy().format(cost)),
                        Text.of("balance", plugin.economy().format(plugin.economy().balance(player))));
                return;
            } else {
                charged = true;
                plugin.messages().send(player, "cost.charged", Text.of("cost", plugin.economy().format(cost)));
            }
        }

        searching.put(uuid, Boolean.TRUE);
        activeSearches.incrementAndGet();

        plugin.messages().send(player, "search.started");
        plugin.effects().play("searching", player);

        final boolean refundable = charged;
        resolveLocation(region, player).whenComplete((location, throwable) -> {
            activeSearches.decrementAndGet();
            searching.remove(uuid);

            if (!player.isOnline()) {
                if (refundable && plugin.config().refundOnFail()) plugin.economy().deposit(player, cost);
                return;
            }

            if (throwable != null || location == null) {
                if (refundable && plugin.config().refundOnFail()) {
                    plugin.economy().deposit(player, cost);
                    plugin.messages().send(player, "cost.refunded", Text.of("cost", plugin.economy().format(cost)));
                }
                plugin.messages().send(player, "search.failed", Text.num("attempts", plugin.config().maxAttempts()));
                plugin.effects().play("cancel", player);
                return;
            }

            performTeleport(player, region, location);
        });
    }

    private void performTeleport(Player player, Region region, Location target) {
        Location from = player.getLocation().clone();

        if (plugin.config().dismountBefore()) {
            if (player.isInsideVehicle()) player.leaveVehicle();
            player.eject();
        }

        plugin.data().setBack(player.getUniqueId(), from);

        plugin.effects().play("departure", player);
        plugin.effects().ring("departure", from, 1.2D, 16);

        player.teleportAsync(target, PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success -> {
            if (!Boolean.TRUE.equals(success)) {
                plugin.messages().send(player, "teleport.failed");
                return;
            }
            if (!player.isOnline()) return;

            if (plugin.config().resetFallDamage()) {
                player.setFallDistance(0.0F);
            }

            plugin.effects().play("arrival", player);
            plugin.effects().ring("arrival", target, 1.4D, 20);

            player.showTitle(Title.title(
                    plugin.messages().get(player, "teleport.success-title"),
                    plugin.messages().get(player, "teleport.success-subtitle",
                            Text.num("x", target.getBlockX()),
                            Text.num("y", target.getBlockY()),
                            Text.num("z", target.getBlockZ())),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(1800), Duration.ofMillis(400))));

            plugin.messages().send(player, "teleport.success",
                    Text.parsed("region", region.displayName()),
                    Text.num("x", target.getBlockX()),
                    Text.num("y", target.getBlockY()),
                    Text.num("z", target.getBlockZ()),
                    Text.of("world", target.getWorld() == null ? "" : target.getWorld().getName()));

            applyCooldown(player, region);
            plugin.data().incrementTeleports(player.getUniqueId());
            applyInvulnerability(player);

            Bukkit.getPluginManager().callEvent(new RTPTeleportEvent(player, region, from, target));
        });
    }

    private CompletableFuture<Location> resolveLocation(Region region, Player player) {
        Location cached = plugin.cache().poll(region);
        if (cached == null || cached.getWorld() == null) {
            return plugin.finder().find(region, player);
        }

        CompletableFuture<Location> future = new CompletableFuture<>();
        World world = cached.getWorld();
        world.getChunkAtAsync(cached.getBlockX() >> 4, cached.getBlockZ() >> 4, true).thenAccept(chunk -> {
            Runnable check = () -> {
                if (plugin.safety().isStillSafe(cached, region)) {
                    future.complete(cached);
                } else {
                    plugin.finder().find(region, player).whenComplete((location, throwable) -> future.complete(location));
                }
            };
            if (Bukkit.isPrimaryThread()) {
                check.run();
            } else {
                Bukkit.getScheduler().runTask(plugin, check);
            }
        }).exceptionally(throwable -> {
            plugin.finder().find(region, player).whenComplete((location, error) -> future.complete(location));
            return null;
        });
        return future;
    }

    private void applyInvulnerability(Player player) {
        int seconds = plugin.config().invulnerabilitySeconds();
        if (seconds <= 0) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;

        UUID uuid = player.getUniqueId();
        BukkitTask existing = invulnerability.remove(uuid);
        if (existing != null) existing.cancel();

        player.setInvulnerable(true);
        plugin.messages().send(player, "teleport.invulnerable", Text.of("time", Formatter.time(seconds)));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            invulnerability.remove(uuid);
            Player online = Bukkit.getPlayer(uuid);
            if (online != null && online.getGameMode() != GameMode.CREATIVE && online.getGameMode() != GameMode.SPECTATOR) {
                online.setInvulnerable(false);
            }
        }, seconds * 20L);
        invulnerability.put(uuid, task);
    }

    /** Oyuncu çıkarken dokunulmazlığı geri al. */
    public void clearInvulnerability(Player player) {
        BukkitTask task = invulnerability.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
            if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                player.setInvulnerable(false);
            }
        }
    }

    // ------------------------------------------------------------- /rtp back

    public void teleportBack(Player player) {
        if (!plugin.config().backEnabled()) {
            plugin.messages().send(player, "back.disabled");
            return;
        }
        if (isBusy(player.getUniqueId())) {
            plugin.messages().send(player, "search.already");
            return;
        }

        DataStore.BackEntry entry = plugin.data().back(player.getUniqueId());
        if (entry == null || entry.location() == null || entry.location().getWorld() == null) {
            plugin.messages().send(player, "back.none");
            return;
        }

        double cost = plugin.config().backCost();
        if (cost > 0 && !player.hasPermission("ultrartp.bypass.cost")) {
            if (!plugin.economy().has(player, cost)) {
                plugin.messages().send(player, "cost.insufficient",
                        Text.of("cost", plugin.economy().format(cost)),
                        Text.of("balance", plugin.economy().format(plugin.economy().balance(player))));
                return;
            }
            if (!plugin.economy().withdraw(player, cost)) {
                plugin.messages().send(player, "cost.insufficient",
                        Text.of("cost", plugin.economy().format(cost)),
                        Text.of("balance", plugin.economy().format(plugin.economy().balance(player))));
                return;
            }
            plugin.messages().send(player, "cost.charged", Text.of("cost", plugin.economy().format(cost)));
        }

        Location current = player.getLocation().clone();
        plugin.effects().play("departure", player);

        player.teleportAsync(entry.location(), PlayerTeleportEvent.TeleportCause.PLUGIN).thenAccept(success -> {
            if (!Boolean.TRUE.equals(success)) {
                plugin.messages().send(player, "teleport.failed");
                return;
            }
            plugin.data().setBack(player.getUniqueId(), current);
            plugin.effects().play("arrival", player);
            plugin.messages().send(player, "back.success");
            if (plugin.config().resetFallDamage()) player.setFallDistance(0.0F);
        });
    }

    // ------------------------------------------------------------- yardımcı

    public boolean hasAccess(Player player, Region region) {
        if (region == null) return false;
        if (region.permission() == null) return true;
        return player.hasPermission(region.permission()) || player.hasPermission("ultrartp.region.*");
    }

    public List<Region> accessibleRegions(Player player) {
        List<Region> list = new ArrayList<>();
        for (Region region : plugin.config().regions()) {
            if (region.enabled() && hasAccess(player, region)) {
                list.add(region);
            }
        }
        return list;
    }

    public String cooldownKey(Region region) {
        if (plugin.config().cooldownShared() || region == null) return SHARED_KEY;
        return region.id();
    }

    public long remainingCooldown(Player player, Region region) {
        if (!plugin.config().cooldownEnabled()) return 0L;
        if (player.hasPermission("ultrartp.bypass.cooldown")) return 0L;
        return plugin.data().cooldownRemaining(player.getUniqueId(), cooldownKey(region));
    }

    public int effectiveCooldown(Player player, Region region) {
        if (!plugin.config().cooldownEnabled()) return 0;
        if (player.hasPermission("ultrartp.bypass.cooldown")) return 0;

        int base = region != null && region.cooldown() >= 0 ? region.cooldown() : plugin.config().cooldownSeconds();

        for (Map.Entry<String, Integer> entry : plugin.config().cooldownGroups().entrySet()) {
            if (player.hasPermission("ultrartp.cooldown." + entry.getKey().toLowerCase(Locale.ROOT))) {
                base = Math.min(base, entry.getValue());
            }
        }
        return Math.max(0, base);
    }

    private void applyCooldown(Player player, Region region) {
        int seconds = effectiveCooldown(player, region);
        if (seconds > 0) {
            plugin.data().setCooldown(player.getUniqueId(), cooldownKey(region), seconds);
        }
    }

    public double effectiveCost(Player player, Region region) {
        if (!plugin.config().costEnabled()) return 0.0D;
        if (player.hasPermission("ultrartp.bypass.cost")) return 0.0D;
        if (!plugin.economy().isAvailable()) return 0.0D;

        double cost = region != null && region.cost() >= 0 ? region.cost() : plugin.config().costAmount();
        return Math.max(0.0D, cost);
    }

    public int effectiveWarmup(Player player) {
        if (!plugin.config().warmupEnabled()) return 0;
        if (player.hasPermission("ultrartp.bypass.warmup")) return 0;
        return plugin.config().warmupSeconds();
    }

    // ------------------------------------------------------------- geri sayım modu

    private boolean usesTitle(String mode) {
        return mode.equals("TITLE") || mode.equals("BOTH") || mode.equals("ALL");
    }

    private boolean usesActionBar(String mode) {
        return mode.equals("ACTIONBAR") || mode.equals("BOTH") || mode.equals("ALL");
    }

    private boolean usesBossBar(String mode) {
        return mode.equals("BOSSBAR") || mode.equals("ALL");
    }

    private BossBar.Color bossBarColor() {
        try {
            return BossBar.Color.valueOf(plugin.config().bossbarColor());
        } catch (IllegalArgumentException ex) {
            return BossBar.Color.PURPLE;
        }
    }

    private BossBar.Overlay bossBarOverlay() {
        try {
            return BossBar.Overlay.valueOf(plugin.config().bossbarOverlay());
        } catch (IllegalArgumentException ex) {
            return BossBar.Overlay.NOTCHED_10;
        }
    }
}
