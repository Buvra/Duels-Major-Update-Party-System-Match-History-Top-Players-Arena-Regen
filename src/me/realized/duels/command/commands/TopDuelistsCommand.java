package me.realized.duels.command.commands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.command.BaseCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TopDuelistsCommand extends BaseCommand {

    public TopDuelistsCommand(DuelsPlugin plugin) {
        super(plugin, "eniyiduellocular", "duels.top", true);
    }

    @Override
    protected void execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        this.plugin.getTopDuelistsMenu().open(player, 0);
    }
}