package me.realized.duels.duel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.match.MatchStartEvent;
import me.realized.duels.api.event.match.MatchEndEvent.Reason;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.arena.MatchImpl;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.data.MatchData;
import me.realized.duels.data.UserData;
import me.realized.duels.data.UserManagerImpl;
import me.realized.duels.hook.hooks.VaultHook;
import me.realized.duels.inventories.InventoryManager;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.player.PlayerInfo;
import me.realized.duels.player.PlayerInfoManager;
import me.realized.duels.queue.Queue;
import me.realized.duels.queue.QueueManager;
import me.realized.duels.setting.Settings;
import me.realized.duels.teleport.Teleport;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.Log;
import me.realized.duels.util.NumberUtil;
import me.realized.duels.util.PlayerUtil;
import me.realized.duels.util.StringUtil;
import me.realized.duels.util.TextBuilder;
import me.realized.duels.util.compat.CompatUtil;
import me.realized.duels.util.compat.Titles;
import me.realized.duels.util.function.Pair;
import me.realized.duels.util.inventory.InventoryUtil;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;

public class DuelManager implements Loadable {
    private static final Calendar GREGORIAN_CALENDAR = new GregorianCalendar();

    // /chest ayarları
    private static final long CHEST_OPEN_AT_START_MILLIS = 15000L;
    private static final long CHEST_OPEN_AFTER_END_MILLIS = 15000L;
    private static final long CHEST_GUI_BYPASS_MILLIS = 3000L;
    private static final String DUEL_WIN_SOUND = "ui.toast.challenge_complete";
    private static final String DUEL_WIN_TITLE = "§aKAZANDIN";

    // Bahisli düello duyuru eşiği
    private static final int DUEL_BROADCAST_BET_THRESHOLD = 500;

    private final DuelsPlugin plugin;
    private final Config config;
    private final Lang lang;
    private final UserManagerImpl userDataManager;
    private final ArenaManagerImpl arenaManager;
    private final PlayerInfoManager playerManager;
    private final InventoryManager inventoryManager;
    private final BossBarManager bossBarManager;

    private QueueManager queueManager;
    private Teleport teleport;
    private VaultHook vault;
    private int durationCheckTask;

    // Düello başında ilk 15 saniye /chest açık
    private final Map<UUID, Long> chestStartUntil = new HashMap<UUID, Long>();

    // Düello bitince 15 saniye /chest açık
    private final Map<UUID, Long> chestEndUntil = new HashMap<UUID, Long>();

    // /chest komutu çalıştığında kısa süreli inventory bypass
    private final Map<UUID, Long> chestOpenBypass = new HashMap<UUID, Long>();
    private final Map<UUID, Long> chestAutoCloseUntil = new HashMap<UUID, Long>();

    public DuelManager(DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.lang = plugin.getLang();
        this.userDataManager = plugin.getUserManager();
        this.arenaManager = plugin.getArenaManager();
        this.playerManager = plugin.getPlayerManager();
        this.inventoryManager = plugin.getInventoryManager();
        this.bossBarManager = new BossBarManager(plugin);

        plugin.doSyncAfter(new Runnable() {
            @Override
            public void run() {
                Bukkit.getPluginManager().registerEvents(new DuelListener(), DuelManager.this.plugin);
            }
        }, 1L);
    }

    public BossBarManager getBossBarManager() {
        return this.bossBarManager;
    }


    public void handleLoad() {
        this.queueManager = this.plugin.getQueueManager();
        this.teleport = this.plugin.getTeleport();
        this.vault = (VaultHook) this.plugin.getHookManager().getHook(VaultHook.class);

        if (this.config.getMaxDuration() > 0) {
            this.durationCheckTask = this.plugin.doSyncRepeat(new Runnable() {
                @Override
                public void run() {
                    for (ArenaImpl arena : DuelManager.this.arenaManager.getArenasImpl()) {
                        MatchImpl match = arena.getMatch();
                        if (match != null && match.getDurationInMillis() >= (long) (DuelManager.this.config.getMaxDuration() * 60) * 1000L && arena.size() > 1) {
                            for (Player player : match.getAllPlayers()) {
                                DuelManager.this.handleTie(player, arena, match, true);
                                DuelManager.this.lang.sendMessage(player, "DUEL.on-end.tie", new Object[0]);
                            }
                            arena.endMatch((UUID) null, (UUID) null, Reason.MAX_TIME_REACHED);
                        }
                    }
                }
            }, 0L, 20L).getTaskId();
        }

        this.bossBarManager.start();
    }

