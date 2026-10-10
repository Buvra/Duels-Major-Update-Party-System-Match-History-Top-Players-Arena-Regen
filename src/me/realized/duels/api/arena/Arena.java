/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.arena;

import me.realized.duels.api.match.Match;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Arena {
    @NotNull
    public String getName();

    public boolean isDisabled();

    public boolean setDisabled(@Nullable CommandSender var1, boolean var2);

    public boolean setDisabled(boolean var1);

    @Nullable
    public Location getPosition(int var1);

    public boolean setPosition(@Nullable Player var1, int var2, @NotNull Location var3);

    public boolean setPosition(int var1, @NotNull Location var2);

    public boolean isUsed();

    @Nullable
    public Match getMatch();

    public boolean has(@NotNull Player var1);

    public boolean isRemoved();
}
