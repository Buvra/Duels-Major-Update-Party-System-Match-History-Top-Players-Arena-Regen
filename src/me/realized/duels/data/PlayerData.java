package me.realized.duels.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.realized.duels.player.PlayerInfo;
import me.realized.duels.util.Log;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

public class PlayerData {

    private static final transient String ITEM_LOAD_FAILURE = "Could not load item %s!";

    private Map<String, Map<Integer, ItemData>> items = new HashMap<String, Map<Integer, ItemData>>();
    private Collection<PotionEffectData> effects = new ArrayList<PotionEffectData>();
    private double health;
    private int hunger;
    private LocationData location;
    private List<ItemData> extra = new ArrayList<ItemData>();

    public static PlayerData fromPlayerInfo(PlayerInfo info) {
        if (info == null) {
            return null;
        }
        return new PlayerData(info);
    }

    private PlayerData() {
        // serializer için
    }

    private PlayerData(PlayerInfo info) {
        this.health = info.getHealth();
        this.hunger = info.getHunger();
        this.location = LocationData.fromLocation(info.getLocation());

        if (info.getItems() != null) {
            for (Map.Entry<String, Map<Integer, ItemStack>> entry : info.getItems().entrySet()) {
                if (entry == null || entry.getValue() == null) {
                    continue;
                }

                Map<Integer, ItemData> data = new HashMap<Integer, ItemData>();

                for (Map.Entry<Integer, ItemStack> itemEntry : entry.getValue().entrySet()) {
                    if (itemEntry == null || itemEntry.getValue() == null) {
                        continue;
                    }

                    ItemData itemData = ItemData.fromItemStack(itemEntry.getValue());
                    if (itemData != null) {
                        data.put(itemEntry.getKey(), itemData);
                    }
                }

                this.items.put(entry.getKey(), data);
            }
        }

        if (info.getEffects() != null) {
            for (PotionEffect effect : info.getEffects()) {
                PotionEffectData effectData = PotionEffectData.fromPotionEffect(effect);
                if (effectData != null) {
                    this.effects.add(effectData);
                }
            }
        }

        if (info.getExtra() != null) {
            for (ItemStack item : info.getExtra()) {
                ItemData itemData = ItemData.fromItemStack(item);
                if (itemData != null) {
                    this.extra.add(itemData);
                }
            }
        }
    }

    public PlayerInfo toPlayerInfo() {
        List<PotionEffect> potionEffects = new ArrayList<PotionEffect>();

        if (this.effects != null) {
            for (PotionEffectData effectData : this.effects) {
                if (effectData == null) {
                    continue;
                }

                PotionEffect effect = effectData.toPotionEffect();
                if (effect != null) {
                    potionEffects.add(effect);
                }
            }
        }

        Location bukkitLocation = this.location != null ? this.location.toLocation() : null;
        PlayerInfo info = new PlayerInfo(potionEffects, this.health, this.hunger, bukkitLocation);

        if (this.items != null) {
            for (Map.Entry<String, Map<Integer, ItemData>> entry : this.items.entrySet()) {
                if (entry == null || entry.getValue() == null) {
                    continue;
                }

                Map<Integer, ItemStack> data = new HashMap<Integer, ItemStack>();

                for (Map.Entry<Integer, ItemData> itemEntry : entry.getValue().entrySet()) {
                    Integer slot = itemEntry.getKey();
                    ItemData itemData = itemEntry.getValue();

                    if (itemData == null) {
                        Log.warn(String.format(ITEM_LOAD_FAILURE, "null"));
                        continue;
                    }

                    ItemStack item = itemData.toItemStack(false);
                    if (item == null) {
                        Log.warn(String.format(ITEM_LOAD_FAILURE, itemData.toString()));
                        continue;
                    }

                    data.put(slot, item);
                }

                info.getItems().put(entry.getKey(), data);
            }
        }

        if (this.extra != null) {
            for (ItemData itemData : this.extra) {
                if (itemData == null) {
                    continue;
                }

                ItemStack item = itemData.toItemStack(false);
                if (item != null) {
                    info.getExtra().add(item);
                }
            }
        }

        return info;
    }

    public Map<String, Map<Integer, ItemData>> getItems() {
        return this.items;
    }

    public void setItems(Map<String, Map<Integer, ItemData>> items) {
        this.items = items != null ? items : new HashMap<String, Map<Integer, ItemData>>();
    }

    public Collection<PotionEffectData> getEffects() {
        return this.effects;
    }

    public void setEffects(Collection<PotionEffectData> effects) {
        this.effects = effects != null ? effects : new ArrayList<PotionEffectData>();
    }

    public double getHealth() {
        return this.health;
    }

    public void setHealth(double health) {
        this.health = health;
    }

    public int getHunger() {
        return this.hunger;
    }

    public void setHunger(int hunger) {
        this.hunger = hunger;
    }

    public LocationData getLocation() {
        return this.location;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }

    public List<ItemData> getExtra() {
        return this.extra;
    }

    public void setExtra(List<ItemData> extra) {
        this.extra = extra != null ? extra : new ArrayList<ItemData>();
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "items=" + items +
                ", effects=" + effects +
                ", health=" + health +
                ", hunger=" + hunger +
                ", location=" + location +
                ", extra=" + extra +
                '}';
    }
}