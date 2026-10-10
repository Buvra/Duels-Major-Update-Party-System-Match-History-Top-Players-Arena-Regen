package me.realized.duels.party;

import java.util.Set;
import java.util.UUID;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.kit.KitImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 2v2 - 5v5 katı eşitlik ve lider denetimli parti düellosu maçı.
 */
public class PartyDuelMatch extends PartyMatch {

    public PartyDuelMatch(
            @NotNull final UUID leaderA,
            @NotNull final UUID leaderB,
            @NotNull final Set<UUID> membersA,
            @NotNull final Set<UUID> membersB,
            @NotNull final ArenaImpl arena,
            @Nullable final KitImpl kit
    ) {
        super(leaderA, leaderB, membersA, membersB, arena, kit);
    }
}
