package me.realized.duels.party;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.util.PlayerUtil;
import me.realized.duels.util.function.Pair;
import me.realized.duels.util.TextBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 500 CCU altında TPS düşüşü yaratmayan, O(1) arama süresine sahip Parti Yöneticisi.
 *
 * <p>Performans Mimarisi:
 * <ul>
 *   <li>Tüm oyuncu referansları sadece {@link UUID} olarak saklanır.</li>
 *   <li>{@code partyByPlayer} ve {@code matchByPlayer} için {@link ConcurrentHashMap} kullanılır.</li>
 *   <li>O(1) dost ateşi (Friendly Fire) kontrolü ile hasar anında sıfır döngü çalıştırılır.</li>
 * </ul>
 */
public class PartyManager {

    private final DuelsPlugin plugin;
    private final Map<UUID, Party> partyByPlayer = new ConcurrentHashMap<UUID, Party>();
    private final Map<UUID, PartyMatch> matchByPlayer = new ConcurrentHashMap<UUID, PartyMatch>();
    private final Map<UUID, PartyDuelRequest> duelRequests = new ConcurrentHashMap<UUID, PartyDuelRequest>();
    private final Map<UUID, Long> chatInvitePrompts = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, UUID> pendingPartyInvites = new ConcurrentHashMap<UUID, UUID>();

