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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class KitSelectButton extends BaseButton {

    public KitSelectButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.DIAMOND_SWORD)
                .name("§b§l⚔ KİTLİ DÜELLO §a(EŞYA KAYBI YOK)")
                .editMeta(meta -> {
                    try {
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    } catch (Throwable ignored) {}
                })
                .build());
    }

    @Override
    public void update(final Player player) {
        if (config.isKitSelectingUsePermission() && !player.hasPermission(Permissions.KIT_SELECTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            setLore("§cBu seçeneği kullanmak için yetkiniz yok.");
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final boolean isKitActive = !settings.isOwnInventory() && settings.getKit() != null;
        
        // Eğer bir kit seçilmişse, butonun ikonunu o kitin ikonu (displayed) yap; seçilmemişse varsayılan elmas kılıç yap:
        final ItemStack displayBase = (isKitActive && settings.getKit().getDisplayed() != null)
                ? settings.getKit().getDisplayed().clone()
                : ItemBuilder.of(Material.DIAMOND_SWORD).build();
        setDisplayed(displayBase);

        setGlow(isKitActive);

        setDisplayName(isKitActive ? "§b§l⚔ KİTLİ DÜELLO §a(AKTİF - EŞYA KAYBI YOK)" : "§b§l⚔ KİTLİ DÜELLO §7(EŞYA KAYBI YOK)");

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Tür: §a§lGÜVENLİ DÜELLO");
        lore.add("§7Seçili Kit: " + (settings.getKit() != null ? "§e§l" + settings.getKit().getName() : "§cSeçilmedi"));
        lore.add("§7Durum: " + (isKitActive ? "§a✔ AKTİF (Kitli Mod)" : (settings.isOwnInventory() ? "§c✖ Devre Dışı (Kendi Eşyaların Aktif)" : "§eKit Seçilmeli")));
        lore.add(" ");
        lore.add("§a✔ Kendi eşyalarınız güvendedir.");
        lore.add("§a✔ Özel kit eşyaları verilir.");
        lore.add("§a✔ Düello bitince eşyalarınız eksiksiz geri gelir!");
        lore.add(" ");
        lore.add("§eKit seçmek veya değiştirmek");
        lore.add("§eiçin tıklayın!");
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        if (config.isKitSelectingUsePermission() && !player.hasPermission(Permissions.KIT_SELECTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            lang.sendMessage(player, "ERROR.no-permission", "permission", Permissions.KIT_SELECTING);
            return;
        }

        kitManager.getGui().open(player);
    }
}
