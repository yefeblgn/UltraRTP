package com.yefeblgn.ultrartp.api;

import com.yefeblgn.ultrartp.model.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Oyuncu ışınlanma isteği gönderdiğinde, tüm kontroller geçildikten hemen sonra çağrılır.
 * İptal edilirse ışınlanma başlamaz (para çekilmez).
 */
public class RTPPreTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Region region;
    private boolean cancelled;

    public RTPPreTeleportEvent(Player player, Region region) {
        this.player = player;
        this.region = region;
    }

    public Player getPlayer() {
        return player;
    }

    public Region getRegion() {
        return region;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
