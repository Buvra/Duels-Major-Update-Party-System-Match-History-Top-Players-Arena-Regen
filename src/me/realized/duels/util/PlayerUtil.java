package me.realized.duels.util;

import me.realized.duels.util.compat.CompatUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class PlayerUtil {

    private static final double DEFAULT_MAX_HEALTH = 20.0D;
    private static final int DEFAULT_MAX_FOOD_LEVEL = 20;

    public static double getMaxHealth(final Player player) {
        return player.getMaxHealth();
    }

    private static void setMaxHealth(final Player player) {
        player.setHealth(getMaxHealth(player));
    }

    public static void reset(final Player player) {
        reset(player, true);
    }

    public static void reset(final Player player, final boolean clearInventory) {
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        setMaxHealth(player);
        player.setFoodLevel(DEFAULT_MAX_FOOD_LEVEL);
        player.setItemOnCursor(null);

        final Inventory top = player.getOpenInventory().getTopInventory();

        if (top != null && top.getType() == InventoryType.CRAFTING) {
            top.clear();
        }

        if (clearInventory) {
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().clear();
        }
        player.updateInventory();
    }

    private PlayerUtil() {}
}