    public void handleUnload() {
        this.plugin.cancelTask(this.durationCheckTask);
        this.bossBarManager.stop();

        for (ArenaImpl arena : this.arenaManager.getArenasImpl()) {
            MatchImpl match = arena.getMatch();
            if (match != null) {
                int size = arena.size();
                boolean winnerDecided = size == 1;
                if (winnerDecided) {
                    Player winner = arena.first();
                    this.lang.sendMessage(winner, "DUEL.on-end.plugin-disable", new Object[0]);
                    this.handleWin(winner, arena.getOpponent(winner), arena, match);
                } else {
                    boolean ongoing = size > 1;
                    for (Player player : match.getAllPlayers()) {
                        this.lang.sendMessage(player, "DUEL.on-end.plugin-disable", new Object[0]);
                        this.handleTie(player, arena, match, ongoing);
                    }
                }
                arena.endMatch((UUID) null, (UUID) null, Reason.PLUGIN_DISABLE);
            }
        }
    }

    private void grantPostDuelChestAccess(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        this.chestStartUntil.remove(uuid);
        this.chestEndUntil.put(uuid, System.currentTimeMillis() + CHEST_OPEN_AFTER_END_MILLIS);
        this.chestOpenBypass.remove(uuid);
    }

    private void grantPostDuelChestAccess(Player... players) {
        if (players == null) {
            return;
        }

        for (Player player : players) {
            this.grantPostDuelChestAccess(player);
        }
    }

    private void scheduleChestAutoClose(Player player, long closeAt) {
        if (player == null || closeAt <= 0L) {
            return;
        }

        final UUID uuid = player.getUniqueId();
        this.chestAutoCloseUntil.put(uuid, closeAt);

        long delayMillis = Math.max(50L, closeAt - System.currentTimeMillis());
        long delayTicks = Math.max(1L, (delayMillis + 49L) / 50L);

        this.plugin.doSyncAfter(new Runnable() {
            @Override
            public void run() {
                Long trackedUntil = DuelManager.this.chestAutoCloseUntil.get(uuid);
                if (trackedUntil == null || trackedUntil.longValue() != closeAt) {
                    return;
                }

                Player online = Bukkit.getPlayer(uuid);
                DuelManager.this.chestAutoCloseUntil.remove(uuid);

                if (online == null || !online.isOnline()) {
                    return;
                }

                Inventory top = online.getOpenInventory().getTopInventory();
                if (top != null && top.getType() != InventoryType.CRAFTING) {
                    online.closeInventory();
                    online.sendMessage("§c/chest suresi doldu.");
                }
            }
        }, delayTicks);
    }

    private void clearChestState(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();
        this.chestStartUntil.remove(uuid);
        this.chestEndUntil.remove(uuid);
        this.chestOpenBypass.remove(uuid);
        this.chestAutoCloseUntil.remove(uuid);
    }

    private void playVictoryEffect(Player winner) {
        if (winner == null || !winner.isOnline()) {
            return;
        }

        winner.playSound(winner.getLocation(), DUEL_WIN_SOUND, 100.0F, 1.0F);
            Titles.send(winner, DUEL_WIN_TITLE, "", 0, 40, 10);
    }

