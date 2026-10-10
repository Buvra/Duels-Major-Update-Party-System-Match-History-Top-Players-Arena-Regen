package me.realized.duels.gui.confirm;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.request.RequestImpl;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class OwnInventoryConfirmGui {

    private OwnInventoryConfirmGui() {}

    public static void openForSender(final Player player, final Player target, final Settings settings, final DuelsPlugin plugin) {
        final OwnInventoryConfirmHolder holder = new OwnInventoryConfirmHolder(
                OwnInventoryConfirmHolder.Type.SENDER,
                player,
                target,
                settings,
                null
        );

        final Inventory inv = Bukkit.createInventory(holder, 27, "§4§l⚠ DİKKAT: EŞYA KAYBI RİSKİ");
        holder.setInventory(inv);

        setupBaseLayout(inv);

        // Slot 13: Opponent Head & Risk warning
        final List<String> warnLore = new ArrayList<>();
        warnLore.add("§8§m---------------------------------");
        warnLore.add("§7Rakip: §e" + target.getName());
        warnLore.add("§7Oyun Modu: §6🎒 Kendi Eşyalarınla Düello");
        if (settings != null && settings.getBet() > 0) {
            final int bet = settings.getBet();
            warnLore.add("§6💰 Para Bahsi: §a" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet) + "$");
            warnLore.add("§e🏆 Toplam Havuz: §6" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet * 2) + "$");
        }
        if (settings != null && settings.isItemBetting()) {
            warnLore.add("§d📦 Ek Eşya Bahsi: §aAktif");
        }
        warnLore.add(" ");
        warnLore.add("§c§lÖNEMLİ TEHLİKE UYARISI:");
        warnLore.add("§7Bu düelloda önceden belirlenmiş bir kit");
        warnLore.add("§7yerine §eüzerinizdeki kendi eşyalarınız §7kullanılır.");
        warnLore.add(" ");
        warnLore.add("§4§lDİKKAT: KAYBEDERSENİZ TÜM EŞYALARINIZ");
        warnLore.add("§4§lRAKİP OYUNCUYA DEVREDİLECEKTİR!");
        warnLore.add(" ");
        warnLore.add("§eBu riski kabul edip teklif göndermek");
        warnLore.add("§eistiyor musunuz?");
        warnLore.add("§8§m---------------------------------");

        inv.setItem(13, ItemBuilder.of(Items.HEAD.clone())
                .head(target.getName())
                .name("§c§l⚠ KENDİ EŞYALARINLA DÜELLO ⚠")
                .lore(warnLore)
                .build());

        // Slot 11: Accept & Send
        inv.setItem(11, ItemBuilder.of(Material.EMERALD_BLOCK)
                .name("§a§l✔ RİSKİ KABUL ET VE GÖNDER")
                .lore(
                        "§8§m---------------------------------",
                        "§7Eşya kaybı riskini kabul ediyorum.",
                        "§7Düello teklifini rakibe gönder.",
                        " ",
                        "§eGöndermek için tıklayın!",
                        "§8§m---------------------------------"
                )
                .build());

        // Slot 15: Cancel & Back
        inv.setItem(15, ItemBuilder.of(Material.REDSTONE_BLOCK)
                .name("§c§l✖ VAZGEÇ (GERİ DÖN)")
                .lore(
                        "§8§m---------------------------------",
                        "§7Bu riski almak istemiyorum.",
                        "§7Ayarlar menüsüne geri dön.",
                        " ",
                        "§cİptal etmek için tıklayın.",
                        "§8§m---------------------------------"
                )
                .build());

        player.openInventory(inv);
    }

    public static void openForReceiver(final Player player, final Player target, final RequestImpl request, final DuelsPlugin plugin) {
        final OwnInventoryConfirmHolder holder = new OwnInventoryConfirmHolder(
                OwnInventoryConfirmHolder.Type.RECEIVER,
                player,
                target,
                null,
                request
        );

        final Inventory inv = Bukkit.createInventory(holder, 27, "§4§l⚠ DİKKAT: EŞYA KAYBI RİSKİ");
        holder.setInventory(inv);

        setupBaseLayout(inv);

        // Slot 13: Opponent Head & Risk warning
        final List<String> warnLore = new ArrayList<>();
        warnLore.add("§8§m---------------------------------");
        warnLore.add("§7Rakip: §e" + target.getName());
        warnLore.add("§7Oyun Modu: §6🎒 Kendi Eşyalarınla Düello");
        if (request != null && request.getBet() > 0) {
            final int bet = request.getBet();
            warnLore.add("§6💰 Para Bahsi: §a" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet) + "$");
            warnLore.add("§e🏆 Toplam Havuz: §6" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet * 2) + "$");
        }
        if (request != null && request.canBetItems()) {
            warnLore.add("§d📦 Ek Eşya Bahsi: §aAktif");
        }
        warnLore.add(" ");
        warnLore.add("§c§lÖNEMLİ TEHLİKE UYARISI:");
        warnLore.add("§e" + target.getName() + " §7size §ckendi eşyalarınızla");
        warnLore.add("§7düello yapmak üzere meydan okudu.");
        warnLore.add(" ");
        warnLore.add("§4§lDİKKAT: KAYBEDERSENİZ TÜM EŞYALARINIZ");
        warnLore.add("§4§lRAKİP OYUNCUYA DEVREDİLECEKTİR!");
        warnLore.add(" ");
        warnLore.add("§eBu riski kabul edip düelloya");
        warnLore.add("§ebaşlamak istiyor musunuz?");
        warnLore.add("§8§m---------------------------------");

        inv.setItem(13, ItemBuilder.of(Items.HEAD.clone())
                .head(target.getName())
                .name("§c§l⚠ KENDİ EŞYALARINLA DÜELLO ⚠")
                .lore(warnLore)
                .build());

        // Slot 11: Accept & Start
        inv.setItem(11, ItemBuilder.of(Material.EMERALD_BLOCK)
                .name("§a§l✔ RİSKİ KABUL ET VE BAŞLA")
                .lore(
                        "§8§m---------------------------------",
                        "§7Eşya kaybı riskini kabul ediyorum.",
                        "§7Düelloyu hemen başlat!",
                        " ",
                        "§eBaşlamak için tıklayın!",
                        "§8§m---------------------------------"
                )
                .build());

        // Slot 15: Deny & Cancel
        inv.setItem(15, ItemBuilder.of(Material.REDSTONE_BLOCK)
                .name("§c§l✖ REDDET / VAZGEÇ")
                .lore(
                        "§8§m---------------------------------",
                        "§7Bu riski almak istemiyorum.",
                        "§7Düello teklifini reddet.",
                        " ",
                        "§cReddetmek için tıklayın.",
                        "§8§m---------------------------------"
                )
                .build());

        player.openInventory(inv);
    }

    private static void setupBaseLayout(final Inventory inv) {
        final ItemStack border = ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 14).name(" ").build();
        final ItemStack inner = ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
        for (int i = 9; i <= 17; i++) {
            inv.setItem(i, inner);
        }
    }
}
