package me.realized.duels.party.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.party.Party;
import me.realized.duels.party.PartyManager;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 27 Slotluk interaktif Sandık Menüsü (Chest GUI) - /parti veya /p.
 */
public class PartyGUI {

    private static final ItemStack FILLER = ItemBuilder.of("STAINED_GLASS_PANE", 1, (short) 15).name(" ").build();

    private static ItemBuilder playerHead(final String owner) {
        return ItemBuilder.of("SKULL_ITEM", 1, (short) 3).head(owner);
    }

    /**
     * Ana Parti Menüsünü açar.
     */
    public static void openMainMenu(@NotNull final Player player, @NotNull final Party party) {
        final PartyMenuHolder holder = new PartyMenuHolder(party);
        final Inventory inv = Bukkit.createInventory(holder, 27, "§8Parti Yönetimi");
        holder.setInventory(inv);

        // Boşlukları cam ile doldur
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, FILLER);
        }

        final boolean isLeader = party.isLeader(player.getUniqueId());
        final Player leaderPlayer = Bukkit.getPlayer(party.getLeader());
        final String leaderName = leaderPlayer != null ? leaderPlayer.getName() : "Bilinmeyen";

        // Slot 10: Parti Bilgisi
        final List<String> infoLore = new ArrayList<String>();
        infoLore.add("§7Kapasite: §e" + party.size() + "/" + Party.MAX_MEMBERS + " Oyuncu");
        infoLore.add("§7Lider: §f" + leaderName);
        infoLore.add("§7Üyeler:");
        for (UUID uid : party.getMembers()) {
            final Player member = Bukkit.getPlayer(uid);
            final boolean online = member != null && member.isOnline();
            final String name = online ? member.getName() : Bukkit.getOfflinePlayer(uid).getName();
            final String status = online ? "§a[Çevrimiçi]" : "§c[Çevrimdışı]";
            infoLore.add("  §8▪ §f" + name + " " + status + (party.isLeader(uid) ? " §6(Lider)" : ""));
        }

        inv.setItem(10, playerHead(leaderName)
                .name("§6§lParti Bilgisi")
                .lore(infoLore)
                .build());

        // Slot 12: Oyuncu Davet Et
        inv.setItem(12, ItemBuilder.of(Material.NAME_TAG)
                .name("§a§lOyuncu Davet Et")
                .lore(
                        "§7Sohbete oyuncu adı yazarak",
                        "§7partinize davet etmek için tıklayın.",
                        "",
                        "§eTıklayarak davet başlatın."
                )
                .build());

        // Slot 14: Üye Yönetimi / Oyuncu At (Yalnızca lidere)
        if (isLeader) {
            inv.setItem(14, ItemBuilder.of(Material.IRON_AXE)
                    .name("§c§lÜye Yönetimi (Oyuncu At)")
                    .lore("§7Bir üyeyi partiden çıkarmak", "§7için tıklayın.")
                    .build());
        } else {
            inv.setItem(14, ItemBuilder.of(Material.BARRIER)
                    .name("§7Üye Yönetimi")
                    .lore("§cBu işlem yalnızca lider içindir!")
                    .build());
        }

        // Slot 16: Partiden Ayrıl / Dağıt
        if (isLeader) {
            inv.setItem(16, ItemBuilder.of(Material.TNT)
                    .name("§c§lPartiyi Dağıt")
                    .lore("§7Partiyi tamamen kapatmak", "§7için tıklayın.")
                    .build());
        } else {
            inv.setItem(16, ItemBuilder.of(Material.REDSTONE)
                    .name("§c§lPartiden Ayrıl")
                    .lore("§7Partiden çıkmak için", "§7tıklayın.")
                    .build());
        }

        // Slot 22: Parti Düellosu Başlat (Yalnızca Lider)
        if (isLeader) {
            inv.setItem(22, ItemBuilder.of(Material.DIAMOND_SWORD)
                    .name("§6§lParti Düellosu Başlat")
                    .lore(
                            "§7Lobideki diğer partilere 2v2 - 5v5",
                            "§7düello teklif etmek için tıklayın.",
                            "",
                            "§aTıklayarak lider listesini açın."
                    )
                    .build());
        } else {
            inv.setItem(22, ItemBuilder.of(Material.BARRIER)
                    .name("§7Parti Düellosu")
                    .lore("§cBu işlemi yalnızca parti lideri yapabilir!")
                    .build());
        }

        player.openInventory(inv);
    }

    /**
     * Üye Yönetimi (Oyuncu Atma) alt menüsü.
     */
    public static void openMemberManageMenu(@NotNull final Player player, @NotNull final Party party) {
        final PartyMemberHolder holder = new PartyMemberHolder(party);
        final Inventory inv = Bukkit.createInventory(holder, 27, "§8Üye Yönetimi - Oyuncu At");
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, FILLER);
        }

        int slot = 10;
        for (UUID uid : party.getMembers()) {
            if (uid.equals(player.getUniqueId())) {
                continue; // Kendini atamaz
            }

            final Player member = Bukkit.getPlayer(uid);
            final String name = member != null ? member.getName() : Bukkit.getOfflinePlayer(uid).getName();

            inv.setItem(slot++, playerHead(name)
                    .name("§c§l" + name)
                    .lore("§7Partiden çıkarmak için tıkla.")
                    .build());
            if (slot == 17) slot = 19;
        }

        // Slot 18: Geri Dön
        inv.setItem(18, ItemBuilder.of(Material.ARROW).name("§cGeri Dön").build());

        player.openInventory(inv);
    }

    /**
     * Oyuncu Davet Etme alt menüsü (Lobideki boş oyuncuları listeler).
     */
    public static void openInviteMenu(@NotNull final Player player, @NotNull final Party party, @NotNull final PartyManager partyManager) {
        final PartyInviteHolder holder = new PartyInviteHolder(party);
        final Inventory inv = Bukkit.createInventory(holder, 54, "§8Oyuncu Davet Et");
        holder.setInventory(inv);

        for (int i = 0; i < 54; i++) {
            inv.setItem(i, FILLER);
        }

        int slot = 0;
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;
            if (target.equals(player) || partyManager.isInParty(target.getUniqueId())) {
                continue;
            }

            inv.setItem(slot++, playerHead(target.getName())
                    .name("§a§l" + target.getName())
                    .lore("§7Partiye davet etmek için tıkla.")
                    .build());
        }

        inv.setItem(45, ItemBuilder.of(Material.ARROW).name("§cGeri Dön").build());

        player.openInventory(inv);
    }

    /**
     * Bir partide olmayan oyuncu /parti yazdığında açılan parti kurma menüsü.
     */
    public static void openCreateMenu(@NotNull final Player player) {
        final PartyCreateHolder holder = new PartyCreateHolder();
        final Inventory inv = Bukkit.createInventory(holder, 27, "§8Parti Menüsü");
        holder.setInventory(inv);

        for (int i = 0; i < 27; i++) {
            inv.setItem(i, FILLER);
        }

        // Slot 11: Parti Bilgisi
        inv.setItem(11, ItemBuilder.of(Material.BOOK)
                .name("§6§lParti Bilgisi")
                .lore(
                        "§7Parti kurarak arkadaşlarınızla",
                        "§7takım olabilir ve diğer partilerle",
                        "§72v2'den 5v5'e kadar düellolar",
                        "§7yapabilirsiniz.",
                        "",
                        "§eMaksimum Kapasite: §f5 Oyuncu"
                )
                .build());

        // Slot 13: Parti Oluştur
        inv.setItem(13, ItemBuilder.of(Material.NETHER_STAR)
                .name("§a§lParti Oluştur")
                .lore(
                        "§7Yeni bir parti kurmak ve",
                        "§7lider olmak için tıklayın.",
                        "",
                        "§a► Tıklayarak partiyi oluşturun!"
                )
                .build());

        // Slot 15: Gelen Davet Bilgisi
        inv.setItem(15, ItemBuilder.of(Material.PAPER)
                .name("§b§lGelen Davetler")
                .lore(
                        "§7Eğer başka bir liderden parti",
                        "§7daveti aldıysanız, sohbete:",
                        "§e/parti katil <lider>",
                        "§7yazarak katılabilirsiniz."
                )
                .build());

        player.openInventory(inv);
    }
}
