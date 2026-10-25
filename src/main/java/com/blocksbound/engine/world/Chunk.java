package com.blocksbound.engine.world;

import java.util.Arrays;

public class Chunk {
    public static final int SIZE = 16;
    public static final int HEIGHT = 64;
    private static final int BLOCK_COUNT = SIZE * SIZE * HEIGHT;

    private final short[] blocks = new short[BLOCK_COUNT];
    private boolean dirty = true;

    public static int getSize() {
        return SIZE;
    }

    public static int getHeight() {
        return HEIGHT;
    }

    public int getBlock(int x, int y, int z) {
        if (!contains(x, y, z)) {
            return BlockType.AIR.getId();
        }
        return blocks[index(x, y, z)] & 0xFFFF;
    }

    public void setBlock(int x, int y, int z, int value) {
        if (!contains(x, y, z)) {
            return;
        }
        int index = index(x, y, z);
        if ((blocks[index] & 0xFFFF) != value) {
            blocks[index] = (short) value;
            dirty = true;
        }
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    public void markDirty() {
        dirty = true;
    }

    public short[] copyBlocks() {
        return Arrays.copyOf(blocks, blocks.length);
    }

    public void loadBlocks(short[] savedBlocks) {
        if (savedBlocks.length != BLOCK_COUNT) {
            throw new IllegalArgumentException("Unexpected block count for chunk");
        }
        System.arraycopy(savedBlocks, 0, blocks, 0, blocks.length);
        dirty = true;
    }

    private static boolean contains(int x, int y, int z) {
        return x >= 0 && x < SIZE && y >= 0 && y < HEIGHT && z >= 0 && z < SIZE;
    }

    private static int index(int x, int y, int z) {
        return x + z * SIZE + y * SIZE * SIZE;
    }
}
