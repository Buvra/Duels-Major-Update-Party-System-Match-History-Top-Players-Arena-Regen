/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.kit;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface Kit {
    @NotNull
    public String getName();

    @NotNull
    public ItemStack getDisplayed();

    public boolean isUsePermission();

    public void setUsePermission(boolean var1);

    public boolean isArenaSpecific();

    public void setArenaSpecific(boolean var1);

    public boolean equip(@NotNull Player var1);

    public boolean isRemoved();
}
