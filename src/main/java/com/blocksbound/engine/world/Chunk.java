package com.blocksbound.engine.world;

public class Chunk {
    private static final int SIZE = 16;
    private final short[] blocks = new short[SIZE * SIZE * SIZE];

    public Chunk() {
        for (int x = 0; x < SIZE; x++) {
            for (int z = 0; z < SIZE; z++) {
                for (int y = 0; y < SIZE; y++) {
                    if (y == 0) {
                        setBlock(x, y, z, BlockType.STONE.getId());
                    } else if (y == 1) {
                        setBlock(x, y, z, BlockType.DIRT.getId());
                    }
                }
            }
        }
    }

    public static int getSize() {
        return SIZE;
    }

    public int getBlock(int x, int y, int z) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return 0;
        }
        return blocks[index(x, y, z)] & 0xFFFF;
    }

    public void setBlock(int x, int y, int z, int value) {
        if (x < 0 || x >= SIZE || y < 0 || y >= SIZE || z < 0 || z >= SIZE) {
            return;
        }
        blocks[index(x, y, z)] = (short) value;
    }

    private int index(int x, int y, int z) {
        return x + (z * SIZE) + (y * SIZE * SIZE);
    }
}
