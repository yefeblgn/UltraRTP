package com.yefeblgn.ultrartp.gui;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Formatter;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Oyuncu ışınlanma menüsü: bölgeler, bilgi kartı, rastgele bölge ve geri dönüş.
 */
public final class MainMenu extends Menu {

    public MainMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected Component title() {
        return plugin.messages().get(player, "gui.main-title");
    }

    @Override
    protected int rows() {
        return plugin.config().guiRows();
    }

    @Override
    protected void build() {
        List<Region> regions = new ArrayList<>(plugin.config().regions());
        int autoSlot = 10;

        for (Region region : regions) {
            if (!region.enabled()) continue;

            int slot = region.slot();
            if (slot < 0 || slot >= rows() * 9) {
                slot = autoSlot++;
            }
            set(slot, regionItem(region), event -> {
                close();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.teleports().request(player, region));
            });
        }

        set(plugin.config().guiInfoSlot(), infoItem());

        set(plugin.config().guiRandomSlot(), randomItem(), event -> {
            List<Region> available = new ArrayList<>();
            for (Region region : plugin.teleports().accessibleRegions(player)) {
                if (Bukkit.getWorld(region.worldName()) != null) available.add(region);
            }
            if (available.isEmpty()) {
                plugin.messages().send(player, "region.none-available");
                return;
            }
            Region picked = available.get(ThreadLocalRandom.current().nextInt(available.size()));
            close();
            Bukkit.getScheduler().runTask(plugin, () -> plugin.teleports().request(player, picked));
        });

        if (plugin.config().backEnabled() && player.hasPermission("ultrartp.back")) {
            set(plugin.config().guiBackSlot(), returnItem(), event -> {
                close();
                Bukkit.getScheduler().runTask(plugin, () -> plugin.teleports().teleportBack(player));
            });
        }

        set(plugin.config().guiCloseSlot(), ItemBuilder
                .icon(plugin.config().guiIcon("close", "BARRIER"), Material.BARRIER, plugin.itemsAdder())
                .name(plugin.messages().item(player, "gui.close-name"))
                .hideAll()
                .build(), event -> close());

        fillEmpty();
    }

    private ItemStack regionItem(Region region) {
        boolean access = plugin.teleports().hasAccess(player, region);
        long cooldown = plugin.teleports().remainingCooldown(player, region);
        double cost = plugin.teleports().effectiveCost(player, region);
        World world = Bukkit.getWorld(region.worldName());

        ItemBuilder.LoreBuilder lore = ItemBuilder.LoreBuilder.create();
        for (String line : region.lore()) {
            lore.add(line);
        }
        lore.blank();
        lore.add(plugin.messages().item(player, "gui.world-lore",
                Text.of("world", world == null ? region.worldName() : world.getName())));
        lore.add(plugin.messages().item(player, "gui.radius-lore",
                Text.of("value", region.minRadius() + " - " + region.maxRadius())));

        if (cost > 0) {
            lore.add(plugin.messages().item(player, "gui.cost-lore",
                    Text.of("cost", plugin.economy().format(cost))));
        } else {
            lore.add(plugin.messages().item(player, "gui.free-lore"));
        }

        lore.blank();
        if (world == null) {
            lore.add(plugin.messages().item(player, "gui.disabled-lore"));
        } else if (!access) {
            lore.add(plugin.messages().item(player, "gui.no-permission-lore"));
        } else if (cooldown > 0) {
            lore.add(plugin.messages().item(player, "gui.cooldown-lore",
                    Text.of("time", Formatter.time(cooldown))));
        } else {
            lore.add(plugin.messages().item(player, "gui.click-lore"));
        }

        return ItemBuilder.icon(region.icon(), plugin.config().itemsAdderFallback(), plugin.itemsAdder())
                .name(Text.item(region.displayName()))
                .lore(lore)
                .glow(access && cooldown <= 0 && world != null)
                .hideAll()
                .build();
    }

    private ItemStack infoItem() {
        long cooldown = plugin.teleports().remainingCooldown(player,
                plugin.config().region(plugin.config().defaultRegionId()));

        return ItemBuilder
                .icon(plugin.config().guiIcon("info", "PLAYER_HEAD"), Material.PLAYER_HEAD, plugin.itemsAdder())
                .head(player)
                .name(plugin.messages().item(player, "gui.info-name"))
                .lore(plugin.messages().itemList(player, "gui.info-lore",
                        Text.of("player", player.getName()),
                        Text.of("balance", plugin.economy().format(plugin.economy().balance(player))),
                        Text.num("count", plugin.data().teleportCount(player.getUniqueId())),
                        Text.of("time", cooldown > 0
                                ? Formatter.time(cooldown)
                                : plugin.messages().rawString("time.now"))))
                .hideAll()
                .build();
    }

    private ItemStack randomItem() {
        return ItemBuilder
                .icon(plugin.config().guiIcon("random", "ENDER_EYE"), Material.ENDER_EYE, plugin.itemsAdder())
                .name(plugin.messages().item(player, "gui.random-name"))
                .lore(plugin.messages().itemList(player, "gui.random-lore"))
                .glow(true)
                .hideAll()
                .build();
    }

    private ItemStack returnItem() {
        return ItemBuilder
                .icon(plugin.config().guiIcon("back", "ARROW"), Material.ARROW, plugin.itemsAdder())
                .name(plugin.messages().item(player, "gui.return-name"))
                .lore(plugin.messages().itemList(player, "gui.return-lore",
                        Text.of("cost", plugin.economy().format(plugin.config().backCost()))))
                .hideAll()
                .build();
    }
}
