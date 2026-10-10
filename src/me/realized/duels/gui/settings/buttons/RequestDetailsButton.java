package me.realized.duels.gui.settings.buttons;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RequestDetailsButton extends BaseButton {

    public RequestDetailsButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Items.HEAD.clone()).name("§e§lDÜELLO BİLGİLERİ").build());
    }

    @Override
    public void update(final Player player) {
        final Settings settings = settingManager.getSafely(player);
        if (settings == null || settings.getTarget() == null) {
            return;
        }

        final Player target = Bukkit.getPlayer(settings.getTarget());

        if (target == null) {
            settings.reset();
            player.closeInventory();
            lang.sendMessage(player, "ERROR.player.no-longer-online");
            return;
        }

        setOwner(target);
        setDisplayName("§e§lDÜELLO BİLGİLERİ §7(§f" + target.getName() + "§7)");

        final String modeStr;
        if (settings.isOwnInventory()) {
            modeStr = "§c§l🎒 Kendi Eşyalarınla §4(EŞYA KAYBI VAR!)";
        } else if (settings.getKit() != null) {
            modeStr = "§b§l⚔ Kitli §7(" + settings.getKit().getName() + ") §a(Güvenli - Eşya Kaybı Yok)";
        } else {
            modeStr = "§c✖ Seçilmedi";
        }

        final String arenaStr = settings.getArena() != null ? "§a§l" + settings.getArena().getName() : "§7Rastgele Arena";

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Rakip: §f" + target.getName());
        lore.add("§7Oyun Modu: " + modeStr);
        lore.add("§7Arena: " + arenaStr);

        if (config.isMoneyBettingEnabled() && settings.getBet() > 0) {
            final int bet = settings.getBet();
            lore.add("§6💰 Para Bahsi: §a" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet) + "$");
            lore.add("§e🏆 Toplam Havuz: §6" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet * 2) + "$");
            lore.add("§c⚠ Kaybeden taraf §4" + java.text.NumberFormat.getInstance(java.util.Locale.US).format(bet) + "$ §ckaybeder!");
        }

        if (config.isItemBettingEnabled()) {
            lore.add("§7Eşya Bahsi: " + (settings.isItemBetting() ? "§a✔ Açık (Ortadaki Eşyalar)" : "§c✖ Kapalı"));
        }

        lore.add("§8§m------------------------");
        lore.add("§7Ayarları değiştirmek için");
        lore.add("§7aşağıdaki butonları kullanabilirsiniz.");
        lore.add("§8§m------------------------");

        setLore(lore);
    }
}
