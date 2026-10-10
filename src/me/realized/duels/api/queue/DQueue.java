/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.queue;

import java.util.List;
import me.realized.duels.api.kit.Kit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DQueue {
    @Nullable
    public Kit getKit();

    public int getBet();

    public boolean isInQueue(@NotNull Player var1);

    @NotNull
    public List<Player> getQueuedPlayers();

    public boolean isRemoved();
}
