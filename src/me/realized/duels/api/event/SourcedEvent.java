/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public abstract class SourcedEvent
extends Event {
    private final CommandSender source;

    protected SourcedEvent(@Nullable CommandSender source) {
        this.source = source;
    }

    @Nullable
    public CommandSender getSource() {
        return this.source;
    }

    public boolean hasSource() {
        return this.getSource() != null;
    }
}
