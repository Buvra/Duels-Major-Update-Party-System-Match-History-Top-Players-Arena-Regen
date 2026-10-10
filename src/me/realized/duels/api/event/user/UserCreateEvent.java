/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.user;

import java.util.Objects;
import me.realized.duels.api.user.User;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class UserCreateEvent
extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final User user;

    public UserCreateEvent(@NotNull User user) {
        Objects.requireNonNull(user, "user");
        this.user = user;
    }

    @NotNull
    public User getUser() {
        return this.user;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
