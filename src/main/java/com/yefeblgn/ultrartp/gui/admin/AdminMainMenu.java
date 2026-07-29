package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Yönetim panelinin ana ekranı.
 */
public final class AdminMainMenu extends AdminMenu {

    public AdminMainMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return plugin.messages().get(player, "gui.admin-title");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        set(10, ItemBuilder.of(Material.CLOCK)
                .name(plugin.messages().item(player, "admin.gui.teleport-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add(plugin.messages().item(player, "admin.gui.teleport-lore")))
                .hideAll()
                .build(), event -> openLater(new AdminTeleportMenu(plugin, player)));

        set(12, ItemBuilder.of(Material.SHIELD)
                .name(plugin.messages().item(player, "admin.gui.safety-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add(plugin.messages().item(player, "admin.gui.safety-lore")))
                .hideAll()
                .build(), event -> openLater(new AdminSafetyMenu(plugin, player)));

        set(14, ItemBuilder.of(Material.FIREWORK_ROCKET)
                .name(plugin.messages().item(player, "admin.gui.effects-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add(plugin.messages().item(player, "admin.gui.effects-lore")))
                .hideAll()
                .build(), event -> openLater(new AdminEffectsMenu(plugin, player)));

        set(16, ItemBuilder.of(Material.FILLED_MAP)
                .name(plugin.messages().item(player, "admin.gui.regions-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add(plugin.messages().item(player, "admin.gui.regions-lore")))
                .hideAll()
                .build(), event -> openLater(new AdminRegionListMenu(plugin, player)));

        set(29, ItemBuilder.of(Material.ENDER_CHEST)
                .name(plugin.messages().item(player, "admin.gui.cache-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Hazır konum: <aqua><value>",
                                Text.num("value", plugin.cache().totalSize()))
                        .add("<dark_gray>▪ <gray>Detay: <white><value>",
                                Text.of("value", plugin.cache().describe()))
                        .blank()
                        .add(plugin.messages().item(player, "admin.gui.cache-lore")))
                .hideAll()
                .build(), event -> {
            if (event.isRightClick()) {
                plugin.cache().clear();
                plugin.messages().send(player, "admin.cache-cleared");
            } else {
                plugin.cache().forceRefill();
                plugin.messages().send(player, "admin.cache-refilling");
            }
            rebuild();
        });

        set(31, ItemBuilder.of(Material.BOOK)
                .name(plugin.messages().item(player, "admin.gui.stats-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Toplam ışınlanma: <aqua><value>",
                                Text.num("value", plugin.data().totalTeleports()))
                        .add("<dark_gray>▪ <gray>Aktif bölge: <aqua><value>",
                                Text.num("value", plugin.config().regions().stream().filter(r -> r.enabled()).count()))
                        .add("<dark_gray>▪ <gray>Dil: <aqua><value>",
                                Text.of("value", plugin.config().language()))
                        .blank()
                        .add("<dark_gray>▪ <gray>Vault: <value>",
                                Text.parsed("value", status(plugin.economy().isAvailable())))
                        .add("<dark_gray>▪ <gray>PlaceholderAPI: <value>",
                                Text.parsed("value", status(plugin.placeholders().isAvailable())))
                        .add("<dark_gray>▪ <gray>ItemsAdder: <value>",
                                Text.parsed("value", status(plugin.itemsAdder().isAvailable()))))
                .hideAll()
                .build());

        set(33, ItemBuilder.of(Material.REDSTONE)
                .name(plugin.messages().item(player, "admin.gui.reload-name"))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add(plugin.messages().item(player, "admin.gui.reload-lore")))
                .glow(true)
                .hideAll()
                .build(), event -> {
            long start = System.currentTimeMillis();
            plugin.reloadEverything();
            plugin.messages().send(player, "general.reload-success",
                    Text.num("value", System.currentTimeMillis() - start));
            openLater(new AdminMainMenu(plugin, player));
        });

        closeButton(40);
        fillEmpty();
    }

    private String status(boolean value) {
        return plugin.messages().rawString(value ? "admin.gui.enabled" : "admin.gui.disabled");
    }

    @Override
    protected void afterChange() {
        // ana ekranda değişiklik yok
    }
}
