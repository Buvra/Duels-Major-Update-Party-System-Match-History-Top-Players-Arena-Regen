/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.match;

import java.util.List;
import java.util.Set;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.kit.Kit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Match {
    @NotNull
    public Arena getArena();

    public long getStart();

    @Nullable
    public Kit getKit();

    @NotNull
    public List<ItemStack> getItems(@NotNull Player var1);

    public int getBet();

    public boolean isFinished();

    @NotNull
    public Set<Player> getPlayers();

    @NotNull
    public Set<Player> getStartingPlayers();
}
