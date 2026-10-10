/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.spectate;

import java.util.Objects;
import me.realized.duels.api.event.SourcedEvent;
import me.realized.duels.api.spectate.Spectator;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public abstract class SpectateEvent
extends SourcedEvent {
    private final Player source;
    private final Spectator spectator;

    SpectateEvent(@NotNull Player source, @NotNull Spectator spectator) {
        super((CommandSender)source);
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(spectator, "spectator");
        this.source = source;
        this.spectator = spectator;
    }

    @NotNull
    public Player getSource() {
        return this.source;
    }

    @NotNull
    public Spectator getSpectator() {
        return this.spectator;
    }
}
