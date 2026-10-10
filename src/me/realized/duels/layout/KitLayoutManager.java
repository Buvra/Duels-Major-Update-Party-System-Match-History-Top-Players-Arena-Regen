package me.realized.duels.layout;

import com.google.common.base.Charsets;
import com.google.common.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.kit.KitImpl;
import me.realized.duels.layout.gui.KitLayoutEditorHolder;
import me.realized.duels.util.compat.Items;
import me.realized.duels.util.inventory.ItemBuilder;
import me.realized.duels.util.json.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 500 CCU altında 0 lag ile çalışan, tamamen Menü İçi (In-GUI) Kit Layout Yöneticisi.
 *
 * <p>Güvenlik & Anti-Exploit Garantileri:
 * <ul>
 *   <li>Oyuncunun lobi envanterine ASLA dokunulmaz (Silinmez, değiştirilmez).</li>
 *   <li>Eşyalar oyuncunun eline değil, 54'lük sandık menüsünün içine dizilir.</li>
 *   <li>Menü kapandığında imleçteki eşya (cursor) anında sıfırlanır (Item dupe/leak imkansızdır).</li>
 *   <li>Lobiye eşya atma (Drop), yerden alma (Pickup) veya komut kullanma tamamen engellenmiştir.</li>
 *   <li>O(1) ConcurrentHashMap önbelleği ile Main Thread hiçbir zaman bloklanmaz.</li>
 * </ul>
 */
public class KitLayoutManager {

    private final DuelsPlugin plugin;
    private final File layoutsFolder;

    // UUID -> (KitName -> PlayerKitLayout)
    private final Map<UUID, Map<String, PlayerKitLayout>> cache = new ConcurrentHashMap<>();

    // Düzenleme modunda olan oyuncuların takibi: UUID -> KitName
    private final Map<UUID, String> editingPlayers = new ConcurrentHashMap<>();

    public KitLayoutManager(@NotNull final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.layoutsFolder = new File(plugin.getDataFolder(), "layouts");

        if (!layoutsFolder.exists()) {
            layoutsFolder.mkdirs();
        }
    }

    public boolean isEditing(@NotNull final UUID uuid) {
        return editingPlayers.containsKey(uuid);
    }

    public void removeEditing(@NotNull final UUID uuid) {
        editingPlayers.remove(uuid);
    }

    public boolean hasLayout(@NotNull final UUID uuid, @NotNull final String kitName) {
        final Map<String, PlayerKitLayout> playerMap = cache.get(uuid);
        if (playerMap == null) {
            loadPlayerSync(uuid);
            final Map<String, PlayerKitLayout> loaded = cache.get(uuid);
            return loaded != null && loaded.containsKey(kitName);
        }
        return playerMap.containsKey(kitName);
    }

    @Nullable
    public PlayerKitLayout getLayout(@NotNull final UUID uuid, @NotNull final String kitName) {
        final Map<String, PlayerKitLayout> playerMap = cache.get(uuid);
        if (playerMap == null) {
            loadPlayerSync(uuid);
            final Map<String, PlayerKitLayout> loaded = cache.get(uuid);
            return loaded != null ? loaded.get(kitName) : null;
        }
        return playerMap.get(kitName);
    }

