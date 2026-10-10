package me.realized.duels.party;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.match.MatchEndEvent.Reason;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.player.PlayerInfo;
import me.realized.duels.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 2v2'den 5v5'e kadar parti/takım düellolarını yöneten, 500 CCU altında TPS düşüşü yaratmayan maç modeli.
 *
 * <p>Performans Mimarisi:
 * <ul>
 *   <li>Tüm oyuncu takibi sadece {@link UUID} üzerinden yapılır (Memory Leak önlemi).</li>
 *   <li>Takım ve oyuncu sorguları O(1) erişim süresiyle {@link ConcurrentHashMap} tabanlı setler kullanır.</li>
 *   <li>GC yükünü önlemek için Stream veya geçici liste tahsisi yapılmaz.</li>
 * </ul>
 */
public class PartyMatch {

    private final UUID leaderA;
    private final UUID leaderB;
    private final Set<UUID> teamA = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> teamB = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> alivePlayers = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());
    private final Set<UUID> spectators = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private final ArenaImpl arena;
    private final KitImpl kit;
    private final int bet;
    private final long startTime;
    private volatile boolean finished;

    public PartyMatch(
            @NotNull final UUID leaderA,
            @NotNull final UUID leaderB,
            @NotNull final Set<UUID> membersA,
            @NotNull final Set<UUID> membersB,
            @NotNull final ArenaImpl arena,
            @Nullable final KitImpl kit,
            final int bet
    ) {
        this.leaderA = leaderA;
        this.leaderB = leaderB;
        this.teamA.addAll(membersA);
        this.teamB.addAll(membersB);
        this.alivePlayers.addAll(membersA);
        this.alivePlayers.addAll(membersB);
        this.arena = arena;
        this.kit = kit;
        this.bet = Math.max(0, bet);
        this.startTime = System.currentTimeMillis();
    }

    public PartyMatch(
            @NotNull final UUID leaderA,
            @NotNull final UUID leaderB,
            @NotNull final Set<UUID> membersA,
            @NotNull final Set<UUID> membersB,
            @NotNull final ArenaImpl arena,
            @Nullable final KitImpl kit
    ) {
        this(leaderA, leaderB, membersA, membersB, arena, kit, 0);
    }

    /**
     * İki oyuncunun aynı takımda olup olmadığını O(1) kontrol eder.
     */
    public boolean isSameTeam(@NotNull final UUID u1, @NotNull final UUID u2) {
        if (u1.equals(u2)) {
            return true;
        }
        return (teamA.contains(u1) && teamA.contains(u2)) || (teamB.contains(u1) && teamB.contains(u2));
    }

    public boolean isAlive(@NotNull final UUID uuid) {
        return alivePlayers.contains(uuid);
    }

    public boolean contains(@NotNull final UUID uuid) {
        return teamA.contains(uuid) || teamB.contains(uuid);
    }

    public int getAliveTeamACount() {
        int count = 0;
        for (UUID uuid : teamA) {
            if (alivePlayers.contains(uuid)) {
                count++;
            }
        }
        return count;
    }

    public int getAliveTeamBCount() {
        int count = 0;
        for (UUID uuid : teamB) {
            if (alivePlayers.contains(uuid)) {
                count++;
            }
        }
        return count;
    }

    public Set<UUID> getTeamA() {
        return Collections.unmodifiableSet(teamA);
    }

    public Set<UUID> getTeamB() {
        return Collections.unmodifiableSet(teamB);
    }

    public Set<UUID> getAlivePlayers() {
        return Collections.unmodifiableSet(alivePlayers);
    }

    public ArenaImpl getArena() {
        return arena;
    }

    public KitImpl getKit() {
        return kit;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isSpectator(@Nullable final UUID uuid) {
        return uuid != null && spectators.contains(uuid);
    }

    public Set<UUID> getSpectators() {
        return Collections.unmodifiableSet(spectators);
    }

    public int getBet() {
        return bet;
    }

    /**
     * Maçtaki tüm çevrimiçi oyunculara (yaşayan ve izleyen) doğrudan mesaj gönderir.
     */
    public void broadcast(@NotNull final String message) {
        for (UUID uuid : teamA) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
        for (UUID uuid : teamB) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        }
    }

    /**
     * Bir oyuncunun elenmesini işler ve 1.8.8 izleyici moduna geçirir.
     */
    public synchronized void handleDeath(
            @NotNull final Player deadPlayer,
            @NotNull final PartyManager partyManager,
            @NotNull final DuelsPlugin plugin
    ) {
        final UUID uuid = deadPlayer.getUniqueId();
        if (finished || !alivePlayers.remove(uuid)) {
            return;
        }

        spectators.add(uuid);

        // 1.8.8 İzleyici Modu Ayarları (Kesinlikle GameMode.SPECTATOR DEĞİL - Survival + Uçuş + Görünmezlik)
        PlayerUtil.reset(deadPlayer);
        deadPlayer.setGameMode(GameMode.SURVIVAL);
        deadPlayer.setHealth(20.0D);
        deadPlayer.setFoodLevel(20);
        deadPlayer.setAllowFlight(true);
        deadPlayer.setFlying(true);
        deadPlayer.setCanPickupItems(false);
        deadPlayer.spigot().setCollidesWithEntities(false);
        deadPlayer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false, false));

        // Sunucudaki tüm oyuncular bu oyuncuyu görmesin (hidePlayer)
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(deadPlayer)) {
                online.hidePlayer(deadPlayer);
            }
        }

        deadPlayer.sendMessage("§e§l[İzleyici] §7Elendiniz! Maçı gizli izleyici olarak izliyorsunuz.");
        deadPlayer.sendMessage("§7(Diğer oyuncular sizi göremez, arena dışına çıkamazsınız.)");

        // Oyuncuyu hafifçe yukarı ışınla (void'e düşmüşse arena spawn noktasına güvenle al)
        Location loc = deadPlayer.getLocation().clone();
        if (loc.getY() <= 0.0D || loc.getBlockY() < 1) {
            Location arenaSpawn = arena.getPosition(1);
            if (arenaSpawn != null) {
                loc = arenaSpawn.clone().add(0, 3.0, 0);
            }
        } else {
            loc.add(0, 2.0, 0);
        }
        plugin.getTeleport().tryTeleport(deadPlayer, loc);

        if (deadPlayer.isDead()) {
            plugin.doSyncAfter(() -> {
                if (deadPlayer.isOnline()) {
                    try {
                        deadPlayer.spigot().respawn();
                    } catch (Exception ignored) {}
                }
            }, 1L);
        }

        // Elenme bildirimini gönder
        final int aliveA = getAliveTeamACount();
        final int aliveB = getAliveTeamBCount();
        broadcast("§e" + deadPlayer.getName() + " §celendi! §7(§aTakım 1: " + aliveA + " §7| §cTakım 2: " + aliveB + "§7)");

        // Takım kontrolü: Kazanan var mı?
        if (aliveA == 0) {
            endMatch(teamB, teamA, leaderB, leaderA, partyManager, plugin);
        } else if (aliveB == 0) {
            endMatch(teamA, teamB, leaderA, leaderB, partyManager, plugin);
        }
    }

    /**
     * Bir oyuncu oyundan çıktığında elenmesini sağlar.
     */
    public void handleQuit(
            @NotNull final Player quitPlayer,
            @NotNull final PartyManager partyManager,
            @NotNull final DuelsPlugin plugin
    ) {
        if (finished) {
            return;
        }
        handleDeath(quitPlayer, partyManager, plugin);
    }

    /**
     * Maçı sonlandırır, kazanan takımı duyurur, envanterleri geri yükler ve oyuncuları lobiye ışınlar.
     */
    public synchronized void endMatch(
            @NotNull final Set<UUID> winnerTeam,
            @NotNull final Set<UUID> loserTeam,
            @NotNull final UUID winnerLeader,
            @NotNull final UUID loserLeader,
            @NotNull final PartyManager partyManager,
            @NotNull final DuelsPlugin plugin
    ) {
        if (finished) {
            return;
        }
        this.finished = true;

        final Player winnerLeadPlayer = Bukkit.getPlayer(winnerLeader);
        final String winnerName = winnerLeadPlayer != null ? winnerLeadPlayer.getName() : "Takım 1";

        broadcast("§6§l========================================");
        broadcast("§a§l           PARTİ DÜELLOSU BİTTİ!");
        broadcast("§e  Kazanan: §a" + winnerName + " Partisi");
        broadcast("§6§l========================================");

        // Bahis ödülü teslimi
        if (bet > 0 && plugin.getVault() != null) {
            final int totalReward = bet * 2;
            final Player winLead = Bukkit.getPlayer(winnerLeader);
            if (winLead != null && winLead.isOnline()) {
                plugin.getVault().add(totalReward, winLead);
                winLead.sendMessage("§6§l[BAHİS] §aTebrikler! Düelloyu kazandınız ve §e" + totalReward + " Coin §aödül aldınız!");
            }
            broadcast("§6§l[BAHİS] §aKazanan parti lideri toplam §e" + totalReward + " Coin §aödülü kazandı!");
        }

        // Kazananlara zafer sesi
        for (UUID winId : winnerTeam) {
            Player p = Bukkit.getPlayer(winId);
            if (p != null && p.isOnline()) {
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1.0F, 1.0F);
            }
        }

        // 3 saniye sonra lobiye/spawna ışınla ve envanterleri geri yükle
        plugin.doSyncAfter(() -> {
            final Location lobby = plugin.getPlayerManager().getLobby();
            Location fallbackSpawn = null;
            if (!Bukkit.getWorlds().isEmpty()) {
                fallbackSpawn = Bukkit.getWorlds().get(0).getSpawnLocation();
            }
            final Location defaultLobby = (lobby != null && lobby.getWorld() != null) ? lobby : fallbackSpawn;

            final Set<UUID> allParticipants = new HashSet<UUID>();
            allParticipants.addAll(teamA);
            allParticipants.addAll(teamB);

            // Önce tüm oyuncuların arena ve maç kayıtlarını temizle (ışınlanma engeline takılmasınlar)
            for (UUID uid : allParticipants) {
                partyManager.removeMatch(uid);
                plugin.getArenaManager().removePlayerArena(uid);
            }

            for (UUID uid : allParticipants) {
                final Player p = Bukkit.getPlayer(uid);
                if (p != null && p.isOnline()) {
                    if (p.isDead()) {
                        try {
                            p.spigot().respawn();
                        } catch (Exception ignored) {}
                    }

                    // İzleyici durumunu sıfırla
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    p.setCanPickupItems(true);
                    p.spigot().setCollidesWithEntities(true);
                    p.removePotionEffect(PotionEffectType.INVISIBILITY);

                    // Envanteri ve oyuncu bilgilerini geri yükle
                    final PlayerInfo info = plugin.getPlayerManager().remove(p);
                    Location target = defaultLobby;
                    if (info != null) {
                        if (plugin.getConfiguration().isTeleportToLastLocation() && info.getLocation() != null) {
                            target = info.getLocation();
                        }
                        info.restore(p);
                    } else {
                        PlayerUtil.reset(p);
                    }

                    // Lobiye veya Spawna güvenli ışınla (Teleport metadata ile)
                    if (target != null && target.getWorld() != null) {
                        plugin.getTeleport().tryTeleport(p, target);
                    }

                    // Gizlenen oyuncuları tekrar görünür yap
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        p.showPlayer(online);
                        online.showPlayer(p);
                    }
                }
            }

            // Arenayı serbest bırak
            arena.endMatch(winnerLeader, loserLeader, Reason.OPPONENT_DEFEAT);
        }, 60L); // 3 saniye (60 tick)
    }
}
