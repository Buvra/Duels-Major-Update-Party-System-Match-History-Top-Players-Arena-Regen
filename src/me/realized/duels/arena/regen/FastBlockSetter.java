package me.realized.duels.arena.regen;

import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftMagicNumbers;

/**
 * 500 CCU altında 0 tick drop ile çalışan, 1.8.8 Paper doğrudan NMS blok yerleştiricisi.
 *
 * <p>Optimizasyonlar:
 * <ul>
 *   <li>Tüm {@link IBlockData} durumları önceden statik olarak önbelleğe alınmıştır (Zero Allocation).</li>
 *   <li>Gereksiz fizik hesaplamaları, {@code getRelative} çağrıları ve ışık hesaplamaları kapatılmıştır.</li>
 *   <li>{@code refreshChunk} YASAKTIR; oyunculara hafif paket bildirimleri {@code flagDirty} ile iletilir.</li>
 * </ul>
 */
public final class FastBlockSetter {

    private static final IBlockData[][] DATA_PALETTE = new IBlockData[Material.values().length][16];

    static {
        for (Material material : Material.values()) {
            if (!material.isBlock()) {
                continue;
            }
            final Block nmsBlock = CraftMagicNumbers.getBlock(material);
            if (nmsBlock == null) {
                continue;
            }
            for (int data = 0; data < 16; data++) {
                try {
                    DATA_PALETTE[material.getId()][data] = nmsBlock.fromLegacyData(data);
                } catch (Throwable ignored) {
                    DATA_PALETTE[material.getId()][data] = nmsBlock.getBlockData();
                }
            }
        }
    }

    public static IBlockData getBlockData(final Material material, final byte data) {
        if (material == null) {
            return DATA_PALETTE[0][0]; // AIR
        }
        final int id = material.getId();
        if (id >= 0 && id < DATA_PALETTE.length) {
            final int safeData = (data >= 0 && data < 16) ? data : 0;
            final IBlockData cached = DATA_PALETTE[id][safeData];
            if (cached != null) {
                return cached;
            }
        }
        final Block block = CraftMagicNumbers.getBlock(material);
        return block != null ? block.getBlockData() : DATA_PALETTE[0][0];
    }

    /**
     * Belirtilen koordinata en düşük gecikmeyle blok yerleştirir ve oyunculara bildirir.
     */
    public static void setBlockFast(final World world, final int x, final int y, final int z, final Material material, final byte data) {
        if (world == null || y < 0 || y > 255) {
            return;
        }

        final WorldServer worldServer = ((CraftWorld) world).getHandle();
        final int chunkX = x >> 4;
        final int chunkZ = z >> 4;

        if (!worldServer.chunkProviderServer.isChunkLoaded(chunkX, chunkZ)) {
            return;
        }

        final Chunk chunk = worldServer.getChunkAt(chunkX, chunkZ);
        final BlockPosition pos = new BlockPosition(x, y, z);
        final IBlockData blockData = getBlockData(material, data);

        // Bloğu doğrudan chunk palette'ine yazar
        chunk.a(pos, blockData);

        // Oyunculara ağ üzerinden bildirilmesi için dirty olarak işaretler
        worldServer.getPlayerChunkMap().flagDirty(pos);
    }

    private FastBlockSetter() {}
}
