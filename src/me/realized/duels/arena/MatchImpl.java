package me.realized.duels.arena;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import me.realized.duels.api.match.Match;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.queue.Queue;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 500 CCU altında bellek sızıntısını (memory leak) ve GC yükünü önlemek için
 * Player yerine doğrudan UUID kullanan yüksek performanslı Match uygulaması.
 */
public class MatchImpl implements Match {

    private final ArenaImpl arena;
    private final long start;
    private final KitImpl kit;
    private final Map<UUID, List<ItemStack>> items;
    private final int bet;
    private final Queue source;
    private volatile boolean finished;
    private final List<ItemStack> wonItems = new java.util.concurrent.CopyOnWriteArrayList<ItemStack>();

    // UUID eşleştirmesi: false = hayatta, true = elenmiş/ölmüş
    private final Map<UUID, Boolean> players = new ConcurrentHashMap<UUID, Boolean>();

    MatchImpl(final ArenaImpl arena, final KitImpl kit, final Map<UUID, List<ItemStack>> items, final int bet, final Queue source) {
        this.arena = arena;
        this.start = System.currentTimeMillis();
        this.kit = kit;
        this.items = items;
        this.bet = bet;
        this.source = source;
    }

    public Map<UUID, Boolean> getPlayerMap() {
        return players;
    }

    public void add(@NotNull final UUID uuid) {
        players.put(uuid, false);
    }

    public void remove(@NotNull final UUID uuid) {
        if (players.containsKey(uuid)) {
            players.put(uuid, true);
        }
    }

    public Set<UUID> getPlayerUuids() {
        return players.keySet();
    }

    public Set<Player> getAlivePlayers() {
        final Set<Player> alive = new HashSet<Player>();
        for (Map.Entry<UUID, Boolean> entry : players.entrySet()) {
            if (!entry.getValue()) {
                final Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null && p.isOnline()) {
                    alive.add(p);
                }
            }
        }
        return alive;
    }

    public int getAliveCount() {
        int count = 0;
        for (Boolean dead : players.values()) {
            if (!dead) {
                count++;
            }
        }
        return count;
    }

    public Set<Player> getAllPlayers() {
        final Set<Player> all = new HashSet<Player>();
        for (UUID uuid : players.keySet()) {
            final Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                all.add(p);
            }
        }
        return all;
    }

    public boolean isDead(@NotNull final UUID uuid) {
        return players.getOrDefault(uuid, true);
    }

    public boolean isDead(@NotNull final Player player) {
        return isDead(player.getUniqueId());
    }

    public boolean isOwnInventory() {
        return kit == null;
    }

    public boolean isFromQueue() {
        return source != null;
    }

    public List<ItemStack> getItems() {
        return items != null ? items.values().stream().flatMap(Collection::stream).collect(Collectors.toList()) : Collections.emptyList();
    }

    public List<ItemStack> getWonItems() {
        return wonItems;
    }

    void setFinished() {
        finished = true;
    }

    public long getDurationInMillis() {
        return System.currentTimeMillis() - start;
    }

    @NotNull
    @Override
    public List<ItemStack> getItems(@NotNull final Player player) {
        Objects.requireNonNull(player, "player");
        if (this.items == null) {
            return Collections.emptyList();
        }
        final List<ItemStack> items = this.items.get(player.getUniqueId());
        return items != null ? items : Collections.emptyList();
    }

    @NotNull
    @Override
    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(getAlivePlayers());
    }

    @NotNull
    @Override
    public Set<Player> getStartingPlayers() {
        return Collections.unmodifiableSet(getAllPlayers());
    }

    public ArenaImpl getArena() {
        return this.arena;
    }

    public long getStart() {
        return this.start;
    }

    public KitImpl getKit() {
        return this.kit;
    }

    public int getBet() {
        return this.bet;
    }

    public Queue getSource() {
        return this.source;
    }

    public boolean isFinished() {
        return this.finished;
    }
}
