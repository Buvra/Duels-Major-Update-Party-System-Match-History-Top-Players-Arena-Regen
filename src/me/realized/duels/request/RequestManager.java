package me.realized.duels.request;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.request.RequestSendEvent;
import me.realized.duels.config.Config;
import me.realized.duels.config.Lang;
import me.realized.duels.setting.Settings;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.TextBuilder;
import me.realized.duels.util.compat.Titles;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class RequestManager implements Loadable, Listener {

    private final Config config;
    private final Lang lang;
    private final Map<UUID, Map<UUID, RequestImpl>> requests = new HashMap<>();
    private final Map<UUID, UUID> latestRequest = new java.util.concurrent.ConcurrentHashMap<>();

    public RequestManager(final DuelsPlugin plugin) {
        this.config = plugin.getConfiguration();
        this.lang = plugin.getLang();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void handleLoad() {}

    @Override
    public void handleUnload() {
        requests.clear();
        latestRequest.clear();
    }

    private Map<UUID, RequestImpl> get(final Player player, final boolean create) {
        Map<UUID, RequestImpl> cached = requests.get(player.getUniqueId());

        if (cached == null && create) {
            requests.put(player.getUniqueId(), cached = new HashMap<>());
            return cached;
        }

        return cached;
    }

    public void send(final Player sender, final Player target, final Settings settings) {
        final RequestImpl request = new RequestImpl(sender, target, settings);
        final RequestSendEvent event = new RequestSendEvent(sender, target, request);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return;
        }

        get(sender, true).put(target.getUniqueId(), request);
        latestRequest.put(target.getUniqueId(), sender.getUniqueId());
        final String kit = settings.getKit() != null ? settings.getKit().getName() : lang.getMessage("GENERAL.not-selected");
        final String ownInventory = settings.isOwnInventory() ? lang.getMessage("GENERAL.enabled") : lang.getMessage("GENERAL.disabled");
        final String arena = settings.getArena() != null ? settings.getArena().getName() : lang.getMessage("GENERAL.random");
        final int betAmount = settings.getBet();
        final String itemBetting = settings.isItemBetting() ? lang.getMessage("GENERAL.enabled") : lang.getMessage("GENERAL.disabled");

        final String modeStr;
        final String warningStr;
        if (settings.isOwnInventory()) {
            modeStr = "§c§l🎒 Kendi Eşyalarınla §4(EŞYA KAYBI VAR!)";
            warningStr = "§4§l⚠ DİKKAT: §cBu düelloda kaybedenin üzerindeki tüm eşyalar rakibe geçecektir!";
        } else {
            modeStr = "§b§l⚔ Kitli Düello §7(" + kit + ") §a(Güvenli - Eşya Kaybı Yok)";
            warningStr = "§a§l✔ GÜVENLİ: §7Kendi eşyalarınız kaybolmaz, kit eşyalarıyla savaşırsınız.";
        }

        lang.sendMessage(sender, "COMMAND.duel.request.send.sender",
            "name", target.getName(), "kit", kit, "own_inventory", ownInventory, "mode", modeStr, "warning", warningStr, "arena", arena, "bet_amount", betAmount, "item_betting", itemBetting);
        lang.sendMessage(target, "COMMAND.duel.request.send.receiver",
            "name", sender.getName(), "kit", kit, "own_inventory", ownInventory, "mode", modeStr, "warning", warningStr, "arena", arena, "bet_amount", betAmount, "item_betting", itemBetting);

        if (settings.isOwnInventory()) {
            target.sendMessage("§4§l[LDuels] §c§l⚠ DİKKAT: §e" + sender.getName() + " §csizinle §ckendi eşyalarınızla §cdüello yapmak istiyor! Kaybederseniz TÜM EŞYALARINIZ rakibe geçecektir!");
            try {
                Titles.send(target, "§c§lDÜELLO İSTEĞİ!", "§cKendi Eşyalarınla (EŞYA KAYBI VAR)", 10, 40, 10);
            } catch (Throwable ignored) {}
        } else {
            try {
                Titles.send(target, "§b§lDÜELLO İSTEĞİ!", "§aKitli Düello (" + kit + ") - Güvenli", 10, 40, 10);
            } catch (Throwable ignored) {}
        }

        final String path = "COMMAND.duel.request.send.clickable-text.";

        TextBuilder
            .of(lang.getMessage(path + "info.text"), null, null, Action.SHOW_TEXT, lang.getMessage(path + "info.hover-text"))
            .add(lang.getMessage(path + "accept.text"),
                ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender.getName(),
                Action.SHOW_TEXT, lang.getMessage(path + "accept.hover-text"))
            .add(lang.getMessage(path + "deny.text"),
                ClickEvent.Action.RUN_COMMAND, "/duel deny " + sender.getName(),
                Action.SHOW_TEXT, lang.getMessage(path + "deny.hover-text"))
            .send(target);
        TextBuilder.of(lang.getMessage(path + "extra.text"), null, null, Action.SHOW_TEXT, lang.getMessage(path + "extra.hover-text")).send(target);
    }

    public RequestImpl get(final Player sender, final Player target) {
        final Map<UUID, RequestImpl> cached = get(sender, false);

        if (cached == null) {
            return null;
        }

        final RequestImpl request = cached.get(target.getUniqueId());

        if (request == null) {
            return null;
        }

        if (System.currentTimeMillis() - request.getCreation() >= config.getExpiration() * 1000L) {
            cached.remove(target.getUniqueId());
            return null;
        }

        return request;
    }

    public boolean has(final Player sender, final Player target) {
        return get(sender, target) != null;
    }

    public RequestImpl remove(final Player sender, final Player target) {
        latestRequest.remove(target.getUniqueId());
        final Map<UUID, RequestImpl> cached = get(sender, false);

        if (cached == null) {
            return null;
        }

        final RequestImpl request = cached.remove(target.getUniqueId());

        if (request == null) {
            return null;
        }

        if (System.currentTimeMillis() - request.getCreation() >= config.getExpiration() * 1000L) {
            cached.remove(target.getUniqueId());
            return null;
        }

        return request;
    }

    /**
     * O(1) hızında hedefe gelen en son düello isteğinin sahibini bulur.
     */
    public Player getLatestSender(final Player target) {
        final UUID senderId = latestRequest.get(target.getUniqueId());
        if (senderId != null) {
            final Player sender = Bukkit.getPlayer(senderId);
            if (sender != null && sender.isOnline() && get(sender, target) != null) {
                return sender;
            }
            latestRequest.remove(target.getUniqueId());
        }

        for (final Map.Entry<UUID, Map<UUID, RequestImpl>> entry : requests.entrySet()) {
            final RequestImpl req = entry.getValue().get(target.getUniqueId());
            if (req != null && System.currentTimeMillis() - req.getCreation() < config.getExpiration() * 1000L) {
                final Player sender = Bukkit.getPlayer(entry.getKey());
                if (sender != null && sender.isOnline()) {
                    latestRequest.put(target.getUniqueId(), sender.getUniqueId());
                    return sender;
                }
            }
        }
        return null;
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        requests.remove(event.getPlayer().getUniqueId());
        latestRequest.remove(event.getPlayer().getUniqueId());
    }
}