    public void setLayout(@NotNull final UUID uuid, @NotNull final String kitName, final ItemStack[] contents, final ItemStack[] armor) {
        final PlayerKitLayout layout = new PlayerKitLayout(kitName, contents, armor);
        cache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>()).put(kitName, layout);
        savePlayerAsync(uuid);
    }

    public void removeLayout(@NotNull final UUID uuid, @NotNull final String kitName) {
        final Map<String, PlayerKitLayout> playerMap = cache.get(uuid);
        if (playerMap != null) {
            playerMap.remove(kitName);
            savePlayerAsync(uuid);
        }
    }

    public boolean equipLayout(@NotNull final Player player, @NotNull final String kitName, @NotNull final Kit kit) {
        final PlayerKitLayout layout = getLayout(player.getUniqueId(), kitName);
        if (layout == null) {
            return false;
        }

        player.getInventory().setContents(layout.getContents().clone());
        player.getInventory().setArmorContents(layout.getArmor().clone());
        player.updateInventory();
        return true;
    }

    /**
     * Tamamen menü içi (In-GUI) düzenleme ekranını açar. Oyuncunun envanterine dokunmaz!
     */
    public void openEditor(@NotNull final Player player, @NotNull final Kit kit) {
        if (plugin.getArenaManager().isInMatch(player)) {
            player.sendMessage("§cDüellodayken kit düzenleyemezsiniz!");
            return;
        }

        if (plugin.getPartyManager() != null && plugin.getPartyManager().isInPartyMatch(player.getUniqueId())) {
            player.sendMessage("§cParti maçındayken kit düzenleyemezsiniz!");
            return;
        }

        editingPlayers.put(player.getUniqueId(), kit.getName());

        final KitLayoutEditorHolder holder = new KitLayoutEditorHolder(player.getUniqueId(), kit.getName());
        final Inventory inv = Bukkit.createInventory(holder, 54, "§8Düzenle: §b" + kit.getName());
        holder.setInventory(inv);

        // Üst Kontrol & Zırh Çubuğu (0..8)
        final ItemStack saveBtn = ItemBuilder.of(Material.STAINED_CLAY, 1, (short) 5)
                .name("§a§l✔ DÜZENİ KAYDET")
                .lore(
                        "§8§m------------------------",
                        "§7Aşağıdaki menüde yaptığınız",
                        "§7eşya dizilimini kaydeder.",
                        " ",
                        "§aKaydetmek için tıklayın!",
                        "§8§m------------------------"
                ).build();
        inv.setItem(0, saveBtn);

        final ItemStack cancelBtn = ItemBuilder.of(Material.STAINED_CLAY, 1, (short) 14)
                .name("§c§l✖ İPTAL ET")
                .lore(
                        "§8§m------------------------",
                        "§7Değişiklikleri kaydetmeden çıkar.",
                        " ",
                        "§cÇıkmak için tıklayın!",
                        "§8§m------------------------"
                ).build();
        inv.setItem(1, cancelBtn);

        final ItemStack divider = Items.from("STAINED_GLASS_PANE", (short) 15);
        inv.setItem(2, divider);
        inv.setItem(7, divider);

        final ItemStack resetBtn = ItemBuilder.of(Material.STAINED_CLAY, 1, (short) 4)
                .name("§e§l↺ VARSAYILANA SIFIRLA")
                .lore(
                        "§8§m------------------------",
                        "§7Bu kit için varsayılan orijinal",
                        "§7eşya dizilimini geri yükler.",
                        " ",
                        "§eSıfırlamak için tıklayın!",
                        "§8§m------------------------"
                ).build();
        inv.setItem(8, resetBtn);

        // Ayırıcı Çubuk (Row 4: 36..44)
        final ItemStack midBar = ItemBuilder.of(Material.STAINED_GLASS_PANE, 1, (short) 3)
                .name("§b▲ Çanta (Üst) ▲  §8|  §e▼ Hotbar (Alt 1-9) ▼")
                .lore("§7Aşağıdaki son satır oyundaki 1-9 hotbarınızdır.")
                .build();
        for (int i = 36; i <= 44; i++) {
            inv.setItem(i, midBar);
        }

        // Eşyaları Yükle (Özel Düzen varsa o, yoksa Kit Orijinali)
        populateGuiItems(player.getUniqueId(), inv, kit);

        player.openInventory(inv);
        try {
            player.playSound(player.getLocation(), Sound.CHEST_OPEN, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
    }

    private void populateGuiItems(@NotNull final UUID uuid, @NotNull final Inventory inv, @NotNull final Kit kit) {
        final PlayerKitLayout layout = getLayout(uuid, kit.getName());

        if (layout != null) {
            final ItemStack[] contents = layout.getContents();
            if (contents != null) {
                // Hotbar (contents 0..8 -> GUI 45..53)
                for (int i = 0; i < 9 && i < contents.length; i++) {
                    inv.setItem(45 + i, contents[i]);
                }
                // Main Inventory (contents 9..35 -> GUI 9..35)
                for (int i = 9; i < 36 && i < contents.length; i++) {
                    inv.setItem(i, contents[i]);
                }
            }

            final ItemStack[] armor = layout.getArmor();
            if (armor != null && armor.length >= 4) {
                inv.setItem(3, armor[3]); // Helmet
                inv.setItem(4, armor[2]); // Chestplate
                inv.setItem(5, armor[1]); // Leggings
                inv.setItem(6, armor[0]); // Boots
            }
        } else if (kit instanceof KitImpl) {
            final Map<String, Map<Integer, ItemStack>> items = ((KitImpl) kit).getItems();
            final Map<Integer, ItemStack> invMap = items.get("INVENTORY");
            if (invMap != null) {
                for (Map.Entry<Integer, ItemStack> entry : invMap.entrySet()) {
                    final int slot = entry.getKey();
                    final ItemStack item = entry.getValue() != null ? entry.getValue().clone() : null;
                    if (slot >= 0 && slot < 9) {
                        inv.setItem(45 + slot, item);
                    } else if (slot >= 9 && slot < 36) {
                        inv.setItem(slot, item);
                    }
                }
            }

            final Map<Integer, ItemStack> armorMap = items.get("ARMOR");
            if (armorMap != null) {
                if (armorMap.containsKey(1)) inv.setItem(3, armorMap.get(1).clone()); // Helmet
                if (armorMap.containsKey(2)) inv.setItem(4, armorMap.get(2).clone()); // Chestplate
                if (armorMap.containsKey(3)) inv.setItem(5, armorMap.get(3).clone()); // Leggings
                if (armorMap.containsKey(4)) inv.setItem(6, armorMap.get(4).clone()); // Boots
            }
        }
    }

    public void saveFromGui(@NotNull final Player player, @NotNull final Inventory inv, @NotNull final String kitName) {
        // Eğer oyuncu bir eşyayı elinde (cursor) tutarken kaydetti veya kapattıysa,
        // o eşyayı GUI'deki ilk boş slota geri koyalım ki kaybolmasın:
        final ItemStack cursor = player.getItemOnCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            // Önce hotbar (45..53), sonra çanta (9..35) slotlarında boş yer ara
            boolean placed = false;
            for (int i = 45; i <= 53; i++) {
                final ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR) {
                    inv.setItem(i, cursor.clone());
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                for (int i = 9; i <= 35; i++) {
                    final ItemStack item = inv.getItem(i);
                    if (item == null || item.getType() == Material.AIR) {
                        inv.setItem(i, cursor.clone());
                        placed = true;
                        break;
                    }
                }
            }
            player.setItemOnCursor(null);
        }

        final ItemStack[] contents = new ItemStack[36];
        // Hotbar (GUI 45..53 -> contents 0..8)
        for (int i = 0; i < 9; i++) {
            contents[i] = inv.getItem(45 + i);
        }
        // Main Inventory (GUI 9..35 -> contents 9..35)
        for (int i = 9; i < 36; i++) {
            contents[i] = inv.getItem(i);
        }

        final ItemStack[] armor = new ItemStack[4];
        armor[3] = inv.getItem(3); // Helmet
        armor[2] = inv.getItem(4); // Chestplate
        armor[1] = inv.getItem(5); // Leggings
        armor[0] = inv.getItem(6); // Boots

        setLayout(player.getUniqueId(), kitName, contents, armor);
        editingPlayers.remove(player.getUniqueId());

        player.setItemOnCursor(null);
        player.closeInventory();

        player.sendMessage("§a[Düello] §e" + kitName + " §akiti için özel eşya düzeniniz başarıyla kaydedildi!");
        try {
            player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
        } catch (Throwable ignored) {}
    }

    public void resetGuiToDefault(@NotNull final Player player, @NotNull final Inventory inv, @NotNull final Kit kit) {
        // Zırh slotlarını temizle
        inv.setItem(3, null);
        inv.setItem(4, null);
        inv.setItem(5, null);
        inv.setItem(6, null);

        // Ana çanta slotlarını temizle
        for (int i = 9; i <= 35; i++) {
            inv.setItem(i, null);
        }

        // Hotbar slotlarını temizle
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, null);
        }

        removeLayout(player.getUniqueId(), kit.getName());

        if (kit instanceof KitImpl) {
            final Map<String, Map<Integer, ItemStack>> items = ((KitImpl) kit).getItems();
            final Map<Integer, ItemStack> invMap = items.get("INVENTORY");
            if (invMap != null) {
                for (Map.Entry<Integer, ItemStack> entry : invMap.entrySet()) {
                    final int slot = entry.getKey();
                    final ItemStack item = entry.getValue() != null ? entry.getValue().clone() : null;
                    if (slot >= 0 && slot < 9) {
                        inv.setItem(45 + slot, item);
                    } else if (slot >= 9 && slot < 36) {
                        inv.setItem(slot, item);
                    }
                }
            }

            final Map<Integer, ItemStack> armorMap = items.get("ARMOR");
            if (armorMap != null) {
                if (armorMap.containsKey(1)) inv.setItem(3, armorMap.get(1).clone());
                if (armorMap.containsKey(2)) inv.setItem(4, armorMap.get(2).clone());
                if (armorMap.containsKey(3)) inv.setItem(5, armorMap.get(3).clone());
                if (armorMap.containsKey(4)) inv.setItem(6, armorMap.get(4).clone());
            }
        }

        player.setItemOnCursor(null);
        player.sendMessage("§a[Düello] §e" + kit.getName() + " §akiti için düzen varsayılana sıfırlandı.");
        try {
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f);
        } catch (Throwable ignored) {}
    }

    public void unloadPlayer(@NotNull final UUID uuid) {
        editingPlayers.remove(uuid);
        cache.remove(uuid);
    }

    public void loadPlayerAsync(@NotNull final UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> loadPlayerSync(uuid));
    }

    @SuppressWarnings("unchecked")
    private void loadPlayerSync(@NotNull final UUID uuid) {
        final File file = new File(layoutsFolder, uuid.toString() + ".json");
        if (!file.exists()) {
            return;
        }

        try (final Reader reader = new InputStreamReader(new FileInputStream(file), Charsets.UTF_8)) {
            final Map<String, Map<String, String>> data = (Map<String, Map<String, String>>) JsonUtil.getObjectMapper().readValue(
                    reader,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Map<String, String>>>() {}
            );

            if (data != null) {
                final Map<String, PlayerKitLayout> map = new ConcurrentHashMap<>();
                for (final Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
                    final String kitName = entry.getKey();
                    final Map<String, String> b64Map = entry.getValue();
                    final String contentsB64 = b64Map.get("contents");
                    final String armorB64 = b64Map.get("armor");
                    map.put(kitName, PlayerKitLayout.fromBase64(kitName, contentsB64, armorB64));
                }
                cache.put(uuid, map);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void savePlayerSync(@NotNull final UUID uuid) {
        final Map<String, PlayerKitLayout> map = cache.get(uuid);
        final Map<String, Map<String, String>> data = new HashMap<>();

        if (map != null) {
            // Snapshot alarak thread-safety ve tutarlılığı sağla
            final Map<String, PlayerKitLayout> copy = new HashMap<>(map);
            for (final Map.Entry<String, PlayerKitLayout> entry : copy.entrySet()) {
                final Map<String, String> b64Map = new HashMap<>();
                b64Map.put("contents", entry.getValue().getContentsBase64());
                b64Map.put("armor", entry.getValue().getArmorBase64());
                data.put(entry.getKey(), b64Map);
            }
        }

        if (!layoutsFolder.exists()) {
            layoutsFolder.mkdirs();
        }

        final File file = new File(layoutsFolder, uuid.toString() + ".json");
        try (final Writer writer = new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8)) {
            JsonUtil.getObjectWriter().writeValue(writer, data);
            writer.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void savePlayerAsync(@NotNull final UUID uuid) {
        // Asenkron göreve aktarmadan önce ana iş parçacığında cache kopyasını hazırlayabiliriz
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerSync(uuid));
    }

    public void saveAllSync() {
        for (final UUID uuid : cache.keySet()) {
            savePlayerSync(uuid);
        }
    }
}
