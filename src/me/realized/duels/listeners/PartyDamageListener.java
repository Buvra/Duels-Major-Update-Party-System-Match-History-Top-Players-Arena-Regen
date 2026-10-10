package me.realized.duels.listeners;

import java.util.Collection;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.party.PartyManager;
import me.realized.duels.party.PartyMatch;
import me.realized.duels.util.EventUtil;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

/**
 * 500 CCU altında saniyede yüzlerce kez tetiklenebilecek PvP hasar dinleyicisi.
 *
 * <p>Performans ve GC Optimizasyonları:
 * <ul>
 *   <li>Geçici nesne, {@code ArrayList}, {@code Stream} veya {@code filter} kullanımı YASAKTIR.</li>
 *   <li>Vurulan varlık {@link Player} değilse anında döner (Early return).</li>
 *   <li>Dost ateşi kontrolleri O(1) doğrudan harita sorgularıyla gerçekleştirilir.</li>
 * </ul>
 */
public class PartyDamageListener implements Listener {

    private final DuelsPlugin plugin;
    private final PartyManager partyManager;

    public PartyDamageListener(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.partyManager = plugin.getPartyManager();
    }

    /**
     * Oyuncu ve mermi kaynaklı hasarlarda sıfır yük dost ateşi koruması.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(final EntityDamageByEntityEvent event) {
        // Vurulan varlık Player değilse anında çık
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player victim = (Player) event.getEntity();
        final Player damager = EventUtil.getDamager(event);

        if (damager == null) {
            return;
        }

        // İzleyicilerin hasar vermesini veya almasını engelle
        final PartyMatch damagerMatch = partyManager.getMatch(damager.getUniqueId());
        if (damagerMatch != null && damagerMatch.isSpectator(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        final PartyMatch victimMatch = partyManager.getMatch(victim.getUniqueId());
        if (victimMatch != null && victimMatch.isSpectator(victim.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Tek bir O(1) kontrolüyle dost ateşi kontrolü
        if (partyManager.isFriendlyFire(damager.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
            damager.sendMessage("§cAynı takımdaki oyunculara saldıramazsınız!");
        }
    }

    /**
     * Alevli ok veya yanıcı varlık kaynaklı dost ateşi tutuşmalarını engeller.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCombust(final org.bukkit.event.entity.EntityCombustByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player victim = (Player) event.getEntity();
        Player damager = null;
        if (event.getCombuster() instanceof Player) {
            damager = (Player) event.getCombuster();
        } else if (event.getCombuster() instanceof org.bukkit.entity.Projectile && ((org.bukkit.entity.Projectile) event.getCombuster()).getShooter() instanceof Player) {
            damager = (Player) ((org.bukkit.entity.Projectile) event.getCombuster()).getShooter();
        }

        if (damager != null && partyManager.isFriendlyFire(damager.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Negatif iksirlerin aynı takımdaki oyuncuları etkilemesini sıfır yük ile engeller.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPotionSplash(final PotionSplashEvent event) {
        final ProjectileSource shooter = event.getPotion().getShooter();
        if (!(shooter instanceof Player)) {
            return;
        }

        final Player damager = (Player) shooter;
        final Collection<PotionEffect> effects = event.getPotion().getEffects();

        // İksirin negatif bir etki taşıyıp taşımadığını döngüyle kontrol et (GC tahsisi yok)
        boolean hasNegative = false;
        for (PotionEffect effect : effects) {
            final PotionEffectType type = effect.getType();
            if (type.equals(PotionEffectType.HARM) ||
                type.equals(PotionEffectType.POISON) ||
                type.equals(PotionEffectType.WEAKNESS) ||
                type.equals(PotionEffectType.SLOW)) {
                hasNegative = true;
                break;
            }
        }

        if (!hasNegative) {
            return;
        }

        for (LivingEntity entity : event.getAffectedEntities()) {
            if (entity instanceof Player && partyManager.isFriendlyFire(damager.getUniqueId(), entity.getUniqueId())) {
                event.setIntensity(entity, 0.0D);
            }
        }
    }

    /**
     * Parti maçı içindeki ölümcül hasarları yakalayarak gecikmesiz 1.8.8 izleyici geçişi sağlar.
     * Hasarı iptal eder ve canı 20'ye sabitler, böylece vanilla kırmızı ölüm ekranı asla tetiklenmez.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFatalDamage(final EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getEntity();
        final PartyMatch match = partyManager.getMatch(player.getUniqueId());

        if (match == null || match.isFinished()) {
            return;
        }

        // İzleyicilerin hasar almasını engelle
        if (match.isSpectator(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Boşluk (void) veya ölümcül hasar kontrolü
        boolean isVoid = event.getCause() == EntityDamageEvent.DamageCause.VOID || player.getLocation().getY() <= 0.0D;
        boolean isFatal = isVoid || (player.getHealth() - event.getFinalDamage() <= 0.0D);

        if (isFatal) {
            event.setCancelled(true);
            player.setFallDistance(0.0F);
            player.setFireTicks(0);
            player.setHealth(20.0D);
            player.setFoodLevel(20);

            if (match.isAlive(player.getUniqueId())) {
                match.handleDeath(player, partyManager, plugin);
            } else if (isVoid) {
                Location safeLoc = match.getArena().getPosition(1);
                if (safeLoc != null) {
                    plugin.getTeleport().tryTeleport(player, safeLoc.clone().add(0, 3.0, 0));
                }
            }
        }
    }

    /**
     * Beklenmeyen ölümlerde (örn. /kill veya bypass) maçı ve oyuncuyu güvende tutar.
     * Otomatik respawn çağırarak oyuncunun kırmızı ölüm ekranında kalmasını engeller.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();
        final PartyMatch match = partyManager.getMatch(player.getUniqueId());

        if (match != null) {
            event.getDrops().clear();
            event.setDroppedExp(0);

            // Anında otomatik respawn çağırarak respawn ekranını yok et
            plugin.doSyncAfter(() -> {
                if (player.isOnline()) {
                    try {
                        player.spigot().respawn();
                    } catch (Exception ignored) {}
                }
            }, 1L);

            if (!match.isFinished() && match.isAlive(player.getUniqueId())) {
                match.handleDeath(player, partyManager, plugin);
            }
        }
    }

    /**
     * Parti maçındayken oyundan çıkan oyuncunun elenmesini sağlar.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final PartyMatch match = partyManager.getMatch(player.getUniqueId());

        if (match != null) {
            match.handleQuit(player, partyManager, plugin);
        }

        // Normal parti üyeliğinden güvenli çıkış
        partyManager.removeMember(player.getUniqueId());
        partyManager.removeDuelRequest(player.getUniqueId());
        partyManager.cancelChatInvitePrompt(player.getUniqueId());
    }

    /**
     * İzleyici modundaki oyuncuların arena sınırları dışına uçmasını ve uzak chunk'ları yükleyip
     * lag veya açık yaratmasını engeller. (GameMode.SPECTATOR yerine güvenli 1.8.8 hayalet uçuş koruması)
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpectatorMove(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final PartyMatch match = partyManager.getMatch(player.getUniqueId());
        if (match == null || !match.isSpectator(player.getUniqueId())) {
            return;
        }

        final Location to = event.getTo();
        final Location pos1 = match.getArena().getPosition(1);
        final Location pos2 = match.getArena().getPosition(2);
        if (pos1 == null || pos2 == null || pos1.getWorld() == null || to == null) {
            return;
        }

        // Sadece tam blok geçişlerinde çalıştır (CPU ve tick tasarrufu)
        final Location from = event.getFrom();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        final double centerX = (pos1.getX() + pos2.getX()) * 0.5D;
        final double centerZ = (pos1.getZ() + pos2.getZ()) * 0.5D;
        final double maxRadius = Math.max(Math.abs(pos1.getX() - pos2.getX()), Math.abs(pos1.getZ() - pos2.getZ())) + 25.0D;

        final double dx = to.getX() - centerX;
        final double dz = to.getZ() - centerZ;
        final double distSq = dx * dx + dz * dz;

        if (distSq > maxRadius * maxRadius || to.getY() < 0 || to.getY() > 250) {
            event.setTo(event.getFrom());
            player.sendMessage("§cArena sınırları dışına çıkamazsınız!");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreak(final BlockBreakEvent event) {
        final PartyMatch match = partyManager.getMatch(event.getPlayer().getUniqueId());
        if (match != null && match.isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlace(final BlockPlaceEvent event) {
        final PartyMatch match = partyManager.getMatch(event.getPlayer().getUniqueId());
        if (match != null && match.isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent event) {
        final PartyMatch match = partyManager.getMatch(event.getPlayer().getUniqueId());
        if (match != null && match.isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent event) {
        final PartyMatch match = partyManager.getMatch(event.getPlayer().getUniqueId());
        if (match != null && match.isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(final PlayerPickupItemEvent event) {
        final PartyMatch match = partyManager.getMatch(event.getPlayer().getUniqueId());
        if (match != null && match.isSpectator(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
