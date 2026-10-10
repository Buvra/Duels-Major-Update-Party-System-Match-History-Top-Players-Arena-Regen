/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.event.arena;

import java.util.Objects;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.event.SourcedEvent;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ArenaEvent
extends SourcedEvent {
    private final Arena arena;

    ArenaEvent(@Nullable CommandSender source, @NotNull Arena arena) {
        super(source);
        Objects.requireNonNull(arena, "arena");
        this.arena = arena;
    }

    @NotNull
    public Arena getArena() {
        return this.arena;
    }
}
