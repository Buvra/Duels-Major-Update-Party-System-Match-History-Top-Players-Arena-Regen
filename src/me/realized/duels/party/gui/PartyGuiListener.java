package me.realized.duels.party.gui;

import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.party.Party;
import me.realized.duels.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class PartyGuiListener implements Listener {

    private final DuelsPlugin plugin;
    private final PartyManager partyManager;

    public PartyGuiListener(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(final InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        final InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();
        final ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        // 1. Ana Parti Menüsü
        if (holder instanceof PartyMenuHolder) {
            event.setCancelled(true);
            handleMainMenuClick(player, (PartyMenuHolder) holder, event.getRawSlot());
            return;
        }

        // 2. Düello Seçim Menüsü
        if (holder instanceof PartyDuelHolder) {
            event.setCancelled(true);
            handleDuelMenuClick(player, (PartyDuelHolder) holder, clicked, event.getRawSlot());
            return;
        }

        // 3. Üye Yönetimi Menüsü
        if (holder instanceof PartyMemberHolder) {
            event.setCancelled(true);
            handleMemberManageClick(player, (PartyMemberHolder) holder, clicked, event.getRawSlot());
            return;
        }

        // 4. Davet Menüsü
        if (holder instanceof PartyInviteHolder) {
            event.setCancelled(true);
            handleInviteMenuClick(player, (PartyInviteHolder) holder, clicked, event.getRawSlot());
            return;
        }

        // 5. Parti Kurma Menüsü (Partisi olmayanlar için)
        if (holder instanceof PartyCreateHolder) {
            event.setCancelled(true);
            handleCreateMenuClick(player, (PartyCreateHolder) holder, event.getRawSlot());
        }
    }

    private void handleMainMenuClick(final Player player, final PartyMenuHolder holder, final int slot) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.closeInventory();
            player.sendMessage("§cBir partide değilsiniz!");
            return;
        }

        final boolean isLeader = party.isLeader(player.getUniqueId());

        switch (slot) {
            case 12: // Davet Et
                if (!isLeader) {
                    player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
                    return;
                }
                if (party.isFull()) {
                    player.sendMessage("§cPartiniz maksimum 5 kişi olabilir!");
                    return;
                }
                player.closeInventory();
                partyManager.startChatInvitePrompt(player.getUniqueId());
                player.sendMessage("");
                player.sendMessage("§6§l[Parti] §eDavet etmek istediğiniz oyuncunun adını sohbete yazın:");
                player.sendMessage("§7(İşlemi iptal etmek için sohbete §c'iptal' §7yazabilirsiniz)");
                player.sendMessage("");
                break;
            case 14: // Üye Yönetimi
                if (!isLeader) {
                    player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
                    return;
                }
                PartyGUI.openMemberManageMenu(player, party);
                break;
            case 16: // Ayrıl / Dağıt
                if (partyManager.isInPartyMatch(player.getUniqueId())) {
                    player.sendMessage("§cDüello maçındayken partiden ayrılamazsınız!");
                    return;
                }
                if (isLeader) {
                    party.broadcast("§cParti, lider tarafından dağıtıldı!");
                    partyManager.disbandParty(party);
                    player.closeInventory();
                } else {
                    final String name = player.getName();
                    partyManager.removeMember(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage("§cPartiden ayrıldınız.");
                    party.broadcast("§e" + name + " §cpartiden ayrıldı.");
                }
                break;
            case 22: // Parti Düellosu Başlat
                if (!isLeader) {
                    player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
                    return;
                }
                PartyDuelGUI.openDuelMenu(player, party, partyManager);
                break;
            default:
                break;
        }
    }

    private void handleDuelMenuClick(final Player player, final PartyDuelHolder holder, final ItemStack clicked, final int slot) {
        final Party myParty = partyManager.getParty(player.getUniqueId());
        if (myParty == null) {
            player.closeInventory();
            player.sendMessage("§cBir partide değilsiniz!");
            return;
        }

        if (!myParty.isLeader(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
            return;
        }

        if (slot == 45) { // Geri Dön
            PartyGUI.openMainMenu(player, myParty);
            return;
        }

        if (slot == 53) { // Yenile
            PartyDuelGUI.openDuelMenu(player, myParty, partyManager);
            return;
        }

        if (clicked.getType() != Material.SKULL_ITEM || !clicked.hasItemMeta()) {
            return;
        }

        // Tıklanan liderin adını al
        String title = clicked.getItemMeta().getDisplayName();
        if (title == null) return;
        title = ChatColor.stripColor(title);
        final String targetLeaderName = title.replace(" Partisi", "").trim();

        final Player targetLeader = Bukkit.getPlayerExact(targetLeaderName);
        if (targetLeader == null || !targetLeader.isOnline()) {
            player.sendMessage("§cBelirtilen parti lideri artık çevrimdışı!");
            PartyDuelGUI.openDuelMenu(player, myParty, partyManager);
            return;
        }

        final Party targetParty = partyManager.getParty(targetLeader.getUniqueId());
        if (targetParty == null || !targetParty.isLeader(targetLeader.getUniqueId())) {
            player.sendMessage("§cBu parti artık mevcut değil!");
            PartyDuelGUI.openDuelMenu(player, myParty, partyManager);
            return;
        }

        final int myCount = myParty.getOnlineCount();
        final int targetCount = targetParty.getOnlineCount();

        if (myCount != targetCount) {
            player.sendMessage("§cParti düellosu için her iki tarafın oyuncu sayısı eşit olmalıdır! (Örn: Karşı taraf " + targetCount + " kişiyse siz de " + targetCount + " kişi olmalısınız)");
            return;
        }

        if (myCount < 2 || myCount > 5) {
            player.sendMessage("§cParti düellosu yapabilmek için her iki partide 2 ila 5 kişi olmalıdır!");
            return;
        }

        if (partyManager.isInPartyMatch(player.getUniqueId()) || partyManager.isInPartyMatch(targetLeader.getUniqueId())) {
            player.sendMessage("§cPartilerden biri zaten bir maçta bulunuyor!");
            return;
        }

        partyManager.sendDuelRequest(player.getUniqueId(), targetLeader.getUniqueId(), null);
        player.closeInventory();

        player.sendMessage("§a" + targetLeader.getName() + " partisine " + myCount + "v" + myCount + " düello isteği gönderildi!");
        myParty.broadcast("§eLideriniz §b" + targetLeader.getName() + " §epartisine " + myCount + "v" + myCount + " düello teklifi gönderdi.");

        targetLeader.sendMessage("§6§l[DÜELLO] §e" + player.getName() + " §apartisi (" + myCount + "v" + myCount + ") partinize meydan okudu!");
        targetLeader.sendMessage("§aKabul etmek için: §e/duello kabul " + player.getName() + " §aveya §e/duello parti kabul");
        targetParty.broadcast("§e" + player.getName() + " partisi partinize " + myCount + "v" + myCount + " düello teklif etti!");
    }

    private void handleMemberManageClick(final Player player, final PartyMemberHolder holder, final ItemStack clicked, final int slot) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null || !party.isLeader(player.getUniqueId())) {
            player.closeInventory();
            player.sendMessage("§cBu işlemi yalnızca parti lideri yapabilir!");
            return;
        }

        if (slot == 18) {
            PartyGUI.openMainMenu(player, party);
            return;
        }

        if (clicked.getType() != Material.SKULL_ITEM || !clicked.hasItemMeta()) {
            return;
        }

        String targetName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (targetName == null) return;
        targetName = targetName.trim();

        UUID targetId = null;
        for (UUID uid : party.getMembers()) {
            final Player p = Bukkit.getPlayer(uid);
            final String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uid).getName();
            if (targetName.equalsIgnoreCase(name)) {
                targetId = uid;
                break;
            }
        }

        if (targetId == null || !party.contains(targetId)) {
            player.sendMessage("§cBu oyuncu partinizde değil!");
            return;
        }

        partyManager.removeMember(targetId);
        party.broadcast("§e" + targetName + " §cpartiden atıldı.");

        final Player target = Bukkit.getPlayer(targetId);
        if (target != null && target.isOnline()) {
            target.sendMessage("§cPartiden atıldınız!");
        }

        PartyGUI.openMemberManageMenu(player, party);
    }

    private void handleInviteMenuClick(final Player player, final PartyInviteHolder holder, final ItemStack clicked, final int slot) {
        final Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            PartyGUI.openMainMenu(player, party);
            return;
        }

        if (clicked.getType() != Material.SKULL_ITEM || !clicked.hasItemMeta()) {
            return;
        }

        if (party.isFull()) {
            player.sendMessage("§cPartiniz maksimum 5 kişi olabilir!");
            return;
        }

        String targetName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        if (targetName == null) return;
        targetName = targetName.trim();

        final Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cBelirtilen oyuncu artık çevrimdışı!");
            PartyGUI.openInviteMenu(player, party, partyManager);
            return;
        }

        if (partyManager.isInParty(target.getUniqueId())) {
            player.sendMessage("§cBu oyuncu zaten başka bir partide!");
            PartyGUI.openInviteMenu(player, party, partyManager);
            return;
        }

        if (party.hasInvite(target.getUniqueId())) {
            player.sendMessage("§cBu oyuncuya zaten bekleyen bir davet gönderilmiş!");
            return;
        }

        party.addInvite(target.getUniqueId());
        party.broadcast("§e" + target.getName() + " §apartinize davet edildi.");
        target.sendMessage("§e" + player.getName() + " §asizi partisine davet etti!");
        target.sendMessage("§aKatılmak için: §e/parti katil " + player.getName());
        player.sendMessage("§a" + target.getName() + " oyuncusuna davet gönderildi!");

        PartyGUI.openInviteMenu(player, party, partyManager);
    }

    private void handleCreateMenuClick(final Player player, final PartyCreateHolder holder, final int slot) {
        if (slot == 13) {
            player.closeInventory();
            player.chat("/parti kur");
        }
    }

    /**
     * Liderin sohbete yazdığı oyuncu adını yakalayarak sıfır yük ile parti daveti gönderir.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(final org.bukkit.event.player.AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!partyManager.isWaitingChatInvite(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        partyManager.cancelChatInvitePrompt(player.getUniqueId());

        final String targetName = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> partyManager.handleChatInvite(player, targetName));
    }
}
