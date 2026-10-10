package me.realized.duels.layout.gui;

import java.util.UUID;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class KitLayoutEditorHolder implements InventoryHolder {

    private final UUID playerUuid;
    private final String kitName;
    private Inventory inventory;

    public KitLayoutEditorHolder(final UUID playerUuid, final String kitName) {
        this.playerUuid = playerUuid;
        this.kitName = kitName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getKitName() {
        return kitName;
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }
}
