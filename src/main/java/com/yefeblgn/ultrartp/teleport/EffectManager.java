package com.yefeblgn.ultrartp.teleport;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.EffectSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Parçacık ve ses efektlerini oynatır.
 */
public final class EffectManager {

    private final UltraRTP plugin;

    public EffectManager(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void play(String key, Player player) {
        if (player == null) return;
        play(key, player.getLocation());
    }

    public void play(String key, Location location) {
        EffectSet set = plugin.config().effect(key);
        spawn(set, location, set.count());
        sound(set, location);
    }

    /**
     * Isınma sırasında oyuncunun çevresinde dönen sarmal.
     *
     * @param tick ışınlanma başlangıcından itibaren geçen tick
     */
    public void warmupSpiral(Player player, int tick) {
        if (player == null) return;
        EffectSet set = plugin.config().effect("warmup");
        if (!set.particleEnabled()) return;

        Location base = player.getLocation();
        World world = base.getWorld();
        if (world == null) return;

        if (!set.spiral()) {
            spawn(set, base.clone().add(0, 1, 0), set.count());
            return;
        }

        int points = Math.max(1, set.count() / 3);
        double radius = set.radius();
        for (int i = 0; i < points; i++) {
            double angle = (tick * 0.22D) + (i * (Math.PI * 2.0D / points));
            double height = 0.15D + ((tick % 40) / 40.0D) * 2.0D;
            Location point = base.clone().add(
                    Math.cos(angle) * radius,
                    height,
                    Math.sin(angle) * radius);
            spawnAt(set, point, 1, 0, 0, 0);
        }
    }

    /**
     * Işınlanma anında halka patlaması.
     */
    public void ring(String key, Location center, double radius, int points) {
        EffectSet set = plugin.config().effect(key);
        if (!set.particleEnabled() || center.getWorld() == null) return;

        for (int i = 0; i < points; i++) {
            double angle = i * (Math.PI * 2.0D / points);
            Location point = center.clone().add(Math.cos(angle) * radius, 0.2D, Math.sin(angle) * radius);
            spawnAt(set, point, 1, 0, 0, 0);
        }
    }

    public void sound(String key, Location location) {
        sound(plugin.config().effect(key), location);
    }

    // ------------------------------------------------------------- iç kısım

    private void spawn(EffectSet set, Location location, int count) {
        if (!set.particleEnabled() || location == null || location.getWorld() == null) return;
        spawnAt(set, location.clone().add(0, 1, 0), count, set.offsetX(), set.offsetY(), set.offsetZ());
    }

    private void spawnAt(EffectSet set, Location location, int count, double offX, double offY, double offZ) {
        World world = location.getWorld();
        if (world == null) return;

        try {
            world.spawnParticle(set.particle(), location, count, offX, offY, offZ, set.extra(), data(set));
        } catch (Throwable throwable) {
            if (plugin.config().debug()) {
                plugin.getLogger().warning("Parçacık oynatılamadı (" + set.particle() + "): " + throwable.getMessage());
            }
        }
    }

    private void sound(EffectSet set, Location location) {
        if (!set.soundEnabled() || set.soundKey() == null || location == null || location.getWorld() == null) return;
        try {
            location.getWorld().playSound(location, set.soundKey(), SoundCategory.PLAYERS, set.volume(), set.pitch());
        } catch (Throwable throwable) {
            if (plugin.config().debug()) {
                plugin.getLogger().warning("Ses oynatılamadı (" + set.soundKey() + "): " + throwable.getMessage());
            }
        }
    }

    /**
     * Parçacık türü ek veri istiyorsa uygun nesneyi üretir.
     */
    private Object data(EffectSet set) {
        Class<?> type = set.particle().getDataType();
        if (type == Void.class) return null;

        if (type == Particle.DustOptions.class) {
            return new Particle.DustOptions(set.color() == null ? Color.WHITE : set.color(), Math.max(0.1F, set.size()));
        }
        if (type == Particle.DustTransition.class) {
            return new Particle.DustTransition(
                    set.color() == null ? Color.WHITE : set.color(),
                    Color.WHITE,
                    Math.max(0.1F, set.size()));
        }
        if (type == org.bukkit.block.data.BlockData.class) {
            return Material.STONE.createBlockData();
        }
        if (type == ItemStack.class) {
            return new ItemStack(Material.ENDER_PEARL);
        }
        if (type == Color.class) {
            return set.color() == null ? Color.WHITE : set.color();
        }
        if (type == Float.class) {
            return set.size();
        }
        if (type == Integer.class) {
            return 1;
        }
        return null;
    }
}
