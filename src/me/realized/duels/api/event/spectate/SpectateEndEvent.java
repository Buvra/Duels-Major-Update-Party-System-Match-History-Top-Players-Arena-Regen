/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.spectate;

import me.realized.duels.api.event.spectate.SpectateEvent;
import me.realized.duels.api.spectate.Spectator;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SpectateEndEvent
extends SpectateEvent {
    private static final HandlerList handlers = new HandlerList();

    public SpectateEndEvent(@NotNull Player source, @NotNull Spectator spectator) {
        super(source, spectator);
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
