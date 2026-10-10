package me.realized.duels.command.commands.duel.subcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.party.Party;
import me.realized.duels.party.PartyDuelRequest;
import me.realized.duels.party.PartyManager;
import me.realized.duels.util.TextBuilder;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /duello parti ve /duel parti alt komutu.
 * 2v2'den 5v5'e kadar parti düellosu isteklerini ve kabul/red süreçlerini yönetir.
 */
public class PartyDuelSubCommand extends BaseCommand {

    private final PartyManager partyManager;

    public PartyDuelSubCommand(final DuelsPlugin plugin) {
        super(plugin, "parti", "parti <rakip_lider/kabul/reddet>", "Karşı partiye düello isteği atar veya gelen isteği yanıtlar.", 1, true, "party");
        this.partyManager = plugin.getPartyManager();
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        final Player player = (Player) sender;

        if (args.length < 2) {
            final PartyDuelRequest req = partyManager.getDuelRequest(player.getUniqueId());
            if (req != null && !req.isExpired()) {
                partyManager.acceptPartyDuel(player);
                return;
            }
            player.sendMessage("§cKullanım: /duello parti <rakip_lider> [bahis]");
            player.sendMessage("§cGelen isteği kabul etmek için: §e/duello parti kabul (veya /duello kabul)");
            player.sendMessage("§cGelen isteği reddetmek için: §e/duello parti reddet (veya /duello reddet)");
            return;
        }

        final String action = args[1];

        if (action.equalsIgnoreCase("kabul") || action.equalsIgnoreCase("accept")) {
            partyManager.acceptPartyDuel(player);
            return;
        }

        if (action.equalsIgnoreCase("reddet") || action.equalsIgnoreCase("deny")) {
            handleReddet(player);
            return;
        }

        int bet = 0;
        if (args.length >= 3) {
            try {
                bet = Integer.parseInt(args[2]);
                if (bet < 0) {
                    player.sendMessage("§cBahis miktarı negatif olamaz!");
                    return;
                }
            } catch (NumberFormatException ex) {
                player.sendMessage("§cGeçersiz bahis miktarı! Lütfen bir sayı girin.");
                return;
            }
        }

        handleMeydanOku(player, action, bet);
    }

