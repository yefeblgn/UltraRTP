package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Güvenli konum kontrollerinin ayarları.
 */
public final class AdminSafetyMenu extends AdminMenu {

    public AdminSafetyMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return plugin.messages().get(player, "gui.admin-safety-title");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        toggle(10, "safety.allow-water", "allow-water", Material.WATER_BUCKET);
        toggle(11, "safety.avoid-under-roof", "avoid-roof", Material.STONE);
        number(12, "safety.roof-scan-limit", "roof-scan", Material.LADDER, 5, 1, 320, true);
        number(13, "safety.required-air-above", "air-above", Material.GLASS, 1, 1, 10, true);
        number(14, "safety.danger-scan-radius", "danger-radius", Material.LAVA_BUCKET, 1, 0, 6, true);
        number(15, "safety.min-y", "min-y", Material.BEDROCK, 8, -64, 320, true);
        number(16, "safety.max-y", "max-y", Material.BEACON, 8, -64, 320, true);
        toggle(20, "safety.respect-world-border", "world-border", Material.STRUCTURE_VOID);

        set(24, ItemBuilder.of(Material.BARRIER)
                .name("<white><bold>Kara Listeler</bold>")
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Tehlikeli blok: <aqua><value>",
                                Text.num("value", plugin.config().unsafeBlocks().size()))
                        .add("<dark_gray>▪ <gray>Yasak zemin: <aqua><value>",
                                Text.num("value", plugin.config().unsafeGround().size()))
                        .add("<dark_gray>▪ <gray>Yasak biyom: <aqua><value>",
                                Text.num("value", plugin.config().blockedBiomes().size()))
                        .blank()
                        .add("<gray>Bu listeler <white>config.yml <gray>üzerinden düzenlenir."))
                .hideAll()
                .build());

        backButton(36, new AdminMainMenu(plugin, player));
        closeButton(40);
        fillEmpty();
    }
}
