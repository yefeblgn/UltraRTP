package com.yefeblgn.ultrartp.command;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.gui.MainMenu;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /rtp komutu.
 */
public final class RTPCommand implements CommandExecutor, TabCompleter {

    private final UltraRTP plugin;

    public RTPCommand(UltraRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("ultrartp.use")) {
            plugin.messages().send(player, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            if (plugin.config().guiOpenOnPlainCommand() && player.hasPermission("ultrartp.menu")) {
                new MainMenu(plugin, player).open();
            } else {
                plugin.teleports().request(player, defaultRegion(player));
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu", "gui", "menü" -> {
                if (!player.hasPermission("ultrartp.menu")) {
                    plugin.messages().send(player, "general.no-permission");
                    return true;
                }
                new MainMenu(plugin, player).open();
            }
            case "back", "geri", "dön", "don" -> {
                if (!player.hasPermission("ultrartp.back")) {
                    plugin.messages().send(player, "general.no-permission");
                    return true;
                }
                plugin.teleports().teleportBack(player);
            }
            case "cancel", "iptal" -> {
                if (!plugin.teleports().cancel(player, "warmup.cancelled")) {
                    plugin.messages().send(player, "warmup.cancelled");
                }
            }
            case "list", "liste", "bolgeler", "bölgeler" -> sendRegionList(player);
            case "help", "yardim", "yardım", "?" -> plugin.messages().sendList(player, "help");
            default -> {
                Region region = plugin.config().region(sub);
                if (region == null) {
                    plugin.messages().send(player, "region.not-found", Text.of("value", args[0]));
                    return true;
                }
                plugin.teleports().request(player, region);
            }
        }
        return true;
    }

    private void sendRegionList(Player player) {
        plugin.messages().send(player, "region.list-header");
        for (Region region : plugin.teleports().accessibleRegions(player)) {
            plugin.messages().send(player, "region.list-entry",
                    Text.of("value", region.id()),
                    Text.parsed("region", region.displayName()));
        }
    }

    private Region defaultRegion(Player player) {
        Region region = plugin.config().region(plugin.config().defaultRegionId());
        if (region != null && region.enabled() && plugin.teleports().hasAccess(player, region)) {
            return region;
        }
        List<Region> accessible = plugin.teleports().accessibleRegions(player);
        return accessible.isEmpty() ? null : accessible.get(0);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player player)) return out;

        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> options = new ArrayList<>(List.of("menu", "back", "iptal", "list", "help"));
            for (Region region : plugin.teleports().accessibleRegions(player)) {
                options.add(region.id());
            }
            for (String option : options) {
                if (option.startsWith(partial)) out.add(option);
            }
        }
        return out;
    }
}
