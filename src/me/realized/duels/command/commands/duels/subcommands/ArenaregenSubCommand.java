package me.realized.duels.command.commands.duels.subcommands;

import java.util.Arrays;
import java.util.List;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.regen.ArenaRegenManager;
import me.realized.duels.arena.regen.ResetZone;
import me.realized.duels.arena.regen.Selection;
import me.realized.duels.arena.ArenaImpl;
import me.realized.duels.command.BaseCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ArenaregenSubCommand extends BaseCommand {

    private final ArenaRegenManager manager;

    public ArenaregenSubCommand(final DuelsPlugin plugin) {
        super(plugin, "arenaregen", "arenaregen [create|delete|reset|list]", "Arena yenileme bolgelerini yonetir.", 1, false, "ar");
        this.manager = plugin.getArenaRegenManager();
    }

    private ArenaRegenManager getManager() {
        if (this.manager != null) {
            return this.manager;
        }
        return plugin.getArenaRegenManager();
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        if (args.length <= 1) {
            sender.sendMessage("§6§l===== ARENA REGEN KOMUTLARI =====");
            sender.sendMessage("§e/" + label + " ar create <arena> §7- Secili alanda arena icin sifirlama bolgesi olusturur.");
            sender.sendMessage("§e/" + label + " ar delete <arena> §7- Belirtilen arenanin sifirlama bolgesini siler.");
            sender.sendMessage("§e/" + label + " ar reset <arena> §7- Arenayi aninda 0 lag ile sifirlar.");
            sender.sendMessage("§e/" + label + " ar list §7- Mevcut tum sifirlama bolgelerini listeler.");
            sender.sendMessage("§6§l=================================");
            return;
        }

        final String sub = args[1].toLowerCase();
        final ArenaRegenManager mgr = getManager();
        if (mgr == null) {
            sender.sendMessage("§cArenaRegen sistemi henüz yüklenmedi!");
            return;
        }

        switch (sub) {
            case "create":
            case "olustur": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cBu komut yalnizca oyuncular tarafindan kullanilabilir!");
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage("§cKullanim: /" + label + " ar create <arena>");
                    return;
                }
                final Player player = (Player) sender;
                final String arenaName = args[2];
                final ArenaImpl arena = (ArenaImpl) plugin.getArenaManager().get(arenaName);
                if (arena == null) {
                    sender.sendMessage("§c'" + arenaName + "' adinda bir arena bulunamadi!");
                    return;
                }
                final Selection selection = mgr.getSelection(player);
                if (!selection.isSelected()) {
                    sender.sendMessage("§cOnce secim aletiyle iki kose noktasini secmelisiniz!");
                    return;
                }
                if (mgr.getZone(arenaName) != null) {
                    sender.sendMessage("§c'" + arenaName + "' arenasinin zaten bir sifirlama bolgesi var!");
                    return;
                }
                if (mgr.createZone(arena, selection)) {
                    final ResetZone zone = mgr.getZone(arenaName);
                    sender.sendMessage("§a[ArenaRegen] '" + arenaName + "' bolgesi basariyla olusturuldu! (" + (zone != null ? zone.getTotalOriginalBlocks() : 0) + " blok)");
                } else {
                    sender.sendMessage("§cBolge olusturulurken bir hata olustu!");
                }
                break;
            }
            case "delete":
            case "sil": {
                if (args.length < 3) {
                    sender.sendMessage("§cKullanim: /" + label + " ar delete <arena>");
                    return;
                }
                final String arenaName = args[2];
                if (mgr.deleteZone(arenaName)) {
                    sender.sendMessage("§a[ArenaRegen] '" + arenaName + "' sifirlama bolgesi silindi.");
                } else {
                    sender.sendMessage("§c'" + arenaName + "' arenasina ait bir sifirlama bolgesi bulunamadi!");
                }
                break;
            }
            case "reset":
            case "sifirla": {
                if (args.length < 3) {
                    sender.sendMessage("§cKullanim: /" + label + " ar reset <arena>");
                    return;
                }
                final String arenaName = args[2];
                final ResetZone zone = mgr.getZone(arenaName);
                if (zone == null) {
                    sender.sendMessage("§c'" + arenaName + "' arenasina ait bir sifirlama bolgesi bulunamadi!");
                    return;
                }
                sender.sendMessage("§e[ArenaRegen] '" + arenaName + "' arenasi sifirlaniyor (0 lag)...");
                zone.reset(() -> sender.sendMessage("§a[ArenaRegen] '" + arenaName + "' arenasi basariyla sifirlandi!"));
                break;
            }
            case "list":
            case "liste": {
                if (mgr.getZones().isEmpty()) {
                    sender.sendMessage("§cAktif bir sifirlama bolgesi bulunmuyor.");
                    return;
                }
                sender.sendMessage("§6§l===== ARENA SIFIRLAMA BOLGELERI =====");
                for (ResetZone z : mgr.getZones()) {
                    sender.sendMessage("§8▪ §b" + z.getName() + " §7(Dunya: §f" + z.getWorldName() + "§7, Blok: §a" + z.getTotalOriginalBlocks() + "§7)");
                }
                sender.sendMessage("§6§l====================================");
                break;
            }
            default:
                sender.sendMessage("§cBilinmeyen alt komut! Kullanim: /" + label + " ar [create|delete|reset|list]");
                break;
        }
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 2) {
            return Arrays.asList("create", "delete", "reset", "list");
        }
        return null;
    }
}
