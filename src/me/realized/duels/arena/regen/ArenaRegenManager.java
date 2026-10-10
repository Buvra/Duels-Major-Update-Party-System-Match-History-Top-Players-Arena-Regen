package me.realized.duels.arena.regen;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaImpl;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ArenaRegen sistemini Duels çekirdeğine bağlayan, O(1) uzamsal chunk indeksli yönetici.
 */
public class ArenaRegenManager {

    private final DuelsPlugin plugin;
    private final ArenaRegenConfig config;
    private final File folder;

    private final Map<String, ResetZone> zones = new ConcurrentHashMap<String, ResetZone>();
    private final Map<Long, Set<ResetZone>> zonesByChunk = new ConcurrentHashMap<Long, Set<ResetZone>>();
    private final Map<UUID, Selection> selections = new ConcurrentHashMap<UUID, Selection>();

    public ArenaRegenManager(@NotNull final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = new ArenaRegenConfig(plugin);
        this.folder = new File(plugin.getDataFolder(), "zones");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public void loadZones() {
        zones.clear();
        zonesByChunk.clear();

        final File[] files = folder.listFiles((dir, name) -> name.endsWith(".dat") || name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        final java.util.Set<String> processed = new java.util.HashSet<String>();
        for (File file : files) {
            final String rawName = file.getName();
            final String arenaName = rawName.substring(0, rawName.lastIndexOf('.'));
            if (!processed.add(arenaName.toLowerCase())) {
                continue;
            }

            final ArenaImpl arena = (ArenaImpl) plugin.getArenaManager().get(arenaName);
            if (arena == null) {
                continue;
            }

            try {
                final ResetZone zone = new ResetZone(plugin, config, arena, folder);
                registerZone(zone);
                plugin.getLogger().info("[ArenaRegen] '" + arenaName + "' bolgesi basariyla yuklendi (" + zone.getTotalOriginalBlocks() + " blok).");
            } catch (Exception ex) {
                plugin.getLogger().warning("[ArenaRegen] '" + arenaName + "' bolgesi yuklenemedi: " + ex.getMessage());
            }
        }
    }

    public void save() {
        for (ResetZone zone : zones.values()) {
            zone.save();
        }
    }

    private void registerZone(@NotNull final ResetZone zone) {
        zones.put(zone.getName().toLowerCase(), zone);

        final int minChunkX = zone.getMinX() >> 4;
        final int maxChunkX = zone.getMaxX() >> 4;
        final int minChunkZ = zone.getMinZ() >> 4;
        final int maxChunkZ = zone.getMaxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                final long chunkKey = ((long) cx << 32) | ((long) cz & 0xFFFFFFFFL);
                zonesByChunk.computeIfAbsent(chunkKey, k -> Collections.newSetFromMap(new ConcurrentHashMap<ResetZone, Boolean>())).add(zone);
            }
        }
    }

    private void unregisterZone(@NotNull final ResetZone zone) {
        zones.remove(zone.getName().toLowerCase());

        final int minChunkX = zone.getMinX() >> 4;
        final int maxChunkX = zone.getMaxX() >> 4;
        final int minChunkZ = zone.getMinZ() >> 4;
        final int maxChunkZ = zone.getMaxZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                final long chunkKey = ((long) cx << 32) | ((long) cz & 0xFFFFFFFFL);
                final Set<ResetZone> set = zonesByChunk.get(chunkKey);
                if (set != null) {
                    set.remove(zone);
                    if (set.isEmpty()) {
                        zonesByChunk.remove(chunkKey);
                    }
                }
            }
        }
    }

    @Nullable
    public ResetZone getZone(@Nullable final String arenaName) {
        return arenaName != null ? zones.get(arenaName.toLowerCase()) : null;
    }

    public Collection<ResetZone> getZones() {
        return zones.values();
    }

    /**
     * O(1) Chunk indeksi ve doğrudan integer sınır kontrolü ile saniyede binlerce çağrıda 0 lag.
     */
    @Nullable
    public ResetZone findZone(final int x, final int y, final int z, @NotNull final String worldName) {
        final long chunkKey = BlockInfo.getChunkKey(x, z);
        final Set<ResetZone> set = zonesByChunk.get(chunkKey);
        if (set == null || set.isEmpty()) {
            return null;
        }

        for (ResetZone zone : set) {
            if (zone.contains(x, y, z, worldName)) {
                return zone;
            }
        }
        return null;
    }

    public boolean createZone(@NotNull final ArenaImpl arena, @NotNull final Selection selection) {
        if (zones.containsKey(arena.getName().toLowerCase()) || !selection.isSelected()) {
            return false;
        }

        final ResetZone zone = new ResetZone(plugin, config, arena, folder, selection.getFirst(), selection.getSecond());
        zone.captureBlocks();
        zone.save();
        registerZone(zone);
        return true;
    }

    public boolean deleteZone(@NotNull final String arenaName) {
        final ResetZone zone = zones.get(arenaName.toLowerCase());
        if (zone == null) {
            return false;
        }

        unregisterZone(zone);
        zone.delete();
        return true;
    }

    @NotNull
    public Selection getSelection(@NotNull final Player player) {
        return selections.computeIfAbsent(player.getUniqueId(), k -> new Selection());
    }

    public void removeSelection(@NotNull final UUID playerId) {
        selections.remove(playerId);
    }

    public ArenaRegenConfig getConfig() {
        return config;
    }
}
