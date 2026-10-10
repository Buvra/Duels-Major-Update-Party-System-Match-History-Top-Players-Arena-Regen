package me.realized.duels.gui.betting;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.hook.hooks.VaultHook;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class MoneyBettingGui {

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    private MoneyBettingGui() {}

    public static void open(final Player player, final DuelsPlugin plugin) {
        final Settings settings = plugin.getSettingManager().getSafely(player);
        if (settings == null) {
            return;
        }

        final MoneyBettingHolder holder = new MoneyBettingHolder();
        final Inventory inv = Bukkit.createInventory(holder, 27, "§8💰 Para Bahsi Belirle");
        holder.setInventory(inv);

        final ItemStack border = ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 15).name(" ").build();
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }

        updateDisplay(player, inv, settings, plugin);

        // Butonlar
        inv.setItem(10, ItemBuilder.of(Material.EMERALD).name("§a§l+100$").lore("§7Bahse 100$ ekler.").build());
        inv.setItem(11, ItemBuilder.of(Material.EMERALD).name("§a§l+500$").lore("§7Bahse 500$ ekler.").build());
        inv.setItem(12, ItemBuilder.of(Material.EMERALD_BLOCK).name("§a§l+1,000$").lore("§7Bahse 1,000$ ekler.").build());
        inv.setItem(13, ItemBuilder.of(Material.EMERALD_BLOCK).name("§a§l+5,000$").lore("§7Bahse 5,000$ ekler.").build());
        inv.setItem(14, ItemBuilder.of(Material.DIAMOND).name("§b§l+10,000$").lore("§7Bahse 10,000$ ekler.").build());
        inv.setItem(15, ItemBuilder.of(Material.GOLD_BLOCK).name("§6§lTÜM PARAM (ALL-IN)").lore("§7Cüzdanındaki tüm parayı bahse koyar.").build());
        inv.setItem(16, ItemBuilder.of(Material.REDSTONE_BLOCK).name("§c§l↺ BAHSI SIFIRLA").lore("§7Bahsi 0$ yapar (Bahissiz düello).").build());

        // Onayla & Geri Dön
        inv.setItem(22, ItemBuilder.of(Material.STAINED_CLAY, 1, (short) 5)
                .name("§a§l✔ ONAYLA VE GERİ DÖN")
                .lore("§7Belirlediğiniz bahisle düello ayarlarına döner.")
                .build());

        player.openInventory(inv);
    }

    public static void updateDisplay(final Player player, final Inventory inv, final Settings settings, final DuelsPlugin plugin) {
        final int bet = settings.getBet();
        final VaultHook vault = plugin.getVault();
        final double balance = (vault != null && vault.getEconomy() != null) ? vault.getEconomy().getBalance(player) : 0;

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Mevcut Bahis: §a" + (bet > 0 ? FORMAT.format(bet) + "$" : "0$ (Bahissiz)"));
        if (bet > 0) {
            lore.add("§eToplam Kazanç: §6" + FORMAT.format(bet * 2) + "$ §7(Havuz)");
        }
        lore.add("§7Cüzdan Bakiyeniz: §f" + FORMAT.format((long) balance) + "$");
        lore.add(" ");
        lore.add("§c§lUYARI:");
        lore.add("§7Düello başladığında her iki taraftan da");
        lore.add("§7bu miktar kesilir. Kazanan havuzu alır!");
        lore.add("§8§m------------------------");

        inv.setItem(4, ItemBuilder.of(Material.GOLD_INGOT)
                .name("§6§lMEVCUT BAHİS: §a" + (bet > 0 ? FORMAT.format(bet) + "$" : "0$"))
                .lore(lore)
                .build());
    }

    public static void handleClick(final Player player, final int slot, final Inventory inv, final DuelsPlugin plugin) {
        final Settings settings = plugin.getSettingManager().getSafely(player);
        if (settings == null) {
            return;
        }

        final VaultHook vault = plugin.getVault();
        final double balance = (vault != null && vault.getEconomy() != null) ? vault.getEconomy().getBalance(player) : 0;
        final int currentBet = settings.getBet();

        int addAmount = 0;

        switch (slot) {
            case 10:
                addAmount = 100;
                break;
            case 11:
                addAmount = 500;
                break;
            case 12:
                addAmount = 1000;
                break;
            case 13:
                addAmount = 5000;
                break;
            case 14:
                addAmount = 10000;
                break;
            case 15: // All-in
                final int allIn = (int) Math.floor(balance);
                if (allIn <= 0) {
                    player.sendMessage("§cBunu yapmak için yeterli paranız yok!");
                    try { player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f); } catch (Throwable ignored) {}
                    return;
                }
                settings.setBet(allIn);
                try { player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 2.0f); } catch (Throwable ignored) {}
                updateDisplay(player, inv, settings, plugin);
                return;
            case 16: // Reset
                settings.setBet(0);
                try { player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f); } catch (Throwable ignored) {}
                updateDisplay(player, inv, settings, plugin);
                return;
            case 22: // Confirm & Back
                player.closeInventory();
                settings.openGui(player);
                try { player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f); } catch (Throwable ignored) {}
                return;
            default:
                return;
        }

        if (addAmount > 0) {
            final int newBet = currentBet + addAmount;
            if (newBet > balance) {
                player.sendMessage("§cBakiye yetersiz! Cüzdanınızda yalnızca " + FORMAT.format((long) balance) + "$ bulunuyor.");
                try { player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1.0f, 1.0f); } catch (Throwable ignored) {}
                return;
            }

            settings.setBet(newBet);
            try { player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f); } catch (Throwable ignored) {}
            updateDisplay(player, inv, settings, plugin);
        }
    }
}
