package com.yefeblgn.ultrartp.command;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.gui.admin.AdminMainMenu;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.util.Text;
import org.bukkit.Bukkit;
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
 * /rtpadmin komutu.
 */
public final class RTPAdminCommand implements CommandExecutor, TabCompleter {

    private final UltraRTP plugin;

    public RTPAdminCommand(UltraRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("ultrartp.admin")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.messages().sendList(sender, "help-admin");
                return true;
            }
            plugin.messages().send(player, "admin.panel-open");
            new AdminMainMenu(plugin, player).open();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload", "yenile" -> {
                long start = System.currentTimeMillis();
                try {
                    plugin.reloadEverything();
                    plugin.messages().send(sender, "general.reload-success",
                            Text.num("value", System.currentTimeMillis() - start));
                } catch (Exception ex) {
                    plugin.messages().send(sender, "general.reload-failed", Text.of("value", String.valueOf(ex.getMessage())));
                    plugin.getLogger().severe("Yeniden yükleme hatası: " + ex.getMessage());
                }
            }
            case "cache", "depo" -> handleCache(sender, args);
            case "cooldown", "bekleme" -> handleCooldown(sender, args);
            case "tp", "isinla", "ışınla" -> handleTeleport(sender, args);
            case "panel" -> {
                if (sender instanceof Player player) {
                    new AdminMainMenu(plugin, player).open();
                } else {
                    plugin.messages().send(sender, "general.player-only");
                }
            }
            default -> plugin.messages().sendList(sender, "help-admin");
        }
        return true;
    }

    private void handleCache(CommandSender sender, String[] args) {
        String action = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "info";
        switch (action) {
            case "clear", "temizle" -> {
                plugin.cache().clear();
                plugin.messages().send(sender, "admin.cache-cleared");
            }
            case "refill", "doldur" -> {
                plugin.cache().forceRefill();
                plugin.messages().send(sender, "admin.cache-refilling");
            }
            default -> plugin.messages().send(sender, "admin.cache-info",
                    Text.of("value", plugin.cache().describe()));
        }
    }

    private void handleCooldown(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("reset")) {
            plugin.messages().sendList(sender, "help-admin");
            return;
        }

        if (args[2].equals("*")) {
            int count = plugin.data().clearAllCooldowns();
            plugin.messages().send(sender, "cooldown.reset-all", Text.num("count", count));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            plugin.messages().send(sender, "general.player-not-found", Text.of("value", args[2]));
            return;
        }
        plugin.data().clearCooldowns(target.getUniqueId());
        plugin.messages().send(sender, "cooldown.reset", Text.of("player", target.getName()));
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendList(sender, "help-admin");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.messages().send(sender, "general.player-not-found", Text.of("value", args[1]));
            return;
        }

        Region region;
        if (args.length > 2) {
            region = plugin.config().region(args[2]);
            if (region == null) {
                plugin.messages().send(sender, "region.not-found", Text.of("value", args[2]));
                return;
            }
        } else {
            region = plugin.config().region(plugin.config().defaultRegionId());
            if (region == null) {
                plugin.messages().send(sender, "region.none-available");
                return;
            }
        }

        plugin.teleports().forceTeleport(target, region);
        plugin.messages().send(sender, "teleport.other", Text.of("player", target.getName()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("ultrartp.admin")) return out;

        if (args.length == 1) {
            complete(out, args[0], List.of("panel", "reload", "cache", "cooldown", "tp"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "cache" -> complete(out, args[1], List.of("info", "clear", "refill"));
                case "cooldown" -> complete(out, args[1], List.of("reset"));
                case "tp" -> {
                    List<String> names = new ArrayList<>();
                    for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
                    complete(out, args[1], names);
                }
                default -> {
                }
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "cooldown" -> {
                    List<String> names = new ArrayList<>();
                    names.add("*");
                    for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
                    complete(out, args[2], names);
                }
                case "tp" -> {
                    List<String> regions = new ArrayList<>();
                    for (Region region : plugin.config().regions()) regions.add(region.id());
                    complete(out, args[2], regions);
                }
                default -> {
                }
            }
        }
        return out;
    }

    private void complete(List<String> out, String partial, List<String> options) {
        String lower = partial.toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(option);
        }
    }
}
