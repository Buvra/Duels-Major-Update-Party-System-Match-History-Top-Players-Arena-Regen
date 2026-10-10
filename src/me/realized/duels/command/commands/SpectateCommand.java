package me.realized.duels.command.commands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.spectate.SpectateManager;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.arena.MatchImpl;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.spectate.SpectatorImpl;
import me.realized.duels.util.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCommand extends BaseCommand {

    public SpectateCommand(DuelsPlugin plugin) {
        super(plugin, "spectate", "duels.spectate", true);
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        SpectatorImpl spectator = this.spectateManager.get(player);

        if (spectator != null) {
            this.spectateManager.stopSpectating(player);
            this.lang.sendMessage(player, "COMMAND.spectate.stop-spectate",
                    new Object[]{"name", spectator.getTargetName()});
            return;
        }

        if (args.length == 0) {
            this.plugin.getSpectateMenu().open(player, 0);
            return;
        }

        if (this.config.isSpecRequiresClearedInventory() && InventoryUtil.hasItem(player)) {
            this.lang.sendMessage(sender, "ERROR.duel.inventory-not-empty", new Object[0]);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            this.lang.sendMessage(sender, "ERROR.player.not-found", new Object[]{"name", args[0]});
            return;
        }

        this.startSpectate(player, target);
    }

    public void startSpectate(Player player, Player target) {
        SpectateManager.Result result = this.spectateManager.startSpectating(player, target);

        switch (result) {
            case EVENT_CANCELLED:
                return;

            case IN_MATCH:
                this.lang.sendMessage(player, "ERROR.duel.already-in-match.sender", new Object[0]);
                return;

            case IN_QUEUE:
                this.lang.sendMessage(player, "ERROR.duel.already-in-queue", new Object[0]);
                return;

            case ALREADY_SPECTATING:
                this.lang.sendMessage(player, "ERROR.spectate.already-spectating.sender", new Object[0]);
                return;

            case TARGET_NOT_IN_MATCH:
                this.lang.sendMessage(player, "ERROR.spectate.not-in-match", new Object[]{"name", target.getName()});
                return;

            case SUCCESS:
                ArenaImpl arena = this.arenaManager.get(target);
                if (arena == null || arena.getMatch() == null) {
                    return;
                }

                MatchImpl match = arena.getMatch();
                Player opponent = arena.getOpponent(target);
                String opponentName = opponent != null ? opponent.getName() : "-";

                this.lang.sendMessage(player, "COMMAND.spectate.start-spectate", new Object[]{
                        "name", target.getName(),
                        "opponent", opponentName,
                        "arena", arena.getName(),
                        "bet_amount", match.getBet()
                });
                return;

            default:
                return;
        }
    }
}
