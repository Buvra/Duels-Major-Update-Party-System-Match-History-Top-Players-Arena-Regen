package me.realized.duels.arena.regen;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.api.event.match.MatchEndEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Sıfır GC baskısıyla çalışan, O(1) uzamsal filtrelemeli blok olay dinleyicisi.
 */
public class ArenaRegenListener implements Listener {

    private final DuelsPlugin plugin;
    private final ArenaRegenManager manager;
    private final ArenaRegenConfig config;

    public ArenaRegenListener(@NotNull final DuelsPlugin plugin, @NotNull final ArenaRegenManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.config = manager.getConfig();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchEnd(final MatchEndEvent event) {
        if (event.getMatch() == null || event.getMatch().getArena() == null) {
            return;
        }

        final String arenaName = event.getMatch().getArena().getName();
        final ResetZone zone = manager.getZone(arenaName);
        if (zone != null) {
            zone.reset(null);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPermission("duels.admin") || !event.hasItem() || !event.hasBlock()) {
            return;
        }

        final ItemStack item = event.getItem();
        if (item.getType() != config.getSelectingTool()) {
            return;
        }

        event.setCancelled(true);
        final Block clicked = event.getClickedBlock();
        final Selection selection = manager.getSelection(player);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            selection.setFirst(clicked.getLocation());
            player.sendMessage("§a[ArenaRegen] §e1. Konum belirlendi: §f" + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            selection.setSecond(clicked.getLocation());
            player.sendMessage("§a[ArenaRegen] §e2. Konum belirlendi: §f" + clicked.getX() + ", " + clicked.getY() + ", " + clicked.getZ());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        final Block b = event.getBlock();
        final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
        if (zone != null) {
            zone.recordChange(b.getX(), b.getY(), b.getZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(final BlockBreakEvent event) {
        final Block b = event.getBlock();
        final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
        if (zone == null) {
            return;
        }

        // Orijinal arena bloğu kırma koruması
        if (!config.isAllowArenaBlockBreak() && zone.isOriginalBlock(b.getX(), b.getY(), b.getZ())) {
            final Player player = event.getPlayer();
            if (plugin.getArenaManager().isInMatch(player)) {
                event.setCancelled(true);
                player.sendMessage("§cArenanın orijinal bloklarını kıramazsınız!");
                return;
            }
        }

        zone.recordChange(b.getX(), b.getY(), b.getZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBucketEmpty(final PlayerBucketEmptyEvent event) {
        if (event.getBlockClicked() == null) return;
        final Block target = event.getBlockClicked().getRelative(event.getBlockFace());
        final ResetZone zone = manager.findZone(target.getX(), target.getY(), target.getZ(), target.getWorld().getName());
        if (zone != null) {
            if (!config.isAllowArenaBlockBreak() && zone.isOriginalBlock(target.getX(), target.getY(), target.getZ())) {
                event.setCancelled(true);
                return;
            }
            zone.recordChange(target.getX(), target.getY(), target.getZ());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(final PlayerBucketFillEvent event) {
        if (event.getBlockClicked() == null) return;
        final Block target = event.getBlockClicked();
        final ResetZone zone = manager.findZone(target.getX(), target.getY(), target.getZ(), target.getWorld().getName());
        if (zone != null) {
            zone.recordChange(target.getX(), target.getY(), target.getZ());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFlow(final BlockFromToEvent event) {
        final Block to = event.getToBlock();
        final ResetZone zone = manager.findZone(to.getX(), to.getY(), to.getZ(), to.getWorld().getName());
        if (zone == null) {
            return;
        }

        // Su veya lav, arenanın orijinal bloklarını (merdiven, meşale, şalter vb.) ASLA kıramaz/yıkamaz!
        if (!config.isAllowArenaBlockBreak() && zone.isOriginalBlock(to.getX(), to.getY(), to.getZ())) {
            event.setCancelled(true);
            return;
        }

        zone.recordChange(to.getX(), to.getY(), to.getZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(final org.bukkit.event.block.BlockPhysicsEvent event) {
        final Block b = event.getBlock();
        final Material type = b.getType();
        // Duvara veya zemine bağlı merdiven, meşale, sarmaşık gibi hassas blokların su/fizik ile düşmesini engelle
        if (type == Material.LADDER || type == Material.TORCH || type == Material.REDSTONE_TORCH_ON ||
            type == Material.REDSTONE_TORCH_OFF || type == Material.VINE || type == Material.LEVER ||
            type == Material.STONE_BUTTON || type == Material.WOOD_BUTTON || type == Material.TRIPWIRE_HOOK ||
            type == Material.SIGN_POST || type == Material.WALL_SIGN) {
            final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
            if (zone != null && !config.isAllowArenaBlockBreak() && zone.isOriginalBlock(b.getX(), b.getY(), b.getZ())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFade(final BlockFadeEvent event) {
        if (!config.isPreventBlockMelt()) return;
        final Block b = event.getBlock();
        final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
        if (zone != null) {
            final Material mat = event.getNewState().getType();
            if (mat == Material.AIR || mat.name().contains("WATER")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBurn(final BlockBurnEvent event) {
        if (!config.isPreventBlockBurn()) return;
        final Block b = event.getBlock();
        final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
        if (zone != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockIgnite(final BlockIgniteEvent event) {
        if (!config.isPreventFireSpread()) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD) {
            final Block b = event.getBlock();
            final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
            if (zone != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLeafDecay(final LeavesDecayEvent event) {
        if (!config.isPreventLeafDecay()) return;
        final Block b = event.getBlock();
        final ResetZone zone = manager.findZone(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
        if (zone != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(final PlayerQuitEvent event) {
        manager.removeSelection(event.getPlayer().getUniqueId());
    }
}
