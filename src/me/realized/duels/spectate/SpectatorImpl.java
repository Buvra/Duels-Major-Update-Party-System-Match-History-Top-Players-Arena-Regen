package me.realized.duels.spectate;

import java.util.Objects;
import java.util.UUID;
import me.realized.duels.api.spectate.Spectator;
import me.realized.duels.arena.ArenaImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SpectatorImpl implements Spectator {
    private final UUID uuid;
    private final UUID targetUuid;
    private final String targetName;
    private final ArenaImpl arena;

    SpectatorImpl(final Player owner, final Player target, final ArenaImpl arena) {
        this.uuid = owner.getUniqueId();
        this.targetUuid = target.getUniqueId();
        this.targetName = target.getName();
        this.arena = arena;
    }

    @Nullable
    @Override
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    @Nullable
    @Override
    public Player getTarget() {
        return Bukkit.getPlayer(targetUuid);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final SpectatorImpl spectator = (SpectatorImpl) other;
        return Objects.equals(uuid, spectator.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public UUID getTargetUuid() {
        return this.targetUuid;
    }

    public String getTargetName() {
        return this.targetName;
    }

    public ArenaImpl getArena() {
        return this.arena;
    }
}
