/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.kit;

import java.util.Objects;
import me.realized.duels.api.event.kit.KitEvent;
import me.realized.duels.api.kit.Kit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class KitEquipEvent
extends KitEvent
implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player source;
    private boolean cancelled;

    public KitEquipEvent(@NotNull Player source, @NotNull Kit kit) {
        super((CommandSender)source, kit);
        Objects.requireNonNull(source, "source");
        this.source = source;
    }

    @NotNull
    public Player getSource() {
        return this.source;
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
