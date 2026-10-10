package me.realized.duels.data;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import me.realized.duels.util.collection.StreamUtil;
import me.realized.duels.util.compat.CompatUtil;
import me.realized.duels.util.compat.Identifiers;
import me.realized.duels.util.yaml.YamlUtil;
import org.bukkit.inventory.ItemStack;

public class ItemData {

    private Map<String, Object> item;

    public static ItemData fromItemStack(ItemStack item) {
        if (item == null) {
            return null;
        }
        return new ItemData(item);
    }

    @SuppressWarnings("unchecked")
    private static void patchItemFlags(Map<String, Object> item) {
        if (item == null || item.isEmpty()) {
            return;
        }

        Object meta = item.get("meta");
        if (!(meta instanceof Map)) {
            return;
        }

        Map<String, Object> metaAsMap = (Map<String, Object>) meta;
        Object itemFlags = metaAsMap.get("ItemFlags");

        if (itemFlags instanceof Iterable) {
            Set<String> flags = StreamUtil.asStream((Iterable<?>) itemFlags)
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            metaAsMap.put("ItemFlags", flags);
        }
    }

    private ItemData() {
    }

    @SuppressWarnings("unchecked")
    private ItemData(ItemStack item) {
        try {
            item = Identifiers.removeIdentifier(item);
            String dumped = YamlUtil.bukkitYamlDump(item);
            Object loaded = YamlUtil.yamlLoad(dumped);

            if (loaded instanceof Map) {
                this.item = (Map<String, Object>) loaded;
            }
        } catch (Exception ex) {
            this.item = null;
        }
    }

    public ItemStack toItemStack(boolean kitItem) {
        if (this.item == null || this.item.isEmpty()) {
            return null;
        }

        try {
            if (CompatUtil.isPre1_12()) {
                patchItemFlags(this.item);
            }

            String dumped = YamlUtil.yamlDump(this.item);
            ItemStack itemStack = (ItemStack) YamlUtil.bukkitYamlLoadAs(dumped, ItemStack.class);

            if (itemStack == null) {
                return null;
            }

            return kitItem ? Identifiers.addIdentifier(itemStack) : itemStack;
        } catch (Exception ex) {
            return null;
        }
    }

    public ItemStack toItemStack() {
        return this.toItemStack(true);
    }

    public Map<String, Object> getItem() {
        return this.item;
    }

    public void setItem(Map<String, Object> item) {
        this.item = item;
    }
}