package me.realized.duels.command.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.spectate.SpectateManager;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.arena.MatchImpl;
import me.realized.duels.util.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

public class SpectateMenu implements Listener {

    private static final String TITLE = ChatColor.DARK_GRAY + "İzlenebilir Düellolar";
    private static final String TITLE_PREFIX = ChatColor.stripColor(TITLE);
    private static final int INVENTORY_SIZE = 36;
    private static final int DUEL_SLOTS = 27;
    private static final String TARGET_LORE_PREFIX = ChatColor.DARK_GRAY + "target:";
    private static final String TARGET_UUID_LORE_PREFIX = ChatColor.DARK_GRAY + "targetuuid:";
    private static final String PREVIOUS_LABEL = ChatColor.GREEN + "Önceki Sayfa";
    private static final String NEXT_LABEL = ChatColor.GREEN + "Sonraki Sayfa";
    private static final String CLOSE_LABEL = ChatColor.RED + "Kapat";
    private static final String INFO_LABEL = ChatColor.YELLOW + "Düello Bilgisi";

    private final DuelsPlugin plugin;
    private final Map<UUID, Integer> pages = new HashMap<UUID, Integer>();
    private final Map<UUID, BukkitTask> refreshTasks = new HashMap<UUID, BukkitTask>();

    public SpectateMenu(DuelsPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        List<ArenaImpl> arenas = this.getSpectatableArenas();
        int maxPage = Math.max(0, (arenas.size() - 1) / DUEL_SLOTS);

        if (page < 0) {
            page = 0;
        }

        if (page > maxPage) {
            page = maxPage;
        }

        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                TITLE + ChatColor.GRAY + " (" + ChatColor.WHITE + "Sayfa " + (page + 1) + ChatColor.GRAY + ")");
        this.pages.put(player.getUniqueId(), page);
        this.fillInventory(inventory, page, maxPage, arenas);

