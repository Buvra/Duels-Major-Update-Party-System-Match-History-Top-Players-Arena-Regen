/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.kit;

import java.util.List;
import me.realized.duels.api.kit.Kit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface KitManager {
    @Nullable
    public Kit get(@NotNull String var1);

    @Nullable
    public Kit create(@NotNull Player var1, @NotNull String var2);

    @Nullable
    public Kit remove(@Nullable CommandSender var1, @NotNull String var2);

    @Nullable
    public Kit remove(@NotNull String var1);

    @NotNull
    public List<Kit> getKits();
}
