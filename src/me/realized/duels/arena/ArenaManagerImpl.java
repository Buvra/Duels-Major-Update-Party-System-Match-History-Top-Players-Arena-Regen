package me.realized.duels.arena;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.arena.ArenaManager;
import me.realized.duels.api.event.arena.ArenaCreateEvent;
import me.realized.duels.api.event.arena.ArenaRemoveEvent;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.data.ArenaData;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.queue.Queue;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.Log;
import me.realized.duels.util.StringUtil;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.gui.MultiPageGui;
import me.realized.duels.util.inventory.ItemBuilder;
import me.realized.duels.util.io.FileUtil;
import me.realized.duels.util.json.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 500 CCU altında Stream ve liste araması maliyetlerini sıfıra indiren,
 * O(1) doğrudan UUID ve İsim haritaları kullanan ArenaManager.
 */
public class ArenaManagerImpl implements Loadable, ArenaManager {

    private static final String FILE_NAME = "arenas.json";
    private static final String ERROR_NOT_ALPHANUMERIC = "Could not load arena %s: Name is not alphanumeric.";
    private static final String ARENAS_LOADED = "Loaded %s arena(s).";

    private final DuelsPlugin plugin;
    private final Config config;
    private final Lang lang;
    private final File file;

    private final List<ArenaImpl> arenas = new ArrayList<ArenaImpl>();
    private final Map<String, ArenaImpl> arenaByName = new ConcurrentHashMap<String, ArenaImpl>();
    private final Map<UUID, ArenaImpl> arenaByPlayer = new ConcurrentHashMap<UUID, ArenaImpl>();

    private MultiPageGui<DuelsPlugin> gui;

    public ArenaManagerImpl(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.lang = plugin.getLang();
        this.file = new File(plugin.getDataFolder(), FILE_NAME);
        Bukkit.getPluginManager().registerEvents(new ArenaListener(), plugin);
    }

    @Override
    public void handleLoad() throws IOException {
        gui = new MultiPageGui<DuelsPlugin>(plugin, lang.getMessage("GUI.arena-selector.title"), config.getArenaSelectorRows(), arenas);
        gui.setSpaceFiller(Items.from(config.getArenaSelectorFillerType(), config.getArenaSelectorFillerData()));
        gui.setPrevButton(ItemBuilder.of(Material.PAPER).name(lang.getMessage("GUI.kit-selector.buttons.previous-page.name")).build());
        gui.setNextButton(ItemBuilder.of(Material.PAPER).name(lang.getMessage("GUI.kit-selector.buttons.next-page.name")).build());
        gui.setEmptyIndicator(ItemBuilder.of(Material.PAPER).name(lang.getMessage("GUI.kit-selector.buttons.empty.name")).build());
        plugin.getGuiListener().addGui(gui);

        if (FileUtil.checkNonEmpty(file, true)) {
            try (Reader reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8)) {
                final List<ArenaData> data = JsonUtil.getObjectMapper().readValue(reader, new TypeReference<List<ArenaData>>() {});
                if (data != null) {
                    for (final ArenaData arenaData : data) {
                        if (!StringUtil.isAlphanumeric(arenaData.getName())) {
                            Log.warn(this, String.format(ERROR_NOT_ALPHANUMERIC, arenaData.getName()));
                            continue;
                        }
                        final ArenaImpl arena = arenaData.toArena(plugin);
                        arenas.add(arena);
                        arenaByName.put(arena.getName().toLowerCase(), arena);
                        arena.ensureChunksLoaded();
                    }
                }
            }
        }

