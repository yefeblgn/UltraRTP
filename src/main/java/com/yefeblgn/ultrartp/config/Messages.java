package com.yefeblgn.ultrartp.config;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * lang/&lt;dil&gt;.yml dosyalarını yönetir.
 * <p>
 * Eksik anahtarlar jar içindeki varsayılan dosyadan tamamlanır, böylece
 * güncellemelerde dil dosyası bozulmaz.
 */
public final class Messages {

    private static final String[] BUNDLED = {"tr", "en"};

    private final UltraRTP plugin;
    private YamlConfiguration lang;
    private String prefix = "";

    public Messages(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists() && !langFolder.mkdirs()) {
            plugin.getLogger().warning("lang klasörü oluşturulamadı!");
        }

        // Paketteki dilleri diske çıkar (varsa dokunma)
        for (String bundled : BUNDLED) {
            File target = new File(langFolder, bundled + ".yml");
            if (!target.exists()) {
                copyResource("lang/" + bundled + ".yml", target);
            }
        }

        String selected = plugin.config().language();
        File file = new File(langFolder, selected + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("Dil dosyası bulunamadı: lang/" + selected + ".yml -> 'en' kullanılıyor.");
            file = new File(langFolder, "en.yml");
            if (!file.exists()) {
                copyResource("lang/en.yml", file);
            }
        }

        this.lang = YamlConfiguration.loadConfiguration(file);

        // Eksik anahtarlar için jar içindeki aynı dili (yoksa en) varsayılan yap
        InputStream defaults = plugin.getResource("lang/" + selected + ".yml");
        if (defaults == null) defaults = plugin.getResource("lang/en.yml");
        if (defaults != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaults, StandardCharsets.UTF_8));
            lang.setDefaults(defConfig);
            lang.options().copyDefaults(true);
            try {
                lang.save(file);
            } catch (IOException ignored) {
            }
        }

        this.prefix = rawString("prefix");
    }

    private void copyResource(String resource, File target) {
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                plugin.getLogger().warning("Kaynak bulunamadı: " + resource);
                return;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Klasör oluşturulamadı: " + parent.getPath());
            }
            Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            plugin.getLogger().severe(resource + " kopyalanamadı: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------- okuma

    public String rawString(String path) {
        String value = lang.getString(path);
        if (value == null && lang.getDefaults() != null) {
            value = lang.getDefaults().getString(path);
        }
        return value != null ? value : path;
    }

    public List<String> rawList(String path) {
        List<String> list = lang.getStringList(path);
        if (list.isEmpty() && lang.getDefaults() != null) {
            list = lang.getDefaults().getStringList(path);
        }
        if (list.isEmpty()) {
            String single = rawString(path);
            if (!single.equals(path)) {
                List<String> fallback = new ArrayList<>();
                fallback.add(single);
                return fallback;
            }
        }
        return list;
    }

    /** Placeholder'ları çözerek Component üretir. */
    public Component get(CommandSender viewer, String path, TagResolver... resolvers) {
        return Text.mm(applyExternal(viewer, rawString(path)), withPrefix(resolvers));
    }

    public Component get(String path, TagResolver... resolvers) {
        return get(null, path, resolvers);
    }

    /** Eşya ismi/lore için (italik kapalı). */
    public Component item(CommandSender viewer, String path, TagResolver... resolvers) {
        return Text.item(applyExternal(viewer, rawString(path)), withPrefix(resolvers));
    }

    public List<Component> itemList(CommandSender viewer, String path, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        TagResolver[] all = withPrefix(resolvers);
        for (String line : rawList(path)) {
            out.add(Text.item(applyExternal(viewer, line), all));
        }
        return out;
    }

    public List<Component> list(CommandSender viewer, String path, TagResolver... resolvers) {
        List<Component> out = new ArrayList<>();
        TagResolver[] all = withPrefix(resolvers);
        for (String line : rawList(path)) {
            out.add(Text.mm(applyExternal(viewer, line), all));
        }
        return out;
    }

    // ------------------------------------------------------------- gönderme

    public void send(CommandSender target, String path, TagResolver... resolvers) {
        if (target == null) return;
        String raw = rawString(path);
        if (raw == null || raw.isBlank() || raw.equals(path)) return;
        target.sendMessage(get(target, path, resolvers));
    }

    public void sendList(CommandSender target, String path, TagResolver... resolvers) {
        if (target == null) return;
        for (Component line : list(target, path, resolvers)) {
            target.sendMessage(line);
        }
    }

    public void actionBar(Player player, String path, TagResolver... resolvers) {
        if (player == null) return;
        player.sendActionBar(get(player, path, resolvers));
    }

    private TagResolver[] withPrefix(TagResolver[] resolvers) {
        TagResolver[] all = new TagResolver[resolvers.length + 1];
        System.arraycopy(resolvers, 0, all, 0, resolvers.length);
        all[resolvers.length] = Text.parsed("prefix", prefix);
        return all;
    }

    /** PlaceholderAPI aktifse diğer eklentilerin placeholder'larını çözer. */
    private String applyExternal(CommandSender viewer, String input) {
        if (input == null) return "";
        if (!plugin.config().placeholderParseMessages()) return input;
        if (input.indexOf('%') < 0) return input;
        Player player = viewer instanceof Player p ? p : null;
        return plugin.placeholders().parse(player, input);
    }

    public String prefix() {
        return prefix;
    }

    public String languageName() {
        return plugin.config().language().toLowerCase(Locale.ROOT);
    }
}
