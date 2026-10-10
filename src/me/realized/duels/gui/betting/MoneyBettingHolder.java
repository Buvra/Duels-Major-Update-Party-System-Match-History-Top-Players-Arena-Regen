package me.realized.duels.gui.betting;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class MoneyBettingHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }
}
