/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.command.CommandSender
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.command;

import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SubCommand {
    private final String name;
    private final String usage;
    private final String description;
    private final String permission;
    private final boolean playerOnly;
    private final int length;
    private final String[] aliases;

    public SubCommand(@NotNull String name, @Nullable String usage, @Nullable String description, @Nullable String permission, boolean playerOnly, int length, String ... aliases) {
        Objects.requireNonNull(name, "name");
        this.name = name;
        this.usage = usage;
        this.description = description;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.length = Math.max(length, 1);
        this.aliases = aliases;
    }

    public String getName() {
        return this.name;
    }

    public String getUsage() {
        return this.usage;
    }

    public String getDescription() {
        return this.description;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean isPlayerOnly() {
        return this.playerOnly;
    }

    public int getLength() {
        return this.length;
    }

    public String[] getAliases() {
        return this.aliases;
    }

    public abstract void execute(CommandSender var1, String var2, String[] var3);
}