    private void handleTie(Player player, ArenaImpl arena, MatchImpl match, boolean alive) {
        arena.remove(player);
        this.bossBarManager.remove(arena, match);
        this.grantPostDuelChestAccess(player);

        if (this.vault != null && match.getBet() > 0) {
            this.vault.add(match.getBet(), new Player[] { player });
        }

        PlayerInfo info = this.playerManager.get(player);
        List<ItemStack> items = match.getItems(player);
        if (alive) {
            PlayerUtil.reset(player);
            this.playerManager.remove(player);
            if (info != null) {
                this.teleport.tryTeleport(player, info.getLocation());
                info.restore(player);
            } else {
                this.teleport.tryTeleport(player, this.playerManager.getLobby());
            }
            InventoryUtil.addOrDrop(player, items);
        } else if (info != null) {
            info.getExtra().addAll(items);
        } else {
            InventoryUtil.addOrDrop(player, items);
        }
    }

    private void handleWin(Player player, Player opponent, ArenaImpl arena, MatchImpl match) {
        arena.remove(player);
        this.bossBarManager.remove(arena, match);
        this.grantPostDuelChestAccess(player, opponent);

        String opponentName = opponent != null ? opponent.getName() : this.lang.getMessage("GENERAL.none");
        if (this.vault != null && match.getBet() > 0) {
            int amount = match.getBet() * 2;
            this.vault.add(amount, new Player[] { player });
            this.lang.sendMessage(player, "DUEL.reward.money.message", new Object[] { "name", opponentName, "money", amount });
            String title = this.lang.getMessage("DUEL.reward.money.title", new Object[] { "name", opponentName, "money", amount });
            if (title != null) {
                Titles.send(player, title, (String) null, 0, 20, 50);
            }
        }

        PlayerInfo info = this.playerManager.get(player);
        List<ItemStack> items = match.getItems();
        if (!player.isDead()) {
            this.playerManager.remove(player);
            if (!match.isOwnInventory()) {
                PlayerUtil.reset(player);
            }

            if (info != null) {
                this.teleport.tryTeleport(player, info.getLocation());
                if (!match.isOwnInventory()) {
                    info.restore(player);
                } else {
                    PlayerUtil.reset(player, false);
                }
            }

            if (InventoryUtil.addOrDrop(player, items)) {
                this.lang.sendMessage(player, "DUEL.reward.items.message", new Object[] { "name", opponentName });
            }

            if (match.isOwnInventory()) {
                player.sendMessage("§6[Düello] §a§lTEBRİKLER! §eDüelloyu başarıyla kazandın!");
                Titles.send(player, "§a§lKAZANDIN!", "§eDüelloyu kazandın!", 10, 40, 10);
            }
        } else if (info != null) {
            info.getExtra().addAll(items);
        }
    }

