package me.realized.duels.queue;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.api.queue.DQueue;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Queue extends BaseButton implements DQueue {
    private final Kit kit;
    private final int bet;
    private final List<QueueEntry> players = new LinkedList<>();
    private final Set<java.util.UUID> playerUuids = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<java.util.UUID, Boolean>());
    private boolean removed;

    public Queue(final DuelsPlugin plugin, final Kit kit, final int bet) {
        super(plugin, ItemBuilder.of((plugin.getConfiguration().isInheritKitItemType() && kit != null) ? kit.getDisplayed().clone() : ItemBuilder.of(Material.DIAMOND_SWORD).build()).name(plugin.getLang().getMessage("GUI.queues.buttons.queue.name", "kit", kit != null ? kit.getName() : plugin.getLang().getMessage("GENERAL.none"), "bet_amount", bet, "in_queue", 0, "in_match", 0)).lore(plugin.getLang().getMessage("GUI.queues.buttons.queue.lore", "kit", kit != null ? kit.getName() : plugin.getLang().getMessage("GENERAL.none"), "bet_amount", bet, "in_queue", 0, "in_match", 0).split("\n")).build());
        this.kit = kit;
        this.bet = bet;
    }

    @Override
    public boolean isInQueue(@NotNull final Player player) {
        return playerUuids.contains(player.getUniqueId());
    }

    @NotNull
    @Override
    public List<Player> getQueuedPlayers() {
        final List<Player> result = new java.util.ArrayList<>();
        for (QueueEntry entry : players) {
            Player p = entry.getPlayer();
            if (p != null && p.isOnline()) {
                result.add(p);
            }
        }
        return Collections.unmodifiableList(result);
    }

    void addPlayer(final QueueEntry entry) {
        playerUuids.add(entry.getUuid());
        players.add(entry);
        update();
        queueManager.getGui().calculatePages();
    }

    boolean removePlayer(final Player player) {
        playerUuids.remove(player.getUniqueId());
        if (players.removeIf(entry -> entry.getUuid().equals(player.getUniqueId()))) {
            update();
            queueManager.getGui().calculatePages();
            return true;
        }
        return false;
    }

    boolean removeAll(final Set<QueueEntry> playersToRemove) {
        for (QueueEntry entry : playersToRemove) {
            playerUuids.remove(entry.getUuid());
        }
        if (this.players.removeAll(playersToRemove)) {
            update();
            return true;
        }
        return false;
    }

    public long getPlayersInMatch() {
        return arenaManager.getPlayersInMatch(this);
    }

    public void update() {
        int inQueue = players.size();
        long inMatch = getPlayersInMatch();
        setDisplayName(lang.getMessage("GUI.queues.buttons.queue.name", "kit", kit != null ? kit.getName() : lang.getMessage("GENERAL.none"), "bet_amount", bet, "in_queue", inQueue, "in_match", inMatch));
        setLore(lang.getMessage("GUI.queues.buttons.queue.lore", "kit", kit != null ? kit.getName() : lang.getMessage("GENERAL.none"), "bet_amount", bet, "in_queue", inQueue, "in_match", inMatch).split("\n"));
    }

    @Override
    public void onClick(final Player player) {
        queueManager.addToQueue(player, this);
    }

    @Override
    public String toString() {
        return (kit != null ? kit.getName() : "none") + " - $" + bet;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final Queue queue = (Queue) other;
        return bet == queue.bet && Objects.equals(kit, queue.kit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kit, bet);
    }

    public Kit getKit() {
        return this.kit;
    }

    public int getBet() {
        return this.bet;
    }

    public List<QueueEntry> getPlayers() {
        return this.players;
    }

    public boolean isRemoved() {
        return this.removed;
    }

    void setRemoved(final boolean removed) {
        this.removed = removed;
    }
}
