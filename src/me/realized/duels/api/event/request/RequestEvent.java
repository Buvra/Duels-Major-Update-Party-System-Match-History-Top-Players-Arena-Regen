/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.request;

import java.util.Objects;
import me.realized.duels.api.event.SourcedEvent;
import me.realized.duels.api.request.Request;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class RequestEvent
extends SourcedEvent {
    private final Player source;
    private final Player target;
    private final Request request;

    RequestEvent(@NotNull Player source, @NotNull Player target, @NotNull Request request) {
        super((CommandSender)source);
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(request, "request");
        this.source = source;
        this.target = target;
        this.request = request;
    }

    @NotNull
    public Player getSource() {
        return this.source;
    }

    @NotNull
    public Player getTarget() {
        return this.target;
    }

    @NotNull
    public Request getRequest() {
        return this.request;
    }
}
