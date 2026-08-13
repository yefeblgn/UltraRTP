package com.yefeblgn.ultrartp.api;

import com.yefeblgn.ultrartp.model.Zone;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Oyuncu bir RTP bölgesine (RTPZone) girdiğinde çağrılır.
 * İptal edilirse oyuncu bölge sayacına dahil edilmez.
 */
public class RTPZoneEnterEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Zone zone;
    private boolean cancelled;

    public RTPZoneEnterEvent(Player player, Zone zone) {
        this.player = player;
        this.zone = zone;
    }

    public Player getPlayer() {
        return player;
    }

    public Zone getZone() {
        return zone;
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
