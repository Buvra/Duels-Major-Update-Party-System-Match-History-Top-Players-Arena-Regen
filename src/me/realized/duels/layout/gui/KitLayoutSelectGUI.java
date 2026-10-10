package me.realized.duels.layout.gui;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.layout.KitLayoutManager;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class KitLayoutSelectGUI {

    private static final ItemStack BORDER = Items.from("STAINED_GLASS_PANE", (short) 15);

    private KitLayoutSelectGUI() {}

    public static void open(@NotNull final Player player, @NotNull final DuelsPlugin plugin) {
        final KitLayoutManager layoutManager = plugin.getKitLayoutManager();
        final KitLayoutSelectHolder holder = new KitLayoutSelectHolder();
        final Inventory inv = Bukkit.createInventory(holder, 54, "§8Kit Envanter Düzenleme");
        holder.setInventory(inv);

        // Fill background
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, BORDER);
        }

        final List<me.realized.duels.api.kit.Kit> kits = plugin.getKitManager().getKits();
        int slot = 10;

        for (final me.realized.duels.api.kit.Kit kit : kits) {
            // Skip edge columns (col 0 and col 8) for clean aesthetic
            if (slot % 9 == 8) {
                slot += 2;
            }
            if (slot >= 44) {
                break;
            }

            final boolean hasLayout = layoutManager != null && layoutManager.hasLayout(player.getUniqueId(), kit.getName());
            final List<String> lore = new ArrayList<>();
            lore.add("§8§m------------------------");
            lore.add("§7Kit: §f" + kit.getName());
            lore.add("§7Düzen: " + (hasLayout ? "§a✔ Özel Dizilim Kayıtlı" : "§7Varsayılan (Default)"));
            lore.add(" ");
            lore.add("§eSol Tık: §7Envanter düzenini ayarla");
            if (hasLayout) {
                lore.add("§cSağ Tık: §7Varsayılan düzene sıfırla");
            }
            lore.add("§8§m------------------------");

            ItemStack icon = kit.getDisplayed() != null ? kit.getDisplayed().clone() : ItemBuilder.of(Material.DIAMOND_SWORD).build();
            inv.setItem(slot++, ItemBuilder.of(icon)
                    .name("§b§l⚔ " + kit.getName())
                    .lore(lore)
                    .build());
        }

        // Slot 49: Info item
        inv.setItem(49, ItemBuilder.of(Material.BOOK)
                .name("§6§lKit Envanter Düzenleyicisi")
                .lore(
                        "§8§m------------------------",
                        "§7Her kit için eşyalarınızı dilediğiniz",
                        "§7hotbar (1-9) ve çanta slotlarına dizebilirsiniz.",
                        "§7Düellolarda belirlediğiniz bu özel",
                        "§7dizilim otomatik olarak envanterinize gelir.",
                        "§8§m------------------------"
                )
                .build());

        // Slot 45: Close button
        inv.setItem(45, ItemBuilder.of(Material.ARROW).name("§cKapat").build());

        player.openInventory(inv);
    }
}
