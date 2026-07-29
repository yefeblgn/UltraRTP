package com.yefeblgn.ultrartp.teleport;

import com.yefeblgn.ultrartp.model.Region;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Devam eden bir ışınlanma bekleme (warmup) oturumu.
 */
public final class WarmupSession {

    private final UUID playerId;
    private final Region region;
    private final Location origin;
    private final int totalTicks;

    private BukkitTask task;
    private BossBar bossBar;
    private int elapsedTicks;

    public WarmupSession(UUID playerId, Region region, Location origin, int totalTicks) {
        this.playerId = playerId;
        this.region = region;
        this.origin = origin;
        this.totalTicks = Math.max(1, totalTicks);
    }

    public UUID playerId() {
        return playerId;
    }

    public Region region() {
        return region;
    }

    public Location origin() {
        return origin;
    }

    public int totalTicks() {
        return totalTicks;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public void tick() {
        elapsedTicks++;
    }

    public boolean finished() {
        return elapsedTicks >= totalTicks;
    }

    public int remainingSeconds() {
        return Math.max(0, (int) Math.ceil((totalTicks - elapsedTicks) / 20.0D));
    }

    public double progress() {
        return Math.min(1.0D, (double) elapsedTicks / totalTicks);
    }

    public BukkitTask task() {
        return task;
    }

    public void task(BukkitTask task) {
        this.task = task;
    }

    public BossBar bossBar() {
        return bossBar;
    }

    public void bossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }
}
