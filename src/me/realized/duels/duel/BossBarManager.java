package me.realized.duels.duel;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.arena.MatchImpl;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BossBarManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, FakeBossBar> bars = new HashMap<>();
    private final Map<UUID, UUID> extraViewers = new HashMap<>();
    private int taskId;

    public BossBarManager(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        this.taskId = this.plugin.doSyncRepeat(() -> this.tick(), 20L, 20L).getTaskId();
    }

    public void stop() {
        if (this.taskId != 0) {
            this.plugin.cancelTask(this.taskId);
            this.taskId = 0;
        }

        for (FakeBossBar bar : this.bars.values()) {
            bar.destroy();
        }

        this.bars.clear();
        this.extraViewers.clear();
    }

    public void addViewer(Player viewer, Player target) {
        if (viewer == null || target == null) {
            return;
        }

        this.extraViewers.put(viewer.getUniqueId(), target.getUniqueId());
    }

    public void remove(Player player) {
        if (player == null) {
            return;
        }

        this.extraViewers.remove(player.getUniqueId());

        FakeBossBar bar = this.bars.remove(player.getUniqueId());
        if (bar != null) {
            bar.destroy();
        }
    }

    public void remove(ArenaImpl arena, MatchImpl match) {
        for (Player player : this.getViewers(arena, match)) {
            this.remove(player);
        }
    }

    public void update(ArenaImpl arena, MatchImpl match, int maxDurationMinutes) {
        if (arena == null || match == null || maxDurationMinutes <= 0) {
            this.remove(arena, match);
            return;
        }

        long remaining = this.getRemainingMillis(match, maxDurationMinutes);
        float progress = this.getProgress(match, maxDurationMinutes);
        String title = this.buildTitle(match, remaining);

        Set<Player> viewers = this.getViewers(arena, match);

        for (Player viewer : viewers) {
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            FakeBossBar bar = this.bars.get(viewer.getUniqueId());
            if (bar == null) {
                bar = new FakeBossBar(viewer, title, progress);
                this.bars.put(viewer.getUniqueId(), bar);
                bar.spawn();
            } else {
                bar.update(title, progress);
            }
        }

        Iterator<Map.Entry<UUID, FakeBossBar>> iterator = this.bars.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, FakeBossBar> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player == null || !player.isOnline() || !viewers.contains(player)) {
                entry.getValue().destroy();
                iterator.remove();
            }
        }
    }

    private void tick() {
        int maxDuration = this.plugin.getConfiguration().getMaxDuration();
        if (maxDuration <= 0) {
            return;
        }

        Iterator<Map.Entry<UUID, UUID>> viewerIterator = this.extraViewers.entrySet().iterator();
        while (viewerIterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = viewerIterator.next();

            Player viewer = Bukkit.getPlayer(entry.getKey());
            Player target = Bukkit.getPlayer(entry.getValue());

            if (viewer == null || !viewer.isOnline() || target == null || !target.isOnline()) {
                if (viewer != null) {
                    this.remove(viewer);
                }
                viewerIterator.remove();
                continue;
            }

            ArenaImpl arena = this.plugin.getArenaManager().get(target);
            if (arena == null || arena.getMatch() == null) {
                this.remove(viewer);
                viewerIterator.remove();
            }
        }

        for (ArenaImpl arena : this.plugin.getArenaManager().getArenasImpl()) {
            MatchImpl match = arena.getMatch();

            if (match == null || arena.size() <= 0) {
                this.remove(arena, match);
                continue;
            }

            this.update(arena, match, maxDuration);
        }
    }

    private long getRemainingMillis(MatchImpl match, int maxDurationMinutes) {
        long maxMillis = maxDurationMinutes * 60L * 1000L;
        long elapsed = System.currentTimeMillis() - match.getStart();
        return Math.max(0L, maxMillis - elapsed);
    }

    private float getProgress(MatchImpl match, int maxDurationMinutes) {
        long maxMillis = maxDurationMinutes * 60L * 1000L;
        if (maxMillis <= 0L) {
            return 100.0F;
        }

        double remaining = this.getRemainingMillis(match, maxDurationMinutes);
        return (float) Math.max(1.0D, Math.min(100.0D, (remaining / (double) maxMillis) * 100.0D));
    }

    private String buildTitle(MatchImpl match, long remainingMillis) {
        Set<Player> players = match.getAllPlayers();
        Iterator<Player> iterator = players.iterator();

        String first = "Oyuncu-1";
        String second = "Oyuncu-2";

        if (iterator.hasNext()) {
            first = iterator.next().getName();
        }

        if (iterator.hasNext()) {
            second = iterator.next().getName();
        }

        return "§e" + first + " §7vs §e" + second + " §8- §6Kalan: §e" + this.formatDuration(remainingMillis);
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Set<Player> getViewers(ArenaImpl arena, MatchImpl match) {
        Set<Player> viewers = new HashSet<>();

        if (match != null) {
            viewers.addAll(match.getAllPlayers());
        }

        if (arena != null) {
            for (me.realized.duels.api.spectate.Spectator spectator : this.plugin.getSpectateManager().getSpectators(arena)) {
                if (spectator == null) {
                    continue;
                }

                Player player = spectator.getPlayer();
                if (player != null && player.isOnline()) {
                    viewers.add(player);
                }
            }
        }

        return viewers;
    }
}
