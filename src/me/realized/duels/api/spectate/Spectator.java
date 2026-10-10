/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.spectate;

import java.util.UUID;
import me.realized.duels.api.arena.Arena;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Spectator {
    @NotNull
    public UUID getUuid();

    @Nullable
    public Player getPlayer();

    @NotNull
    public UUID getTargetUuid();

    @Nullable
    public Player getTarget();

    @NotNull
    public Arena getArena();
}
