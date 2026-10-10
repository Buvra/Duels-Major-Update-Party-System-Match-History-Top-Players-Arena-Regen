/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.event.queue;

import java.util.Objects;
import me.realized.duels.api.event.SourcedEvent;
import me.realized.duels.api.queue.DQueue;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class QueueEvent
extends SourcedEvent {
    private final DQueue queue;

    QueueEvent(@Nullable CommandSender source, @NotNull DQueue queue) {
        super(source);
        Objects.requireNonNull(queue, "queue");
        this.queue = queue;
    }

    @NotNull
    public DQueue getQueue() {
        return this.queue;
    }
}
