package com.yefeblgn.ultrartp.command;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.model.Region;
import com.yefeblgn.ultrartp.model.Zone;
import com.yefeblgn.ultrartp.util.Formatter;
import com.yefeblgn.ultrartp.util.Text;
import com.yefeblgn.ultrartp.zone.ZoneSelection;
import com.yefeblgn.ultrartp.zone.ZoneWand;
import org.bukkit.Bukkit;
import org.bukkit.World;
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
 * /rtpzone komutu - çubukla bölge seçimi ve otomatik ışınlanma bölgeleri.
 */
public final class RTPZoneCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "çubuk", "oluştur", "liste", "kaldır", "düzenle", "bilgi", "ışınla", "yardım");

    private static final List<String> SETTINGS = List.of(
            "dünya", "cooldown", "aktif", "isim", "min-yarıçap", "max-yarıçap", "şekil", "yükseklik", "seçim");

    private final UltraRTP plugin;

    public RTPZoneCommand(UltraRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("ultrartp.zone")) {
            plugin.messages().send(sender, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                giveWand(player);
            } else {
                plugin.messages().sendList(sender, "help-zone");
            }
            return true;
        }

        switch (normalize(args[0])) {
            case "cubuk", "wand", "sopa" -> {
                if (sender instanceof Player player) {
                    giveWand(player);
                } else {
                    plugin.messages().send(sender, "general.player-only");
                }
            }
            case "olustur", "create", "ekle", "add" -> handleCreate(sender, args);
            case "liste", "list" -> handleList(sender);
            case "kaldir", "sil", "remove", "delete" -> handleRemove(sender, args);
            case "duzenle", "edit", "ayarla", "set" -> handleEdit(sender, args);
            case "bilgi", "info" -> handleInfo(sender, args);
            case "isinla", "tetikle", "tp", "trigger" -> handleTrigger(sender, args);
            default -> plugin.messages().sendList(sender, "help-zone");
        }
        return true;
    }

    // ------------------------------------------------------------ çubuk

    private void giveWand(Player player) {
        player.getInventory().addItem(ZoneWand.create(plugin));
        plugin.messages().send(player, "zone.wand-given");
    }

    // ------------------------------------------------------------ oluştur

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.messages().send(sender, "general.player-only");
            return;
        }
        if (args.length < 4) {
            plugin.messages().send(sender, "zone.create-usage");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_-]{1,32}")) {
            plugin.messages().send(sender, "zone.invalid-name", Text.of("value", args[1]));
            return;
        }
        if (plugin.zones().zone(id) != null) {
            plugin.messages().send(sender, "zone.exists", Text.of("value", id));
            return;
        }

        World target = Bukkit.getWorld(args[2]);
        if (target == null) {
            plugin.messages().send(sender, "zone.world-missing", Text.of("value", args[2]));
            return;
        }

        Integer interval = parseInt(args[3]);
        if (interval == null || interval < 1) {
            plugin.messages().send(sender, "general.invalid-number");
            return;
        }

        ZoneSelection selection = plugin.zones().peekSelection(player.getUniqueId());
        if (selection == null || !selection.complete()) {
            plugin.messages().send(sender, "zone.selection-missing");
            return;
        }
        if (!selection.sameWorld()) {
            plugin.messages().send(sender, "zone.selection-different-world");
            return;
        }

        Zone zone = Zone.create(id, selection.first(), selection.second(), target.getName(), interval);
        plugin.zones().add(zone);
        plugin.zones().clearSelection(player.getUniqueId());

        plugin.messages().send(sender, "zone.created",
                Text.of("value", zone.id()),
                Text.of("world", zone.targetWorld()),
                Text.num("time", zone.interval()),
                Text.num("count", zone.area()));
    }

    // ------------------------------------------------------------ liste / bilgi

    private void handleList(CommandSender sender) {
        if (plugin.zones().zones().isEmpty()) {
            plugin.messages().send(sender, "zone.list-empty");
            return;
        }

        plugin.messages().send(sender, "zone.list-header",
                Text.num("count", plugin.zones().zones().size()));

        for (Zone zone : plugin.zones().zones()) {
            plugin.messages().send(sender, "zone.list-entry",
                    Text.of("value", zone.id()),
                    Text.parsed("zone", zone.displayName()),
                    Text.of("world", zone.targetWorld()),
                    Text.num("time", zone.interval()),
                    Text.parsed("status", statusText(zone)));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Zone zone = requireZone(sender, args);
        if (zone == null) return;

        int remaining = plugin.zones().remaining(zone);
        plugin.messages().sendList(sender, "zone.info",
                Text.of("value", zone.id()),
                Text.parsed("zone", zone.displayName()),
                Text.parsed("status", statusText(zone)),
                Text.of("world", zone.worldName()),
                Text.of("target", zone.targetWorld()),
                Text.num("time", zone.interval()),
                Text.of("remaining", remaining < 0 ? "-" : Formatter.time(remaining)),
                Text.of("bounds", zone.describeBounds()),
                Text.num("count", zone.area()),
                Text.num("min", zone.minRadius()),
                Text.num("max", zone.maxRadius()),
                Text.of("shape", zone.shape().name()),
                Text.parsed("height", zone.fullHeight()
                        ? plugin.messages().rawString("admin.gui.enabled")
                        : plugin.messages().rawString("admin.gui.disabled")));
    }

    private String statusText(Zone zone) {
        return plugin.messages().rawString(zone.enabled() ? "admin.gui.enabled" : "admin.gui.disabled");
    }

    // ------------------------------------------------------------ kaldır

    private void handleRemove(CommandSender sender, String[] args) {
        Zone zone = requireZone(sender, args);
        if (zone == null) return;

        plugin.zones().remove(zone.id());
        plugin.messages().send(sender, "zone.removed", Text.of("value", zone.id()));
    }

    // ------------------------------------------------------------ düzenle

    private void handleEdit(CommandSender sender, String[] args) {
        Zone zone = requireZone(sender, args);
        if (zone == null) return;

        if (args.length < 4 && !normalize(args.length > 2 ? args[2] : "").equals("secim")) {
            plugin.messages().send(sender, "zone.edit-usage");
            return;
        }

        String setting = normalize(args[2]);
        String value = args.length > 3 ? args[3] : "";

        switch (setting) {
            case "dunya", "world", "hedef" -> {
                World world = Bukkit.getWorld(value);
                if (world == null) {
                    plugin.messages().send(sender, "zone.world-missing", Text.of("value", value));
                    return;
                }
                zone.targetWorld(world.getName());
                done(sender, zone, "dünya", world.getName());
            }
            case "cooldown", "sure", "interval" -> {
                Integer seconds = parseInt(value);
                if (seconds == null || seconds < 1) {
                    plugin.messages().send(sender, "general.invalid-number");
                    return;
                }
                zone.interval(seconds);
                done(sender, zone, "cooldown", seconds + "sn");
            }
            case "aktif", "enabled", "durum" -> {
                boolean enabled = parseBoolean(value, zone.enabled());
                zone.enabled(enabled);
                done(sender, zone, "aktif", String.valueOf(enabled));
            }
            case "isim", "ad", "name" -> {
                String name = String.join(" ", List.of(args).subList(3, args.length));
                zone.displayName(name);
                done(sender, zone, "isim", name);
            }
            case "min-yaricap", "min-radius", "minyaricap" -> {
                Integer radius = parseInt(value);
                if (radius == null || radius < 0) {
                    plugin.messages().send(sender, "general.invalid-number");
                    return;
                }
                zone.minRadius(radius);
                done(sender, zone, "min-yarıçap", String.valueOf(zone.minRadius()));
            }
            case "max-yaricap", "max-radius", "maxyaricap" -> {
                Integer radius = parseInt(value);
                if (radius == null || radius < 1) {
                    plugin.messages().send(sender, "general.invalid-number");
                    return;
                }
                zone.maxRadius(radius);
                done(sender, zone, "max-yarıçap", String.valueOf(zone.maxRadius()));
            }
            case "sekil", "shape" -> {
                Region.Shape shape;
                try {
                    shape = Region.Shape.valueOf(value.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    plugin.messages().send(sender, "zone.invalid-value", Text.of("value", value));
                    return;
                }
                zone.shape(shape);
                done(sender, zone, "şekil", shape.name());
            }
            case "yukseklik", "height", "fullheight" -> {
                boolean full = parseBoolean(value, zone.fullHeight());
                zone.fullHeight(full);
                done(sender, zone, "yükseklik", String.valueOf(full));
            }
            case "secim", "selection", "alan" -> {
                if (!(sender instanceof Player player)) {
                    plugin.messages().send(sender, "general.player-only");
                    return;
                }
                ZoneSelection selection = plugin.zones().peekSelection(player.getUniqueId());
                if (selection == null || !selection.complete()) {
                    plugin.messages().send(sender, "zone.selection-missing");
                    return;
                }
                if (!selection.sameWorld()) {
                    plugin.messages().send(sender, "zone.selection-different-world");
                    return;
                }
                zone.applySelection(selection.first(), selection.second());
                done(sender, zone, "seçim", zone.describeBounds());
            }
            default -> plugin.messages().send(sender, "zone.unknown-setting", Text.of("value", args[2]));
        }
    }

    private void done(CommandSender sender, Zone zone, String setting, String value) {
        plugin.zones().update(zone);
        plugin.messages().send(sender, "zone.edited",
                Text.of("value", zone.id()),
                Text.of("setting", setting),
                Text.of("result", value));
    }

    // ------------------------------------------------------------ tetikle

    private void handleTrigger(CommandSender sender, String[] args) {
        Zone zone = requireZone(sender, args);
        if (zone == null) return;

        int count = plugin.zones().trigger(zone);
        if (count == 0) {
            plugin.messages().send(sender, "zone.empty", Text.of("value", zone.id()));
            return;
        }
        plugin.messages().send(sender, "zone.triggered",
                Text.of("value", zone.id()),
                Text.num("count", count));
    }

    // ------------------------------------------------------------ yardımcı

    private Zone requireZone(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.messages().sendList(sender, "help-zone");
            return null;
        }
        Zone zone = plugin.zones().zone(args[1]);
        if (zone == null) {
            plugin.messages().send(sender, "zone.not-found", Text.of("value", args[1]));
            return null;
        }
        return zone;
    }

    private static Integer parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean parseBoolean(String value, boolean current) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "evet", "aç", "ac", "açık", "acik", "1", "on" -> true;
            case "false", "hayır", "hayir", "kapat", "kapalı", "kapali", "0", "off" -> false;
            default -> !current;
        };
    }

    /** Türkçe karakterleri sadeleştirir, böylece "düzenle" ve "duzenle" aynı komuttur. */
    private static String normalize(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
                .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
                .replace("İ", "i");
    }

    // ------------------------------------------------------------ tab tamamlama

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("ultrartp.zone")) return out;

        if (args.length == 1) {
            complete(out, args[0], SUBCOMMANDS);
            return out;
        }

        String sub = normalize(args[0]);

        if (args.length == 2) {
            switch (sub) {
                case "olustur", "create", "ekle", "add" -> complete(out, args[1], List.of("<bölge_ismi>"));
                case "kaldir", "sil", "remove", "delete", "duzenle", "edit", "ayarla", "set",
                     "bilgi", "info", "isinla", "tetikle", "tp", "trigger" -> complete(out, args[1], zoneIds());
                default -> {
                }
            }
            return out;
        }

        if (args.length == 3) {
            switch (sub) {
                case "olustur", "create", "ekle", "add" -> complete(out, args[2], worldNames());
                case "duzenle", "edit", "ayarla", "set" -> complete(out, args[2], SETTINGS);
                default -> {
                }
            }
            return out;
        }

        if (args.length == 4) {
            switch (sub) {
                case "olustur", "create", "ekle", "add" ->
                        complete(out, args[3], List.of("30", "60", "120", "300", "600"));
                case "duzenle", "edit", "ayarla", "set" -> complete(out, args[3], settingValues(args[2]));
                default -> {
                }
            }
        }
        return out;
    }

    private List<String> settingValues(String setting) {
        return switch (normalize(setting)) {
            case "dunya", "world", "hedef" -> worldNames();
            case "cooldown", "sure", "interval" -> List.of("30", "60", "120", "300", "600");
            case "aktif", "enabled", "durum", "yukseklik", "height", "fullheight" -> List.of("true", "false");
            case "sekil", "shape" -> List.of("SQUARE", "CIRCLE");
            case "min-yaricap", "min-radius", "minyaricap" -> List.of("0", "250", "500", "1000");
            case "max-yaricap", "max-radius", "maxyaricap" -> List.of("2500", "5000", "7500", "10000");
            case "isim", "ad", "name" -> List.of("<görünen_isim>");
            default -> List.of();
        };
    }

    private List<String> worldNames() {
        List<String> names = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) names.add(world.getName());
        return names;
    }

    private List<String> zoneIds() {
        List<String> ids = new ArrayList<>();
        for (Zone zone : plugin.zones().zones()) ids.add(zone.id());
        return ids;
    }

    private void complete(List<String> out, String partial, List<String> options) {
        String lower = normalize(partial);
        for (String option : options) {
            if (normalize(option).startsWith(lower)) out.add(option);
        }
    }
}
