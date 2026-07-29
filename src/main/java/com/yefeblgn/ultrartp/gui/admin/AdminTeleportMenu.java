package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Bekleme süresi, ışınlanma gecikmesi, ücret ve depo ayarları.
 */
public final class AdminTeleportMenu extends AdminMenu {

    public AdminTeleportMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return plugin.messages().get(player, "gui.admin-teleport-title");
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        // Işınlanma gecikmesi
        toggle(10, "teleport.warmup.enabled", "warmup-enabled", Material.CLOCK);
        number(11, "teleport.warmup.seconds", "warmup-seconds", Material.REPEATER, 1, 1, 300, true);
        toggle(12, "teleport.warmup.cancel-on-move", "warmup-move", Material.LEATHER_BOOTS);
        toggle(13, "teleport.warmup.cancel-on-damage", "warmup-damage", Material.IRON_SWORD);
        number(15, "teleport.invulnerability-seconds", "invulnerability", Material.TOTEM_OF_UNDYING, 1, 0, 300, true);

        // Bekleme süresi
        toggle(19, "teleport.cooldown.enabled", "cooldown-enabled", Material.COMPARATOR);
        number(20, "teleport.cooldown.seconds", "cooldown-seconds", Material.CLOCK, 10, 0, 86400, true);
        toggle(21, "teleport.cooldown.shared", "cooldown-shared", Material.CHAIN);

        // Geri dönme
        toggle(24, "teleport.back.enabled", "back-enabled", Material.ENDER_PEARL);
        number(25, "teleport.back.cost", "back-cost", Material.ENDER_EYE, 25, 0, 1_000_000, false);

        // Ekonomi
        toggle(28, "teleport.cost.enabled", "cost-enabled", Material.GOLD_INGOT);
        number(29, "teleport.cost.amount", "cost-amount", Material.GOLD_BLOCK, 50, 0, 1_000_000, false);
        toggle(30, "teleport.cost.refund-on-fail", "cost-refund", Material.EMERALD);

        // Arama / depo
        number(32, "general.max-attempts", "max-attempts", Material.SPYGLASS, 5, 1, 500, true);
        toggle(33, "cache.enabled", "cache-enabled", Material.CHEST);
        number(34, "cache.size-per-region", "cache-size", Material.ENDER_CHEST, 1, 1, 100, true);

        backButton(45, new AdminMainMenu(plugin, player));
        closeButton(49);
        fillEmpty();
    }
}
