package me.realized.duels.arena.regen;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.Permissions;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.command.BaseCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /arenaregen ve /ar komutu.
 */
public class ArenaRegenCommand extends BaseCommand {

    private final ArenaRegenManager manager;

    public ArenaRegenCommand(final DuelsPlugin plugin) {
        super(plugin, "arenaregen", null, null, Permissions.ADMIN, 0, false, "ar");
        this.manager = plugin.getArenaRegenManager();
    }

    private ArenaRegenManager getManager() {
        if (this.manager != null) {
            return this.manager;
        }
        return plugin.getArenaRegenManager();
    }

    @Override
    protected boolean executeFirst(final CommandSender sender, final String label, final String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        final String sub = args[0].toLowerCase();
        switch (sub) {
            case "create":
            case "olustur":
            case "kur":
                return handleCreate(sender, args);
            case "delete":
            case "sil":
            case "kaldir":
                return handleDelete(sender, args);
            case "reset":
            case "sifirla":
            case "yenile":
                return handleReset(sender, args);
            case "list":
            case "liste":
                return handleList(sender);
            default:
                sendHelp(sender, label);
                return true;
        }
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {}

    private void sendHelp(final CommandSender sender, final String label) {
        final ArenaRegenManager mgr = getManager();
        final String toolName = (mgr != null && mgr.getConfig() != null && mgr.getConfig().getSelectingTool() != null)
                ? mgr.getConfig().getSelectingTool().name()
                : "IRON_HOE";

        sender.sendMessage("§6§l===== ARENA REGEN (SIFIRLAMA) KOMUTLARI =====");
        sender.sendMessage("§e/" + label + " create <arena> §7- Seçili alanda arena için sıfırlama bölgesi oluşturur.");
        sender.sendMessage("§e/" + label + " delete <arena> §7- Belirtilen arenanın sıfırlama bölgesini siler.");
        sender.sendMessage("§e/" + label + " reset <arena> §7- Arenayı anında 0 lag ile sıfırlar.");
        sender.sendMessage("§e/" + label + " list §7- Mevcut tüm sıfırlama bölgelerini listeler.");
        sender.sendMessage("§7Not: Seçim yapmak için elinize §f" + toolName + " §7alın.");
        sender.sendMessage("§6§l==============================================");
    }

    private boolean handleCreate(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cBu komut yalnızca oyuncular tarafından kullanılabilir!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage("§cKullanım: /ar create <arena>");
            return true;
        }

        final Player player = (Player) sender;
        final String arenaName = args[1];
        final ArenaImpl arena = (ArenaImpl) plugin.getArenaManager().get(arenaName);

        if (arena == null) {
            sender.sendMessage("§c'" + arenaName + "' adında bir arena bulunamadı!");
            return true;
        }

        final ArenaRegenManager mgr = getManager();
        if (mgr == null) {
            sender.sendMessage("§cArenaRegen sistemi henüz yüklenmedi!");
            return true;
        }

        final Selection selection = mgr.getSelection(player);
        if (!selection.isSelected()) {
            final String tool = (mgr.getConfig() != null && mgr.getConfig().getSelectingTool() != null) ? mgr.getConfig().getSelectingTool().name() : "IRON_HOE";
            sender.sendMessage("§cÖnce " + tool + " ile iki köşe noktasını seçmelisiniz!");
            return true;
        }

        if (mgr.getZone(arenaName) != null) {
            sender.sendMessage("§c'" + arenaName + "' arenasının zaten bir sıfırlama bölgesi var! Silmek için: /ar delete " + arenaName);
            return true;
        }

        final boolean success = mgr.createZone(arena, selection);
        if (success) {
            final ResetZone zone = mgr.getZone(arenaName);
            final int count = zone != null ? zone.getTotalOriginalBlocks() : 0;
            sender.sendMessage("§a[ArenaRegen] '" + arenaName + "' için sıfırlama bölgesi başarıyla oluşturuldu! (" + count + " blok tarandı)");
        } else {
            sender.sendMessage("§cBölge oluşturulurken bir hata oluştu!");
        }
        return true;
    }

    private boolean handleDelete(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cKullanım: /ar delete <arena>");
            return true;
        }

        final ArenaRegenManager mgr = getManager();
        if (mgr == null) {
            sender.sendMessage("§cArenaRegen sistemi henüz yüklenmedi!");
            return true;
        }

        final String arenaName = args[1];
        final boolean success = mgr.deleteZone(arenaName);
        if (success) {
            sender.sendMessage("§a[ArenaRegen] '" + arenaName + "' sıfırlama bölgesi başarıyla silindi.");
        } else {
            sender.sendMessage("§c'" + arenaName + "' arenasına ait bir sıfırlama bölgesi bulunamadı!");
        }
        return true;
    }

    private boolean handleReset(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cKullanım: /ar reset <arena>");
            return true;
        }

        final ArenaRegenManager mgr = getManager();
        if (mgr == null) {
            sender.sendMessage("§cArenaRegen sistemi henüz yüklenmedi!");
            return true;
        }

        final String arenaName = args[1];
        final ResetZone zone = mgr.getZone(arenaName);
        if (zone == null) {
            sender.sendMessage("§c'" + arenaName + "' arenasına ait bir sıfırlama bölgesi bulunamadı!");
            return true;
        }

        sender.sendMessage("§e[ArenaRegen] '" + arenaName + "' arenası sıfırlanıyor (0 lag)...");
        zone.reset(() -> sender.sendMessage("§a[ArenaRegen] '" + arenaName + "' arenası başarıyla sıfırlandı!"));
        return true;
    }

    private boolean handleList(final CommandSender sender) {
        final ArenaRegenManager mgr = getManager();
        if (mgr == null || mgr.getZones().isEmpty()) {
            sender.sendMessage("§cAktif bir sıfırlama bölgesi bulunmuyor.");
            return true;
        }

        sender.sendMessage("§6§l===== ARENA SIFIRLAMA BÖLGELERİ =====");
        for (ResetZone zone : mgr.getZones()) {
            sender.sendMessage("§8▪ §b" + zone.getName() + " §7(Dünya: §f" + zone.getWorldName() + "§7, Blok: §a" + zone.getTotalOriginalBlocks() + "§7)");
        }
        sender.sendMessage("§6§l=====================================");
        return true;
    }
}
