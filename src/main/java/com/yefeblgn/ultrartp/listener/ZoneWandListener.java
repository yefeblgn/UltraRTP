package com.yefeblgn.ultrartp.listener;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.Text;
import com.yefeblgn.ultrartp.zone.ZoneSelection;
import com.yefeblgn.ultrartp.zone.ZoneWand;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Seçim çubuğu: sağ tık ➜ 1. köşe, sol tık ➜ 2. köşe.
 */
public final class ZoneWandListener implements Listener {

    private final UltraRTP plugin;

    public ZoneWandListener(UltraRTP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!ZoneWand.isWand(plugin, event.getItem())) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("ultrartp.zone")) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_BLOCK) return;

        event.setCancelled(true);

        ZoneSelection selection = plugin.zones().selection(player.getUniqueId());
        Location location = block.getLocation();

        if (action == Action.RIGHT_CLICK_BLOCK) {
            selection.first(location);
            plugin.messages().send(player, "zone.pos1-set", corner(location));
        } else {
            selection.second(location);
            plugin.messages().send(player, "zone.pos2-set", corner(location));
        }

        plugin.effects().zoneWarning(player, 5);

        if (selection.complete()) {
            if (!selection.sameWorld()) {
                plugin.messages().send(player, "zone.selection-different-world");
            } else {
                plugin.messages().send(player, "zone.selection-ready",
                        Text.num("count", selection.area()));
            }
        }
    }

    /** Yaratıcı moddaki sol tık blok kırmasın. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!ZoneWand.isWand(plugin, event.getPlayer().getInventory().getItemInMainHand())) return;
        if (!event.getPlayer().hasPermission("ultrartp.zone")) return;
        event.setCancelled(true);
    }

    private TagResolver[] corner(Location location) {
        return new TagResolver[]{
                Text.num("x", location.getBlockX()),
                Text.num("y", location.getBlockY()),
                Text.num("z", location.getBlockZ()),
                Text.of("world", location.getWorld() == null ? "" : location.getWorld().getName())
        };
    }
}
