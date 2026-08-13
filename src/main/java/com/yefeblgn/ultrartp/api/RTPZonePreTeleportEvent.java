package com.yefeblgn.ultrartp.api;

import com.yefeblgn.ultrartp.model.Zone;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bir oyuncu RTP bölgesi (RTPZone) tarafından ışınlanmadan hemen önce çağrılır.
 * İptal edilirse oyuncu o ışınlanmadan muaf tutulur.
 */
public class RTPZonePreTeleportEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Zone zone;
    private Location targetLocation;
    private boolean cancelled;

    public RTPZonePreTeleportEvent(Player player, Zone zone, Location targetLocation) {
        this.player = player;
        this.zone = zone;
        this.targetLocation = targetLocation;
    }

    public Player getPlayer() {
        return player;
    }

    public Zone getZone() {
        return zone;
    }

    public Location getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(Location targetLocation) {
        this.targetLocation = targetLocation;
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
