package com.yefeblgn.ultrartp.api;

import com.yefeblgn.ultrartp.model.Zone;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Oyuncu bir RTP bölgesinden (RTPZone) çıktığında çağrılır.
 */
public class RTPZoneLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Zone zone;

    public RTPZoneLeaveEvent(Player player, Zone zone) {
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
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
