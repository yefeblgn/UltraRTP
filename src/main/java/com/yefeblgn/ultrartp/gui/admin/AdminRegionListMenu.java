package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Bölge listesi - düzenlemek için bir bölgeye tıkla.
 */
public final class AdminRegionListMenu extends AdminMenu {

    public AdminRegionListMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return plugin.messages().get(player, "gui.admin-regions-title");
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        int slot = 10;
        for (Region region : plugin.config().regions()) {
            if (slot >= 35) break;
            if (slot % 9 == 8) slot += 2;

            boolean worldLoaded = Bukkit.getWorld(region.worldName()) != null;

            set(slot, ItemBuilder.icon(region.icon(), plugin.config().itemsAdderFallback(), plugin.itemsAdder())
                    .name(Text.item(region.displayName()))
                    .lore(ItemBuilder.LoreBuilder.create()
                            .add("<dark_gray>▪ <gray>Kimlik: <white><value>", Text.of("value", region.id()))
                            .add("<dark_gray>▪ <gray>Durum: <value>", Text.parsed("value",
                                    plugin.messages().rawString(region.enabled()
                                            ? "admin.gui.enabled" : "admin.gui.disabled")))
                            .add("<dark_gray>▪ <gray>Dünya: <white><value> <state>",
                                    Text.of("value", region.worldName()),
                                    Text.parsed("state", worldLoaded ? "" : "<red>(yüklü değil)"))
                            .add("<dark_gray>▪ <gray>Yarıçap: <white><value>",
                                    Text.of("value", region.minRadius() + " - " + region.maxRadius()))
                            .add("<dark_gray>▪ <gray>Şekil: <white><value>", Text.of("value", region.shape().name()))
                            .add("<dark_gray>▪ <gray>Merkez: <white><value>", Text.of("value", region.centerMode().name()))
                            .add("<dark_gray>▪ <gray>Depo: <aqua><value>", Text.num("value", plugin.cache().size(region.id())))
                            .blank()
                            .add("<yellow>» Tıkla: düzenle"))
                    .glow(region.enabled())
                    .hideAll()
                    .build(), event -> openLater(new AdminRegionEditMenu(plugin, player, region.id())));

            slot++;
        }

        set(4, ItemBuilder.of(Material.WRITABLE_BOOK)
                .name("<white><bold>Yeni Bölge</bold>")
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<gray>Yeni bölgeler <white>config.yml <gray>içinde")
                        .add("<gray>tanımlanır, ardından panelden düzenlenir.")
                        .blank()
                        .add("<gray>Toplam bölge: <aqua><value>",
                                Text.num("value", plugin.config().regions().size())))
                .hideAll()
                .build());

        backButton(36, new AdminMainMenu(plugin, player));
        closeButton(40);
        fillEmpty();
    }
}
