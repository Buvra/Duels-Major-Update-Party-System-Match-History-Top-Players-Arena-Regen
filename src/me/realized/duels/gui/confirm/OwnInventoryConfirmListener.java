package me.realized.duels.gui.confirm;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.command.commands.duel.subcommands.AcceptCommand;
import me.realized.duels.request.RequestImpl;
import me.realized.duels.setting.Settings;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class OwnInventoryConfirmListener implements Listener {

    private final DuelsPlugin plugin;

    public OwnInventoryConfirmListener(final DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        final Inventory inv = event.getInventory();
        if (inv == null || !(inv.getHolder() instanceof OwnInventoryConfirmHolder)) {
            return;
        }

        event.setCancelled(true);

        final OwnInventoryConfirmHolder holder = (OwnInventoryConfirmHolder) inv.getHolder();
        final Player player = (Player) event.getWhoClicked();
        final Player target = holder.getTarget();
        final int slot = event.getRawSlot();

        if (target == null || !target.isOnline()) {
            player.closeInventory();
            player.sendMessage("§cRakip oyuncu artık çevrimiçi değil!");
            return;
        }

        if (slot == 11) {
            // ACCEPT BUTTON
            player.closeInventory();

            if (holder.getType() == OwnInventoryConfirmHolder.Type.SENDER) {
                final Settings settings = holder.getSettings();
                if (settings != null) {
                    plugin.getRequestManager().send(player, target, settings);
                    try {
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                    } catch (Throwable ignored) {}
                }
            } else if (holder.getType() == OwnInventoryConfirmHolder.Type.RECEIVER) {
                final RequestImpl request = holder.getRequest();
                if (request != null) {
                    AcceptCommand.executeAccept(plugin, player, target, request);
                    try {
                        player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
                    } catch (Throwable ignored) {}
                }
            }
        } else if (slot == 15) {
            // CANCEL / DENY BUTTON
            player.closeInventory();

            if (holder.getType() == OwnInventoryConfirmHolder.Type.SENDER) {
                final Settings settings = holder.getSettings();
                if (settings != null) {
                    settings.openGui(player);
                }
                try {
                    player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                } catch (Throwable ignored) {}
            } else if (holder.getType() == OwnInventoryConfirmHolder.Type.RECEIVER) {
                plugin.getRequestManager().remove(target, player);
                player.sendMessage("§6[Düello] §cDüello teklifini reddettiniz.");
                if (target.isOnline()) {
                    target.sendMessage("§6[Düello] §c" + player.getName() + " eşya kaybı riskini kabul etmedi ve teklifi reddetti.");
                }
                try {
                    player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
                } catch (Throwable ignored) {}
            }
        }
    }
}
