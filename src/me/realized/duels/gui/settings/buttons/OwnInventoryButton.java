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

public class OwnInventoryButton extends BaseButton {

    public OwnInventoryButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.CHEST).name("§c§l🎒 KENDİ EŞYALARINLA §4(EŞYA KAYBI VAR!)").build());
    }

    @Override
    public void update(final Player player) {
        if (config.isOwnInventoryUsePermission() && !player.hasPermission(Permissions.OWN_INVENTORY) && !player.hasPermission(Permissions.SETTING_ALL)) {
            setLore("§cBu seçeneği kullanmak için yetkiniz yok.");
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final boolean isOwnInvActive = settings != null && settings.isOwnInventory();
        setGlow(isOwnInvActive);

        setDisplayName(isOwnInvActive ? "§c§l🎒 KENDİ EŞYALARINLA §a(AKTİF - EŞYA KAYBI VAR!)" : "§c§l🎒 KENDİ EŞYALARINLA §4(EŞYA KAYBI VAR!)");

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Tür: §c§lRİSKLİ DÜELLO");
        lore.add("§7Durum: " + (isOwnInvActive ? "§a✔ AKTİF (Kendi Eşyalarınla)" : "§c✖ Devre Dışı (Kit Modu Aktif)"));
        lore.add(" ");
        lore.add("§7Kendi üzerinizdeki zırh, kılıç ve");
        lore.add("§7eşyalarınızla savaşırsınız.");
        lore.add(" ");
        lore.add("§4§l⚠ DİKKAT / TEHLİKE:");
        lore.add("§cKaybeden oyuncunun üzerindeki ve");
        lore.add("§cenvanterindeki §4TÜM EŞYALAR RAKİBE GEÇER!");
        lore.add(" ");
        lore.add(isOwnInvActive ? "§cDevre dışı bırakmak için tıklayın!" : "§eKendi eşyalarınla oynamak için tıklayın!");
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        if (config.isOwnInventoryUsePermission() && !player.hasPermission(Permissions.OWN_INVENTORY) && !player.hasPermission(Permissions.SETTING_ALL)) {
            lang.sendMessage(player, "ERROR.no-permission", "permission", Permissions.OWN_INVENTORY);
            return;
        }

        if (!config.isKitSelectingEnabled()) {
            lang.sendMessage(player, "ERROR.duel.mode-fixed");
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        settings.setOwnInventory(!settings.isOwnInventory());
        settings.updateGui(player);
    }
}