        Log.info(this, String.format(ARENAS_LOADED, arenas.size()));
        gui.calculatePages();
    }

    @Override
    public void handleUnload() {
        if (gui != null) {
            plugin.getGuiListener().removeGui(gui);
        }
        arenas.clear();
        arenaByName.clear();
        arenaByPlayer.clear();
    }

    void saveArenas() {
        final List<ArenaData> data = new ArrayList<ArenaData>();
        for (final ArenaImpl arena : arenas) {
            data.add(new ArenaData(arena));
        }
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            JsonUtil.getObjectWriter().writeValue(writer, data);
            writer.flush();
        } catch (IOException ex) {
            Log.error(this, ex.getMessage(), ex);
        }
    }

    public void setPlayerArena(@NotNull final UUID uuid, @NotNull final ArenaImpl arena) {
        arenaByPlayer.put(uuid, arena);
    }

    public void removePlayerArena(@NotNull final UUID uuid) {
        arenaByPlayer.remove(uuid);
    }

    @Nullable
    @Override
    public ArenaImpl get(@NotNull final String name) {
        Objects.requireNonNull(name, "name");
        return arenaByName.get(name.toLowerCase());
    }

    @Nullable
    public ArenaImpl get(@NotNull final UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return arenaByPlayer.get(uuid);
    }

    @Nullable
    @Override
    public ArenaImpl get(@NotNull final Player player) {
        Objects.requireNonNull(player, "player");
        return arenaByPlayer.get(player.getUniqueId());
    }

    @Override
    public boolean isInMatch(@NotNull final Player player) {
        Objects.requireNonNull(player, "player");
        return arenaByPlayer.containsKey(player.getUniqueId());
    }

    public boolean isInMatch(@NotNull final UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return arenaByPlayer.containsKey(uuid);
    }

    @NotNull
    @Override
    public List<Arena> getArenas() {
        return Collections.unmodifiableList(arenas);
    }

    public boolean create(final CommandSender source, final String name) {
        if (get(name) != null) {
            return false;
        }
        final ArenaImpl arena = new ArenaImpl(plugin, name);
        arenas.add(arena);
        arenaByName.put(arena.getName().toLowerCase(), arena);
        saveArenas();
        final ArenaCreateEvent event = new ArenaCreateEvent(source, arena);
        Bukkit.getPluginManager().callEvent(event);
        gui.calculatePages();
        return true;
    }

    public boolean remove(final CommandSender source, final ArenaImpl arena) {
        if (arenas.remove(arena)) {
            arenaByName.remove(arena.getName().toLowerCase());
            arena.setRemoved(true);
            saveArenas();
            final ArenaRemoveEvent event = new ArenaRemoveEvent(source, arena);
            Bukkit.getPluginManager().callEvent(event);
            gui.calculatePages();
            return true;
        }
        return false;
    }

    public List<ArenaImpl> getArenasImpl() {
        return arenas;
    }

    public Set<Player> getPlayers() {
        final Set<Player> players = new HashSet<Player>();
        for (UUID uuid : arenaByPlayer.keySet()) {
            final Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                players.add(p);
            }
        }
        return players;
    }

    public long getPlayersInMatch(final Queue queue) {
        long count = 0;
        for (ArenaImpl arena : arenas) {
            if (arena.isUsed() && arena.getMatch().isFromQueue() && Objects.equals(arena.getMatch().getSource(), queue)) {
                count += 2;
            }
        }
        return count;
    }

    public boolean isSelectable(@Nullable final KitImpl kit, @NotNull final ArenaImpl arena) {
        if (!arena.isAvailable()) {
            return false;
        }
        if (arena.isBoundless()) {
            if (kit == null) {
                return true;
            } else {
                return !kit.isArenaSpecific();
            }
        }
        return arena.isBound(kit);
    }

    public ArenaImpl randomArena(final KitImpl kit) {
        final List<ArenaImpl> available = new ArrayList<ArenaImpl>();
        for (ArenaImpl arena : arenas) {
            if (isSelectable(kit, arena)) {
                available.add(arena);
            }
        }
        return !available.isEmpty() ? available.get(ThreadLocalRandom.current().nextInt(available.size())) : null;
    }

    public List<String> getNames() {
        final List<String> names = new ArrayList<String>(arenas.size());
        for (ArenaImpl arena : arenas) {
            names.add(arena.getName());
        }
        return names;
    }

    public void clearBinds(final KitImpl kit) {
        for (ArenaImpl arena : arenas) {
            if (arena.isBound(kit)) {
                arena.bind(kit);
            }
        }
    }

    private class ArenaListener implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void on(final PlayerInteractEvent event) {
            if (!event.hasBlock() || !config.isPreventInteract()) {
                return;
            }
            final ArenaImpl arena = get(event.getPlayer());
            if (arena == null || !arena.isCounting()) {
                return;
            }
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void on(final EntityDamageEvent event) {
            if (!config.isPreventPvp() || !(event.getEntity() instanceof Player)) {
                return;
            }
            final ArenaImpl arena = get((Player) event.getEntity());
            if (arena == null || !arena.isCounting()) {
                return;
            }
            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void on(final ProjectileLaunchEvent event) {
            if (!config.isPreventLaunchProjectile()) {
                return;
            }
            final ProjectileSource shooter = event.getEntity().getShooter();
            if (!(shooter instanceof Player)) {
                return;
            }
            final ArenaImpl arena = get((Player) shooter);
            if (arena == null || !arena.isCounting()) {
                return;
            }
            event.setCancelled(true);
        }

        @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
        public void on(final PlayerMoveEvent event) {
            if (!config.isPreventMovement()) {
                return;
            }

            final Location from = event.getFrom();
            final Location to = event.getTo();

            // Sadece tam blok geçişlerinde çalış (Yaw/Pitch ve mikro hareketleri ele)
            if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
                return;
            }

            final ArenaImpl arena = get(event.getPlayer());
            if (arena == null || !arena.isCounting()) {
                return;
            }

            event.setTo(event.getFrom());
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockPlace(final BlockPlaceEvent event) {
            final ArenaImpl arena = get(event.getPlayer());
            if (arena != null && arena.isUsed()) {
                arena.recordBlockChange(event.getBlockReplacedState());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBlockBreak(final BlockBreakEvent event) {
            final ArenaImpl arena = get(event.getPlayer());
            if (arena != null && arena.isUsed()) {
                arena.recordBlockChange(event.getBlock().getState());
            }
        }

        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
            final ArenaImpl arena = get(event.getPlayer());
            if (arena != null && arena.isUsed() && event.getBlockClicked() != null) {
                arena.recordBlockChange(event.getBlockClicked().getRelative(event.getBlockFace()).getState());
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onQuit(final PlayerQuitEvent event) {
            arenaByPlayer.remove(event.getPlayer().getUniqueId());
        }
    }

    public MultiPageGui<DuelsPlugin> getGui() {
        return this.gui;
    }
}
