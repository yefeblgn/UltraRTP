package com.yefeblgn.ultrartp.util;

import com.yefeblgn.ultrartp.hook.ItemsAdderHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Kısa ve zincirlenebilir eşya oluşturucu.
 */
public final class ItemBuilder {

    private final ItemStack item;

    private ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material == null ? Material.STONE : material));
    }

    public static ItemBuilder of(ItemStack stack) {
        return new ItemBuilder(stack == null ? new ItemStack(Material.STONE) : stack.clone());
    }

    /**
     * Config'ten gelen ikon tanımını çözümler.
     * Önce ItemsAdder, sonra Material denenir.
     */
    public static ItemBuilder icon(String spec, Material fallback, ItemsAdderHook hook) {
        if (spec != null && !spec.isBlank() && hook != null && ItemsAdderHook.looksCustom(spec)) {
            ItemStack custom = hook.resolve(spec);
            if (custom != null) {
                return new ItemBuilder(custom);
            }
        }
        return of(Registries.material(spec, fallback));
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder name(Component component) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(component);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder name(String miniMessage, TagResolver... resolvers) {
        return name(Text.item(miniMessage, resolvers));
    }

    public ItemBuilder lore(List<Component> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lines);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder lore(LoreBuilder builder) {
        return lore(builder.build());
    }

    public ItemBuilder glow(boolean glow) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setEnchantmentGlintOverride(glow ? Boolean.TRUE : null);
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder head(OfflinePlayer owner) {
        if (item.getItemMeta() instanceof SkullMeta skull && owner != null) {
            skull.setOwningPlayer(owner);
            item.setItemMeta(skull);
        }
        return this;
    }

    public ItemBuilder hideAll() {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
            item.setItemMeta(meta);
        }
        return this;
    }

    public ItemStack build() {
        return item;
    }

    /**
     * Satır satır lore oluşturmak için küçük yardımcı.
     */
    public static final class LoreBuilder {
        private final List<Component> lines = new ArrayList<>();

        public static LoreBuilder create() {
            return new LoreBuilder();
        }

        public LoreBuilder add(String miniMessage, TagResolver... resolvers) {
            lines.add(Text.item(miniMessage, resolvers));
            return this;
        }

        public LoreBuilder add(Component component) {
            lines.add(component.colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            return this;
        }

        public LoreBuilder addAll(List<Component> components) {
            lines.addAll(components);
            return this;
        }

        public LoreBuilder addIf(boolean condition, String miniMessage, TagResolver... resolvers) {
            if (condition) add(miniMessage, resolvers);
            return this;
        }

        public LoreBuilder blank() {
            lines.add(Component.empty());
            return this;
        }

        public List<Component> build() {
            return lines;
        }
    }
}
