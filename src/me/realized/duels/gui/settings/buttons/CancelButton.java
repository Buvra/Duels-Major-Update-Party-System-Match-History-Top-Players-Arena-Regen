package me.realized.duels.gui.settings.buttons;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CancelButton extends BaseButton {

    public CancelButton(final DuelsPlugin plugin) {
        super(plugin, ItemBuilder.of(Material.REDSTONE_BLOCK).name("§c§l✖ İPTAL ET").build());
    }

    @Override
    public void update(final Player player) {
        final List<String> lore = new ArrayList<>();
        lore.add("§8§m------------------------");
        lore.add("§7Düello teklifini iptal eder");
        lore.add("§7ve bu menüyü kapatır.");
        lore.add(" ");
        lore.add("§cKapatmak için tıklayın.");
        lore.add("§8§m------------------------");
        setLore(lore);
    }

    @Override
    public void onClick(final Player player) {
        player.closeInventory();
    }
}
