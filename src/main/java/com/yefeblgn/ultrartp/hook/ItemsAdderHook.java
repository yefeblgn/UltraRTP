package com.yefeblgn.ultrartp.hook;

import com.yefeblgn.ultrartp.UltraRTP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * ItemsAdder entegrasyonu.
 * <p>
 * Derleme sırasında ItemsAdder'a bağımlılık oluşmasın diye tamamen reflection ile çalışır;
 * ItemsAdder kurulu değilse eklenti sorunsuz çalışmaya devam eder.
 * <p>
 * Config'te ikon olarak şu yazımlar kullanılabilir:
 * <pre>
 *   icon: "ia:namespace:item_id"
 *   icon: "itemsadder:namespace:item_id"
 *   icon: "namespace:item_id"     (geçerli bir Material değilse ItemsAdder denenir)
 * </pre>
 */
public final class ItemsAdderHook {

    private final UltraRTP plugin;

    private boolean available;
    private Method getInstance;
    private Method getItemStack;

    public ItemsAdderHook(UltraRTP plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        this.available = false;
        this.getInstance = null;
        this.getItemStack = null;

        if (!plugin.config().itemsAdderEnabled()) {
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            return;
        }

        try {
            Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
            this.getInstance = customStack.getMethod("getInstance", String.class);
            this.getItemStack = customStack.getMethod("getItemStack");
            this.available = true;
            plugin.getLogger().info("ItemsAdder bulundu - özel eşya ikonları etkin.");
        } catch (Throwable throwable) {
            plugin.getLogger().warning("ItemsAdder bulundu fakat API'ye erişilemedi: " + throwable.getMessage());
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * Verilen ikon tanımı ItemsAdder eşyası gibi mi görünüyor?
     */
    public static boolean looksCustom(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("ia:") || value.startsWith("itemsadder:")) return true;
        if (!value.contains(":")) return false;
        if (value.startsWith("minecraft:")) return false;
        // "namespace:id" biçiminde ve geçerli bir Material değilse özel eşya sayılır
        return Material.matchMaterial(raw.trim()) == null;
    }

    /**
     * ItemsAdder eşyasını çözümler. Bulunamazsa {@code null} döner.
     */
    public ItemStack resolve(String raw) {
        if (!available || raw == null || raw.isBlank()) return null;

        String id = raw.trim();
        String lower = id.toLowerCase(Locale.ROOT);
        if (lower.startsWith("ia:")) {
            id = id.substring(3);
        } else if (lower.startsWith("itemsadder:")) {
            id = id.substring(11);
        }
        if (!id.contains(":")) return null;

        try {
            Object custom = getInstance.invoke(null, id);
            if (custom == null) return null;
            Object item = getItemStack.invoke(custom);
            if (item instanceof ItemStack stack) {
                return stack.clone();
            }
        } catch (Throwable throwable) {
            if (plugin.config().debug()) {
                plugin.getLogger().warning("ItemsAdder eşyası çözümlenemedi (" + raw + "): " + throwable.getMessage());
            }
        }
        return null;
    }
}
