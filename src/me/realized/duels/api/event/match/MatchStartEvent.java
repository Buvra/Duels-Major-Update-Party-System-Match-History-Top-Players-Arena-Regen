/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.event.match;

import java.util.Objects;
import me.realized.duels.api.event.match.MatchEvent;
import me.realized.duels.api.match.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MatchStartEvent
extends MatchEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Player[] players;

    public MatchStartEvent(@NotNull Match match, Player ... players) {
        super(match);
        Objects.requireNonNull(players, "players");
        this.players = players;
    }

    @NotNull
    public Player[] getPlayers() {
        return this.players;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public HandlerList getHandlers() {
        return handlers;
    }
}
