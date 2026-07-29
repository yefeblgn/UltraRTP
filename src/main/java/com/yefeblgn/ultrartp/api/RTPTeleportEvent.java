package com.yefeblgn.ultrartp.api;

import com.yefeblgn.ultrartp.model.Region;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Oyuncu başarıyla ışınlandıktan sonra çağrılır.
 */
public class RTPTeleportEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Region region;
    private final Location from;
    private final Location to;

    public RTPTeleportEvent(Player player, Region region, Location from, Location to) {
        this.player = player;
        this.region = region;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() {
        return player;
    }

    public Region getRegion() {
        return region;
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
