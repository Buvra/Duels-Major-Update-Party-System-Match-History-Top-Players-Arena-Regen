package me.realized.duels.command.commands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.data.MatchData;
import me.realized.duels.data.UserData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class TopDuelistsMenu implements Listener {

    private static final String TITLE = "§8En İyi Düellocular";
    private static final int INVENTORY_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final long CACHE_DURATION = 15000L;

    private final DuelsPlugin plugin;

    private final Map<UUID, Integer> pages = new HashMap<UUID, Integer>();

    private volatile List<TopPlayer> cachedTopPlayers = new ArrayList<TopPlayer>();
    private volatile long lastCacheUpdate = 0L;
    private final AtomicBoolean updating = new AtomicBoolean(false);

    public TopDuelistsMenu(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        if (isCacheValid()) {
            openWithCache(player, page);
            return;
        }

        openLoadingInventory(player);

        if (this.updating.compareAndSet(false, true)) {
            final Player viewer = player;
            final int targetPage = page;

            this.plugin.doAsync(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<UserData> snapshot = plugin.getUserManager().getUsersSnapshot();
                        List<TopPlayer> computed = new ArrayList<TopPlayer>();

                        for (UserData userData : snapshot) {
                            if (userData == null) {
                                continue;
                            }

                            String name = userData.getName();
                            UUID uuid = userData.getUuid();

                            if (name == null || name.trim().isEmpty() || uuid == null) {
                                continue;
                            }

                            double totalWonMoney = calculateTotalWonMoney(userData);

                            computed.add(new TopPlayer(
                                    uuid,
                                    name,
                                    userData.getWins(),
                                    userData.getLosses(),
                                    totalWonMoney
                            ));
                        }

                        Collections.sort(computed, new Comparator<TopPlayer>() {
                            @Override
                            public int compare(TopPlayer o1, TopPlayer o2) {
                                int winCompare = Integer.compare(o2.getWins(), o1.getWins());
                                if (winCompare != 0) {
                                    return winCompare;
                                }

                                int lossCompare = Integer.compare(o1.getLosses(), o2.getLosses());
                                if (lossCompare != 0) {
                                    return lossCompare;
                                }

                                return o1.getName().compareToIgnoreCase(o2.getName());
                            }
                        });

                        cachedTopPlayers = computed;
                        lastCacheUpdate = System.currentTimeMillis();

                        plugin.doSync(new Runnable() {
                            @Override
                            public void run() {
                                if (viewer.isOnline()) {
                                    openWithCache(viewer, targetPage);
                                }
                            }
                        });
                    } finally {
                        updating.set(false);
                    }
                }
            });
        }
    }

    private void openWithCache(Player player, int page) {
        List<TopPlayer> list = this.cachedTopPlayers != null
                ? this.cachedTopPlayers
                : new ArrayList<TopPlayer>();

        int maxPage = Math.max(0, (list.size() - 1) / PAGE_SIZE);

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

        fillBottomBar(inventory);

        if (list.isEmpty()) {
            List<String> lore = new ArrayList<String>();
            lore.add("§7Henüz sıralanacak oyuncu verisi yok.");

            inventory.setItem(22, createItem(Material.PAPER, 0, "§eVeri Bulunamadı", lore));
            inventory.setItem(49, createInfoItem(0, 0, page, maxPage));
            inventory.setItem(53, createItem(Material.BARRIER, 0, "§cKapat", new ArrayList<String>()));
            player.openInventory(inventory);
            return;
        }

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, list.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            TopPlayer topPlayer = list.get(i);
            int rank = i + 1;

            inventory.setItem(slot, createPlayerHead(topPlayer, rank));
            slot++;
        }

        if (page > 0) {
            inventory.setItem(48, createItem(Material.ARROW, 0, "§aÖnceki Sayfa", new ArrayList<String>()));
        }

        inventory.setItem(49, createInfoItem(list.size(), end - start, page, maxPage));

        if (page < maxPage) {
            inventory.setItem(50, createItem(Material.ARROW, 0, "§aSonraki Sayfa", new ArrayList<String>()));
        }

        inventory.setItem(53, createItem(Material.BARRIER, 0, "§cKapat", new ArrayList<String>()));

        player.openInventory(inventory);
    }

    private void openLoadingInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, TITLE);

        fillBottomBar(inventory);

        List<String> lore = new ArrayList<String>();
        lore.add("§7Oyuncular async olarak sıralanıyor.");
        lore.add("§7Lütfen kısa bir an bekle.");

        inventory.setItem(22, createItem(Material.WATCH, 0, "§eYükleniyor...", lore));
        inventory.setItem(53, createItem(Material.BARRIER, 0, "§cKapat", new ArrayList<String>()));

        player.openInventory(inventory);
    }

    private ItemStack createInfoItem(int totalPlayers, int pageCount, int page, int maxPage) {
        List<String> lore = new ArrayList<String>();
        lore.add("§7Sıralama: §eEn çok düello kazananlar");
        lore.add("§7Toplam oyuncu: §f" + totalPlayers);
        lore.add("§7Bu sayfadaki oyuncu: §f" + pageCount);
        lore.add("§7");
        lore.add("§7Sayfa: §f" + (page + 1) + "§7/§f" + (maxPage + 1));
        lore.add("§7");
        return createItem(Material.BOOK, 0, "§eSıralama Bilgisi", lore);
    }

    private ItemStack createPlayerHead(TopPlayer topPlayer, int rank) {
        ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        if (meta != null) {
            String name = topPlayer.getName();
            UUID uuid = topPlayer.getUuid();

            if (name != null && !name.trim().isEmpty()) {
                meta.setOwner(name);
            } else if (uuid != null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (offlinePlayer != null && offlinePlayer.getName() != null) {
                    name = offlinePlayer.getName();
                    meta.setOwner(name);
                }
            }

            if (name == null || name.trim().isEmpty()) {
                name = "Bilinmiyor";
            }

            meta.setDisplayName(getRankColor(rank) + "#" + rank + " §f" + name);

            List<String> lore = new ArrayList<String>();
            lore.add("§7Oyuncu: §f" + name);
            lore.add("§7Galibiyet: §a" + topPlayer.getWins());
            lore.add("§7Mağlubiyet: §c" + topPlayer.getLosses());
            lore.add("§7Toplam Düello: §f" + (topPlayer.getWins() + topPlayer.getLosses()));
            lore.add("§7Toplam Kazanılan Para: §6" + formatMoney(topPlayer.getTotalWonMoney()));
            lore.add("§7");
            lore.add("§7Sıra: " + getRankColor(rank) + "#" + rank);

            double kd = topPlayer.getLosses() <= 0
                    ? topPlayer.getWins()
                    : (double) topPlayer.getWins() / (double) topPlayer.getLosses();

            lore.add("§7K/D: §e" + String.format("%.2f", kd));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private double calculateTotalWonMoney(UserData userData) {
        if (userData == null || userData.getName() == null) {
            return 0D;
        }

        double total = 0D;
        String name = userData.getName();

        for (MatchData match : userData.getMatchData()) {
            if (match == null) {
                continue;
            }

            if (match.getWinner() != null
                    && match.getWinner().equalsIgnoreCase(name)
                    && match.getBetAmount() > 0) {
                total += (match.getBetAmount() * 2D);
            }
        }

        return total;
    }

    private String formatMoney(double amount) {
        if (amount <= 0) {
            return "0";
        }

        if (amount == (long) amount) {
            return String.valueOf((long) amount);
        }

        return String.format("%.2f", amount);
    }

    private String getRankColor(int rank) {
        if (rank == 1) {
            return "§6";
        }

        if (rank == 2) {
            return "§7";
        }

        if (rank == 3) {
            return "§c";
        }

        return "§e";
    }

    private void fillBottomBar(Inventory inventory) {
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createItem(Material.STAINED_GLASS_PANE, 7, " ", new ArrayList<String>()));
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

    private boolean isCacheValid() {
        return this.cachedTopPlayers != null
                && System.currentTimeMillis() - this.lastCacheUpdate <= CACHE_DURATION;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (event.getView() == null || event.getView().getTitle() == null) {
            return;
        }

        if (!ChatColor.stripColor(event.getView().getTitle()).startsWith("En İyi Düellocular")) {
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
            open(player, page - 1);
            return;
        }

        if (displayName.equalsIgnoreCase("Sonraki Sayfa")) {
            open(player, page + 1);
            return;
        }

        if (displayName.equalsIgnoreCase("Kapat")) {
            player.closeInventory();
        }
    }

    private static final class TopPlayer {

        private final UUID uuid;
        private final String name;
        private final int wins;
        private final int losses;
        private final double totalWonMoney;

        private TopPlayer(UUID uuid, String name, int wins, int losses, double totalWonMoney) {
            this.uuid = uuid;
            this.name = name;
            this.wins = wins;
            this.losses = losses;
            this.totalWonMoney = totalWonMoney;
        }

        public UUID getUuid() {
            return this.uuid;
        }

        public String getName() {
            return this.name;
        }

        public int getWins() {
            return this.wins;
        }

        public int getLosses() {
            return this.losses;
        }

        public double getTotalWonMoney() {
            return this.totalWonMoney;
        }
    }
}
