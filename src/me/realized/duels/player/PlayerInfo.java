package me.realized.duels.player;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.realized.duels.util.PlayerUtil;
import me.realized.duels.util.inventory.InventoryUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerInfo {
    private final Map<String, Map<Integer, ItemStack>> items = new HashMap<>();
    private final List<PotionEffect> effects;
    private final double health;
    private final int hunger;
    private final List<ItemStack> extra = new ArrayList<>();
    private Location location;

    public PlayerInfo(final List<PotionEffect> effects, final double health, final int hunger, final Location location) {
        this.effects = effects;
        this.health = health;
        this.hunger = hunger;
        this.location = location;
    }

    public PlayerInfo(final Player player) {
        this(Lists.newArrayList(player.getActivePotionEffects()), player.getHealth(), player.getFoodLevel(), player.getLocation().clone());
        InventoryUtil.addToMap(player.getInventory(), items);
    }

    public void restore(final Player player) {
        final double maxHealth = PlayerUtil.getMaxHealth(player);
        player.addPotionEffects(effects);
        player.setHealth(health > maxHealth ? maxHealth : health);
        player.setFoodLevel(hunger);
        InventoryUtil.fillFromMap(player.getInventory(), items);
        InventoryUtil.addOrDrop(player, extra);
        player.updateInventory();
    }

    public Map<String, Map<Integer, ItemStack>> getItems() {
        return this.items;
    }

    public List<PotionEffect> getEffects() {
        return this.effects;
    }

    public double getHealth() {
        return this.health;
    }

    public int getHunger() {
        return this.hunger;
    }

    public List<ItemStack> getExtra() {
        return this.extra;
    }

    public Location getLocation() {
        return this.location;
    }

    public void setLocation(final Location location) {
        this.location = location;
    }
}
