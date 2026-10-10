package me.realized.duels.setting;

import org.bukkit.Location;

public class CachedInfo {
    private Location location;
    private String duelzone;

    public CachedInfo(final Location location, final String duelzone) {
        this.location = location;
        this.duelzone = duelzone;
    }

    CachedInfo() {
        this(null, null);
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }

    public String getDuelzone() {
        return this.duelzone;
    }

    public void setDuelzone(final String duelzone) {
        this.duelzone = duelzone;
    }
}
