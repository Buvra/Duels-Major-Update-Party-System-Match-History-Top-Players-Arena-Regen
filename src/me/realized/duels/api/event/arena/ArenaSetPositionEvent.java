/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.event.arena;

import java.util.Objects;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.event.arena.ArenaEvent;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArenaSetPositionEvent
extends ArenaEvent
implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private int pos;
    private Location location;
    private boolean cancelled;

    public ArenaSetPositionEvent(@Nullable CommandSender source, @NotNull Arena arena, int pos, @NotNull Location location) {
        super(source, arena);
        Objects.requireNonNull(location, "location");
        this.pos = pos;
        this.location = location;
    }

    public int getPos() {
        return this.pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
