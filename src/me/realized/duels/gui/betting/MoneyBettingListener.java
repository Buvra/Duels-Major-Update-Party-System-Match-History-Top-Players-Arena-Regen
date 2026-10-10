package me.realized.duels.gui.betting;

import me.realized.duels.DuelsPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class MoneyBettingListener implements Listener {

    private final DuelsPlugin plugin;

    public MoneyBettingListener(final DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        final Inventory inv = event.getInventory();
        if (inv == null || !(inv.getHolder() instanceof MoneyBettingHolder)) {
            return;
        }

        event.setCancelled(true);

        final Player player = (Player) event.getWhoClicked();
        final int slot = event.getRawSlot();

        if (slot >= 0 && slot < 27) {
            MoneyBettingGui.handleClick(player, slot, inv, plugin);
        }
    }
}