    private void handleReddet(final Player player) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
            return;
        }

        final PartyDuelRequest request = partyManager.getDuelRequest(player.getUniqueId());
        if (request == null) {
            player.sendMessage("§cPartinize gelen aktif bir düello isteği bulunmuyor!");
            return;
        }

        partyManager.removeDuelRequest(player.getUniqueId());
        player.sendMessage("§cParti düellosu isteği reddedildi.");

        final Player senderLeader = Bukkit.getPlayer(request.getSenderLeader());
        if (senderLeader != null && senderLeader.isOnline()) {
            senderLeader.sendMessage("§c" + player.getName() + " parti düellosu isteğinizi reddetti.");
        }
    }

    private void handleMeydanOku(final Player player, final String targetLeaderName, final int bet) {
        final Party senderParty = partyManager.getParty(player.getUniqueId());
        if (senderParty == null || !senderParty.isLeader(player.getUniqueId())) {
            player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
            return;
        }

        final Player targetLeader = Bukkit.getPlayerExact(targetLeaderName);
        if (targetLeader == null || !player.canSee(targetLeader)) {
            player.sendMessage("§cBelirtilen oyuncu bulunamadı veya çevrimdışı!");
            return;
        }

        if (targetLeader.equals(player)) {
            player.sendMessage("§cKendi partinize düello isteği gönderemezsiniz!");
            return;
        }

        final Party targetParty = partyManager.getParty(targetLeader.getUniqueId());
        if (targetParty == null || !targetParty.isLeader(targetLeader.getUniqueId())) {
            player.sendMessage("§cBelirtilen oyuncu bir partinin lideri değil!");
            return;
        }

        final int countA = senderParty.getOnlineCount();
        final int countB = targetParty.getOnlineCount();

        if (countA != countB) {
            player.sendMessage("§cParti düellosu için her iki tarafın oyuncu sayısı eşit olmalıdır! (Örn: Karşı taraf " + countB + " kişiyse siz de " + countB + " kişi olmalısınız)");
            return;
        }

        if (countA < 2 || countA > 5) {
            player.sendMessage("§cParti düelloları 2v2 ile 5v5 arasında olmalıdır! (Şu anki: " + countA + " kişi)");
            return;
        }

        if (partyManager.isInPartyMatch(player.getUniqueId()) || partyManager.isInPartyMatch(targetLeader.getUniqueId())) {
            player.sendMessage("§cPartilerden biri şu anda bir maçta bulunuyor!");
            return;
        }

        if (bet > 0) {
            if (plugin.getVault() == null) {
                player.sendMessage("§cSunucuda ekonomi sistemi (Vault) bulunamadı!");
                return;
            }
            if (!plugin.getVault().has(bet, player)) {
                player.sendMessage("§cYeterli paranız yok! Gereken bahis miktarı: §e" + bet + " Coin");
                return;
            }
        }

        final ArenaImpl arena = plugin.getArenaManager().randomArena(null);
        if (arena == null) {
            player.sendMessage("§cŞu anda uygun bir arena bulunmuyor!");
            return;
        }

        partyManager.sendDuelRequest(player.getUniqueId(), targetLeader.getUniqueId(), null, bet);

        final String betText = bet > 0 ? "§6" + bet + " Coin" : "§7Yok (Dostluk Maçı)";
        final String rewardText = bet > 0 ? "§e" + (bet * 2) + " Coin" : "§7-";

        try {
            targetLeader.playSound(targetLeader.getLocation(), Sound.NOTE_PLING, 1.0F, 1.5F);
        } catch (Throwable ignored) {}

        targetLeader.sendMessage("§6§m--------------------------------------------------");
        targetLeader.sendMessage("§6§l           PARTİ DÜELLOSU TEKLİFİ");
        targetLeader.sendMessage("§f  " + player.getName() + " §7partisi sizin partinize meydan okudu!");
        targetLeader.sendMessage("§7  Format: §e" + countA + "v" + countA);
        targetLeader.sendMessage("§7  Bahis: " + betText + (bet > 0 ? " §7(Ödül Havuzu: " + rewardText + "§7)" : ""));
        targetLeader.sendMessage("");

        TextBuilder
            .of("     ")
            .add("§a§l[✔ KABUL ET]",
                ClickEvent.Action.RUN_COMMAND, "/duello parti kabul",
                HoverEvent.Action.SHOW_TEXT, "§aParti düellosunu kabul etmek için tıklayın!\n§7Komut: /duello parti kabul")
            .add("    ")
            .add("§c§l[✖ REDDET]",
                ClickEvent.Action.RUN_COMMAND, "/duello parti reddet",
                HoverEvent.Action.SHOW_TEXT, "§cParti düellosunu reddetmek için tıklayın!\n§7Komut: /duello parti reddet")
            .send(targetLeader);

        targetLeader.sendMessage("");
        targetLeader.sendMessage("§7  (30 saniye geçerlidir. Butonlara tıklayabilirsiniz)");
        targetLeader.sendMessage("§6§m--------------------------------------------------");

        targetParty.broadcast("§6§l[DÜELLO] §e" + player.getName() + " §apartisi partinize §e" + countA + "v" + countA + (bet > 0 ? " §6(" + bet + " Coin Bahisli)" : "") + " §adüello teklif etti! Liderinizin yanıtı bekleniyor.");

        player.sendMessage("§6§m--------------------------------------------------");
        player.sendMessage("§a§l           DÜELLO TEKLİFİ GÖNDERİLDİ");
        player.sendMessage("§7  Hedef: §e" + targetLeader.getName() + " Partisi");
        player.sendMessage("§7  Format: §e" + countA + "v" + countA);
        if (bet > 0) {
            player.sendMessage("§7  Bahis: §6" + bet + " Coin §7(Ödül Havuzu: §e" + rewardText + "§7)");
        }
        player.sendMessage("§7  Yanıt bekleniyor (30 saniye geçerli)...");
        player.sendMessage("§6§m--------------------------------------------------");

        senderParty.broadcast("§6§l[DÜELLO] §eLideriniz §b" + targetLeader.getName() + " §epartisine §a" + countA + "v" + countA + (bet > 0 ? " §6(" + bet + " Coin Bahisli)" : "") + " §edüello teklifi gönderdi.");
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 2) {
            final List<String> completions = new ArrayList<String>();
            completions.add("kabul");
            completions.add("reddet");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOnline() && !p.equals(sender)) {
                    final Party party = partyManager.getParty(p.getUniqueId());
                    if (party != null && party.isLeader(p.getUniqueId())) {
                        completions.add(p.getName());
                    }
                }
            }
            return handleTabCompletion(args[1], completions);
        } else if (args.length == 3) {
            final List<String> betSuggestions = new ArrayList<String>();
            betSuggestions.add("100");
            betSuggestions.add("500");
            betSuggestions.add("1000");
            betSuggestions.add("5000");
            betSuggestions.add("10000");
            return handleTabCompletion(args[2], betSuggestions);
        }
        return Collections.emptyList();
    }
}
