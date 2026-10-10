/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.NotNull
 */
package me.realized.duels.api.user;

import java.util.List;
import java.util.UUID;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.api.user.MatchInfo;
import org.jetbrains.annotations.NotNull;

public interface User {
    @NotNull
    public UUID getUuid();

    @NotNull
    public String getName();

    public int getWins();

    public void setWins(int var1);

    public int getLosses();

    public void setLosses(int var1);

    public boolean canRequest();

    public void setRequests(boolean var1);

    @NotNull
    public List<MatchInfo> getMatches();

    public int getRating();

    public int getRating(@NotNull Kit var1);

    public void resetRating();

    public void resetRating(@NotNull Kit var1);

    public void reset();
}
