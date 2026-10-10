package me.realized.duels.arena.regen;

import org.bukkit.Location;

public class Selection {

    private Location first;
    private Location second;

    public boolean isSelected() {
        return first != null && second != null && first.getWorld() != null && first.getWorld().equals(second.getWorld());
    }

    public Location getFirst() {
        return first;
    }

    public void setFirst(final Location first) {
        this.first = first;
    }

    public Location getSecond() {
        return second;
    }

    public void setSecond(final Location second) {
        this.second = second;
    }
}
