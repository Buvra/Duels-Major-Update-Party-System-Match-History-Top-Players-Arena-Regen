/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.request;

import me.realized.duels.api.event.request.RequestEvent;
import me.realized.duels.api.request.Request;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RequestSendEvent
extends RequestEvent
implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    public RequestSendEvent(@NotNull Player source, @NotNull Player target, @NotNull Request request) {
        super(source, target, request);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
