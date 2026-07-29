package com.yefeblgn.ultrartp.gui.admin;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.gui.Menu;
import com.yefeblgn.ultrartp.util.Formatter;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import com.yefeblgn.ultrartp.util.Text;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Yönetim menüleri için ortak bileşenler: aç/kapa düğmeleri, sayı ayarlayıcılar,
 * geri düğmesi ve config yazma yardımcıları.
 */
public abstract class AdminMenu extends Menu {

    protected AdminMenu(UltraRTP plugin, Player player) {
        super(plugin, player);
    }

    // ------------------------------------------------------------- bileşenler

    /**
     * Boolean bir ayarı aç/kapa düğmesi olarak yerleştirir.
     */
    protected void toggle(int slot, String path, String nameKey, Material icon) {
        boolean value = plugin.config().raw().getBoolean(path);
        String name = plugin.messages().rawString("admin.gui." + nameKey);
        String state = plugin.messages().rawString(value ? "admin.gui.enabled" : "admin.gui.disabled");

        ItemStack item = ItemBuilder.of(icon)
                .name("<white><bold><name></bold>", Text.of("name", name))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Durum: <state>", Text.of("name", name), Text.parsed("state", state))
                        .blank()
                        .add(plugin.messages().rawString("admin.gui.toggle-hint")))
                .glow(value)
                .hideAll()
                .build();

        set(slot, item, event -> {
            plugin.config().setAndSave(path, !value);
            plugin.messages().send(player, "admin.value-set",
                    Text.of("value", name),
                    Text.of("count", String.valueOf(!value)));
            afterChange();
            rebuild();
        });
    }

    /**
     * Sayısal bir ayarı +/- düğmesi olarak yerleştirir.
     *
     * @param integer true ise tam sayı olarak saklanır
     */
    protected void number(int slot, String path, String nameKey, Material icon,
                          double step, double min, double max, boolean integer) {
        double value = plugin.config().raw().getDouble(path);
        String name = plugin.messages().rawString("admin.gui." + nameKey);
        String shown = integer ? String.valueOf((long) value) : Formatter.number(value);
        String stepText = integer ? String.valueOf((long) step) : Formatter.number(step);

        ItemStack item = ItemBuilder.of(icon)
                .name("<white><bold><name></bold>", Text.of("name", name))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Değer: <aqua><value>", Text.of("value", shown))
                        .blank()
                        .addAll(plugin.messages().itemList(player, "admin.gui.number-hint",
                                Text.of("value", stepText))))
                .hideAll()
                .build();

        set(slot, item, event -> {
            if (event.getClick() == ClickType.MIDDLE) {
                askNumber(path, name, min, max, integer);
                return;
            }

            double delta = step * (event.isShiftClick() ? 5 : 1);
            double updated = event.isRightClick() ? value - delta : value + delta;
            updated = Math.max(min, Math.min(max, updated));

            plugin.config().setAndSave(path, integer ? (Object) (int) Math.round(updated) : (Object) updated);
            plugin.messages().send(player, "admin.value-set",
                    Text.of("value", name),
                    Text.of("count", integer ? String.valueOf((long) updated) : Formatter.number(updated)));
            afterChange();
            rebuild();
        });
    }

    /**
     * Metin tabanlı bir ayarı sohbetten alır.
     */
    protected void textInput(int slot, String path, String nameKey, Material icon, Consumer<String> validator) {
        String value = plugin.config().raw().getString(path, "");
        String name = plugin.messages().rawString("admin.gui." + nameKey);

        ItemStack item = ItemBuilder.of(icon)
                .name("<white><bold><name></bold>", Text.of("name", name))
                .lore(ItemBuilder.LoreBuilder.create()
                        .add("<dark_gray>▪ <gray>Değer: <aqua><value>", Text.of("value", value))
                        .blank()
                        .add("<yellow>» Tıkla: sohbetten değiştir"))
                .hideAll()
                .build();

        set(slot, item, event -> plugin.chatInput().request(player, input -> {
            if (input != null) {
                if (validator != null) {
                    validator.accept(input);
                } else {
                    plugin.config().setAndSave(path, input);
                }
                plugin.messages().send(player, "admin.value-set",
                        Text.of("value", name), Text.of("count", input));
                afterChange();
            }
            openLater(this);
        }));
    }

    private void askNumber(String path, String name, double min, double max, boolean integer) {
        plugin.chatInput().request(player, input -> {
            if (input != null) {
                try {
                    double parsed = Double.parseDouble(input.replace(',', '.'));
                    parsed = Math.max(min, Math.min(max, parsed));
                    plugin.config().setAndSave(path, integer ? (Object) (int) Math.round(parsed) : (Object) parsed);
                    plugin.messages().send(player, "admin.value-set",
                            Text.of("value", name),
                            Text.of("count", integer ? String.valueOf((long) parsed) : Formatter.number(parsed)));
                    afterChange();
                } catch (NumberFormatException ex) {
                    plugin.messages().send(player, "general.invalid-number");
                }
            }
            openLater(this);
        });
    }

    /** Geri düğmesi. */
    protected void backButton(int slot, Menu target) {
        set(slot, ItemBuilder.of(Material.ARROW)
                .name(plugin.messages().item(player, "gui.back-name"))
                .hideAll()
                .build(), event -> openLater(target));
    }

    protected void closeButton(int slot) {
        set(slot, ItemBuilder.of(Material.BARRIER)
                .name(plugin.messages().item(player, "gui.close-name"))
                .hideAll()
                .build(), event -> close());
    }

    /** Ayar değiştikten sonra çalışacak ek işler (alt sınıflar geçersiz kılabilir). */
    protected void afterChange() {
        plugin.cache().restart();
    }

    protected void handled(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
