package me.realized.duels.arena.regen;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class BlockInfo {

    private final Material type;
    private final byte data;

    public BlockInfo(@NotNull final Material type, final byte data) {
        this.type = type;
        this.data = data;
    }

    public BlockInfo(@NotNull final Block block) {
        this(block.getType(), block.getData());
    }

    public Material getType() {
        return type;
    }

    public byte getData() {
        return data;
    }

    public boolean matches(@NotNull final Block block) {
        return block.getType() == type && block.getData() == data;
    }

    public static long pack(final int x, final int y, final int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) y & 0xFFF) << 26) | ((long) z & 0x3FFFFFF);
    }

    public static int unpackX(final long key) {
        return (int) (key >> 38);
    }

    public static int unpackY(final long key) {
        return (int) ((key >> 26) & 0xFFF);
    }

    public static int unpackZ(final long key) {
        return (int) (key << 38 >> 38);
    }

    public static long getChunkKey(final int blockX, final int blockZ) {
        return ((long) (blockX >> 4) << 32) | ((long) (blockZ >> 4) & 0xFFFFFFFFL);
    }

    @Override
    public String toString() {
        return type.name() + ";" + data;
    }
}
