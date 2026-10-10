package me.realized.duels.command.commands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.data.MatchData;
import me.realized.duels.data.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchHistoryMenu implements Listener {

    private static final String TITLE = "§8Geçmiş Düellolar";
    private static final int INVENTORY_SIZE = 54;
    private static final int MATCH_SLOTS = 45;

    private final DuelsPlugin plugin;
    private final Map<UUID, Integer> pages = new HashMap<UUID, Integer>();

    public MatchHistoryMenu(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        UserData userData = this.plugin.getUserManager().get(player.getUniqueId());
        List<MatchData> matches = userData != null
                ? userData.getMatchData()
                : new ArrayList<MatchData>();

        int maxPage = Math.max(0, (matches.size() - 1) / MATCH_SLOTS);

        if (page < 0) {
            page = 0;
        }

        if (page > maxPage) {
            page = maxPage;
        }

        this.pages.put(player.getUniqueId(), page);

        Inventory inventory = Bukkit.createInventory(
                null,
                INVENTORY_SIZE,
                TITLE + " §7(Sayfa " + (page + 1) + ")"
        );

        int start = page * MATCH_SLOTS;
        int end = Math.min(start + MATCH_SLOTS, matches.size());

        int slot = 0;

        // En yeni maç en üstte olsun
        for (int i = matches.size() - 1 - start; i >= matches.size() - end; i--) {
            if (i < 0 || i >= matches.size()) {
                continue;
            }

            MatchData match = matches.get(i);
            inventory.setItem(slot++, createMatchItem(player, match));
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createItem(Material.STAINED_GLASS_PANE, 7, " ", new ArrayList<String>()));
        }

        if (page > 0) {
            inventory.setItem(48, createItem(Material.ARROW, 0, "§aÖnceki Sayfa", new ArrayList<String>()));
        }

        List<String> infoLore = new ArrayList<String>();
        infoLore.add("§7Toplam maç: §f" + matches.size());
        infoLore.add("§7Galibiyet: §a" + (userData != null ? userData.getWins() : 0));
        infoLore.add("§7Mağlubiyet: §c" + (userData != null ? userData.getLosses() : 0));
        infoLore.add("§7");
        infoLore.add("§7Sayfa: §f" + (page + 1) + "§7/§f" + (maxPage + 1));
        infoLore.add("§7");
        infoLore.add("§7Bahis fiyatı maç detayında gösterilir.");

        inventory.setItem(49, createItem(Material.BOOK, 0, "§eGeçmiş Bilgisi", infoLore));

        if (page < maxPage) {
            inventory.setItem(50, createItem(Material.ARROW, 0, "§aSonraki Sayfa", new ArrayList<String>()));
        }

        inventory.setItem(53, createItem(Material.BARRIER, 0, "§cKapat", new ArrayList<String>()));

        if (matches.isEmpty()) {
            List<String> emptyLore = new ArrayList<String>();
            emptyLore.add("§7Henüz kayıtlı düello geçmişin yok.");
            inventory.setItem(22, createItem(Material.PAPER, 0, "§eGeçmiş Bulunamadı", emptyLore));
        }

        player.openInventory(inventory);
    }

    private ItemStack createMatchItem(Player viewer, MatchData match) {
        boolean won = isWinner(viewer, match);
        String opponent = getOpponentName(viewer, match);
        String result = won ? "§aKazandın" : "§cKaybettin";
        String kit = match.getKit() != null ? match.getKit() : "Yok";
        String date = formatDate(match.getTime());
        String duration = formatDuration(match.getDuration());

        Material material = won ? Material.EMERALD : Material.REDSTONE_BLOCK;

        List<String> lore = new ArrayList<String>();
        lore.add("§7Rakip: §f" + opponent);
        lore.add("§7Sonuç: " + result);
        lore.add("§7Tarih: §f" + date);
        lore.add("§7Süre: §f" + duration);
        lore.add("§7Bitiş Canı: §c" + formatHealth(match.getHealth()));
        lore.add("§7");
        lore.add("§7Kazanan: §a" + safe(match.getWinner()));
        lore.add("§7Kaybeden: §c" + safe(match.getLoser()));
        lore.add("§7");
        lore.add("§7Bahis Fiyatı: §6" + formatBet(match.getBetAmount()));

        return createItem(material, 0, result + " §7- §f" + opponent, lore);
    }

    private boolean isWinner(Player player, MatchData match) {
        return match.getWinner() != null && match.getWinner().equalsIgnoreCase(player.getName());
    }

    private String getOpponentName(Player player, MatchData match) {
        String name = player.getName();

        if (match.getWinner() != null && match.getWinner().equalsIgnoreCase(name)) {
            return safe(match.getLoser());
        }

        if (match.getLoser() != null && match.getLoser().equalsIgnoreCase(name)) {
            return safe(match.getWinner());
        }

        if (match.getWinner() != null && !match.getWinner().equalsIgnoreCase(name)) {
            return match.getWinner();
        }

        if (match.getLoser() != null && !match.getLoser().equalsIgnoreCase(name)) {
            return match.getLoser();
        }

        return "Bilinmiyor";
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return "Bilinmiyor";
        }

        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(millis));
    }

    private String formatDuration(long millis) {
        if (millis <= 0L) {
            return "Bilinmiyor";
        }

        long totalSeconds = millis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        return minutes + " dk " + seconds + " sn";
    }

    private String formatHealth(double health) {
        return String.format("%.1f", health);
    }

    private String formatBet(double amount) {
        if (amount <= 0) {
            return "Yok";
        }

        if (amount == (long) amount) {
            return String.valueOf((long) amount);
        }

        return String.format("%.2f", amount);
    }

    private String safe(String value) {
        return value != null ? value : "Bilinmiyor";
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getView() == null || event.getView().getTitle() == null) {
            return;
        }

        if (!ChatColor.stripColor(event.getView().getTitle()).startsWith("Geçmiş Düellolar")) {
            return;
        }

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(item.getItemMeta().getDisplayName())
                : "";

        int page = this.pages.containsKey(player.getUniqueId()) ? this.pages.get(player.getUniqueId()) : 0;

        if (displayName.equalsIgnoreCase("Önceki Sayfa")) {
            this.open(player, page - 1);
            return;
        }

        if (displayName.equalsIgnoreCase("Sonraki Sayfa")) {
            this.open(player, page + 1);
            return;
        }

        if (displayName.equalsIgnoreCase("Kapat")) {
            player.closeInventory();
        }
    }

    private ItemStack createItem(Material material, int data, String name, List<String> lore) {
        ItemStack item = new ItemStack(material, 1, (short) data);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}