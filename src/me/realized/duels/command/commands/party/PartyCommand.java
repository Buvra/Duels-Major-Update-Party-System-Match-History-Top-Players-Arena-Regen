package me.realized.duels.command.commands.party;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.party.Party;
import me.realized.duels.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 500 CCU altında yüksek performanslı, %100 Türkçe /parti ve /p komutu.
 */
public class PartyCommand extends BaseCommand {

    private final PartyManager partyManager;

    public PartyCommand(final DuelsPlugin plugin) {
        super(plugin, "parti", null, true);
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    protected boolean executeFirst(final CommandSender sender, final String label, final String[] args) {
        final Player player = (Player) sender;

        if (args.length == 0) {
            final Party party = partyManager.getParty(player.getUniqueId());
            if (party != null) {
                me.realized.duels.party.gui.PartyGUI.openMainMenu(player, party);
                return true;
            }
            me.realized.duels.party.gui.PartyGUI.openCreateMenu(player);
            return true;
        }

        final String sub = args[0].toLowerCase();

        switch (sub) {
            case "kur":
                return handleKur(player);
            case "davet":
                return handleDavet(player, args);
            case "katil":
            case "kabul":
            case "accept":
                return handleKatil(player, args);
            case "reddet":
            case "deny":
                return handleReddet(player, args);
            case "ayril":
                return handleAyril(player);
            case "at":
                return handleAt(player, args);
            case "dagit":
                return handleDagit(player);
            case "bilgi":
                return handleBilgi(player);
            case "duello":
            case "duel":
                return handleDuello(player, args);
            default:
                sendHelp(player);
                return true;
        }
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {}

    private void sendHelp(final Player player) {
        player.sendMessage("§6§l===== PARTİ KOMUTLARI =====");
        player.sendMessage("§e/parti §7- Parti yönetim veya kurma menüsünü açar.");
        player.sendMessage("§e/parti kur §7- Yeni bir parti oluşturur.");
        player.sendMessage("§e/parti davet [oyuncu] §7- Belirtilen oyuncuya parti daveti gönderir.");
        player.sendMessage("§e/parti katil <oyuncu> §7- Gelen parti davetini kabul eder.");
        player.sendMessage("§e/parti reddet <oyuncu> §7- Gelen parti davetini reddeder.");
        player.sendMessage("§e/parti duello <lider> [bahis] §7- Karşı partiye düello teklif eder.");
        player.sendMessage("§e/parti ayril §7- Bulunduğunuz partiden ayrılır.");
        player.sendMessage("§e/parti at <oyuncu> §7- Partiden bir üyeyi çıkarır.");
        player.sendMessage("§e/parti dagit §7- Partiyi tamamen kapatır.");
        player.sendMessage("§e/parti bilgi §7- Parti bilgilerini ve üyelerini listeler.");
        player.sendMessage("§6§l===========================");
    }

    private boolean handleKur(final Player player) {
        if (partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage("§cZaten bir partidesiniz!");
            return true;
        }

        final Party party = partyManager.createParty(player.getUniqueId());
        if (party != null) {
            player.sendMessage("§a§l[Parti] §aPartiniz başarıyla kuruldu!");
            player.sendMessage("§7Parti menüsünü açmak için: §e/parti");
            player.sendMessage("§7Oyuncu davet etmek için: §e/parti davet");
        } else {
            player.sendMessage("§cParti kurulurken bir hata oluştu!");
        }
        return true;
    }

    private boolean handleDavet(final Player player, final String[] args) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cBir partide değilsiniz! Parti kurmak için: §e/parti kur");
            return true;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cBu komutu sadece parti lideri kullanabilir!");
            return true;
        }

        if (party.isFull()) {
            player.sendMessage("§cPartiniz maksimum " + Party.MAX_MEMBERS + " kişi olabilir!");
            return true;
        }

        if (args.length < 2) {
            partyManager.startChatInvitePrompt(player.getUniqueId());
            player.sendMessage("");
            player.sendMessage("§6§l[Parti] §eDavet etmek istediğiniz oyuncunun adını sohbete yazın:");
            player.sendMessage("§7(İşlemi iptal etmek için sohbete §c'iptal' §7yazabilirsiniz)");
            player.sendMessage("");
            return true;
        }

        partyManager.handleChatInvite(player, args[1]);
        return true;
    }

