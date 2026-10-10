/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.block.Sign
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.queue.sign;

import java.util.List;
import me.realized.duels.api.queue.sign.QueueSign;
import org.bukkit.block.Sign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface QueueSignManager {
    @Nullable
    public QueueSign get(@NotNull Sign var1);

    @NotNull
    public List<QueueSign> getQueueSigns();
}
