/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.queue.sign;

import me.realized.duels.api.queue.DQueue;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public interface QueueSign {
    @NotNull
    public Location getLocation();

    @NotNull
    public DQueue getQueue();

    public boolean isRemoved();
}
