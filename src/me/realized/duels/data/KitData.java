package me.realized.duels.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.util.Log;
import me.realized.duels.util.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KitData {

    private static final transient String SLOT_LOAD_FAILURE = "Could not load slot %s for kit %s!";
    private static final transient String ITEM_LOAD_FAILURE = "Could not load item %s for kit %s!";

    private String name;
    private ItemData displayed;
    private boolean usePermission;
    private boolean arenaSpecific;
    private Set<KitImpl.Characteristic> characteristics = new HashSet<KitImpl.Characteristic>();
    private Map<String, Map<Integer, ItemData>> items = new HashMap<String, Map<Integer, ItemData>>();

    public static KitData fromKit(KitImpl kit) {
        if (kit == null) {
            return null;
        }
        return new KitData(kit);
    }

    private KitData() {
        // serializer için
    }

    private KitData(KitImpl kit) {
        this.name = kit.getName();
        this.displayed = ItemData.fromItemStack(kit.getDisplayed());
        this.usePermission = kit.isUsePermission();
        this.arenaSpecific = kit.isArenaSpecific();

        if (kit.getCharacteristics() != null) {
            this.characteristics.addAll(kit.getCharacteristics());
        }

        if (kit.getItems() != null) {
            for (Entry<String, Map<Integer, ItemStack>> entry : kit.getItems().entrySet()) {
                if (entry == null || entry.getValue() == null) {
                    continue;
                }

                Map<Integer, ItemData> data = new HashMap<Integer, ItemData>();

                for (Entry<Integer, ItemStack> itemEntry : entry.getValue().entrySet()) {
                    if (itemEntry != null && Objects.nonNull(itemEntry.getValue())) {
                        data.put(itemEntry.getKey(), ItemData.fromItemStack(itemEntry.getValue()));
                    }
                }

                this.items.put(entry.getKey(), data);
            }
        }
    }

    public KitImpl toKit(DuelsPlugin plugin) {
        if (plugin == null) {
            return null;
        }

        ItemStack displayedItem = null;

        if (this.displayed != null) {
            displayedItem = this.displayed.toItemStack();
        }

        if (displayedItem == null) {
            displayedItem = ItemBuilder.of(Material.BARRIER)
                    .name("&cCould not load displayed item for " + this.name + "!")
                    .build();
        }

        Set<KitImpl.Characteristic> characteristicSet = this.characteristics != null
                ? this.characteristics
                : new HashSet<KitImpl.Characteristic>();

        KitImpl kit = new KitImpl(
                plugin,
                this.name,
                displayedItem,
                this.usePermission,
                this.arenaSpecific,
                characteristicSet
        );

        if (this.items != null) {
            for (Entry<String, Map<Integer, ItemData>> entry : this.items.entrySet()) {
                if (entry == null || entry.getValue() == null) {
                    continue;
                }

                Map<Integer, ItemStack> data = new HashMap<Integer, ItemStack>();

                for (Entry<Integer, ItemData> itemEntry : entry.getValue().entrySet()) {
                    Integer slot = itemEntry.getKey();
                    ItemData itemData = itemEntry.getValue();

                    if (itemData == null) {
                        Log.warn(String.format(SLOT_LOAD_FAILURE, slot, kit.getName()));
                        continue;
                    }

                    ItemStack item = itemData.toItemStack();
                    if (item == null) {
                        Log.warn(String.format(ITEM_LOAD_FAILURE, itemData, kit.getName()));
                        continue;
                    }

                    data.put(slot, item);
                }

                kit.getItems().put(entry.getKey(), data);
            }
        }

        return kit;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ItemData getDisplayed() {
        return this.displayed;
    }

    public void setDisplayed(ItemData displayed) {
        this.displayed = displayed;
    }

    public boolean isUsePermission() {
        return this.usePermission;
    }

    public void setUsePermission(boolean usePermission) {
        this.usePermission = usePermission;
    }

    public boolean isArenaSpecific() {
        return this.arenaSpecific;
    }

    public void setArenaSpecific(boolean arenaSpecific) {
        this.arenaSpecific = arenaSpecific;
    }

    public Set<KitImpl.Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    public void setCharacteristics(Set<KitImpl.Characteristic> characteristics) {
        this.characteristics = characteristics != null
                ? characteristics
                : new HashSet<KitImpl.Characteristic>();
    }

    public Map<String, Map<Integer, ItemData>> getItems() {
        return this.items;
    }

    public void setItems(Map<String, Map<Integer, ItemData>> items) {
        this.items = items != null
                ? items
                : new HashMap<String, Map<Integer, ItemData>>();
    }
}