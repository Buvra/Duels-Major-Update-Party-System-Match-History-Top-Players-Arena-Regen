package me.realized.duels.layout;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.command.BaseCommand;
import me.realized.duels.layout.gui.KitLayoutSelectGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 500 CCU optimize /kitduzenle, /layout, /kitedit komutu.
 */
public class KitLayoutCommand extends BaseCommand {

    public KitLayoutCommand(final DuelsPlugin plugin) {
        super(plugin, "kitduzenle", "/kitduzenle [kit]", "Kit esya dizilimi duzenleme ekrani.", null, 0, true, "layout", "kitedit");
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        final Player player = (Player) sender;

        if (args.length == 0) {
            KitLayoutSelectGUI.open(player, plugin);
            return;
        }

        final String kitName = String.join(" ", args);
        final Kit kit = plugin.getKitManager().get(kitName);

        if (kit == null) {
            player.sendMessage("§cLN » §c'" + kitName + "' adında bir kit bulunamadı!");
            return;
        }

        plugin.getKitLayoutManager().openEditor(player, kit);
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            final String arg = args[0].toLowerCase();
            return plugin.getKitManager().getKits().stream()
                    .map(Kit::getName)
                    .filter(name -> name.toLowerCase().startsWith(arg))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
