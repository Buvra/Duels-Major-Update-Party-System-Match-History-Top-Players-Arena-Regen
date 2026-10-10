/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api;

import me.realized.duels.api.arena.ArenaManager;
import me.realized.duels.api.command.SubCommand;
import me.realized.duels.api.kit.KitManager;
import me.realized.duels.api.queue.DQueueManager;
import me.realized.duels.api.queue.sign.QueueSignManager;
import me.realized.duels.api.spectate.SpectateManager;
import me.realized.duels.api.user.UserManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

public interface Duels
extends Plugin {
    @NotNull
    public UserManager getUserManager();

    @NotNull
    public ArenaManager getArenaManager();

    @NotNull
    public KitManager getKitManager();

    @NotNull
    public SpectateManager getSpectateManager();

    @NotNull
    public DQueueManager getQueueManager();

    @NotNull
    public QueueSignManager getQueueSignManager();

    public boolean registerSubCommand(@NotNull String var1, @NotNull SubCommand var2);

    public void registerListener(@NotNull Listener var1);

    public boolean reload();

    public BukkitTask doSync(@NotNull Runnable var1);

    public BukkitTask doSyncAfter(@NotNull Runnable var1, long var2);

    public BukkitTask doSyncRepeat(@NotNull Runnable var1, long var2, long var4);

    public BukkitTask doAsync(@NotNull Runnable var1);

    public BukkitTask doAsyncAfter(@NotNull Runnable var1, long var2);

    public BukkitTask doAsyncRepeat(@NotNull Runnable var1, long var2, long var4);

    public void cancelTask(@NotNull BukkitTask var1);

    public void cancelTask(int var1);

    public void info(@NotNull String var1);

    public void warn(@NotNull String var1);

    public void error(@NotNull String var1);

    public void error(@NotNull String var1, @NotNull Throwable var2);

    public String getVersion();
}
