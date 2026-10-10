package me.realized.duels.command.commands.duel.subcommands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.Permissions;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.util.StringUtil;
import org.bukkit.command.CommandSender;

public class VersionCommand extends BaseCommand {

    public VersionCommand(final DuelsPlugin plugin) {
        super(plugin, "version", null, null, Permissions.DUEL, 1, false, "v");
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        sender.sendMessage(StringUtil.color("&bDuels v" + plugin.getDescription().getVersion() + " (Optimized Build)"));
    }
}
