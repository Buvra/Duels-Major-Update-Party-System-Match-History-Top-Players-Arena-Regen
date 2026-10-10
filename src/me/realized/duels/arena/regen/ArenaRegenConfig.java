package me.realized.duels.arena.regen;

import java.io.File;
import java.io.IOException;
import me.realized.duels.DuelsPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ArenaRegenConfig {

    private final Material selectingTool;
    private final int blocksPerTick;
    private final boolean allowArenaBlockBreak;
    private final boolean removeDroppedItems;
    private final boolean preventBlockBurn;
    private final boolean preventBlockMelt;
    private final boolean preventFireSpread;
    private final boolean preventLeafDecay;

    public ArenaRegenConfig(final DuelsPlugin plugin) {
        final File file = new File(plugin.getDataFolder(), "arenaregen.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ignored) {}
        }

        final FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.addDefault("selecting-tool", "IRON_HOE");
        config.addDefault("blocks-per-tick", 250);
        config.addDefault("allow-arena-block-break", false);
        config.addDefault("remove-dropped-items", true);
        config.addDefault("prevent-block-burn", true);
        config.addDefault("prevent-block-melt", true);
        config.addDefault("prevent-fire-spread", true);
        config.addDefault("prevent-leaf-decay", true);
        config.options().copyDefaults(true);

        try {
            config.save(file);
        } catch (IOException ignored) {}

        final String toolName = config.getString("selecting-tool", "IRON_HOE");
        final Material tool = Material.getMaterial(toolName);
        this.selectingTool = tool != null ? tool : Material.IRON_HOE;
        this.blocksPerTick = Math.max(25, config.getInt("blocks-per-tick", 250));
        this.allowArenaBlockBreak = config.getBoolean("allow-arena-block-break", false);
        this.removeDroppedItems = config.getBoolean("remove-dropped-items", true);
        this.preventBlockBurn = config.getBoolean("prevent-block-burn", true);
        this.preventBlockMelt = config.getBoolean("prevent-block-melt", true);
        this.preventFireSpread = config.getBoolean("prevent-fire-spread", true);
        this.preventLeafDecay = config.getBoolean("prevent-leaf-decay", true);
    }

    public Material getSelectingTool() {
        return selectingTool;
    }

    public int getBlocksPerTick() {
        return blocksPerTick;
    }

    public boolean isAllowArenaBlockBreak() {
        return allowArenaBlockBreak;
    }

    public boolean isRemoveDroppedItems() {
        return removeDroppedItems;
    }

    public boolean isPreventBlockBurn() {
        return preventBlockBurn;
    }

    public boolean isPreventBlockMelt() {
        return preventBlockMelt;
    }

    public boolean isPreventFireSpread() {
        return preventFireSpread;
    }

    public boolean isPreventLeafDecay() {
        return preventLeafDecay;
    }
}
