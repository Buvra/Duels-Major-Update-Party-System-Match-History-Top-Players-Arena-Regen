package me.realized.duels.gui.settings;

import java.util.ArrayList;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.config.Config;
import me.realized.duels.gui.BaseButton;
import me.realized.duels.gui.settings.buttons.ArenaSelectButton;
import me.realized.duels.gui.settings.buttons.CancelButton;
import me.realized.duels.gui.settings.buttons.ItemBettingButton;
import me.realized.duels.gui.settings.buttons.KitSelectButton;
import me.realized.duels.gui.settings.buttons.MoneyBettingButton;
import me.realized.duels.gui.settings.buttons.OwnInventoryButton;
import me.realized.duels.gui.settings.buttons.RequestDetailsButton;
import me.realized.duels.gui.settings.buttons.RequestSendButton;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.gui.SinglePageGui;
import org.bukkit.inventory.ItemStack;

public class SettingsGui extends SinglePageGui<DuelsPlugin> {

    private static final int[][] PATTERNS = {
        {22},                   // 1 button
        {21, 23},               // 2 buttons
        {20, 22, 24},           // 3 buttons
        {19, 21, 23, 25},       // 4 buttons
        {18, 20, 22, 24, 26}    // 5 buttons (Kit, OwnInv, Arena, MoneyBet, ItemBet)
    };

    public SettingsGui(final DuelsPlugin plugin) {
        super(plugin, getTitle(plugin), 5);
        final Config config = plugin.getConfiguration();

        final ItemStack border = Items.from("STAINED_GLASS_PANE", (short) 15); // Black Glass Pane
        final ItemStack inner = Items.from("STAINED_GLASS_PANE", (short) 7);   // Gray Glass Pane
        final ItemStack redAccent = Items.from("STAINED_GLASS_PANE", (short) 14); // Red Glass Pane
        final ItemStack greenAccent = Items.from("STAINED_GLASS_PANE", (short) 5); // Green Glass Pane

        // Fill all 45 slots with border pane
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, border);
        }

        // Inner rows 1 (9..17) and 3 (27..35) with subtle gray pane
        for (int i = 9; i <= 17; i++) {
            inventory.setItem(i, inner);
        }
        for (int i = 27; i <= 35; i++) {
            inventory.setItem(i, inner);
        }

        // Slot 4: Target Opponent Head with Match Details
        set(4, new RequestDetailsButton(plugin));

        final List<BaseButton> buttons = new ArrayList<>();

        if (config.isKitSelectingEnabled()) {
            buttons.add(new KitSelectButton(plugin));
        }

        if (config.isOwnInventoryEnabled()) {
            buttons.add(new OwnInventoryButton(plugin));
        }

        if (config.isArenaSelectingEnabled()) {
            buttons.add(new ArenaSelectButton(plugin));
        }

        if (config.isMoneyBettingEnabled()) {
            buttons.add(new MoneyBettingButton(plugin));
        }

        if (config.isItemBettingEnabled()) {
            buttons.add(new ItemBettingButton(plugin));
        }

        if (!buttons.isEmpty()) {
            final int index = Math.min(buttons.size() - 1, PATTERNS.length - 1);
            final int[] pattern = PATTERNS[index];

            for (int i = 0; i < buttons.size() && i < pattern.length; i++) {
                set(pattern[i], buttons.get(i));
            }
        }

        // Bottom action row:
        // Red accent around cancel button
        inventory.setItem(37, redAccent);
        set(38, new CancelButton(plugin));
        inventory.setItem(39, redAccent);

        // Green accent around send button
        inventory.setItem(41, greenAccent);
        set(42, new RequestSendButton(plugin));
        inventory.setItem(43, greenAccent);
    }

    private static String getTitle(final DuelsPlugin plugin) {
        final String title = plugin.getLang().getMessage("GUI.settings.title");
        if (title == null || title.equals("Request Settings") || title.trim().isEmpty()) {
            return "&8⚔ Düello Ayarları";
        }
        return title;
    }
}
