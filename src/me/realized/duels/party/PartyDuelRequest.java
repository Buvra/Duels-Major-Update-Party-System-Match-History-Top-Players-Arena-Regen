package me.realized.duels.party;

import java.util.UUID;
import me.realized.duels.kit.KitImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * İki parti arasındaki bekleyen düello teklifini temsil eder.
 */
public class PartyDuelRequest {

    private static final long EXPIRE_MILLIS = 30000L; // 30 saniye geçerlilik süresi

    private final UUID senderLeader;
    private final UUID targetLeader;
    private final KitImpl kit;
    private final int bet;
    private final long createdAt;

    public PartyDuelRequest(@NotNull final UUID senderLeader, @NotNull final UUID targetLeader, @Nullable final KitImpl kit, final int bet) {
        this.senderLeader = senderLeader;
        this.targetLeader = targetLeader;
        this.kit = kit;
        this.bet = Math.max(0, bet);
        this.createdAt = System.currentTimeMillis();
    }

    public PartyDuelRequest(@NotNull final UUID senderLeader, @NotNull final UUID targetLeader, @Nullable final KitImpl kit) {
        this(senderLeader, targetLeader, kit, 0);
    }

    public UUID getSenderLeader() {
        return senderLeader;
    }

    public UUID getTargetLeader() {
        return targetLeader;
    }

    @Nullable
    public KitImpl getKit() {
        return kit;
    }

    public int getBet() {
        return bet;
    }

    public boolean isExpired() {
        return (System.currentTimeMillis() - createdAt) > EXPIRE_MILLIS;
    }
}
