/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package me.realized.duels.api.user;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MatchInfo {
    @NotNull
    public String getWinner();

    @NotNull
    public String getLoser();

    @Nullable
    public String getKit();

    public long getCreation();

    public long getDuration();

    public double getHealth();
}