    public void startMatch(Player first, Player second, Settings settings, Map<UUID, List<ItemStack>> items, Queue source) {
        KitImpl kit = settings.getKit();
        if (!settings.isOwnInventory() && kit == null) {
            this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.mode-unselected", new Object[0]);
            this.refundItems(items, first, second);
        } else if (!first.isDead() && !second.isDead()) {
            if (!this.isBlacklistedWorld(first) && !this.isBlacklistedWorld(second)) {
                if (!this.isTagged(first) && !this.isTagged(second)) {
                    if (true) {
                        if (true) {
                            if (!this.config.isPreventCreativeMode() || first.getGameMode() != GameMode.CREATIVE && second.getGameMode() != GameMode.CREATIVE) {
                                ArenaImpl arena = settings.getArena() != null ? settings.getArena() : this.arenaManager.randomArena(kit);
                                if (arena != null && arena.isAvailable()) {
                                    if (kit != null && !this.arenaManager.isSelectable(kit, arena)) {
                                        this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.arena-not-applicable", new Object[] { "kit", kit.getName(), "arena", arena.getName() });
                                        this.refundItems(items, first, second);
                                    } else {
                                        int bet = settings.getBet();
                                        if (bet > 0 && this.vault != null && this.vault.getEconomy() != null) {
                                            if (!this.vault.has(bet, new Player[] { first, second })) {
                                                this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.not-enough-money", new Object[] { "bet_amount", bet });
                                                this.refundItems(items, first, second);
                                                return;
                                            }

                                            this.vault.remove(bet, new Player[] { first, second });
                                        }

                                        MatchImpl match = arena.startMatch(kit, items, settings.getBet(), source);
                                        this.addPlayers(match, arena, kit, arena.getPositions(), first, second);
                                        this.bossBarManager.update(arena, match, this.config.getMaxDuration());

                                        if (bet >= DUEL_BROADCAST_BET_THRESHOLD) {
                                            Bukkit.broadcastMessage(
                                                    "§6[Düello] §e" + first.getName()
                                                            + " §7ile §e" + second.getName()
                                                            + " §7arasında §a" + bet
                                                            + " §7coinlik bahisli düello başladı!"
                                            );
                                        }

                                        long now = System.currentTimeMillis();
                                        this.chestStartUntil.put(first.getUniqueId(), now + CHEST_OPEN_AT_START_MILLIS);
                                        this.chestStartUntil.put(second.getUniqueId(), now + CHEST_OPEN_AT_START_MILLIS);
                                        this.chestEndUntil.remove(first.getUniqueId());
                                        this.chestEndUntil.remove(second.getUniqueId());
                                        this.chestOpenBypass.remove(first.getUniqueId());
                                        this.chestOpenBypass.remove(second.getUniqueId());

                                        if (this.config.isCdEnabled()) {
                                            Map<UUID, Pair<String, Integer>> info = new HashMap<UUID, Pair<String, Integer>>();
                                            info.put(first.getUniqueId(), new Pair<String, Integer>(second.getName(), this.getRating(kit, this.userDataManager.get(second))));
                                            info.put(second.getUniqueId(), new Pair<String, Integer>(first.getName(), this.getRating(kit, this.userDataManager.get(first))));
                                            arena.startCountdown(kit != null ? kit.getName() : this.lang.getMessage("GENERAL.none"), info);
                                        }

                                        MatchStartEvent event = new MatchStartEvent(match, new Player[] { first, second });
                                        Bukkit.getPluginManager().callEvent(event);
                                    }
                                } else {
                                    this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure." + (settings.getArena() != null ? "arena-in-use" : "no-arena-available"), new Object[0]);
                                    this.refundItems(items, first, second);
                                }
                            } else {
                                this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.in-creative-mode", new Object[0]);
                                this.refundItems(items, first, second);
                            }
                        } else {
                            this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.not-in-duelzone", new Object[0]);
                            this.refundItems(items, first, second);
                        }
                    } else {
                        this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.player-moved", new Object[0]);
                        this.refundItems(items, first, second);
                    }
                } else {
                    this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.is-tagged", new Object[0]);
                    this.refundItems(items, first, second);
                }
            } else {
                this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.in-blacklisted-world", new Object[0]);
                this.refundItems(items, first, second);
            }
        } else {
            this.lang.sendMessage(Arrays.asList(first, second), "DUEL.start-failure.player-is-dead", new Object[0]);
            this.refundItems(items, first, second);
        }
    }

    private void refundItems(Map<UUID, List<ItemStack>> items, Player... players) {
        if (items != null) {
            Arrays.stream(players).forEach(player ->
                    InventoryUtil.addOrDrop(player, (Collection) items.getOrDefault(player.getUniqueId(), Collections.emptyList()))
            );
        }
    }

    private boolean isBlacklistedWorld(Player player) {
        return this.config.getBlacklistedWorlds().contains(player.getWorld().getName());
    }

    private boolean isTagged(Player player) {
        return false;
    }

    private boolean notInLoc(Player player, Location location) {
        if (location == null) {
            return false;
        } else {
            Location source = player.getLocation();
            return !source.getWorld().equals(location.getWorld())
                    || source.getBlockX() != location.getBlockX()
                    || source.getBlockY() != location.getBlockY()
                    || source.getBlockZ() != location.getBlockZ();
        }
    }

    private boolean notInDz(Player player, String duelzone) {
        return false;
    }

    private int getRating(KitImpl kit, UserData user) {
        return user != null ? user.getRating(kit) : this.config.getDefaultRating();
    }

