package me.realized.duels.gui.settings.buttons;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.Permissions;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ItemBettingButton extends BaseButton {

    public ItemBettingButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.DIAMOND).name("§d§l💎 EŞYA BAHSİ").build());
    }

    @Override
    public void update(final Player player) {
        if (config.isItemBettingUsePermission() && !player.hasPermission(Permissions.ITEM_BETTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            setLore("§cBu seçeneği kullanmak için yetkiniz yok.");
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final boolean isBetting = settings != null && settings.isItemBetting();
        setGlow(isBetting);

        setDisplayName(isBetting ? "§d§l💎 EŞYA BAHSİ §a(AÇIK)" : "§d§l💎 EŞYA BAHSİ §c(KAPALI)");

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Kazanan taraf rakibin envanterindeki");
        lore.add("§7tüm bahis eşyalarını kazanır!");
        lore.add(" ");
        lore.add("§7Durum: " + (isBetting ? "§a✔ AÇIK" : "§c✖ KAPALI"));
        lore.add(" ");
        lore.add(isBetting ? "§cBahsi kapatmak için tıklayın!" : "§aBahsi açmak için tıklayın!");
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        if (config.isItemBettingUsePermission() && !player.hasPermission(Permissions.ITEM_BETTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            lang.sendMessage(player, "ERROR.no-permission", "permission", Permissions.ITEM_BETTING);
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        settings.setItemBetting(!settings.isItemBetting());
        settings.updateGui(player);
    }
}
