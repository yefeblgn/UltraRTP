package com.yefeblgn.ultrartp.hook;

import com.yefeblgn.ultrartp.UltraRTP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI entegrasyonu.
 * <p>
 * Eklenti yoksa metinler dokunulmadan geri döner; hiçbir sınıf yüklenmez.
 */
public final class PlaceholderHook {

    private final UltraRTP plugin;

    private boolean available;
    private RTPExpansion expansion;

    public PlaceholderHook(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        shutdown();
        this.available = false;

        if (!plugin.config().placeholderApiEnabled()) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return;
        }

        try {
            this.expansion = new RTPExpansion(plugin);
            if (expansion.register()) {
                this.available = true;
                plugin.getLogger().info("PlaceholderAPI bağlandı - %ultrartp_...% kullanılabilir.");
            } else {
                plugin.getLogger().warning("PlaceholderAPI genişletmesi kaydedilemedi.");
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("PlaceholderAPI entegrasyonu başarısız: " + throwable.getMessage());
        }
    }

    public void shutdown() {
        if (expansion != null) {
            try {
                expansion.unregister();
            } catch (Throwable ignored) {
                // yoksay
            }
            expansion = null;
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Metindeki %placeholder% ifadelerini çözer.
     */
    public String parse(Player player, String input) {
        if (!available || input == null || input.indexOf('%') < 0) {
            return input;
        }
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, input);
        } catch (Throwable throwable) {
            return input;
        }
    }
}
