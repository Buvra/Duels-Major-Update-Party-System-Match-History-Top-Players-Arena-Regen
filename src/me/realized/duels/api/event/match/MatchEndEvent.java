/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.event.match;

import java.util.Objects;
import java.util.UUID;
import me.realized.duels.api.event.match.MatchEvent;
import me.realized.duels.api.match.Match;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MatchEndEvent
extends MatchEvent {
    private static final HandlerList handlers = new HandlerList();
    private final UUID winner;
    private final UUID loser;
    private final Reason reason;

    public MatchEndEvent(@NotNull Match match, @Nullable UUID winner, @Nullable UUID loser, @NotNull Reason reason) {
        super(match);
        Objects.requireNonNull(reason, "reason");
        this.winner = winner;
        this.loser = loser;
        this.reason = reason;
    }

    @Nullable
    public UUID getWinner() {
        return this.winner;
    }

    @Nullable
    public UUID getLoser() {
        return this.loser;
    }

    @NotNull
    public Reason getReason() {
        return this.reason;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }

    public static enum Reason {
        OPPONENT_DEFEAT,
        TIE,
        MAX_TIME_REACHED,
        PLUGIN_DISABLE,
        OTHER;

    }
}
