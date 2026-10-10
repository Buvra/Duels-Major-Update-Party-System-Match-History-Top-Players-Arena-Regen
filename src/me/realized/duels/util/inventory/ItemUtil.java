package me.realized.duels.util.inventory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

public final class ItemUtil {

    public static ItemStack itemFrom64(final String data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            try (final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                return (ItemStack) dataInput.readObject();
            }
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String itemsTo64(final ItemStack[] items) {
        if (items == null) {
            return "";
        }
        try {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
                dataOutput.writeInt(items.length);
                for (final ItemStack item : items) {
                    dataOutput.writeObject(item);
                }
            }
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException ex) {
            ex.printStackTrace();
            return "";
        }
    }

    public static ItemStack[] itemsFrom64(final String data) {
        if (data == null || data.trim().isEmpty()) {
            return new ItemStack[0];
        }
        try {
            final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            try (final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
                final int length = dataInput.readInt();
                final ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) {
                    items[i] = (ItemStack) dataInput.readObject();
                }
                return items;
            }
        } catch (ClassNotFoundException | IOException ex) {
            ex.printStackTrace();
            return new ItemStack[0];
        }
    }

    private ItemUtil() {}
}
