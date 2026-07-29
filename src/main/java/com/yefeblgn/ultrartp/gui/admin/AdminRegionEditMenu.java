package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Tek bir bölgenin ayarları.
 */
public final class AdminRegionEditMenu extends AdminMenu {

    private static final List<String> SHAPES = List.of("SQUARE", "CIRCLE");
    private static final List<String> CENTERS = List.of("WORLD_SPAWN", "FIXED", "PLAYER");

    private final String regionId;

    public AdminRegionEditMenu(UltraRTP plugin, Player player, String regionId) {
        super(plugin, player);
        this.regionId = regionId;
    }

    private Region region() {
        return plugin.config().region(regionId);
    }

    private String path(String key) {
        return "regions." + regionId + "." + key;
    }

    @Override
    protected Component title() {
        Region region = region();
        return plugin.messages().get(player, "gui.admin-region-title",
                Text.parsed("region", region == null ? regionId : region.displayName()));
    }

    @Override
    protected int rows() {
        return 5;
    }

    @Override
    protected void build() {
        Region region = region();
        if (region == null) {
            openLater(new AdminRegionListMenu(plugin, player));
            return;
        }

        toggle(10, path("enabled"), "region-enabled", Material.LEVER);
        number(11, path("min-radius"), "region-min-radius", Material.IRON_NUGGET, 100, 0, 30_000_000, true);
        number(12, path("max-radius"), "region-max-radius", Material.IRON_INGOT, 250, 1, 30_000_000, true);
        number(13, path("cost"), "region-cost", Material.GOLD_INGOT, 50, -1, 1_000_000, false);
        number(14, path("cooldown"), "region-cooldown", Material.CLOCK, 15, -1, 86_400, true);

        textInput(15, path("world"), "region-world", Material.GRASS_BLOCK, input -> {
            if (Bukkit.getWorld(input) == null) {
                plugin.messages().send(player, "region.world-missing", Text.of("value", input));
                return;
            }
            plugin.config().setAndSave(path("world"), input);
        });

        textInput(16, path("display-name"), "region-name", Material.NAME_TAG, null);

        set(19, cycleItem("Şekil", region.shape().name(), Material.MAP), event -> {
            int index = SHAPES.indexOf(region.shape().name());
            String next = SHAPES.get((index + 1) % SHAPES.size());
            plugin.config().setAndSave(path("shape"), next);
            afterChange();
            rebuild();
        });

        set(20, cycleItem("Merkez", region.centerMode().name(), Material.COMPASS), event -> {
            int index = CENTERS.indexOf(region.centerMode().name());
            String next = CENTERS.get((index + 1) % CENTERS.size());
            plugin.config().setAndSave(path("center"), next);
            afterChange();
            rebuild();
        });

        set(21, ItemBuilder.icon(region.icon(), plugin.config().itemsAdderFallback(), plugin.itemsAdder())
                .name("<white><bold>İkon</bold>")
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Değer: <aqua><value>", Text.of("value", region.icon()))
                        .blank()
                        .add("<gray>Material adı ya da ItemsAdder id")
                        .add("<gray>örn. <white>ia:mypack:portal_orb")
                        .blank()
                        .add("<yellow>» Tıkla: sohbetten değiştir"))
                .hideAll()
                .build(), event -> plugin.chatInput().request(player, input -> {
            if (input != null) {
                plugin.config().setAndSave(path("icon"), input);
                plugin.messages().send(player, "admin.value-set",
                        Text.of("value", "icon"), Text.of("count", input));
            }
            openLater(this);
        }));

        set(23, ItemBuilder.of(Material.ENDER_EYE)
                .name("<white><bold><name></bold>",
                        Text.of("name", plugin.messages().rawString("admin.gui.region-teleport")))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<gray>Bu bölgeye kontrolleri atlayarak")
                        .add("<gray>test amaçlı ışınlanırsın.")
                        .blank()
                        .add("<yellow>» Tıkla: ışınlan"))
                .hideAll()
                .build(), event -> {
            close();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.teleports().forceTeleport(player, region()));
        });

        set(24, ItemBuilder.of(Material.ENDER_CHEST)
                .name("<white><bold>Bölge Deposu</bold>")
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Hazır konum: <aqua><value>",
                                Text.num("value", plugin.cache().size(region.id())))
                        .add("<dark_gray>▪ <gray>Önbelleklenebilir: <white><value>",
                                Text.of("value", String.valueOf(region.cacheable())))
                        .blank()
                        .add("<yellow>» Tıkla: depoyu doldur"))
                .hideAll()
                .build(), event -> {
            plugin.cache().forceRefill();
            plugin.messages().send(player, "admin.cache-refilling");
            rebuild();
        });

        set(25, ItemBuilder.of(Material.PAPER)
                .name("<white><bold>Yetki</bold>")
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Değer: <aqua><value>",
                                Text.of("value", region.permission() == null ? "-" : region.permission()))
                        .blank()
                        .add("<gray>Boş bırakmak için <white>- <gray>yaz.")
                        .add("<yellow>» Tıkla: sohbetten değiştir"))
                .hideAll()
                .build(), event -> plugin.chatInput().request(player, input -> {
            if (input != null) {
                String value = input.equals("-") ? "" : input.toLowerCase(Locale.ROOT);
                plugin.config().setAndSave(path("permission"), value);
                plugin.messages().send(player, "admin.value-set",
                        Text.of("value", "permission"), Text.of("count", value.isEmpty() ? "-" : value));
            }
            openLater(this);
        }));

        backButton(36, new AdminRegionListMenu(plugin, player));
        closeButton(40);
        fillEmpty();
    }

    private org.bukkit.inventory.ItemStack cycleItem(String label, String value, Material icon) {
        return ItemBuilder.of(icon)
                .name("<white><bold><name></bold>", Text.of("name", label))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Değer: <aqua><value>", Text.of("value", value))
                        .blank()
                        .add("<yellow>» Tıkla: sıradaki"))
                .hideAll()
                .build();
    }
}
