package me.realized.duels.queue;

import java.util.Objects;
import java.util.UUID;
import me.realized.duels.setting.CachedInfo;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Player nesnesi sızıntılarını önlemek için UUID bazlı çalışan QueueEntry.
 */
public class QueueEntry {

    private final UUID uuid;
    private final String name;
    private final CachedInfo info;

    QueueEntry(final Player player, final Location location, final String duelzone) {
        this.uuid = player.getUniqueId();
        this.name = player.getName();
        this.info = new CachedInfo(location, duelzone);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final QueueEntry that = (QueueEntry) other;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    @Nullable
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public CachedInfo getInfo() {
        return this.info;
    }
}
