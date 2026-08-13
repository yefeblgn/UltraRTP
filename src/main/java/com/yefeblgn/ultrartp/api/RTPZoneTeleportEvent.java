package com.yefeblgn.ultrartp.api;

import com.yefeblgn.ultrartp.model.Zone;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Oyuncu bir RTP bölgesi (RTPZone) tarafından başarıyla ışınlandığında çağrılır.
 */
public class RTPZoneTeleportEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Zone zone;
    private final Location from;
    private final Location to;

    public RTPZoneTeleportEvent(Player player, Zone zone, Location from, Location to) {
        this.player = player;
        this.zone = zone;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() {
        return player;
    }

    public Zone getZone() {
        return zone;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