    public PartyManager(@NotNull final DuelsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Oyuncunun UUID'sinden doğrudan partisine O(1) erişim.
     */
    @Nullable
    public Party getParty(@Nullable final UUID playerId) {
        return playerId != null ? partyByPlayer.get(playerId) : null;
    }

    /**
     * Oyuncunun bir partide olup olmadığını kontrol eder. O(1)
     */
    public boolean isInParty(@Nullable final UUID playerId) {
        return playerId != null && partyByPlayer.containsKey(playerId);
    }

    /**
     * Yeni bir parti oluşturur. Oyuncu zaten bir partideyse null döner.
     */
    @Nullable
    public Party createParty(@NotNull final UUID leaderId) {
        Objects.requireNonNull(leaderId, "leaderId");
        if (isInParty(leaderId)) {
            return null;
        }
        final Party party = new Party(leaderId);
        partyByPlayer.put(leaderId, party);
        return party;
    }

    /**
     * Partiyi tamamen dağıtır ve tüm üyelerin eşleştirmelerini O(1) ile temizler.
     */
    public void disbandParty(@Nullable final Party party) {
        if (party == null) {
            return;
        }
        for (UUID member : party.getMembers()) {
            partyByPlayer.remove(member);
        }
        for (UUID invited : party.getInvites()) {
            pendingPartyInvites.remove(invited);
        }
        party.clear();
    }

    /**
     * Partiye yeni üye ekler ve haritaya O(1) kaydeder.
     */
    public boolean addMember(@NotNull final Party party, @NotNull final UUID playerId) {
        Objects.requireNonNull(party, "party");
        Objects.requireNonNull(playerId, "playerId");
        if (isInParty(playerId) || party.isFull()) {
            return false;
        }
        if (party.addMember(playerId)) {
            partyByPlayer.put(playerId, party);
            pendingPartyInvites.remove(playerId);
            return true;
        }
        return false;
    }

    /**
     * Oyuncuyu partisinden çıkarır. Lider ayrıldıysa partiyi devreder veya dağıtır.
     */
    public boolean removeMember(@NotNull final UUID playerId) {
        pendingPartyInvites.remove(playerId);
        final Party party = partyByPlayer.remove(playerId);
        if (party == null) {
            return false;
        }
        party.removeMember(playerId);
        if (party.size() == 0) {
            disbandParty(party);
        } else if (party.isLeader(playerId)) {
            // Lider ayrıldı: Sıradaki üyeyi yeni lider yap
            final UUID newLeader = party.getMembers().iterator().next();
            party.setLeader(newLeader);
            final Player newLeaderPlayer = Bukkit.getPlayer(newLeader);
            final String name = newLeaderPlayer != null ? newLeaderPlayer.getName() : "Bilinmeyen";
            party.broadcast("§eLider partiden ayrıldığı için yeni lider §b" + name + " §eoldu!");
        }
        return true;
    }

    /**
     * Tek bir O(1) kontrolüyle iki oyuncunun aynı partide veya düelloda aynı takımda olup olmadığını döner.
     * Saniyede yüzlerce kez tetiklenebilecek PvP hasar dinleyicisi için optimize edilmiştir.
     */
    public boolean isFriendlyFire(@Nullable final UUID damagerId, @Nullable final UUID victimId) {
        if (damagerId == null || victimId == null || damagerId.equals(victimId)) {
            return false;
        }

        // 1. Düello Maç İçi Takım Kontrolü (O(1))
        final PartyMatch match = matchByPlayer.get(damagerId);
        if (match != null) {
            return match.isSameTeam(damagerId, victimId);
        }

        // 2. Normal Parti İçi Kontrol (O(1))
        final Party damagerParty = partyByPlayer.get(damagerId);
        return damagerParty != null && damagerParty.contains(victimId);
    }

    /**
     * Oyuncunun aktif bir parti maçında olup olmadığını döner. O(1)
     */
    public boolean isInPartyMatch(@Nullable final UUID playerId) {
        return playerId != null && matchByPlayer.containsKey(playerId);
    }

    /**
     * Oyuncunun parti maçını döner. O(1)
     */
    @Nullable
    public PartyMatch getMatch(@Nullable final UUID playerId) {
        return playerId != null ? matchByPlayer.get(playerId) : null;
    }

    public void setMatch(@NotNull final UUID playerId, @NotNull final PartyMatch match) {
        matchByPlayer.put(playerId, match);
    }

    public void removeMatch(@Nullable final UUID playerId) {
        if (playerId != null) {
            matchByPlayer.remove(playerId);
        }
    }

    /**
     * Düello isteği kaydeder (Hedef parti lideri UUID'si ile indekslenir).
     */
    public void sendDuelRequest(@NotNull final UUID senderLeader, @NotNull final UUID targetLeader, @Nullable final KitImpl kit, final int bet) {
        duelRequests.put(targetLeader, new PartyDuelRequest(senderLeader, targetLeader, kit, bet));
    }

    public void sendDuelRequest(@NotNull final UUID senderLeader, @NotNull final UUID targetLeader, @Nullable final KitImpl kit) {
        sendDuelRequest(senderLeader, targetLeader, kit, 0);
    }

    @Nullable
    public PartyDuelRequest getDuelRequest(@Nullable final UUID targetLeader) {
        if (targetLeader == null) {
            return null;
        }
        final PartyDuelRequest request = duelRequests.get(targetLeader);
        if (request != null && request.isExpired()) {
            duelRequests.remove(targetLeader);
            return null;
        }
        return request;
    }

    public void removeDuelRequest(@Nullable final UUID targetLeader) {
        if (targetLeader != null) {
            duelRequests.remove(targetLeader);
        }
    }

    /**
     * Gelen parti düellosu teklifini kabul eder, bahis kesintilerini yapar ve maçı başlatır.
     */
    public boolean acceptPartyDuel(@NotNull final Player acceptingLeader) {
        final Party acceptingParty = getParty(acceptingLeader.getUniqueId());
        if (acceptingParty == null || !acceptingParty.isLeader(acceptingLeader.getUniqueId())) {
            acceptingLeader.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
            return false;
        }

        final PartyDuelRequest request = getDuelRequest(acceptingLeader.getUniqueId());
        if (request == null || request.isExpired()) {
            acceptingLeader.sendMessage("§cPartinize gelen aktif bir parti düellosu isteği bulunmuyor!");
            return false;
        }

        final Player senderLeader = Bukkit.getPlayer(request.getSenderLeader());
        if (senderLeader == null || !senderLeader.isOnline()) {
            acceptingLeader.sendMessage("§cMeydan okuyan parti lideri çevrimdışı!");
            removeDuelRequest(acceptingLeader.getUniqueId());
            return false;
        }

        final Party senderParty = getParty(request.getSenderLeader());
        if (senderParty == null || !senderParty.isLeader(request.getSenderLeader())) {
            acceptingLeader.sendMessage("§cMeydan okuyan parti artık mevcut değil!");
            removeDuelRequest(acceptingLeader.getUniqueId());
            return false;
        }

        final int countA = senderParty.getOnlineCount();
        final int countB = acceptingParty.getOnlineCount();

        if (countA != countB) {
            acceptingLeader.sendMessage("§cParti düellosu için her iki tarafın oyuncu sayısı eşit olmalıdır! (Örn: Karşı taraf " + countA + " kişiyse siz de " + countA + " kişi olmalısınız)");
            return false;
        }

        if (countA < 2 || countA > 5) {
            acceptingLeader.sendMessage("§cParti düellosu yapabilmek için her iki partide 2 ila 5 kişi olmalıdır!");
            return false;
        }

        if (isInPartyMatch(acceptingLeader.getUniqueId()) || isInPartyMatch(senderLeader.getUniqueId())) {
            acceptingLeader.sendMessage("§cPartilerden biri zaten bir maçta!");
            return false;
        }

        final int bet = request.getBet();
        if (bet > 0 && plugin.getVault() != null) {
            if (!plugin.getVault().has(bet, new Player[] { senderLeader, acceptingLeader })) {
                acceptingLeader.sendMessage("§cParti liderlerinden birinin yeterli parası yok! (Gereken bahis: " + bet + " Coin)");
                senderLeader.sendMessage("§cParti liderlerinden birinin yeterli parası yok! (Gereken bahis: " + bet + " Coin)");
                removeDuelRequest(acceptingLeader.getUniqueId());
                return false;
            }
            plugin.getVault().remove(bet, new Player[] { senderLeader, acceptingLeader });
            acceptingLeader.sendMessage("§6§l[BAHİS] §e" + bet + " Coin §abahis kesildi.");
            senderLeader.sendMessage("§6§l[BAHİS] §e" + bet + " Coin §abahis kesildi.");
        }

        final ArenaImpl arena = plugin.getArenaManager().randomArena(request.getKit());
        if (arena == null) {
            if (bet > 0 && plugin.getVault() != null) {
                plugin.getVault().add(bet, new Player[] { senderLeader, acceptingLeader });
            }
            acceptingLeader.sendMessage("§cŞu anda uygun bir arena bulunmuyor!");
            return false;
        }

        removeDuelRequest(acceptingLeader.getUniqueId());
        final boolean success = startPartyMatch(senderParty, acceptingParty, request.getKit(), arena, bet);
        if (!success) {
            if (bet > 0 && plugin.getVault() != null) {
                plugin.getVault().add(bet, new Player[] { senderLeader, acceptingLeader });
            }
            acceptingLeader.sendMessage("§cParti düellosu başlatılırken bir hata oluştu! Lütfen arena ve pozisyonları kontrol edin.");
            return false;
        }

        return true;
    }

    /**
     * İki parti arasında 2v2 - 5v5 düelloyu başlatır.
     */
    public boolean startPartyMatch(
            @NotNull final Party partyA,
            @NotNull final Party partyB,
            @Nullable final KitImpl kit,
            @NotNull final ArenaImpl arena,
            final int bet
    ) {
        final Set<UUID> membersA = new HashSet<UUID>();
        final Set<UUID> membersB = new HashSet<UUID>();

        for (UUID uid : partyA.getMembers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                membersA.add(uid);
            }
        }
        for (UUID uid : partyB.getMembers()) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                membersB.add(uid);
            }
        }

        // Çevrimiçi oyuncu sayıları kesinlikle eşit olmalıdır (2v2, 3v3, 4v4, 5v5)
        if (membersA.size() != membersB.size() || membersA.size() < 2 || membersA.size() > 5) {
            return false;
        }

        final Location pos1 = arena.getPosition(1);
        final Location pos2 = arena.getPosition(2);

        if (pos1 == null || pos2 == null) {
            return false;
        }

        // Arenayı maç durumuna al
        arena.startMatch(kit, null, bet, null);

        final PartyMatch match = new PartyMatch(partyA.getLeader(), partyB.getLeader(), membersA, membersB, arena, kit, bet);

        final List<Location> spawnsA = calculateTeamSpawns(pos1, pos2, membersA.size());
        final List<Location> spawnsB = calculateTeamSpawns(pos2, pos1, membersB.size());

        // Takım A Hazırlığı (Pozisyon 1 yan yana dizilim)
        int indexA = 0;
        for (UUID uid : membersA) {
            final Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                plugin.getPlayerManager().create(p);
                PlayerUtil.reset(p);
                if (kit != null) {
                    kit.equip(p);
                }
                final Location spawnLoc = indexA < spawnsA.size() ? spawnsA.get(indexA) : pos1;
                plugin.getTeleport().tryTeleport(p, spawnLoc);
                arena.add(p);
                setMatch(uid, match);
                indexA++;
            }
        }

        // Takım B Hazırlığı (Pozisyon 2 yan yana dizilim)
        int indexB = 0;
        for (UUID uid : membersB) {
            final Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                plugin.getPlayerManager().create(p);
                PlayerUtil.reset(p);
                if (kit != null) {
                    kit.equip(p);
                }
                final Location spawnLoc = indexB < spawnsB.size() ? spawnsB.get(indexB) : pos2;
                plugin.getTeleport().tryTeleport(p, spawnLoc);
                arena.add(p);
                setMatch(uid, match);
                indexB++;
            }
        }

        final int count = membersA.size();
        match.broadcast("§6§l========================================");
        match.broadcast("§a§l           " + count + "v" + count + " PARTİ DÜELLOSU!");
        match.broadcast("§e  Arena: §f" + arena.getName() + " §7| §eKit: §f" + (kit != null ? kit.getName() : "Özel/Yok"));
        if (bet > 0) {
            match.broadcast("§e  Bahis: §6" + bet + " Coin §7(Ödül Havuzu: §a" + (bet * 2) + " Coin§7)");
        }
        match.broadcast("§6§l========================================");

        final Player leaderA = Bukkit.getPlayer(partyA.getLeader());
        final Player leaderB = Bukkit.getPlayer(partyB.getLeader());
        final String nameA = leaderA != null ? leaderA.getName() : "Takım 1";
        final String nameB = leaderB != null ? leaderB.getName() : "Takım 2";

        final Map<UUID, Pair<String, Integer>> info = new HashMap<UUID, Pair<String, Integer>>();
        for (UUID uid : membersA) {
            info.put(uid, new Pair<String, Integer>(nameB + " Partisi", 0));
        }
        for (UUID uid : membersB) {
            info.put(uid, new Pair<String, Integer>(nameA + " Partisi", 0));
        }

        final String kitName = kit != null ? kit.getName() : "Özel";
        if (plugin.getConfiguration().isCdEnabled()) {
            arena.startCountdown(kitName, info);
        }

        return true;
    }

    public boolean startPartyMatch(
            @NotNull final Party partyA,
            @NotNull final Party partyB,
            @Nullable final KitImpl kit,
            @NotNull final ArenaImpl arena
    ) {
        return startPartyMatch(partyA, partyB, kit, arena, 0);
    }

    /**
     * Takım oyuncularının iç içe doğmasını ve kamera/hitbox çakışmasını önleyen
     * yan yana dizilim (formation) konumlarını hesaplar.
     * Karşı takıma bakan doğrultuya dik (perpendicular) açıyla 1.8 blok aralıklarla dağıtır.
     */
    public List<Location> calculateTeamSpawns(@NotNull final Location center, @NotNull final Location opponent, final int count) {
        final List<Location> spawns = new java.util.ArrayList<Location>(count);
        if (count <= 1) {
            spawns.add(center.clone());
            return spawns;
        }

        final double dx = opponent.getX() - center.getX();
        final double dz = opponent.getZ() - center.getZ();

        double perpX = -dz;
        double perpZ = dx;
        final double len = Math.sqrt(perpX * perpX + perpZ * perpZ);

        if (len > 0.001D) {
            perpX /= len;
            perpZ /= len;
        } else {
            perpX = 1.0D;
            perpZ = 0.0D;
        }

        final double spacing = 1.8D; // Oyuncuların birbirine çarpmayacağı ideal mesafe

        for (int i = 0; i < count; i++) {
            final double offset = (i - (count - 1) / 2.0D) * spacing;
            final Location loc = center.clone().add(perpX * offset, 0, perpZ * offset);

            if (loc.getBlock().getType().isSolid()) {
                spawns.add(center.clone());
            } else {
                spawns.add(loc);
            }
        }

        return spawns;
    }

    /**
     * Lider için sohbete oyuncu ismi girerek davet etme sürecini başlatır (30 saniye geçerli).
     */
    public void startChatInvitePrompt(@NotNull final UUID leaderId) {
        chatInvitePrompts.put(leaderId, System.currentTimeMillis() + 30000L);
    }

    /**
     * Oyuncunun sohbetten davet göndermek için bekleyip beklemediğini kontrol eder. O(1)
     */
    public boolean isWaitingChatInvite(@NotNull final UUID leaderId) {
        final Long expire = chatInvitePrompts.get(leaderId);
        if (expire == null) {
            return false;
        }
        if (System.currentTimeMillis() > expire) {
            chatInvitePrompts.remove(leaderId);
            return false;
        }
        return true;
    }

    /**
     * Bekleyen sohbet daveti sürecini iptal eder. O(1)
     */
    public void cancelChatInvitePrompt(@NotNull final UUID leaderId) {
        chatInvitePrompts.remove(leaderId);
    }

    /**
     * Sohbet üzerinden girilen oyuncu adına davet gönderir.
     */
    public void handleChatInvite(@NotNull final Player player, @NotNull final String input) {
        if (input.equalsIgnoreCase("iptal") || input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§c[Parti] Davet işlemi iptal edildi.");
            return;
        }

        final Party party = getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§c[Parti] Bir partide değilsiniz!");
            return;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§c[Parti] Bu işlemi yalnızca parti lideri yapabilir!");
            return;
        }

        if (party.isFull()) {
            player.sendMessage("§c[Parti] Partiniz maksimum " + Party.MAX_MEMBERS + " kişi olabilir!");
            return;
        }

        final Player target = Bukkit.getPlayerExact(input);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c[Parti] '" + input + "' adlı oyuncu bulunamadı veya çevrimdışı!");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage("§c[Parti] Kendinizi partinize davet edemezsiniz!");
            return;
        }

        if (isInParty(target.getUniqueId())) {
            player.sendMessage("§c[Parti] " + target.getName() + " zaten başka bir partide!");
            return;
        }

        if (party.hasInvite(target.getUniqueId())) {
            player.sendMessage("§c[Parti] " + target.getName() + " adlı oyuncuya zaten bekleyen bir davet gönderilmiş!");
            return;
        }

        party.addInvite(target.getUniqueId());
        pendingPartyInvites.put(target.getUniqueId(), player.getUniqueId());
        party.broadcast("§e" + target.getName() + " §apartinize davet edildi.");

        try {
            target.playSound(target.getLocation(), Sound.NOTE_PLING, 1.0F, 1.5F);
        } catch (Throwable ignored) {}

        target.sendMessage("§6§m--------------------------------------------------");
        target.sendMessage("§e§l               PARTİ DAVETİ");
        target.sendMessage("§f  " + player.getName() + " §7sizi partisine davet etti!");
        target.sendMessage("§7  Parti Durumu: §e" + party.size() + "/5 Oyuncu");
        target.sendMessage("");

        TextBuilder
            .of("     ")
            .add("§a§l[✔ KABUL ET]",
                ClickEvent.Action.RUN_COMMAND, "/parti katil " + player.getName(),
                HoverEvent.Action.SHOW_TEXT, "§aPartiye katılmak için tıklayın\n§7Komut: /parti katil " + player.getName() + " (veya sadece /parti kabul)")
            .add("    ")
            .add("§c§l[✖ REDDET]",
                ClickEvent.Action.RUN_COMMAND, "/parti reddet " + player.getName(),
                HoverEvent.Action.SHOW_TEXT, "§cDaveti reddetmek için tıklayın\n§7Komut: /parti reddet " + player.getName() + " (veya sadece /parti reddet)")
            .send(target);

        target.sendMessage("");
        target.sendMessage("§7  (İşlem yapmak için yukarıdaki butonlara tıklayabilir veya §e/parti kabul §7yazabilirsiniz)");
        target.sendMessage("§6§m--------------------------------------------------");

        player.sendMessage("§a[Parti] §f" + target.getName() + " §aoyuncusuna davet başarıyla gönderildi!");
    }

    /**
     * Oyuncuya gelen bekleyen parti davetinin liderini O(1) hızında döner.
     */
    @Nullable
    public UUID getPendingPartyInvite(@Nullable final UUID playerId) {
        if (playerId == null) {
            return null;
        }
        final UUID leaderId = pendingPartyInvites.get(playerId);
        if (leaderId != null) {
            final Party party = getParty(leaderId);
            if (party != null && party.hasInvite(playerId)) {
                return leaderId;
            }
            pendingPartyInvites.remove(playerId);
        }
        // Geriye dönük güvenlik: herhangi bir partinin davet listesinde var mı?
        for (Party p : partyByPlayer.values()) {
            if (p.hasInvite(playerId)) {
                pendingPartyInvites.put(playerId, p.getLeader());
                return p.getLeader();
            }
        }
        return null;
    }

    public void removePendingPartyInvite(@Nullable final UUID playerId) {
        if (playerId != null) {
            pendingPartyInvites.remove(playerId);
        }
    }
}
