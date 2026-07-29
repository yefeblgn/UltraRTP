package com.yefeblgn.ultrartp.listener;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.teleport.WarmupSession;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Bekleme (warmup) sırasında hareket / hasar kontrolü ve çıkış temizliği.
 */
public final class PlayerListener implements Listener {

    private final UltraRTP plugin;

    public PlayerListener(UltraRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.config().cancelOnMove()) return;

        Player player = event.getPlayer();
        WarmupSession session = plugin.teleports().session(player.getUniqueId());
        if (session == null) return;

        Location origin = session.origin();
        Location to = event.getTo();
        if (origin == null || to == null) return;

        if (origin.getWorld() == null || to.getWorld() == null
                || !origin.getWorld().equals(to.getWorld())) {
            plugin.teleports().cancel(player, "warmup.cancelled-move");
            return;
        }

        double threshold = plugin.config().moveThreshold();
        if (to.distanceSquared(origin) > threshold * threshold) {
            plugin.teleports().cancel(player, "warmup.cancelled-move");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.config().cancelOnDamage()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (!plugin.teleports().isWarmingUp(player.getUniqueId())) return;

        plugin.teleports().cancel(player, "warmup.cancelled-damage");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.teleports().cancel(player, null);
        plugin.teleports().clearInvulnerability(player);
        plugin.chatInput().cancel(player.getUniqueId());
    }
}
