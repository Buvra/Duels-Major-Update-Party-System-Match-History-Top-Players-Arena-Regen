package me.realized.duels.spectate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.arena.Arena;
import me.realized.duels.api.event.spectate.SpectateEndEvent;
import me.realized.duels.api.event.spectate.SpectateStartEvent;
import me.realized.duels.api.spectate.SpectateManager;
import me.realized.duels.api.spectate.SpectateManager.Result;
import me.realized.duels.api.spectate.Spectator;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.arena.MatchImpl;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.player.PlayerInfo;
import me.realized.duels.player.PlayerInfoManager;
import me.realized.duels.teleport.Teleport;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectateManagerImpl implements Loadable, SpectateManager {

    private static final double MAX_SPEC_DISTANCE = 500.0D;
    private static final int LEAVE_ITEM_SLOT = 8;
    private static final Material LEAVE_ITEM_MATERIAL = Material.BARRIER;
    private static final String LEAVE_ITEM_NAME = "§cIzlemekten Ayril";
    private static final String LEAVE_ITEM_MARKER = "§8duels:spectate_leave";

    private final DuelsPlugin plugin;
    private final Config config;
    private final Lang lang;
    private final ArenaManagerImpl arenaManager;
    private final PlayerInfoManager playerManager;
    private final Map<UUID, SpectatorImpl> spectators = new HashMap<UUID, SpectatorImpl>();
    private final Multimap<Arena, SpectatorImpl> arenas = HashMultimap.create();

    private Teleport teleport;

    public SpectateManagerImpl(DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.lang = plugin.getLang();
        this.arenaManager = plugin.getArenaManager();
        this.playerManager = plugin.getPlayerManager();
        Bukkit.getPluginManager().registerEvents(new SpectateListener(), plugin);
    }

    @Override
    public void handleLoad() {
        this.teleport = this.plugin.getTeleport();
    }

    @Override
    public void handleUnload() {
        List<SpectatorImpl> currentSpectators = new ArrayList<SpectatorImpl>(this.spectators.values());

        for (SpectatorImpl spectator : currentSpectators) {
            Player player = Bukkit.getPlayer(spectator.getUuid());
            if (player != null) {
                this.forceStopSpectating(player, spectator);
            }
        }

        this.spectators.clear();
        this.arenas.clear();
    }

    @Nullable
    public SpectatorImpl get(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return this.spectators.get(player.getUniqueId());
    }

    public boolean isSpectating(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        return this.get(player) != null;
    }

    @NotNull
    public SpectateManager.Result startSpectating(@NotNull Player player, @NotNull Player target) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(target, "target");

        if (this.isSpectating(player)) {
            return Result.ALREADY_SPECTATING;
        } else if (this.plugin.getQueueManager().isInQueue(player)) {
            return Result.IN_QUEUE;
        } else if (this.arenaManager.isInMatch(player)) {
            return Result.IN_MATCH;
        } else {
            ArenaImpl arena = this.arenaManager.get(target);
            if (arena == null) {
                return Result.TARGET_NOT_IN_MATCH;
            } else {
                SpectatorImpl spectator = new SpectatorImpl(player, target, arena);
                SpectateStartEvent event = new SpectateStartEvent(player, spectator);
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return Result.EVENT_CANCELLED;
                } else {
                    MatchImpl match = arena.getMatch();

                    if (match != null) {
                        for (Player arenaPlayer : match.getAllPlayers()) {
                            if (arenaPlayer != null && arenaPlayer.isOnline() && arenaPlayer.canSee(player)) {
                                arenaPlayer.hidePlayer(player);
                            }
                        }
                    }

                    this.playerManager.create(player);
                    PlayerUtil.reset(player);

                    this.spectators.put(player.getUniqueId(), spectator);
                    this.arenas.put(arena, spectator);

                    player.setGameMode(GameMode.SURVIVAL);
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    player.setCanPickupItems(false);
                    player.spigot().setCollidesWithEntities(false);
                    player.setVelocity(new Vector(0, 0, 0));
                    player.setFallDistance(0.0F);

                    if (this.config.isSpecAddInvisibilityEffect()) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));
                    }

                    this.teleportToTarget(player, target);
                    this.giveLeaveItem(player);
                    this.attachBossBar(player, target, arena, match);

                    if (!player.hasPermission("duels.spectate.anonymously") && match != null) {
                        for (Player matchPlayer : match.getAllPlayers()) {
                            this.lang.sendMessage(matchPlayer, "SPECTATE.arena-broadcast",
                                    new Object[] { "name", player.getName() });
                        }
                    }

                    return Result.SUCCESS;
                }
            }
        }
    }

    public void stopSpectating(Player player, SpectatorImpl spectator) {
        if (player == null || spectator == null) {
            return;
        }

        this.forceStopSpectating(player, spectator);

        SpectateEndEvent event = new SpectateEndEvent(player, spectator);
        Bukkit.getPluginManager().callEvent(event);
    }

    private void forceStopSpectating(Player player, SpectatorImpl spectator) {
        this.spectators.remove(player.getUniqueId());
        this.arenas.remove(spectator.getArena(), spectator);
        this.plugin.getDuelManager().getBossBarManager().remove(player);

        this.removeLeaveItem(player);

        player.setFlying(false);
        player.setAllowFlight(false);
        player.setCanPickupItems(true);
        player.spigot().setCollidesWithEntities(true);
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0.0F);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);

        PlayerUtil.reset(player);

        PlayerInfo info = this.playerManager.remove(player);
        if (info != null) {
            this.teleport.tryTeleport(player, info.getLocation());
            info.restore(player);
        } else {
            player.setGameMode(GameMode.SURVIVAL);
            this.teleport.tryTeleport(player, this.playerManager.getLobby());
        }

        MatchImpl match = spectator.getArena().getMatch();
        if (match != null) {
            for (Player arenaPlayer : match.getAllPlayers()) {
                if (arenaPlayer != null && arenaPlayer.isOnline()) {
                    arenaPlayer.showPlayer(player);
                }
            }
        }
    }

    public void stopSpectating(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        this.stopSpectating(player, this.get(player));
    }

    public void stopSpectating(ArenaImpl arena) {
        Collection<SpectatorImpl> spectatorsInArena = new ArrayList<SpectatorImpl>(this.getSpectatorsImpl(arena));
        if (!spectatorsInArena.isEmpty()) {
            for (SpectatorImpl spectator : spectatorsInArena) {
                Player player = Bukkit.getPlayer(spectator.getUuid());
                if (player != null) {
                    this.stopSpectating(player, spectator);
                    this.lang.sendMessage(player, "SPECTATE.match-end", new Object[0]);
                }
            }
        }
    }

    @NotNull
    public List<Spectator> getSpectators(@NotNull Arena arena) {
        Objects.requireNonNull(arena, "arena");
        return Collections.unmodifiableList(Lists.newArrayList(this.getSpectatorsImpl(arena)));
    }

    public Collection<SpectatorImpl> getSpectatorsImpl(Arena arena) {
        Collection<SpectatorImpl> collection = this.arenas.asMap().get(arena);
        return collection != null ? collection : Collections.<SpectatorImpl>emptyList();
    }

    public Collection<Player> getAllSpectators() {
        List<Player> players = new ArrayList<Player>();

        for (SpectatorImpl spectator : this.spectators.values()) {
            Player player = Bukkit.getPlayer(spectator.getUuid());
            if (player != null) {
                players.add(player);
            }
        }

        return players;
    }

    private void teleportToTarget(Player player, Player target) {
        if (player == null || target == null || !target.isOnline()) {
            return;
        }

        Location location = target.getLocation().clone().add(0.0D, 1.0D, 0.0D);
        this.teleport.tryTeleport(player, location);
    }

    private void giveLeaveItem(Player player) {
        if (player == null) {
            return;
        }

        ItemStack item = new ItemStack(LEAVE_ITEM_MATERIAL, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(LEAVE_ITEM_NAME);
            meta.setLore(Arrays.asList("§7Izlemeyi bitirmek icin tikla", LEAVE_ITEM_MARKER));
            item.setItemMeta(meta);
        }

        player.getInventory().setItem(LEAVE_ITEM_SLOT, item);
        player.updateInventory();
    }

    private void removeLeaveItem(Player player) {
        if (player == null) {
            return;
        }

        ItemStack item = player.getInventory().getItem(LEAVE_ITEM_SLOT);
        if (this.isLeaveItem(item)) {
            player.getInventory().setItem(LEAVE_ITEM_SLOT, null);
            player.updateInventory();
        }
    }

    private boolean isLeaveItem(ItemStack item) {
        if (item == null || item.getType() != LEAVE_ITEM_MATERIAL || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.hasDisplayName()
                && LEAVE_ITEM_NAME.equals(meta.getDisplayName())
                && meta.hasLore()
                && meta.getLore().contains(LEAVE_ITEM_MARKER);
    }

    private void attachBossBar(Player player, Player target, ArenaImpl arena, MatchImpl match) {
        if (player == null || target == null || arena == null || match == null) {
            return;
        }

        int maxDuration = this.config.getMaxDuration();
        if (maxDuration <= 0) {
            return;
        }

        this.plugin.getDuelManager().getBossBarManager().addViewer(player, target);
        this.plugin.getDuelManager().getBossBarManager().update(arena, match, maxDuration);
    }

    private class SpectateListener implements Listener {

        private SpectateListener() {
        }

        @EventHandler
        public void on(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            SpectatorImpl spectator = SpectateManagerImpl.this.get(player);
            if (spectator != null) {
                SpectateManagerImpl.this.forceStopSpectating(player, spectator);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            if (SpectateManagerImpl.this.isSpectating(player)) {
                String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();
                if (!command.equalsIgnoreCase("spectate")
                        && !command.equalsIgnoreCase("spec")
                        && !SpectateManagerImpl.this.config.getSpecWhitelistedCommands().contains(command)) {
                    event.setCancelled(true);
                    SpectateManagerImpl.this.lang.sendMessage(player, "SPECTATE.prevent.command",
                            new Object[] { "command", event.getMessage() });
                }
            }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void on(PlayerTeleportEvent event) {
            Player player = event.getPlayer();
            SpectatorImpl spectator = SpectateManagerImpl.this.get(player);
            if (spectator != null) {
                event.setCancelled(true);
                SpectateManagerImpl.this.lang.sendMessage(player, "SPECTATE.prevent.teleportation", new Object[0]);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
        public void on(PlayerMoveEvent event) {
            Player player = event.getPlayer();
            SpectatorImpl spectator = SpectateManagerImpl.this.get(player);
            if (spectator == null) {
                return;
            }

            Player target = spectator.getTarget();
            if (target == null || !target.isOnline()) {
                SpectateManagerImpl.this.stopSpectating(player, spectator);
                return;
            }

            Location to = event.getTo();
            if (to == null) {
                return;
            }

            if (!to.getWorld().equals(target.getWorld())
                    || to.distanceSquared(target.getLocation()) > (MAX_SPEC_DISTANCE * MAX_SPEC_DISTANCE)) {
                SpectateManagerImpl.this.teleportToTarget(player, target);
            }
        }

        @EventHandler(priority = EventPriority.LOWEST)
        public void on(PlayerInteractEvent event) {
            Player player = event.getPlayer();
            SpectatorImpl spectator = SpectateManagerImpl.this.get(player);

            if (spectator == null) {
                return;
            }

            ItemStack item = event.getItem();
            if (SpectateManagerImpl.this.isLeaveItem(item)) {
                event.setCancelled(true);
                SpectateManagerImpl.this.stopSpectating(player, spectator);
                SpectateManagerImpl.this.lang.sendMessage(player, "COMMAND.spectate.stop-spectate",
                        new Object[] { "name", spectator.getTargetName() });
                return;
            }

            event.setCancelled(true);
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerPickupItemEvent event) {
            if (SpectateManagerImpl.this.isSpectating(event.getPlayer())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerDropItemEvent event) {
            if (SpectateManagerImpl.this.isSpectating(event.getPlayer())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(InventoryClickEvent event) {
            if (event.getWhoClicked() instanceof Player
                    && SpectateManagerImpl.this.isSpectating((Player) event.getWhoClicked())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(InventoryDragEvent event) {
            if (event.getWhoClicked() instanceof Player
                    && SpectateManagerImpl.this.isSpectating((Player) event.getWhoClicked())) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(EntityDamageByEntityEvent event) {
            Player player;

            if (event.getDamager() instanceof Player) {
                player = (Player) event.getDamager();
            } else {
                if (!(event.getDamager() instanceof Projectile)
                        || !(((Projectile) event.getDamager()).getShooter() instanceof Player)) {
                    return;
                }

                player = (Player) ((Projectile) event.getDamager()).getShooter();
            }

            if (SpectateManagerImpl.this.isSpectating(player)) {
                event.setCancelled(true);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(EntityDamageEvent event) {
            if (event.getEntity() instanceof Player
                    && SpectateManagerImpl.this.isSpectating((Player) event.getEntity())) {
                event.setCancelled(true);
            }
        }
    }
}
