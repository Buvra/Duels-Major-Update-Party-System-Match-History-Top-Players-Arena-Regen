package me.realized.duels.layout;

import me.realized.duels.util.inventory.ItemUtil;
import org.bukkit.inventory.ItemStack;

/**
 * 500 CCU Practice/Duels sunucuları için bellek ve GC dostu Kit Düzeni modeli.
 */
public class PlayerKitLayout {

    private final String kitName;
    private final ItemStack[] contents;
    private final ItemStack[] armor;

    public PlayerKitLayout(final String kitName, final ItemStack[] contents, final ItemStack[] armor) {
        this.kitName = kitName;
        this.contents = contents != null ? contents : new ItemStack[36];
        this.armor = armor != null ? armor : new ItemStack[4];
    }

    public String getKitName() {
        return kitName;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public String getContentsBase64() {
        return ItemUtil.itemsTo64(contents);
    }

    public String getArmorBase64() {
        return ItemUtil.itemsTo64(armor);
    }

    public static PlayerKitLayout fromBase64(final String kitName, final String contentsB64, final String armorB64) {
        final ItemStack[] contents = ItemUtil.itemsFrom64(contentsB64);
        final ItemStack[] armor = ItemUtil.itemsFrom64(armorB64);
        return new PlayerKitLayout(kitName, contents, armor);
    }
}