        player.openInventory(inventory);
        this.startRefreshTask(player);
    }

    private void refreshOpenInventory(Player player, int page) {
        List<ArenaImpl> arenas = this.getSpectatableArenas();
        int maxPage = Math.max(0, (arenas.size() - 1) / DUEL_SLOTS);
        int targetPage = Math.max(0, Math.min(page, maxPage));

        if (targetPage != page) {
            this.open(player, targetPage);
            return;
        }

        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory == null || inventory.getSize() != INVENTORY_SIZE) {
            this.open(player, targetPage);
            return;
        }

        this.pages.put(player.getUniqueId(), targetPage);
        this.fillInventory(inventory, targetPage, maxPage, arenas);
        player.updateInventory();
    }

    private void fillInventory(Inventory inventory, int page, int maxPage, List<ArenaImpl> arenas) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setItem(i, null);
        }

        int start = page * DUEL_SLOTS;
        int end = Math.min(start + DUEL_SLOTS, arenas.size());

        for (int i = start; i < end; i++) {
            ArenaImpl arena = arenas.get(i);
            MatchImpl match = arena.getMatch();
            if (match == null) {
                continue;
            }

            List<Player> players = new ArrayList<Player>(match.getAllPlayers());
            if (players.isEmpty()) {
                continue;
            }

            Player first = players.get(0);
            Player second = players.size() > 1 ? players.get(1) : null;

            String firstName = first != null ? first.getName() : "Oyuncu-1";
            String secondName = second != null ? second.getName() : "Oyuncu-2";
            String targetName = first != null ? first.getName() : null;
            String kit = match.getKit() != null ? match.getKit().getName() : "Yok";
            int bet = match.getBet();

            List<String> lore = new ArrayList<String>();
            lore.add(ChatColor.GRAY + "Arena: " + ChatColor.WHITE + arena.getName());
            lore.add(ChatColor.GRAY + "Kit: " + ChatColor.WHITE + kit);
            lore.add(ChatColor.GRAY + "Bahis: " + ChatColor.GOLD + bet);
            lore.add("");
            lore.add(ChatColor.YELLOW + "İzlemek için tıkla");
            if (targetName != null && !targetName.isEmpty()) {
                lore.add(TARGET_LORE_PREFIX + " " + targetName);
            }
            if (first != null) {
                lore.add(TARGET_UUID_LORE_PREFIX + first.getUniqueId());
            }

            inventory.setItem(i - start, this.createItem(
                    Material.WOOL,
                    5,
                    ChatColor.YELLOW + firstName + ChatColor.GRAY + " vs " + ChatColor.YELLOW + secondName,
                    lore
            ));
        }

        for (int i = 27; i < 36; i++) {
            inventory.setItem(i, this.createItem(Material.STAINED_GLASS_PANE, 7, " ", new ArrayList<String>()));
        }

        if (page > 0) {
            inventory.setItem(30, this.createItem(Material.ARROW, 0, PREVIOUS_LABEL, new ArrayList<String>()));
        }

        List<String> infoLore = new ArrayList<String>();
        infoLore.add(ChatColor.GRAY + "Aktif düello: " + ChatColor.WHITE + arenas.size());
        infoLore.add(ChatColor.GRAY + "Sayfa: " + ChatColor.WHITE + (page + 1) + ChatColor.GRAY + "/" + ChatColor.WHITE + (maxPage + 1));
        infoLore.add("");
        infoLore.add(ChatColor.YELLOW + "Menü otomatik yenilenir");
        inventory.setItem(31, this.createItem(Material.BOOK, 0, INFO_LABEL, infoLore));

        if (page < maxPage) {
            inventory.setItem(32, this.createItem(Material.ARROW, 0, NEXT_LABEL, new ArrayList<String>()));
        }

        inventory.setItem(35, this.createItem(Material.BARRIER, 0, CLOSE_LABEL, new ArrayList<String>()));
    }

    private void startRefreshTask(final Player player) {
        this.stopRefreshTask(player);

        BukkitTask task = this.plugin.doSyncRepeat(new Runnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) {
                    SpectateMenu.this.stopRefreshTask(player);
                    return;
                }

                if (player.getOpenInventory() == null || player.getOpenInventory().getTopInventory() == null) {
                    SpectateMenu.this.stopRefreshTask(player);
                    return;
                }

                String title = player.getOpenInventory().getTitle();
                if (title == null || !ChatColor.stripColor(title).startsWith(TITLE_PREFIX)) {
                    SpectateMenu.this.stopRefreshTask(player);
                    return;
                }

                int currentPage = SpectateMenu.this.pages.containsKey(player.getUniqueId())
                        ? SpectateMenu.this.pages.get(player.getUniqueId())
                        : 0;

                SpectateMenu.this.refreshOpenInventory(player, currentPage);
            }
        }, 20L, 20L);

        this.refreshTasks.put(player.getUniqueId(), task);
    }

    private void stopRefreshTask(Player player) {
        if (player == null) {
            return;
        }

        BukkitTask task = this.refreshTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (event.getView() == null || event.getView().getTitle() == null) {
            return;
        }

        if (!ChatColor.stripColor(event.getView().getTitle()).startsWith(TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        String displayName = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(item.getItemMeta().getDisplayName())
                : "";

        int page = this.pages.containsKey(player.getUniqueId()) ? this.pages.get(player.getUniqueId()) : 0;

        if (displayName.equalsIgnoreCase(ChatColor.stripColor(PREVIOUS_LABEL))) {
            this.open(player, page - 1);
            return;
        }

        if (displayName.equalsIgnoreCase(ChatColor.stripColor(NEXT_LABEL))) {
            this.open(player, page + 1);
            return;
        }

        if (displayName.equalsIgnoreCase(ChatColor.stripColor(CLOSE_LABEL))) {
            player.closeInventory();
            return;
        }

        if (item.getType() != Material.WOOL) {
            return;
        }

        if (this.plugin.getConfiguration().isSpecRequiresClearedInventory() && InventoryUtil.hasItem(player)) {
            this.plugin.getLang().sendMessage(player, "ERROR.duel.inventory-not-empty", new Object[0]);
            player.closeInventory();
            return;
        }

        String targetName = this.extractTarget(item);
        if (targetName == null) {
            targetName = this.guessTargetFromDisplayName(item);
        }

        if (targetName == null) {
            player.sendMessage(ChatColor.RED + "Bu düello artık izlenemiyor.");
            this.open(player, page);
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "Bu oyuncu artık çevrimiçi değil.");
            this.open(player, page);
            return;
        }

        player.closeInventory();
        this.startSpectate(player, target);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        if (event.getView() == null || event.getView().getTitle() == null) {
            return;
        }

        if (!ChatColor.stripColor(event.getView().getTitle()).startsWith(TITLE_PREFIX)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        this.pages.remove(player.getUniqueId());
        this.stopRefreshTask(player);
    }

    private void startSpectate(Player player, Player target) {
        SpectateManager.Result result = this.plugin.getSpectateManager().startSpectating(player, target);

        switch (result) {
            case EVENT_CANCELLED:
                return;

            case IN_MATCH:
                this.plugin.getLang().sendMessage(player, "ERROR.duel.already-in-match.sender", new Object[0]);
                return;

            case IN_QUEUE:
                this.plugin.getLang().sendMessage(player, "ERROR.duel.already-in-queue", new Object[0]);
                return;

            case ALREADY_SPECTATING:
                this.plugin.getLang().sendMessage(player, "ERROR.spectate.already-spectating.sender", new Object[0]);
                return;

            case TARGET_NOT_IN_MATCH:
                this.plugin.getLang().sendMessage(player, "ERROR.spectate.not-in-match", new Object[]{"name", target.getName()});
                return;

            case SUCCESS:
                ArenaImpl arena = this.plugin.getArenaManager().get(target);
                if (arena == null || arena.getMatch() == null) {
                    return;
                }

                MatchImpl match = arena.getMatch();
                Player opponent = arena.getOpponent(target);
                String opponentName = opponent != null ? opponent.getName() : "-";

                this.plugin.getLang().sendMessage(player, "COMMAND.spectate.start-spectate", new Object[]{
                        "name", target.getName(),
                        "opponent", opponentName,
                        "arena", arena.getName(),
                        "bet_amount", match.getBet()
                });
                return;

            default:
                return;
        }
    }

    private List<ArenaImpl> getSpectatableArenas() {
        List<ArenaImpl> arenas = new ArrayList<ArenaImpl>();

        for (ArenaImpl arena : this.plugin.getArenaManager().getArenasImpl()) {
            if (arena == null || arena.getMatch() == null) {
                continue;
            }

            MatchImpl match = arena.getMatch();
            if (match.getAllPlayers() == null || match.getAllPlayers().isEmpty()) {
                continue;
            }

            arenas.add(arena);
        }

        arenas.sort(Comparator.comparing(ArenaImpl::getName, String.CASE_INSENSITIVE_ORDER));
        return arenas;
    }

    private String extractTarget(ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return null;
        }

        for (String line : meta.getLore()) {
            String stripped = ChatColor.stripColor(line);

            if (stripped.startsWith("targetuuid:")) {
                String uuidString = stripped.substring("targetuuid:".length()).trim();
                try {
                    UUID uuid = UUID.fromString(uuidString);
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) {
                        return player.getName();
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (stripped.startsWith("target:")) {
                return stripped.substring("target:".length()).trim();
            }
        }

        return null;
    }

    private String guessTargetFromDisplayName(ItemStack item) {
        if (!item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return null;
        }

        String stripped = ChatColor.stripColor(meta.getDisplayName());
        if (stripped == null || stripped.isEmpty()) {
            return null;
        }

        String[] parts = stripped.split("vs");
        if (parts.length == 0) {
            return null;
        }

        return parts[0].trim();
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
