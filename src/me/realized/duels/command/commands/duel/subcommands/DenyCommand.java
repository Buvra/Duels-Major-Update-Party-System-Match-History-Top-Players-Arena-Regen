package me.realized.duels.command.commands.duel.subcommands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.request.RequestDenyEvent;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.party.PartyDuelRequest;
import me.realized.duels.request.RequestImpl;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DenyCommand extends BaseCommand {

    public DenyCommand(final DuelsPlugin plugin) {
        super(plugin, "deny", "deny [oyuncu]", "Gelen düello veya parti düellosu isteğini reddeder.", 1, true, "reddet");
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        final Player player = (Player) sender;

        // 1. Bekleyen Parti Düellosu İsteği Reddetme (Parti Lideri için)
        if (partyManager != null) {
            final PartyDuelRequest partyReq = partyManager.getDuelRequest(player.getUniqueId());
            if (partyReq != null && !partyReq.isExpired()) {
                if (args.length < 2) {
                    partyManager.removeDuelRequest(player.getUniqueId());
                    player.sendMessage("§cParti düellosu isteği reddedildi.");
                    final Player senderLeader = Bukkit.getPlayer(partyReq.getSenderLeader());
                    if (senderLeader != null && senderLeader.isOnline()) {
                        senderLeader.sendMessage("§c" + player.getName() + " parti düellosu isteğinizi reddetti.");
                    }
                    return;
                } else {
                    final Player specifiedTarget = Bukkit.getPlayerExact(args[1]);
                    if (specifiedTarget != null && partyReq.getSenderLeader().equals(specifiedTarget.getUniqueId())) {
                        partyManager.removeDuelRequest(player.getUniqueId());
                        player.sendMessage("§cParti düellosu isteği reddedildi.");
                        specifiedTarget.sendMessage("§c" + player.getName() + " parti düellosu isteğinizi reddetti.");
                        return;
                    }
                }
            }
        }

        // 2. 1v1 Düello İsteği Reddetme
        Player target = null;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null || !player.canSee(target)) {
                lang.sendMessage(sender, "ERROR.player.not-found", "name", args[1]);
                return;
            }
        } else {
            target = requestManager.getLatestSender(player);
            if (target == null) {
                player.sendMessage("§cReddedilecek bekleyen bir düello isteğiniz bulunmuyor!");
                return;
            }
        }

        final RequestImpl request;
        if ((request = requestManager.remove(target, player)) == null) {
            lang.sendMessage(sender, "ERROR.duel.no-request", "name", target.getName());
            return;
        }

        final RequestDenyEvent event = new RequestDenyEvent(player, target, request);
        Bukkit.getPluginManager().callEvent(event);

        lang.sendMessage(player, "COMMAND.duel.request.deny.receiver", "name", target.getName());
        lang.sendMessage(target, "COMMAND.duel.request.deny.sender", "name", player.getName());
    }
}
