package me.realized.duels.util.compat;

import me.realized.duels.util.compat.nbt.NBT;
import org.bukkit.inventory.ItemStack;

public final class Identifiers {

    private static final String DUELS_ITEM_IDENTIFIER = "DuelsKitContent";

    public static ItemStack addIdentifier(final ItemStack item) {
        return NBT.setItemString(item, DUELS_ITEM_IDENTIFIER, true);
    }

    public static boolean hasIdentifier(final ItemStack item) {
        return NBT.hasItemKey(item, DUELS_ITEM_IDENTIFIER);
    }

    public static ItemStack removeIdentifier(final ItemStack item) {
        return NBT.removeItemTag(item, DUELS_ITEM_IDENTIFIER);
    }

    private Identifiers() {}
}