    private boolean handleKatil(final Player player, final String[] args) {
        if (partyManager.isInParty(player.getUniqueId())) {
            player.sendMessage("§cZaten bir partidesiniz! Ayrılmak için: §e/parti ayril");
            return true;
        }

        Party party = null;
        Player target = null;

        if (args.length < 2) {
            // Argümansız /parti katil veya /parti kabul: O(1) hızında bekleyen daveti bul
            final UUID leaderId = partyManager.getPendingPartyInvite(player.getUniqueId());
            if (leaderId == null) {
                player.sendMessage("§cSize gelen aktif bir parti daveti bulunmuyor!");
                return true;
            }
            party = partyManager.getParty(leaderId);
            target = Bukkit.getPlayer(leaderId);
        } else {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§cBelirtilen oyuncu bulunamadı veya çevrimdışı!");
                return true;
            }
            party = partyManager.getParty(target.getUniqueId());
        }

        if (party == null) {
            player.sendMessage("§cBelirtilen oyuncunun aktif bir partisi bulunmuyor!");
            return true;
        }

        if (!party.hasInvite(player.getUniqueId())) {
            player.sendMessage("§cBu partiden size gelen aktif bir davet bulunmuyor!");
            return true;
        }

        if (party.isFull()) {
            player.sendMessage("§cParti şu anda dolu! (Maksimum " + Party.MAX_MEMBERS + " kişi)");
            return true;
        }

