package me.realized.duels.arena;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedDeque;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.event.arena.ArenaSetPositionEvent;
import me.realized.duels.api.event.arena.ArenaStateChangeEvent;
import me.realized.duels.api.event.match.MatchEndEvent;
import me.realized.duels.api.event.match.MatchEndEvent.Reason;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.queue.Queue;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.function.Pair;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArenaImpl extends BaseButton implements Arena {

    private final String name;
    private boolean disabled;
    private final Set<KitImpl> kits = new HashSet<KitImpl>();
    private final Map<Integer, Location> positions = new HashMap<Integer, Location>();
    private MatchImpl match;
    private Countdown countdown;
    private boolean removed;

    // BuildUHC blok sıfırlama için LIFO kuyruğu
    private final Deque<BlockState> modifiedBlocks = new ConcurrentLinkedDeque<BlockState>();
    private BukkitTask rollbackTask;

    public ArenaImpl(final DuelsPlugin plugin, final String name, final boolean disabled) {
        super(plugin, ItemBuilder.of(Items.EMPTY_MAP).name(plugin.getLang().getMessage("GUI.arena-selector.buttons.arena.name", "name", name)).lore(plugin.getLang().getMessage("GUI.arena-selector.buttons.arena.lore-unavailable").split("\n")).build());
        this.name = name;
        this.disabled = disabled;
    }

    public ArenaImpl(final DuelsPlugin plugin, final String name) {
        this(plugin, name, false);
    }

    public void refreshGui(final boolean available) {
        setLore(lang.getMessage("GUI.arena-selector.buttons.arena.lore-" + (available ? "available" : "unavailable")).split("\n"));
        arenaManager.getGui().calculatePages();
    }

    public void ensureChunksLoaded() {
        final Location p1 = getPosition(1);
        final Location p2 = getPosition(2);
        if (p1 != null && p1.getWorld() != null) {
            final Chunk c1 = p1.getChunk();
            if (!c1.isLoaded()) c1.load(true);
        }
        if (p2 != null && p2.getWorld() != null) {
            final Chunk c2 = p2.getChunk();
            if (!c2.isLoaded()) c2.load(true);
        }
    }

    @Nullable
    @Override
    public Location getPosition(final int pos) {
        return positions.get(pos);
    }

    @Override
    public boolean setPosition(@Nullable final Player source, final int pos, @NotNull final Location location) {
        Objects.requireNonNull(location, "location");
        if (pos <= 0 || pos > 2) {
            return false;
        }
        final ArenaSetPositionEvent event = new ArenaSetPositionEvent(source, this, pos, location);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        positions.put(pos, location);
        ensureChunksLoaded();
        arenaManager.saveArenas();
        refreshGui(isAvailable());
        return true;
    }

    @Override
    public boolean setPosition(final int pos, @NotNull final Location location) {
        return setPosition(null, pos, location);
    }

    @Override
    public boolean setDisabled(@Nullable final CommandSender source, final boolean disabled) {
        final ArenaStateChangeEvent event = new ArenaStateChangeEvent(source, this, disabled);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        this.disabled = event.isDisabled();
        arenaManager.saveArenas();
        refreshGui(isAvailable());
        return true;
    }

    @Override
    public boolean setDisabled(final boolean disabled) {
        return setDisabled(null, disabled);
    }

    public boolean isBoundless() {
        return kits.isEmpty();
    }

    public boolean isBound(@Nullable final KitImpl kit) {
        return kit != null && kits.contains(kit);
    }

    public void bind(final KitImpl kit) {
        if (isBound(kit)) {
            kits.remove(kit);
        } else {
            kits.add(kit);
        }
        arenaManager.saveArenas();
    }

    @Override
    public boolean isUsed() {
        return match != null;
    }

    public boolean isAvailable() {
        return !isDisabled() && !isUsed() && rollbackTask == null && getPosition(1) != null && getPosition(2) != null;
    }

    public MatchImpl startMatch(final KitImpl kit, final Map<UUID, List<ItemStack>> items, final int bet, final Queue source) {
        this.match = new MatchImpl(this, kit, items, bet, source);
        refreshGui(false);
        return match;
    }

    public void endMatch(final UUID winner, final UUID loser, final Reason reason) {
        spectateManager.stopSpectating(this);
        final MatchEndEvent event = new MatchEndEvent(match, winner, loser, reason);
        Bukkit.getPluginManager().callEvent(event);
        final Queue source = match != null ? match.getSource() : null;

        if (match != null) {
            for (UUID uuid : match.getPlayerUuids()) {
                arenaManager.removePlayerArena(uuid);
            }
            match.setFinished();
            match = null;
        }

        cleanupEntities();
        resetBlocks(() -> {
            if (source != null) {
                source.update();
                queueManager.getGui().calculatePages();
            }
            refreshGui(true);
        });
    }

    public void recordBlockChange(@NotNull final BlockState state) {
        modifiedBlocks.add(state);
    }

    /**
     * BuildUHC veya maç esnasında yerleştirilen/kırılan blokları main thread dondurmadan
     * her tick 50 blok olarak zaman dilimli (time-sliced) sıfırlar.
     */
    public void resetBlocks(@Nullable final Runnable onComplete) {
        if (modifiedBlocks.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }

        if (rollbackTask != null) {
            rollbackTask.cancel();
        }

        rollbackTask = plugin.doSyncRepeat(new Runnable() {
            @Override
            public void run() {
                int count = 0;
                while (!modifiedBlocks.isEmpty() && count < 50) {
                    final BlockState state = modifiedBlocks.pollLast();
                    if (state != null) {
                        state.update(true, false); // false = fizik hesaplaması yapma (lag önlemi)
                    }
                    count++;
                }

                if (modifiedBlocks.isEmpty()) {
                    if (rollbackTask != null) {
                        rollbackTask.cancel();
                        rollbackTask = null;
                    }
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            }
        }, 1L, 1L);
    }

    /**
     * Maç bittiğinde arenanın BoundingBox'ı içindeki Item ve Projectile varlıklarını
     * sadece ilgili yüklü chunk'ları tarayarak temizler.
     */
    public void cleanupEntities() {
        final Location p1 = getPosition(1);
        final Location p2 = getPosition(2);
        if (p1 == null || p2 == null || p1.getWorld() == null) {
            return;
        }

        final World world = p1.getWorld();
        final int minX = Math.min(p1.getBlockX(), p2.getBlockX()) - 8;
        final int maxX = Math.max(p1.getBlockX(), p2.getBlockX()) + 8;
        final int minY = Math.max(0, Math.min(p1.getBlockY(), p2.getBlockY()) - 5);
        final int maxY = Math.min(world.getMaxHeight(), Math.max(p1.getBlockY(), p2.getBlockY()) + 20);
        final int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ()) - 8;
        final int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ()) + 8;

        final int minChunkX = minX >> 4;
        final int maxChunkX = maxX >> 4;
        final int minChunkZ = minZ >> 4;
        final int maxChunkZ = maxZ >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (world.isChunkLoaded(cx, cz)) {
                    final Chunk chunk = world.getChunkAt(cx, cz);
                    for (Entity entity : chunk.getEntities()) {
                        if (entity instanceof Item || entity instanceof Projectile) {
                            final Location loc = entity.getLocation();
                            if (loc.getX() >= minX && loc.getX() <= maxX
                                    && loc.getY() >= minY && loc.getY() <= maxY
                                    && loc.getZ() >= minZ && loc.getZ() <= maxZ) {
                                entity.remove();
                            }
                        }
                    }
                }
            }
        }
    }

    public void startCountdown(final String kit, final Map<UUID, Pair<String, Integer>> info) {
        List<String> messages = config.getCdMessages();
        List<String> titles = config.getTitles();
        if (messages == null || messages.isEmpty()) {
            messages = java.util.Arrays.asList(
                "&eDüello &65 &esaniye içinde başlıyor...",
                "&eDüello &64 &esaniye içinde başlıyor...",
                "&eDüello &63 &esaniye içinde başlıyor...",
                "&eDüello &62 &esaniye içinde başlıyor...",
                "&eDüello &61 &esaniye içinde başlıyor...",
                "&aDüello başladı, başarılar!"
            );
        }
        if (titles == null || titles.isEmpty()) {
            titles = java.util.Arrays.asList(
                "&a5",
                "&e4",
                "&63",
                "&c2",
                "&41",
                "&a&lBAŞLADI!"
            );
        }
        this.countdown = new Countdown(plugin, this, kit, info, messages, titles);
        countdown.runTaskTimer(plugin, 0L, 20L);
    }

    boolean isCounting() {
        return countdown != null;
    }

    @Override
    public boolean has(@NotNull final Player player) {
        Objects.requireNonNull(player, "player");
        return isUsed() && !match.isDead(player.getUniqueId());
    }

    public void add(final Player player) {
        if (isUsed()) {
            match.add(player.getUniqueId());
            arenaManager.setPlayerArena(player.getUniqueId(), this);
        }
    }

    public void remove(final Player player) {
        if (isUsed()) {
            match.remove(player.getUniqueId());
            arenaManager.removePlayerArena(player.getUniqueId());
        }
    }

    public boolean isEndGame() {
        return size() <= 1;
    }

    public int size() {
        return isUsed() ? match.getAliveCount() : 0;
    }

    public Player first() {
        if (!isUsed()) return null;
        for (Player p : match.getAlivePlayers()) {
            return p;
        }
        return null;
    }

    public Player getOpponent(final Player player) {
        if (!isUsed()) return null;
        final UUID uuid = player.getUniqueId();
        for (Player other : match.getAllPlayers()) {
            if (!other.getUniqueId().equals(uuid)) {
                return other;
            }
        }
        return null;
    }

    public Set<Player> getPlayers() {
        return isUsed() ? match.getAllPlayers() : Collections.emptySet();
    }

    public void broadcast(final String message) {
        for (Player p : getPlayers()) {
            p.sendMessage(message);
        }
        for (me.realized.duels.spectate.SpectatorImpl s : spectateManager.getSpectatorsImpl(this)) {
            final Player spec = Bukkit.getPlayer(s.getUuid());
            if (spec != null && spec.isOnline()) {
                spec.sendMessage(message);
            }
        }
    }

    @Override
    public void onClick(final Player player) {
        if (!isAvailable()) {
            return;
        }
        final Settings settings = settingManager.getSafely(player);
        final String kitName = settings.getKit() != null ? settings.getKit().getName() : lang.getMessage("GENERAL.none");
        if (!arenaManager.isSelectable(settings.getKit(), this)) {
            lang.sendMessage(player, "ERROR.setting.arena-not-applicable", "kit", kitName, "arena", name);
            return;
        }
        settings.setArena(this);
        settings.openGui(player);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final ArenaImpl arena = (ArenaImpl) other;
        return Objects.equals(name, arena.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return this.name;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public Set<KitImpl> getKits() {
        return this.kits;
    }

    public Map<Integer, Location> getPositions() {
        return this.positions;
    }

    public MatchImpl getMatch() {
        return this.match;
    }

    Countdown getCountdown() {
        return this.countdown;
    }

    void setCountdown(final Countdown countdown) {
        this.countdown = countdown;
    }

    public boolean isRemoved() {
        return this.removed;
    }

    void setRemoved(final boolean removed) {
        this.removed = removed;
    }
}
