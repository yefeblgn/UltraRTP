package com.yefeblgn.ultrartp.teleport;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rastgele koordinat üretip chunk'ları asenkron yükleyerek güvenli konum arar.
 * <p>
 * Chunk üretimi {@code World#getChunkAtAsync} ile yapılır; ana thread bloklanmaz.
 */
public final class LocationFinder {

    private final UltraRTP plugin;

    public LocationFinder(UltraRTP plugin) {
        this.plugin = plugin;
    }

    /**
     * @param player merkez "PLAYER" ise gereklidir, aksi hâlde {@code null} olabilir
     */
    public CompletableFuture<Location> find(Region region, Player player) {
        CompletableFuture<Location> future = new CompletableFuture<>();

        if (region == null) {
            future.complete(null);
            return future;
        }

        World world = Bukkit.getWorld(region.worldName());
        if (world == null) {
            future.complete(null);
            return future;
        }

        double[] center = resolveCenter(region, world, player);
        attempt(world, region, center, 0, future);
        return future;
    }

    private void attempt(World world, Region region, double[] center, int attempt, CompletableFuture<Location> future) {
        if (future.isCancelled()) return;

        int maxAttempts = plugin.config().maxAttempts();
        if (attempt >= maxAttempts) {
            debug("'" + region.id() + "' için " + maxAttempts + " denemede konum bulunamadı.");
            future.complete(null);
            return;
        }

        int[] point = randomPoint(region, center);
        int x = point[0];
        int z = point[1];

        if (plugin.config().respectWorldBorder()) {
            Location probe = new Location(world, x + 0.5D, world.getSpawnLocation().getY(), z + 0.5D);
            if (!world.getWorldBorder().isInside(probe)) {
                retry(world, region, center, attempt + 1, future);
                return;
            }
        }

        world.getChunkAtAsync(x >> 4, z >> 4, true).thenAccept(chunk -> {
            if (future.isDone()) return;
            runSync(() -> {
                Location safe = plugin.safety().findSafe(world, x, z, region);
                if (safe != null) {
                    debug("'" + region.id() + "' -> " + x + "/" + z + " (" + (attempt + 1) + ". deneme)");
                    future.complete(safe);
                } else {
                    retry(world, region, center, attempt + 1, future);
                }
            });
        }).exceptionally(throwable -> {
            runSync(() -> retry(world, region, center, attempt + 1, future));
            return null;
        });
    }

    private void retry(World world, Region region, double[] center, int attempt, CompletableFuture<Location> future) {
        if (future.isDone()) return;
        int delay = plugin.config().attemptDelayTicks();
        if (delay <= 0) {
            attempt(world, region, center, attempt, future);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, () -> attempt(world, region, center, attempt, future), delay);
        }
    }

    private void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    private double[] resolveCenter(Region region, World world, Player player) {
        return switch (region.centerMode()) {
            case FIXED -> new double[]{region.centerX(), region.centerZ()};
            case PLAYER -> player != null
                    ? new double[]{player.getLocation().getX(), player.getLocation().getZ()}
                    : new double[]{world.getSpawnLocation().getX(), world.getSpawnLocation().getZ()};
            case WORLD_SPAWN -> new double[]{world.getSpawnLocation().getX(), world.getSpawnLocation().getZ()};
        };
    }

    private int[] randomPoint(Region region, double[] center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int min = region.minRadius();
        int max = region.maxRadius();

        if (region.shape() == Region.Shape.CIRCLE) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double minSq = (double) min * min;
            double maxSq = (double) max * max;
            double radius = Math.sqrt(random.nextDouble() * (maxSq - minSq) + minSq);
            return new int[]{
                    (int) Math.round(center[0] + radius * Math.cos(angle)),
                    (int) Math.round(center[1] + radius * Math.sin(angle))
            };
        }

        // Kare halka: iç kareyi dışlayacak şekilde örnekle
        int offsetX;
        int offsetZ;
        int guard = 0;
        do {
            offsetX = random.nextInt(-max, max + 1);
            offsetZ = random.nextInt(-max, max + 1);
            guard++;
        } while (Math.max(Math.abs(offsetX), Math.abs(offsetZ)) < min && guard < 32);

        if (Math.max(Math.abs(offsetX), Math.abs(offsetZ)) < min) {
            // Nadir durum: kenara zorla
            offsetX = random.nextBoolean() ? min : -min;
        }

        return new int[]{
                (int) Math.round(center[0]) + offsetX,
                (int) Math.round(center[1]) + offsetZ
        };
    }

    private void debug(String message) {
        if (plugin.config().debug()) {
            plugin.getLogger().info("[RTP] " + message);
        }
    }
}