    private void addPlayers(MatchImpl match, ArenaImpl arena, KitImpl kit, Map<Integer, Location> locations, Player... players) {
        int position = 0;

        for (Player player : players) {
            if (match.getSource() == null) {
                this.queueManager.remove(player);
            }

            if (player.getAllowFlight()) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }

            player.closeInventory();
            this.playerManager.create(player);
            ++position;
            this.teleport.tryTeleport(player, locations.get(position));

            if (kit != null) {
                PlayerUtil.reset(player);
                kit.equip(player);
            }

            if (this.config.isStartCommandsEnabled() && (match.getSource() != null || !this.config.isStartCommandsQueueOnly())) {
                try {
                    for (String command : this.config.getStartCommands()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
                } catch (Exception ex) {
                    Log.warn(this, "Error while running match start commands: " + ex.getMessage());
                }
            }

            arena.add(player);
        }
    }

    private void handleInventories(MatchImpl match) {
        if (this.config.isDisplayInventories()) {
            String color = this.lang.getMessage("DUEL.inventories.name-color");
            TextBuilder builder = TextBuilder.of(this.lang.getMessage("DUEL.inventories.message"));
            Set<Player> players = match.getAllPlayers();
            Iterator<Player> iterator = players.iterator();

            while (iterator.hasNext()) {
                Player player = iterator.next();
                builder.add(StringUtil.color(color + player.getName()), Action.RUN_COMMAND, "/duel _ " + player.getUniqueId());
                if (iterator.hasNext()) {
                    builder.add(StringUtil.color(color + ", "));
                }
            }

            builder.send(players);
        }
    }

    private void handleStats(MatchImpl match, UserData winner, UserData loser, MatchData matchData) {
        if (winner != null && loser != null) {
            winner.addWin();
            loser.addLoss();
            winner.addMatch(matchData);
            loser.addMatch(matchData);
            KitImpl kit = match.getKit();
            int winnerRating = kit == null ? winner.getRating() : winner.getRating(kit);
            int loserRating = kit == null ? loser.getRating() : loser.getRating(kit);
            int change = 0;

            if (this.config.isRatingEnabled() && (match.isFromQueue() || !this.config.isRatingQueueOnly())) {
                change = NumberUtil.getChange(this.config.getKFactor(), winnerRating, loserRating);
                winner.setRating(kit, winnerRating += change);
                loser.setRating(kit, loserRating -= change);
            }

            String message = this.lang.getMessage("DUEL.on-end.opponent-defeat", new Object[] {
                    "winner", winner.getName(),
                    "loser", loser.getName(),
                    "health", matchData.getHealth(),
                    "kit", matchData.getKit(),
                    "arena", match.getArena().getName(),
                    "winner_rating", winnerRating,
                    "loser_rating", loserRating,
                    "change", change
            });

            if (message == null) {
                return;
            }

            if (this.config.isArenaOnlyEndMessage()) {
                match.getArena().broadcast(message);
            }
        }
    }

