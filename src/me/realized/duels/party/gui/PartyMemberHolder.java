package me.realized.duels.party.gui;

import me.realized.duels.party.Party;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PartyMemberHolder implements InventoryHolder {

    private final Party party;
    private Inventory inventory;

    public PartyMemberHolder(final Party party) {
        this.party = party;
    }

    public Party getParty() {
        return party;
    }

    public void setInventory(final Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
