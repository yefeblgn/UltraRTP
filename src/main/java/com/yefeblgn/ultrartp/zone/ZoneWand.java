package com.yefeblgn.ultrartp.zone;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Bölge seçim çubuğu.
 * <p>
 * Sağ tık ➜ 1. köşe, sol tık ➜ 2. köşe.
 */
public final class ZoneWand {

    private static final String KEY = "zone_wand";

    private ZoneWand() {
    }

    public static NamespacedKey key(UltraRTP plugin) {
        return new NamespacedKey(plugin, KEY);
    }

    public static ItemStack create(UltraRTP plugin) {
        ItemStack item = ItemBuilder.of(plugin.config().zoneWandMaterial())
                .name(plugin.messages().item(null, "zone.wand-name"))
                .lore(plugin.messages().itemList(null, "zone.wand-lore"))
                .glow(true)
                .hideAll()
                .build();

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static boolean isWand(UltraRTP plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key(plugin), PersistentDataType.BYTE);
    }
}
