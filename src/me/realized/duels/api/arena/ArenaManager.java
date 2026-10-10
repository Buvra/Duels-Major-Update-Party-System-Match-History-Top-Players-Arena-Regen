/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.arena;

import java.util.List;
import me.realized.duels.api.arena.Arena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ArenaManager {
    @Nullable
    public Arena get(@NotNull String var1);

    @Nullable
    public Arena get(@NotNull Player var1);

    public boolean isInMatch(@NotNull Player var1);

    @NotNull
    public List<Arena> getArenas();
}
