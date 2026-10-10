package me.realized.duels.util.inventory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import me.realized.duels.util.StringUtil;
import me.realized.duels.util.compat.Items;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class ItemBuilder {

    private final ItemStack result;

    private ItemBuilder(final Material type, final int amount, final short durability) {
        this.result = new ItemStack(type, amount);
        Items.setDurability(result, durability);
    }

    private ItemBuilder(final String type, final int amount, final short durability) {
        this(Material.matchMaterial(type), amount, durability);
    }

    private ItemBuilder(final ItemStack item) {
        this.result = item;
    }

    public static ItemBuilder of(final Material type) {
        return of(type, 1);
    }

    public static ItemBuilder of(final Material type, final int amount) {
        return of(type, amount, (short) 0);
    }

    public static ItemBuilder of(final Material type, final int amount, final short durability) {
        return new ItemBuilder(type, amount, durability);
    }

    public static ItemBuilder of(final String type, final int amount, final short durability) {
        return new ItemBuilder(type, amount, durability);
    }

    public static ItemBuilder of(final ItemStack item) {
        return new ItemBuilder(item);
    }

    public ItemBuilder editMeta(final Consumer<ItemMeta> consumer) {
        final ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            consumer.accept(meta);
            result.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder name(final String name) {
        return editMeta(meta -> meta.setDisplayName(StringUtil.color(name)));
    }

    public ItemBuilder lore(final List<String> lore) {
        return editMeta(meta -> meta.setLore(StringUtil.color(lore)));
    }

    public ItemBuilder lore(final String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ItemBuilder enchant(final Enchantment enchantment, final int level) {
        result.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ItemBuilder unbreakable() {
        return editMeta(meta -> {
            try {
                meta.spigot().setUnbreakable(true);
            } catch (Throwable ignored) {}
        });
    }

    public ItemBuilder head(final String owner) {
        return editMeta(meta -> {
            if (owner != null && Items.equals(Items.HEAD, result) && meta instanceof SkullMeta) {
                final SkullMeta skullMeta = (SkullMeta) meta;
                skullMeta.setOwner(owner);
            }
        });
    }

    public ItemBuilder leatherArmorColor(final String color) {
        return editMeta(meta -> {
            if (meta instanceof LeatherArmorMeta) {
                final LeatherArmorMeta leatherArmorMeta = (LeatherArmorMeta) meta;
                if (color != null) {
                    leatherArmorMeta.setColor(DyeColor.valueOf(color).getColor());
                }
            }
        });
    }

    public ItemStack build() {
        return result;
    }
}