    private class DuelListener implements Listener {
        private DuelListener() {
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        public void on(PlayerDeathEvent event) {
            Player player = event.getEntity();
            if (DuelManager.this.plugin.getPartyManager() != null && DuelManager.this.plugin.getPartyManager().isInPartyMatch(player.getUniqueId())) {
                return;
            }
            final ArenaImpl arena = DuelManager.this.arenaManager.get(player);
            if (arena != null) {
                final MatchImpl match = arena.getMatch();
                if (match != null) {
                    Player winnerPreview = arena.getOpponent(player);
                    DuelManager.this.grantPostDuelChestAccess(player, winnerPreview);

                    Inventory top = player.getOpenInventory().getTopInventory();
                    if (top.getType() == InventoryType.CRAFTING) {
                        top.clear();
                    }

                    if (match.isOwnInventory()) {
                        List<ItemStack> loserDrops = new ArrayList<>();
                        for (ItemStack item : player.getInventory().getContents()) {
                            if (item != null && item.getType() != Material.AIR) {
                                loserDrops.add(item.clone());
                            }
                        }
                        for (ItemStack item : player.getInventory().getArmorContents()) {
                            if (item != null && item.getType() != Material.AIR) {
                                loserDrops.add(item.clone());
                            }
                        }

                        // Kendi eşyalarıyla düelloda kaybedenin eşyaları düello arenasında yere saçılır:
                        if (DuelManager.this.config.isOwnInventoryDropInventoryItems()) {
                            Location deathLoc = player.getLocation();
                            if (deathLoc != null && deathLoc.getWorld() != null) {
                                for (ItemStack item : loserDrops) {
                                    deathLoc.getWorld().dropItemNaturally(deathLoc, item);
                                }
                            }
                        }

                        player.getInventory().clear();
                        player.getInventory().setArmorContents(null);
                        event.getDrops().clear();
                        event.setKeepLevel(true);
                        event.setDroppedExp(0);
                        event.setKeepInventory(false);

                        PlayerInfo loserInfo = DuelManager.this.playerManager.get(player);
                        if (loserInfo != null) {
                            loserInfo.getItems().clear();
                        }
                        player.sendMessage("§6[Düello] §c§lKAYBETTİN! §cÜzerindeki tüm eşyalar arenaya saçıldı!");
                        Titles.send(player, "§c§lKAYBETTİN!", "§cEşyalarını kaybettin!", 10, 40, 10);
                    } else {
                        event.getDrops().clear();
                        event.setKeepLevel(true);
                        event.setDroppedExp(0);
                        event.setKeepInventory(false);
                    }

                    DuelManager.this.inventoryManager.create(player, true);
                    arena.remove(player);

                    // Respawn ekranını sıfır gecikmeyle atla ve oyuncuyu doğrudan duello lobisine/spawna gönder
                    DuelManager.this.plugin.doSyncAfter(new Runnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                try {
                                    player.spigot().respawn();
                                } catch (Exception ignored) {}
                            }
                        }
                    }, 1L);

                    if (arena.size() > 0) {
                        DuelManager.this.plugin.doSyncAfter(new Runnable() {
                            @Override
                            public void run() {
                                if (arena.size() == 0) {
                                    for (Player matchPlayer : match.getAllPlayers()) {
                                        DuelManager.this.handleTie(matchPlayer, arena, match, false);
                                        DuelManager.this.lang.sendMessage(matchPlayer, "DUEL.on-end.tie", new Object[0]);
                                    }
                                    DuelManager.this.plugin.doSyncAfter(new Runnable() {
                                        @Override
                                        public void run() {
                                            DuelManager.this.handleInventories(match);
                                        }
                                    }, 1L);
                                    arena.endMatch((UUID) null, (UUID) null, Reason.TIE);
                                } else {
                                    final Player winner = arena.first();
                                    DuelManager.this.playVictoryEffect(winner);
                                    DuelManager.this.inventoryManager.create(winner, false);

                                    final Player loser = player;
                                    final String kitName = match.getKit() != null ? match.getKit().getName() : DuelManager.this.lang.getMessage("GENERAL.none");
                                    double health = Math.ceil(winner.getHealth()) * 0.5D;
                                    long duration = System.currentTimeMillis() - match.getStart();
                                    long time = DuelManager.GREGORIAN_CALENDAR.getTimeInMillis();
                                    double betAmount = match.getBet(); // 🔥 bahis buradan geliyor

                                    MatchData matchData = new MatchData(
                                            winner.getName(),
                                            loser.getName(),
                                            kitName,
                                            time,
                                            duration,
                                            health,
                                            betAmount
                                    );
                                    DuelManager.this.handleStats(match, DuelManager.this.userDataManager.get(winner), DuelManager.this.userDataManager.get(loser), matchData);
                                    DuelManager.this.plugin.doSyncAfter(new Runnable() {
                                        @Override
                                        public void run() {
                                            DuelManager.this.handleInventories(match);
                                        }
                                    }, 1L);
                                    DuelManager.this.plugin.doSyncAfter(new Runnable() {
                                        @Override
                                        public void run() {
                                            DuelManager.this.handleWin(winner, loser, arena, match);
                                            if (DuelManager.this.config.isEndCommandsEnabled() && (match.isFromQueue() || !DuelManager.this.config.isEndCommandsQueueOnly())) {
                                                try {
                                                    for (String command : DuelManager.this.config.getEndCommands()) {
                                                        Bukkit.dispatchCommand(
                                                                Bukkit.getConsoleSender(),
                                                                command.replace("%winner%", winner.getName())
                                                                        .replace("%loser%", loser.getName())
                                                                        .replace("%kit%", kitName)
                                                                        .replace("%arena%", arena.getName())
                                                                        .replace("%bet_amount%", String.valueOf(match.getBet()))
                                                        );
                                                    }
                                                } catch (Exception ex) {
                                                    Log.warn(DuelManager.this, "Error while running match end commands: " + ex.getMessage());
                                                }
                                            }

                                            arena.endMatch(winner.getUniqueId(), loser.getUniqueId(), Reason.OPPONENT_DEFEAT);
                                        }
                                    }, (long) DuelManager.this.config.getTeleportDelay() * 20L);
                                }
                            }
                        }, 1L);
                    }
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(EntityDamageEvent event) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                if (DuelManager.this.plugin.getPartyManager() != null && DuelManager.this.plugin.getPartyManager().isInPartyMatch(player.getUniqueId())) {
                    return;
                }
                ArenaImpl arena = DuelManager.this.arenaManager.get(player);
                if (arena != null && arena.isEndGame()) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler
        public void on(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            if (DuelManager.this.plugin.getPartyManager() != null && DuelManager.this.plugin.getPartyManager().isInPartyMatch(player.getUniqueId())) {
                return;
            }
            DuelManager.this.clearChestState(player);
            DuelManager.this.bossBarManager.remove(player);

            if (DuelManager.this.arenaManager.isInMatch(player)) {
                player.setHealth(0.0D);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerDropItemEvent event) {
            ArenaImpl arena = DuelManager.this.arenaManager.get(event.getPlayer());
            if (arena != null && arena.getMatch() != null && arena.getMatch().isOwnInventory()) {
                return; // Kendi eşyalarıyla düelloda eşya atabilmeli
            }
            if (DuelManager.this.config.isPreventItemDrop() && DuelManager.this.arenaManager.isInMatch(event.getPlayer())) {
                event.setCancelled(true);
                DuelManager.this.lang.sendMessage(event.getPlayer(), "DUEL.prevent.item-drop", new Object[0]);
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerPickupItemEvent event) {
            if (CompatUtil.isPre1_13()) {
                ArenaImpl arena = DuelManager.this.arenaManager.get(event.getPlayer());
                if (arena != null && arena.getMatch() != null && arena.getMatch().isOwnInventory()) {
                    return; // Kendi eşyalarıyla düelloda yerdeki eşyaları toplayabilmeli
                }
                if (DuelManager.this.config.isPreventItemPickup() && DuelManager.this.arenaManager.isInMatch(event.getPlayer())) {
                    event.setCancelled(true);
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerCommandPreprocessEvent event) {
            Player player = event.getPlayer();
            String fullMessage = event.getMessage();
            String command = fullMessage.substring(1).split(" ")[0].toLowerCase();
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            boolean isChestCommand = command.equals("chest") || command.equals("chesti") || command.equals("sandik") || command.equals("ec") || command.equals("enderchest");
            boolean inMatch = DuelManager.this.arenaManager.isInMatch(player);

            ArenaImpl arena = inMatch ? DuelManager.this.arenaManager.get(player) : null;
            MatchImpl match = arena != null ? arena.getMatch() : null;
            boolean isOwnInventoryMatch = match != null && match.isOwnInventory();

            if (isChestCommand) {
                // Kitli düellolarda /chest kesinlikle yasaktır! (Sadece kendi eşyalarınla modda geçerli)
                if (inMatch && !isOwnInventoryMatch) {
                    event.setCancelled(true);
                    player.sendMessage("§cKitli düellolarda /chest komutu kullanılamaz! Yalnızca kendi eşyalarınla düelloda geçerlidir.");
                    return;
                }

                Long endUntil = DuelManager.this.chestEndUntil.get(uuid);
                if (endUntil != null) {
                    if (now <= endUntil) {
                        DuelManager.this.chestOpenBypass.put(uuid, now + CHEST_GUI_BYPASS_MILLIS);
                        DuelManager.this.scheduleChestAutoClose(player, endUntil.longValue());
                        return;
                    }

                    DuelManager.this.chestEndUntil.remove(uuid);
                    DuelManager.this.chestAutoCloseUntil.remove(uuid);
                }

                if (inMatch) {
                    Long startUntil = DuelManager.this.chestStartUntil.get(uuid);

                    if (startUntil != null && now <= startUntil) {
                        DuelManager.this.chestOpenBypass.put(uuid, now + CHEST_GUI_BYPASS_MILLIS);
                        DuelManager.this.scheduleChestAutoClose(player, startUntil.longValue());
                        return;
                    }

                    DuelManager.this.chestAutoCloseUntil.remove(uuid);
                    event.setCancelled(true);
                    player.sendMessage("§c/chest komutu düelloda sadece ilk 15 saniye kullanılabilir.");
                    return;
                }

                return;
            }

            if (inMatch) {
                if (isOwnInventoryMatch) {
                    // Kendi eşyalarıyla düelloda: Whitelist komutlarına izin ver
                    if (DuelManager.this.config.isBlockAllCommands()) {
                        if (DuelManager.this.config.getWhitelistedCommands().contains(command)) {
                            return;
                        }
                    } else if (!DuelManager.this.config.getBlacklistedCommands().contains(command)) {
                        return;
                    }
                } else {
                    // Kitli düelloda: Sandık/çanta komutları whitelistte olsa bile engellenir
                    if (isChestCommand) {
                        event.setCancelled(true);
                        player.sendMessage("§cKitli düellolarda bu komutu kullanamazsınız!");
                        return;
                    }

                    if (DuelManager.this.config.isBlockAllCommands()) {
                        if (DuelManager.this.config.getWhitelistedCommands().contains(command)) {
                            return;
                        }
                    } else if (!DuelManager.this.config.getBlacklistedCommands().contains(command)) {
                        return;
                    }
                }

                event.setCancelled(true);
                DuelManager.this.lang.sendMessage(player, "DUEL.prevent.command", new Object[] { "command", event.getMessage() });
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(PlayerTeleportEvent event) {
            Player player = event.getPlayer();
            Location to = event.getTo();
            if (DuelManager.this.config.isLimitTeleportEnabled()
                    && event.getCause() != TeleportCause.ENDER_PEARL
                    && event.getCause() != TeleportCause.SPECTATE
                    && !player.hasMetadata(Teleport.METADATA_KEY)
                    && DuelManager.this.arenaManager.isInMatch(player)) {
                Location from = event.getFrom();
                if (!from.getWorld().equals(to.getWorld()) || !(from.distance(to) <= DuelManager.this.config.getDistanceAllowed())) {
                    event.setCancelled(true);
                    DuelManager.this.lang.sendMessage(player, "DUEL.prevent.teleportation", new Object[0]);
                }
            }
        }

        @EventHandler(ignoreCancelled = true)
        public void on(InventoryOpenEvent event) {
            if (!DuelManager.this.config.isPreventInventoryOpen()) {
                return;
            }

            Player player = (Player) event.getPlayer();
            UUID uuid = player.getUniqueId();
            long now = System.currentTimeMillis();

            Long bypassUntil = DuelManager.this.chestOpenBypass.get(uuid);
            if (bypassUntil != null) {
                if (now <= bypassUntil) {
                    DuelManager.this.chestOpenBypass.remove(uuid);
                    return;
                }
                DuelManager.this.chestOpenBypass.remove(uuid);
            }

            if (DuelManager.this.arenaManager.isInMatch(player)) {
                event.setCancelled(true);
                DuelManager.this.lang.sendMessage(player, "DUEL.prevent.inventory-open", new Object[0]);
            }
        }

        @EventHandler
        public void on(InventoryCloseEvent event) {
            if (event.getPlayer() instanceof Player && event.getInventory().getType() != InventoryType.CRAFTING) {
                DuelManager.this.chestAutoCloseUntil.remove(((Player) event.getPlayer()).getUniqueId());
            }
        }
    }
}
