package me.realized.duels.command.commands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.command.BaseCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MatchHistoryCommand extends BaseCommand {

    public MatchHistoryCommand(DuelsPlugin plugin) {
        super(plugin, "gecmisduellolar", "duels.history", true);
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        this.plugin.getMatchHistoryMenu().open(player, 0);
    }
}