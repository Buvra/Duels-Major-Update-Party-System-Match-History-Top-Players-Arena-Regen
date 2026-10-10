/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.spectate;

import java.util.List;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.spectate.Spectator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SpectateManager {
    @Nullable
    public Spectator get(@NotNull Player var1);

    public boolean isSpectating(@NotNull Player var1);

    @NotNull
    public Result startSpectating(@NotNull Player var1, @NotNull Player var2);

    public void stopSpectating(@NotNull Player var1);

    @NotNull
    public List<Spectator> getSpectators(@NotNull Arena var1);

    public static enum Result {
        ALREADY_SPECTATING,
        IN_QUEUE,
        IN_MATCH,
        TARGET_NOT_IN_MATCH,
        EVENT_CANCELLED,
        SUCCESS;

    }
}
