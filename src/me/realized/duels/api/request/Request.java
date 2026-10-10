/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.request;

import java.util.UUID;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.kit.Kit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Request {
    @NotNull
    public UUID getSender();

    @NotNull
    public UUID getTarget();

    @Nullable
    public Kit getKit();

    @Nullable
    public Arena getArena();

    public boolean canBetItems();

    public int getBet();
}
