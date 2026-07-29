package com.yefeblgn.ultrartp.listener;

import com.yefeblgn.ultrartp.gui.Menu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/**
 * Eklentiye ait menülerin tıklama olaylarını yönlendirir ve eşya taşımayı engeller.
 */
public final class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Menu menu)) return;

        // Menü açıkken hiçbir eşya hareketine izin verme
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || !(clicked.getHolder() instanceof Menu)) return;

        menu.handleClick(event);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Menu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Menu menu) {
            menu.onClose();
        }
    }
}
