package com.yefeblgn.ultrartp.teleport;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.Locale;
import java.util.Set;

/**
 * Bir koordinatın ışınlanmak için güvenli olup olmadığını denetler.
 * <p>
 * Kontroller: lav/ateş/kaktüs vb. tehlikeli bloklar, su, boşluk, mağara/tavan altı,
 * biyom kara listesi, Y sınırları, dünya sınırı ve çevredeki lav.
 * <p>
 * Tüm metotlar ana thread'de ve chunk yüklendikten sonra çağrılmalıdır.
 */
public final class SafetyChecker {

    private final UltraRTP plugin;

    public SafetyChecker(UltraRTP plugin) {
        this.plugin = plugin;
    }

    /**
     * Verilen X/Z sütununda güvenli bir ayak konumu arar.
     *
     * @return güvenli konum ya da {@code null}
     */
    public Location findSafe(World world, int x, int z, Region region) {
        if (world == null) return null;

        int min = Math.max(resolveMinY(region), world.getMinHeight() + 1);
        int max = Math.min(resolveMaxY(region), world.getMaxHeight() - 3);
        if (min >= max) return null;

        if (hasCeiling(world)) {
            // Nether gibi tavanlı dünyalarda yüzey taraması işe yaramaz -> yukarıdan aşağı tara
            return scanDown(world, x, z, max, min, region);
        }

        int top = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (top < min || top > max) return null;
        return validate(world, x, top, z, region);
    }

    /**
     * Önbellekten gelen bir konumun hâlâ güvenli olup olmadığını doğrular.
     */
    public boolean isStillSafe(Location location, Region region) {
        if (location == null || location.getWorld() == null) return false;
        World world = location.getWorld();
        int groundY = location.getBlockY() - 1;
        return validate(world, location.getBlockX(), groundY, location.getBlockZ(), region) != null;
    }

    private boolean hasCeiling(World world) {
        return world.getEnvironment() == World.Environment.NETHER;
    }

    private Location scanDown(World world, int x, int z, int from, int to, Region region) {
        for (int y = from; y > to; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (!type.isSolid()) continue;

            Location safe = validate(world, x, y, z, region);
            if (safe != null) return safe;

            // Aynı katmanda takılmamak için biraz atla
            y -= 2;
        }
        return null;
    }

    /**
     * {@code groundY} zemin bloğunun Y'si; oyuncu {@code groundY + 1} üzerinde durur.
     */
    private Location validate(World world, int x, int groundY, int z, Region region) {
        if (groundY <= world.getMinHeight() || groundY >= world.getMaxHeight() - 2) return null;

        int minY = resolveMinY(region);
        int maxY = resolveMaxY(region);
        if (groundY < minY || groundY > maxY) return null;

        boolean allowWater = resolveAllowWater(region);
        Set<Material> unsafeBlocks = plugin.config().unsafeBlocks();

        Material ground = world.getBlockAt(x, groundY, z).getType();
        if (ground == Material.WATER) {
            if (!allowWater) return null;
        } else {
            if (!ground.isSolid()) return null;
            if (plugin.config().unsafeGround().contains(ground)) return null;
            if (unsafeBlocks.contains(ground)) return null;
        }

        // Ayak + kafa hizası boş olmalı
        int required = plugin.config().requiredAirAbove();
        for (int i = 1; i <= required; i++) {
            Material above = world.getBlockAt(x, groundY + i, z).getType();
            if (above == Material.WATER) {
                if (!allowWater) return null;
                continue;
            }
            if (above.isSolid() || unsafeBlocks.contains(above)) return null;
        }

        // Mağara / yer altı kontrolü
        if (resolveAvoidRoof(region)) {
            int limit = Math.min(world.getMaxHeight() - 1, groundY + required + plugin.config().roofScanLimit());
            for (int y = groundY + required + 1; y <= limit; y++) {
                if (world.getBlockAt(x, y, z).getType().isSolid()) return null;
            }
        }

        // Biyom kara listesi
        Set<String> blocked = region != null && !region.blockedBiomes().isEmpty()
                ? region.blockedBiomes()
                : plugin.config().blockedBiomes();
        if (!blocked.isEmpty() && blocked.contains(biomeKey(world.getBiome(x, groundY, z)))) {
            return null;
        }

        // Çevredeki lav/ateş
        int radius = plugin.config().dangerScanRadius();
        if (radius > 0) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        Material nearby = world.getBlockAt(x + dx, groundY + dy, z + dz).getType();
                        if (nearby == Material.LAVA || nearby == Material.FIRE || nearby == Material.MAGMA_BLOCK) {
                            return null;
                        }
                    }
                }
            }
        }

        Location location = new Location(world, x + 0.5D, groundY + 1.0D, z + 0.5D);
        if (plugin.config().respectWorldBorder() && !world.getWorldBorder().isInside(location)) {
            return null;
        }
        return location;
    }

    private static String biomeKey(Biome biome) {
        if (biome == null) return "";
        try {
            return biome.getKey().getKey().toLowerCase(Locale.ROOT);
        } catch (Throwable throwable) {
            return biome.toString().toLowerCase(Locale.ROOT);
        }
    }

    private int resolveMinY(Region region) {
        if (region != null && region.minY() != null) return region.minY();
        return plugin.config().minY();
    }

    private int resolveMaxY(Region region) {
        if (region != null && region.maxY() != null) return region.maxY();
        return plugin.config().maxY();
    }

    private boolean resolveAllowWater(Region region) {
        if (region != null && region.allowWater() != null) return region.allowWater();
        return plugin.config().allowWater();
    }

    private boolean resolveAvoidRoof(Region region) {
        if (region != null && region.avoidUnderRoof() != null) return region.avoidUnderRoof();
        return plugin.config().avoidUnderRoof();
    }
}
