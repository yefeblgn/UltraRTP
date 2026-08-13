package com.yefeblgn.ultrartp.zone;

import org.bukkit.Location;

/**
 * Bir oyuncunun çubukla yaptığı iki köşelik seçim.
 */
public final class ZoneSelection {

    private Location first;
    private Location second;

    public Location first() {
        return first;
    }

    public Location second() {
        return second;
    }

    public void first(Location location) {
        this.first = location == null ? null : location.clone();
    }

    public void second(Location location) {
        this.second = location == null ? null : location.clone();
    }

    public boolean complete() {
        return first != null && second != null
                && first.getWorld() != null && second.getWorld() != null;
    }

    /** İki köşe aynı dünyada mı. */
    public boolean sameWorld() {
        return complete() && first.getWorld().equals(second.getWorld());
    }

    public long area() {
        if (!complete()) return 0L;
        long width = Math.abs(first.getBlockX() - second.getBlockX()) + 1L;
        long depth = Math.abs(first.getBlockZ() - second.getBlockZ()) + 1L;
        return width * depth;
    }
}
