package me.realized.duels.util.compat;

import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class Items {

    private static final String PANE = "STAINED_GLASS_PANE";

    public static final ItemStack ORANGE_PANE;
    public static final ItemStack BLUE_PANE;
    public static final ItemStack RED_PANE;
    public static final ItemStack GRAY_PANE;
    public static final ItemStack WHITE_PANE;
    public static final ItemStack GREEN_PANE;
    public static final ItemStack HEAD;
    public static final Material SKELETON_HEAD;
    public static final ItemStack OFF;
    public static final ItemStack ON;
    public static final Material MUSHROOM_SOUP;
    public static final Material EMPTY_MAP;
    public static final Material SIGN;
    public static final ItemStack HEAL_SPLASH_POTION;
    public static final ItemStack WATER_BREATHING_POTION;
    public static final ItemStack ENCHANTED_GOLDEN_APPLE;

    static {
        ORANGE_PANE = ItemBuilder.of(PANE, 1, (short) 1).name(" ").build();
        BLUE_PANE = ItemBuilder.of(PANE, 1, (short) 11).name(" ").build();
        RED_PANE = ItemBuilder.of(PANE, 1, (short) 14).build();
        GRAY_PANE = ItemBuilder.of(PANE, 1, (short) 7).name(" ").build();
        WHITE_PANE = ItemBuilder.of(PANE, 1, (short) 0).name(" ").build();
        GREEN_PANE = ItemBuilder.of(PANE, 1, (short) 5).build();
        HEAD = ItemBuilder.of("SKULL_ITEM", 1, (short) 3).build();
        SKELETON_HEAD = Material.matchMaterial("SKULL_ITEM");
        OFF = ItemBuilder.of("INK_SACK", 1, (short) 8).build();
        ON = ItemBuilder.of("INK_SACK", 1, (short) 10).build();
        MUSHROOM_SOUP = Material.matchMaterial("MUSHROOM_SOUP");
        EMPTY_MAP = Material.matchMaterial("EMPTY_MAP");
        SIGN = Material.matchMaterial("SIGN");
        HEAL_SPLASH_POTION = ItemBuilder.of(Material.POTION, 1, (short) 16421).build();
        WATER_BREATHING_POTION = ItemBuilder.of(Material.POTION, 1, (short) 8237).build();
        ENCHANTED_GOLDEN_APPLE = ItemBuilder.of(Material.GOLDEN_APPLE, 1, (short) 1).build();
    }

    public static boolean equals(final ItemStack item, final ItemStack other) {
        return item != null && other != null && item.getType() == other.getType() && getDurability(item) == getDurability(other);
    }

    public static ItemStack from(final String type, final short data) {
        return ItemBuilder.of(type, 1, data).name(" ").build();
    }

    public static short getDurability(final ItemStack item) {
        return item == null ? 0 : item.getDurability();
    }

    public static void setDurability(final ItemStack item, final short durability) {
        if (item != null) {
            item.setDurability(durability);
        }
    }

    public static boolean isHealSplash(final ItemStack item) {
        return equals(HEAL_SPLASH_POTION, item);
    }

    private Items() {}
}
