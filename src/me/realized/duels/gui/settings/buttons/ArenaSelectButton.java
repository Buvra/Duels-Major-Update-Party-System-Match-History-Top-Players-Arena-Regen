package me.realized.duels.gui.settings.buttons;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.Permissions;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.entity.Player;

public class ArenaSelectButton extends BaseButton {

    public ArenaSelectButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Items.EMPTY_MAP).name("§e§l🏟 ARENA SEÇİMİ").build());
    }

    @Override
    public void update(final Player player) {
        if (config.isArenaSelectingUsePermission() && !player.hasPermission(Permissions.ARENA_SELECTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            setLore("§cBu seçeneği kullanmak için yetkiniz yok.");
            return;
        }

        final Settings settings = settingManager.getSafely(player);
        final boolean hasArena = settings != null && settings.getArena() != null;
        setGlow(hasArena);

        setDisplayName("§e§l🏟 ARENA SEÇİMİ");

        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Düellonun yapılacağı özel");
        lore.add("§7arenayı belirleyin.");
        lore.add(" ");
        lore.add("§7Seçili Arena: " + (hasArena ? "§a§l" + settings.getArena().getName() : "§7Rastgele Arena"));
        lore.add(" ");
        lore.add("§eArenayı değiştirmek için tıklayın!");
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        if (config.isArenaSelectingUsePermission() && !player.hasPermission(Permissions.ARENA_SELECTING) && !player.hasPermission(Permissions.SETTING_ALL)) {
            lang.sendMessage(player, "ERROR.no-permission", "permission", Permissions.ARENA_SELECTING);
            return;
        }

        arenaManager.getGui().open(player);
    }
}
