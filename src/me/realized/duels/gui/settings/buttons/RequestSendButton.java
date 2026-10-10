package me.realized.duels.gui.settings.buttons;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class RequestSendButton extends BaseButton {

    public RequestSendButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.EMERALD_BLOCK).name("§a§l✔ DÜELLO GÖNDER").build());
    }

    @Override
    public void update(final Player player) {
        final Settings settings = settingManager.getSafely(player);
        final boolean ready = settings != null && (settings.isOwnInventory() || settings.getKit() != null);
        setGlow(ready);

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Seçtiğiniz ayarlarla rakibe");
        lore.add("§7meydan okuma teklifi gönderir.");
        lore.add(" ");
        if (ready) {
            lore.add("§a✔ Gönderilmeye Hazır!");
            lore.add(" ");
            lore.add("§eTeklifi göndermek için tıklayın!");
        } else {
            lore.add("§c✖ Lütfen önce Kitli Düello");
            lore.add("§c  veya Kendi Eşyalarını seçin!");
        }
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        final Settings settings = settingManager.getSafely(player);

        if (settings == null || settings.getTarget() == null) {
            if (settings != null) {
                settings.reset();
            }
            player.closeInventory();
            return;
        }

        final Player target = Bukkit.getPlayer(settings.getTarget());

        if (target == null) {
            settings.reset();
            player.closeInventory();
            lang.sendMessage(player, "ERROR.player.no-longer-online");
            return;
        }

        if (!settings.isOwnInventory() && settings.getKit() == null) {
            lang.sendMessage(player, "ERROR.duel.mode-unselected");
            return;
        }

        player.closeInventory();

        if (settings.isOwnInventory()) {
            me.realized.duels.gui.confirm.OwnInventoryConfirmGui.openForSender(player, target, settings, plugin);
            return;
        }

        requestManager.send(player, target, settings);
    }
}