        if (partyManager.addMember(party, player.getUniqueId())) {
            player.sendMessage("§aPartiye başarıyla katıldınız!");
            party.broadcast("§e" + player.getName() + " §apartinize katıldı.");
        } else {
            player.sendMessage("§cPartiye katılırken bir hata oluştu!");
        }
        return true;
    }

    private boolean handleReddet(final Player player, final String[] args) {
        Party party = null;
        Player target = null;

        if (args.length < 2) {
            final UUID leaderId = partyManager.getPendingPartyInvite(player.getUniqueId());
            if (leaderId == null) {
                player.sendMessage("§cReddedilecek bekleyen bir parti davetiniz bulunmuyor!");
                return true;
            }
            party = partyManager.getParty(leaderId);
            target = Bukkit.getPlayer(leaderId);
        } else {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                player.sendMessage("§cBelirtilen oyuncu bulunamadı veya çevrimdışı!");
                return true;
            }
            party = partyManager.getParty(target.getUniqueId());
        }

        if (party != null && party.hasInvite(player.getUniqueId())) {
            party.removeInvite(player.getUniqueId());
            partyManager.removePendingPartyInvite(player.getUniqueId());
            final String name = target != null ? target.getName() : "Parti lideri";
            player.sendMessage("§c" + name + " adlı oyuncunun parti davetini reddettiniz.");
            if (target != null && target.isOnline()) {
                target.sendMessage("§c" + player.getName() + " parti davetinizi reddetti.");
            }
            return true;
        }

        player.sendMessage("§cBu oyuncudan gelen aktif bir parti daveti bulunmuyor!");
        return true;
    }

    private boolean handleDuello(final Player player, final String[] args) {
        if (args.length < 2) {
            final me.realized.duels.party.PartyDuelRequest req = partyManager.getDuelRequest(player.getUniqueId());
            if (req != null && !req.isExpired()) {
                player.sendMessage("§6§l[Parti Düellosu] §ePartinize gelen bekleyen bir düello isteği var!");
                player.sendMessage("§aKabul etmek için: §e/duello parti kabul §a(veya §e/duello kabul§a)");
                player.sendMessage("§cReddetmek için: §e/duello parti reddet §c(veya §e/duello reddet§c)");
                return true;
            }
            player.sendMessage("§cKullanım: /parti duello <rakip_lider> [bahis]");
            player.sendMessage("§cGelen isteği kabul etmek için: §e/duello parti kabul");
            player.sendMessage("§cGelen isteği reddetmek için: §e/duello parti reddet");
            return true;
        }

        final StringBuilder cmd = new StringBuilder("duello parti");
        for (int i = 1; i < args.length; i++) {
            cmd.append(" ").append(args[i]);
        }
        player.performCommand(cmd.toString());
        return true;
    }

    private boolean handleAyril(final Player player) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cBir partide değilsiniz!");
            return true;
        }

        if (partyManager.isInPartyMatch(player.getUniqueId())) {
            player.sendMessage("§cDüello maçındayken partiden ayrılamazsınız!");
            return true;
        }

        final String name = player.getName();
        partyManager.removeMember(player.getUniqueId());
        player.sendMessage("§cPartiden ayrıldınız.");
        party.broadcast("§e" + name + " §cpartiden ayrıldı.");
        return true;
    }

    private boolean handleAt(final Player player, final String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cKullanım: /parti at <oyuncu>");
            return true;
        }

        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cBir partide değilsiniz!");
            return true;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cBu komutu sadece parti lideri kullanabilir!");
            return true;
        }

        if (partyManager.isInPartyMatch(player.getUniqueId())) {
            player.sendMessage("§cDüello maçındayken partiden oyuncu çıkaramazsınız!");
            return true;
        }

        final Player target = Bukkit.getPlayerExact(args[1]);
        UUID targetId = null;
        String targetName = args[1];

        if (target != null) {
            targetId = target.getUniqueId();
            targetName = target.getName();
        } else {
            for (UUID uid : party.getMembers()) {
                final OfflinePlayer op = Bukkit.getOfflinePlayer(uid);
                if (op != null && args[1].equalsIgnoreCase(op.getName())) {
                    targetId = uid;
                    targetName = op.getName();
                    break;
                }
            }
        }

        if (targetId == null || !party.contains(targetId)) {
            player.sendMessage("§cBelirtilen oyuncu partinizde bulunmuyor!");
            return true;
        }

        if (player.getUniqueId().equals(targetId)) {
            player.sendMessage("§cKendinizi partiden atamazsınız! Partiden çıkmak için: §e/parti ayril");
            return true;
        }

        partyManager.removeMember(targetId);
        party.broadcast("§e" + targetName + " §cpartiden atıldı.");
        if (target != null && target.isOnline()) {
            target.sendMessage("§cPartiden atıldınız!");
        }
        return true;
    }

    private boolean handleDagit(final Player player) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cBir partide değilsiniz!");
            return true;
        }

        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cBu komutu sadece parti lideri kullanabilir!");
            return true;
        }

        if (partyManager.isInPartyMatch(player.getUniqueId())) {
            player.sendMessage("§cDüello maçındayken parti dağıtılamaz!");
            return true;
        }

        party.broadcast("§cParti, lider tarafından tamamen dağıtıldı!");
        partyManager.disbandParty(party);
        return true;
    }

    private boolean handleBilgi(final Player player) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cBir partide değilsiniz!");
            return true;
        }

        final Player leaderPlayer = Bukkit.getPlayer(party.getLeader());
        final String leaderName = leaderPlayer != null ? leaderPlayer.getName() : Bukkit.getOfflinePlayer(party.getLeader()).getName();

        player.sendMessage("§6§l===== PARTİ BİLGİSİ =====");
        player.sendMessage("§eLider: §f" + leaderName);
        player.sendMessage("§eÜye Sayısı: §f" + party.size() + "/" + Party.MAX_MEMBERS);
        player.sendMessage("§eÜyeler:");
        for (UUID uid : party.getMembers()) {
            final Player memberPlayer = Bukkit.getPlayer(uid);
            final boolean online = memberPlayer != null && memberPlayer.isOnline();
            final String name = online ? memberPlayer.getName() : Bukkit.getOfflinePlayer(uid).getName();
            final String status = online ? "§a[Çevrimiçi]" : "§c[Çevrimdışı]";
            player.sendMessage("  §7- §f" + name + " " + status + (party.isLeader(uid) ? " §6(Lider)" : ""));
        }
        player.sendMessage("§6§l========================");
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return handleTabCompletion(args[0], Arrays.asList("kur", "davet", "katil", "kabul", "reddet", "duello", "ayril", "at", "dagit", "bilgi"));
        }
        if (args.length == 2) {
            final List<String> names = new ArrayList<String>();
            if (args[0].equalsIgnoreCase("davet") || args[0].equalsIgnoreCase("katil") || args[0].equalsIgnoreCase("kabul") || args[0].equalsIgnoreCase("reddet") || args[0].equalsIgnoreCase("duello")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.equals(sender)) {
                        names.add(p.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("at") && sender instanceof Player) {
                final Party party = partyManager.getParty(((Player) sender).getUniqueId());
                if (party != null) {
                    for (UUID uid : party.getMembers()) {
                        final Player p = Bukkit.getPlayer(uid);
                        if (p != null) names.add(p.getName());
                    }
                }
            }
            return handleTabCompletion(args[1], names);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("duello")) {
            return handleTabCompletion(args[2], Arrays.asList("100", "500", "1000", "5000", "10000"));
        }
        return Collections.emptyList();
    }
}
