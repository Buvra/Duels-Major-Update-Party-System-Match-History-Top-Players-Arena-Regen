package me.realized.duels.layout;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.kit.Kit;
import me.realized.duels.layout.gui.KitLayoutEditorHolder;
import me.realized.duels.layout.gui.KitLayoutSelectGUI;
import me.realized.duels.layout.gui.KitLayoutSelectHolder;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class KitLayoutEditorListener implements Listener {

    private final DuelsPlugin plugin;
    private final KitLayoutManager layoutManager;

    public KitLayoutEditorListener(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.layoutManager = plugin.getKitLayoutManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        final Inventory inv = event.getInventory();
        if (inv == null) {
            return;
        }

        // 1. Kit Seçim Menüsü Tıklamaları
        if (inv.getHolder() instanceof KitLayoutSelectHolder) {
            event.setCancelled(true);
            final int slot = event.getRawSlot();

            if (slot == 45) {
                player.closeInventory();
                return;
            }

            final ItemStack clicked = event.getCurrentItem();
            if (clicked == null || !clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) {
                return;
            }

            final String displayName = clicked.getItemMeta().getDisplayName();
            if (!displayName.startsWith("§b§l⚔ ")) {
                return;
            }

            final String kitName = ChatColor.stripColor(displayName.replace("§b§l⚔ ", "").trim());
            final Kit kit = plugin.getKitManager().get(kitName);
            if (kit == null) {
                return;
            }

            // Sağ Tık: Varsayılana Sıfırla
            if (event.getClick() == ClickType.RIGHT) {
                if (layoutManager.hasLayout(player.getUniqueId(), kit.getName())) {
                    layoutManager.removeLayout(player.getUniqueId(), kit.getName());
                    player.sendMessage("§a[Düello] §e" + kit.getName() + " §akiti için özel düzeniniz sıfırlandı, varsayılana dönüldü.");
                    try {
                        player.playSound(player.getLocation(), Sound.NOTE_PLING, 1.0f, 1.5f);
                    } catch (Throwable ignored) {}
                    KitLayoutSelectGUI.open(player, plugin);
                } else {
                    player.sendMessage("§cBu kit için zaten kayıtlı özel bir düzen bulunmuyor.");
                }
                return;
            }

            // Sol Tık: Düzenleme Ekranını Aç
            player.closeInventory();
            layoutManager.openEditor(player, kit);
            return;
        }

        // 2. Kit Düzenleme Ekranı Tıklamaları (In-GUI Düzenleyici)
        if (inv.getHolder() instanceof KitLayoutEditorHolder) {
            final KitLayoutEditorHolder holder = (KitLayoutEditorHolder) inv.getHolder();
            final String kitName = holder.getKitName();
            final int rawSlot = event.getRawSlot();

            // 1. Ekran dışına tıklama veya geçersiz slot kontrolü
            if (rawSlot < 0) {
                event.setCancelled(true);
                return;
            }

            // 2. Alt Envanter (Oyuncunun kendi lobi envanteri) - KESİNLİKLE KİLİTLİ
            if (rawSlot >= 54) {
                event.setCancelled(true);
                return;
            }

            // 3. Shift-Click ile eşyaların alt envantere sızmasını önle
            if (event.isShiftClick()) {
                event.setCancelled(true);
                return;
            }

            // 4. NUMBER_KEY ve HOTBAR SWAP engeli (1-9 tuşlarına basarak eşyayı envantere çekme açığını kapat)
            if (event.getClick() == ClickType.NUMBER_KEY ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_SWAP ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.HOTBAR_MOVE_AND_READD) {
                event.setCancelled(true);
                return;
            }

            // 5. DROP engeli (Q veya Ctrl+Q ile sandıktan yere eşya düşürme açığını kapat)
            if (event.getClick() == ClickType.DROP ||
                event.getClick() == ClickType.CONTROL_DROP ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.DROP_ALL_CURSOR ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.DROP_ALL_SLOT ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.DROP_ONE_CURSOR ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.DROP_ONE_SLOT) {
                event.setCancelled(true);
                return;
            }

            // 6. DOUBLE CLICK / COLLECT_TO_CURSOR engeli (tüm eşyaları imlece toplayıp dışarı çıkarma)
            if (event.getClick() == ClickType.DOUBLE_CLICK ||
                event.getAction() == org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
                return;
            }

            // Slot 0: Kaydet & Çık
            if (rawSlot == 0) {
                event.setCancelled(true);
                layoutManager.saveFromGui(player, inv, kitName);
                return;
            }

            // Slot 1: İptal Et
            if (rawSlot == 1) {
                event.setCancelled(true);
                layoutManager.removeEditing(player.getUniqueId());
                player.setItemOnCursor(null);
                player.closeInventory();
                player.sendMessage("§c[Düello] Kit düzenleme iptal edildi.");
                return;
            }

            // Slot 8: Varsayılana Sıfırla
            if (rawSlot == 8) {
                event.setCancelled(true);
                final Kit kit = plugin.getKitManager().get(kitName);
                if (kit != null) {
                    layoutManager.resetGuiToDefault(player, inv, kit);
                }
                return;
            }

            // Slot 2, 7 veya 36..44: Bölücü ve Dekoratif Camlar
            if (rawSlot == 2 || rawSlot == 7 || (rawSlot >= 36 && rawSlot <= 44)) {
                event.setCancelled(true);
                return;
            }

            // Diğer slotlar (3..6 zırh, 9..35 ana çanta, 45..53 hotbar 1-9): Sadece menü içinde serbestçe taşınabilir!
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(final org.bukkit.event.inventory.InventoryDragEvent event) {
        final Inventory inv = event.getInventory();
        if (inv != null && inv.getHolder() instanceof KitLayoutEditorHolder) {
            for (final int rawSlot : event.getRawSlots()) {
                // Alt envantere veya buton/cam slotlarına sürükleme kesinlikle yasak
                if (rawSlot >= 54 || rawSlot < 0 || rawSlot == 0 || rawSlot == 1 || rawSlot == 2 || rawSlot == 7 || rawSlot == 8 || (rawSlot >= 36 && rawSlot <= 44)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClose(final InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getPlayer();
        final Inventory inv = event.getInventory();

        if (inv != null && inv.getHolder() instanceof KitLayoutEditorHolder) {
            final KitLayoutEditorHolder holder = (KitLayoutEditorHolder) inv.getHolder();
            if (layoutManager.isEditing(player.getUniqueId())) {
                // Pencereyi kapatınca otomatik olarak kaydet
                layoutManager.saveFromGui(player, inv, holder.getKitName());
            } else {
                player.setItemOnCursor(null);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent event) {
        if (layoutManager.isEditing(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(final PlayerPickupItemEvent event) {
        if (layoutManager.isEditing(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(final PlayerInteractEvent event) {
        if (layoutManager.isEditing(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(final PlayerCommandPreprocessEvent event) {
        if (layoutManager.isEditing(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cKit düzenleme modundayken komut kullanamazsınız! Lütfen önce menüyü kapatın.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(final org.bukkit.event.player.PlayerTeleportEvent event) {
        final Player player = event.getPlayer();
        if (layoutManager.isEditing(player.getUniqueId())) {
            player.setItemOnCursor(null);
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(final org.bukkit.event.entity.PlayerDeathEvent event) {
        final Player player = event.getEntity();
        if (layoutManager.isEditing(player.getUniqueId())) {
            player.setItemOnCursor(null);
            layoutManager.removeEditing(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        event.getPlayer().setItemOnCursor(null);
        layoutManager.unloadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        layoutManager.loadPlayerAsync(event.getPlayer().getUniqueId());
    }
}
