/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Event
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.match;

import java.util.Objects;
import me.realized.duels.api.match.Match;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class MatchEvent
extends Event {
    private final Match match;

    MatchEvent(@NotNull Match match) {
        Objects.requireNonNull(match, "match");
        this.match = match;
    }

    public Match getMatch() {
        return this.match;
    }
}
