/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.queue;

import java.util.List;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.api.queue.DQueue;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DQueueManager {
    @Nullable
    public DQueue get(@Nullable Kit var1, int var2);

    @Nullable
    public DQueue get(@NotNull Player var1);

    @Nullable
    public DQueue create(@Nullable CommandSender var1, @Nullable Kit var2, int var3);

    @Nullable
    public DQueue create(@Nullable Kit var1, int var2);

    @Nullable
    public DQueue remove(@Nullable CommandSender var1, @Nullable Kit var2, int var3);

    @Nullable
    public DQueue remove(@Nullable Kit var1, int var2);

    public boolean isInQueue(@NotNull Player var1);

    public boolean addToQueue(@NotNull Player var1, @NotNull DQueue var2);

    @Nullable
    public DQueue removeFromQueue(@NotNull Player var1);

    @NotNull
    public List<DQueue> getQueues();
}
