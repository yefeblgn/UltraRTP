package com.yefeblgn.ultrartp.gui;

import com.yefeblgn.ultrartp.UltraRTP;
import com.yefeblgn.ultrartp.util.ItemBuilder;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tüm menülerin ortak temeli.
 * <p>
 * {@link InventoryHolder} olarak kendisini kullanır; böylece tıklama olaylarında
 * envanterin bu eklentiye ait olup olmadığı güvenle anlaşılır.
 */
public abstract class Menu implements InventoryHolder {

    protected final UltraRTP plugin;
    protected final Player player;

    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();
    private Inventory inventory;

    protected Menu(UltraRTP plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract Component title();

    protected abstract int rows();

    protected abstract void build();

    public void open() {
        this.inventory = Bukkit.createInventory(this, rows() * 9, title());
        rebuild();
        player.openInventory(inventory);
    }

    public void rebuild() {
        if (inventory == null) return;
        actions.clear();
        inventory.clear();
        build();
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (inventory == null || slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected void fillEmpty() {
        if (!plugin.config().guiFillerEnabled() || inventory == null) return;
        ItemStack filler = ItemBuilder.of(plugin.config().guiFillerMaterial())
                .name(Component.empty())
                .hideAll()
                .build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) {
            action.accept(event);
        }
    }

    /** Menü kapatıldığında çağrılır. */
    public void onClose() {
    }

    protected void close() {
        Bukkit.getScheduler().runTask(plugin, (Runnable) () -> player.closeInventory());
    }

    protected void openLater(Menu menu) {
        Bukkit.getScheduler().runTask(plugin, menu::open);
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            inventory = Bukkit.createInventory(this, rows() * 9, title());
        }
        return inventory;
    }
}
