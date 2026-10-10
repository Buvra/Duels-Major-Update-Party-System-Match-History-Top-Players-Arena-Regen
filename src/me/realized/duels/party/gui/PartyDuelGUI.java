package me.realized.duels.party.gui;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.party.Party;
import me.realized.duels.party.PartyManager;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Lobideki diğer parti liderlerini listeleyen ve 2v2 - 5v5 katı eşitlik denetimli
 * Parti Düellosu Seçim Menüsü.
 */
public class PartyDuelGUI {

    private static final ItemStack FILLER = ItemBuilder.of("STAINED_GLASS_PANE", 1, (short) 15).name(" ").build();

    private static ItemBuilder playerHead(final String owner) {
        return ItemBuilder.of("SKULL_ITEM", 1, (short) 3).head(owner);
    }

    public static void openDuelMenu(@NotNull final Player leader, @NotNull final Party myParty, @NotNull final PartyManager partyManager) {
        final PartyDuelHolder holder = new PartyDuelHolder(myParty);
        final Inventory inv = Bukkit.createInventory(holder, 54, "§8Parti Düellosu Seçimi");
        holder.setInventory(inv);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, FILLER);
        }

        final int myCount = myParty.getOnlineCount();
        int slot = 0;

        for (Player otherLeader : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) {
                break;
            }

            if (otherLeader.equals(leader) || !otherLeader.isOnline()) {
                continue;
            }

            final Party otherParty = partyManager.getParty(otherLeader.getUniqueId());
            if (otherParty == null || !otherParty.isLeader(otherLeader.getUniqueId())) {
                continue;
            }

            final int otherCount = otherParty.getOnlineCount();
            final boolean equal = (myCount == otherCount);
            final boolean validRange = (otherCount >= 2 && otherCount <= 5);

            final List<String> lore = new ArrayList<String>();
            lore.add("§7Lider: §f" + otherLeader.getName());
            lore.add("§7Kişi Sayısı: §f" + otherCount);
            lore.add("");

            if (equal && validRange) {
                lore.add("§a✔ Eşleşmeye Uygun (" + otherCount + "v" + otherCount + ")");
                lore.add("§eDüello teklif etmek için tıklayın!");
            } else if (!validRange) {
                lore.add("§c✖ Geçersiz Sayı (2v2 - 5v5 gerekli)");
            } else {
                lore.add("§c✖ Kişi Sayısı Eşit Değil!");
                lore.add("§7(Sizin: §e" + myCount + " §7- Karşı: §e" + otherCount + "§7)");
                lore.add("§c(Eşit olmadığı için düello başlatılamaz)");
            }

            inv.setItem(slot++, playerHead(otherLeader.getName())
                    .name("§e§l" + otherLeader.getName() + " Partisi")
                    .lore(lore)
                    .build());
        }

        // Slot 45: Geri Dön
        inv.setItem(45, ItemBuilder.of(Material.ARROW).name("§cGeri Dön").build());

        // Slot 49: Bilgi
        inv.setItem(49, ItemBuilder.of(Material.BOOK)
                .name("§6§lEşleşme Kuralı")
                .lore(
                        "§7Parti düellosu için her iki",
                        "§7partinin oyuncu sayısı kesinlikle",
                        "§7eşit olmak zorundadır (2v2, 3v3 vb.)."
                )
                .build());

        // Slot 53: Yenile
        inv.setItem(53, ItemBuilder.of(Material.COMPASS).name("§aYenile").build());

        leader.openInventory(inv);
    }
}
