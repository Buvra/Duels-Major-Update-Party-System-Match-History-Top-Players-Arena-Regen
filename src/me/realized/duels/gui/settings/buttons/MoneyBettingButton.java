package me.realized.duels.gui.settings.buttons;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.Permissions;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.gui.betting.MoneyBettingGui;
import me.realized.duels.hook.hooks.VaultHook;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MoneyBettingButton extends BaseButton {

    private static final NumberFormat FORMAT = NumberFormat.getInstance(Locale.US);

    public MoneyBettingButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.GOLD_INGOT).name("§6§l💰 PARA BAHİSİ").build());
    }

    @Override
    public void update(final Player player) {
        if (config.isMoneyBettingUsePermission() && !player.hasPermission(Permissions.MONEY_BETTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            setLore("§cBu seçeneği kullanmak için yetkiniz yok.");
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final int bet = settings != null ? settings.getBet() : 0;
        final boolean hasBet = bet > 0;
        setGlow(hasBet);

        final VaultHook vault = plugin.getVault();
        final double balance = (vault != null && vault.getEconomy() != null) ? vault.getEconomy().getBalance(player) : 0;

        setDisplayName(hasBet ? "§6§l💰 PARA BAHİSİ: §a" + FORMAT.format(bet) + "$" : "§6§l💰 PARA BAHİSİ §7(Yok)");

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Bu düelloya para bahsi koyun.");
        lore.add("§7Mevcut Bahis: " + (hasBet ? "§a" + FORMAT.format(bet) + "$" : "§c0$ (Bahissiz)"));
        if (hasBet) {
            lore.add("§eToplam Ödül: §6" + FORMAT.format(bet * 2) + "$ §7(Havuz)");
            lore.add(" ");
            lore.add("§c§lUYARI: §7Kaybeden taraf §c" + FORMAT.format(bet) + "$ §7kaybeder!");
            lore.add("§aKazanan taraf §e" + FORMAT.format(bet * 2) + "$ §aalır.");
        }
        lore.add("§7Cüzdanınız: §f" + FORMAT.format((long) balance) + "$");
        lore.add(" ");
        lore.add("§eSol Tık: §7Bahis Ayarlama Menüsü");
        lore.add("§aSağ Tık: §7Hızlıca +1,000$ Ekle");
        lore.add("§cShift + Tık: §7Bahsi Sıfırla (0$)");
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        if (config.isMoneyBettingUsePermission() && !player.hasPermission(Permissions.MONEY_BETTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            lang.sendMessage(player, "ERROR.no-permission", "permission", Permissions.MONEY_BETTING);
            return;
        }

        player.closeInventory();
        MoneyBettingGui.open(player, plugin);
        try {
            player.playSound(player.getLocation(), Sound.CLICK, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
    }
}
