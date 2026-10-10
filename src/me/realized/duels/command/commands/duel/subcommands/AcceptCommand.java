package me.realized.duels.command.commands.duel.subcommands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.request.RequestAcceptEvent;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.request.RequestImpl;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AcceptCommand extends BaseCommand {

    public AcceptCommand(final DuelsPlugin plugin) {
        super(plugin, "accept", "accept [oyuncu]", "Gelen düello veya parti düellosu isteğini kabul eder.", 1, true, "kabul");
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        final Player player = (Player) sender;

        if (config.isRequiresClearedInventory() && InventoryUtil.hasItem(player)) {
            lang.sendMessage(sender, "ERROR.duel.inventory-not-empty");
            return;
        }

        if (config.isPreventCreativeMode() && player.getGameMode() == GameMode.CREATIVE) {
            lang.sendMessage(sender, "ERROR.duel.in-creative-mode");
            return;
        }

        if (arenaManager.isInMatch(player)) {
            lang.sendMessage(sender, "ERROR.duel.already-in-match.sender");
            return;
        }

        if (spectateManager.isSpectating(player)) {
            lang.sendMessage(sender, "ERROR.spectate.already-spectating.sender");
            return;
        }

        // 1. Bekleyen Parti Düellosu İsteği Kontrolü (Parti Lideri için)
        if (partyManager != null) {
            final me.realized.duels.party.PartyDuelRequest partyReq = partyManager.getDuelRequest(player.getUniqueId());

            if (partyReq != null && !partyReq.isExpired()) {
                if (args.length < 2) {
                    partyManager.acceptPartyDuel(player);
                    return;
                } else {
                    final Player specifiedTarget = Bukkit.getPlayerExact(args[1]);
                    if (specifiedTarget != null && partyReq.getSenderLeader().equals(specifiedTarget.getUniqueId())) {
                        partyManager.acceptPartyDuel(player);
                        return;
                    }
                }
            }
        }

        // 2. 1v1 Düello İsteğini Çözümle
        Player target = null;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !player.canSee(target)) {
                lang.sendMessage(sender, "ERROR.player.not-found", "name", args[1]);
                return;
            }
        } else {
            // Argümansız /duello kabul veya /duel accept: En son gelen 1v1 isteği O(1) hızında bul
            target = requestManager.getLatestSender(player);
            if (target == null) {
                player.sendMessage("§cKabul edilecek bekleyen bir düello isteğiniz bulunmuyor!");
                return;
            }
        }

        // Eğer hedef oyuncuyla parti düellosu eşleşmesi varsa (ikisi de parti lideriyse)
        if (partyManager != null) {
            final me.realized.duels.party.Party myParty = partyManager.getParty(player.getUniqueId());
            final me.realized.duels.party.Party targetParty = partyManager.getParty(target.getUniqueId());

            if (myParty != null && targetParty != null && myParty.isLeader(player.getUniqueId()) && targetParty.isLeader(target.getUniqueId())) {
                final me.realized.duels.party.PartyDuelRequest partyReq = partyManager.getDuelRequest(player.getUniqueId());
                if (partyReq != null && !partyReq.isExpired() && partyReq.getSenderLeader().equals(target.getUniqueId())) {
                    partyManager.acceptPartyDuel(player);
                    return;
                }
            }
        }

        final RequestImpl request = requestManager.get(target, player);

        if (request == null) {
            lang.sendMessage(sender, "ERROR.duel.no-request", "name", target.getName());
            return;
        }

        final Settings settings = request.getSettings();
        if (settings != null && settings.isOwnInventory()) {
            me.realized.duels.gui.confirm.OwnInventoryConfirmGui.openForReceiver(player, target, request, plugin);
            return;
        }

        executeAccept(plugin, player, target, request);
    }

    public static void executeAccept(final DuelsPlugin plugin, final Player player, final Player target, final RequestImpl request) {
        final RequestAcceptEvent event = new RequestAcceptEvent(player, target, request);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        plugin.getRequestManager().remove(target, player);

        if (plugin.getArenaManager().isInMatch(target)) {
            plugin.getLang().sendMessage(player, "ERROR.duel.already-in-match.target", "name", target.getName());
            return;
        }

        if (plugin.getSpectateManager().isSpectating(target)) {
            plugin.getLang().sendMessage(player, "ERROR.spectate.already-spectating.target", "name", target.getName());
            return;
        }

        final Settings settings = request.getSettings();
        final String kit = settings.getKit() != null ? settings.getKit().getName() : plugin.getLang().getMessage("GENERAL.not-selected");
        final String arena = settings.getArena() != null ? settings.getArena().getName() : plugin.getLang().getMessage("GENERAL.random");
        final double bet = settings.getBet();
        final String itemBetting = settings.isItemBetting() ? plugin.getLang().getMessage("GENERAL.enabled") : plugin.getLang().getMessage("GENERAL.disabled");
        plugin.getLang().sendMessage(player, "COMMAND.duel.request.accept.receiver",
            "name", target.getName(), "kit", kit, "arena", arena, "bet_amount", bet, "item_betting", itemBetting);
        plugin.getLang().sendMessage(target, "COMMAND.duel.request.accept.sender",
            "name", player.getName(), "kit", kit, "arena", arena, "bet_amount", bet, "item_betting", itemBetting);

        if (settings.isItemBetting()) {
            settings.setBaseLoc(player);
            settings.setDuelzone(player, null);
            plugin.getBettingManager().open(settings, target, player);
        } else {
            plugin.getDuelManager().startMatch(player, target, settings, null, null);
        }
    }
}
