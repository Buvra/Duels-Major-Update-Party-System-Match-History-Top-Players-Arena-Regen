/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.queue.sign;

import java.util.Objects;
import me.realized.duels.api.event.SourcedEvent;
import me.realized.duels.api.queue.sign.QueueSign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class QueueSignEvent
extends SourcedEvent {
    private final Player source;
    private final QueueSign queueSign;

    QueueSignEvent(@NotNull Player source, @NotNull QueueSign queueSign) {
        super((CommandSender)source);
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(queueSign, "queueSign");
        this.source = source;
        this.queueSign = queueSign;
    }

    @NotNull
    public Player getSource() {
        return this.source;
    }

    @NotNull
    public QueueSign getQueueSign() {
        return this.queueSign;
    }
}
