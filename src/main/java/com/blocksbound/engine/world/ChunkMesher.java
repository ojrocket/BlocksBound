package com.blocksbound.engine.world;

import java.util.Arrays;

public final class ChunkMesher {
    private static final int STRIDE = 6;

    private ChunkMesher() {
    }

    public static float[] build(World world, int chunkX, int chunkZ, Chunk chunk) {
        FloatBuilder vertices = new FloatBuilder();
        int originX = chunkX * Chunk.SIZE;
        int originZ = chunkZ * Chunk.SIZE;

        for (int y = 0; y < Chunk.HEIGHT; y++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                for (int x = 0; x < Chunk.SIZE; x++) {
                    int blockId = chunk.getBlock(x, y, z);
                    if (blockId == BlockType.AIR.getId()) {
                        continue;
                    }
                    BlockType type = BlockType.fromId(blockId);
                    float[] color = color(type);
                    int worldX = originX + x;
                    int worldZ = originZ + z;
                    if (isVisible(type, world.getBlockAtWorld(worldX + 1, y, worldZ))) {
                        face(vertices, color, 1.0f,
                                x + 1, y, z, x + 1, y + 1, z, x + 1, y + 1, z + 1, x + 1, y, z + 1);
                    }
                    if (isVisible(type, world.getBlockAtWorld(worldX - 1, y, worldZ))) {
                        face(vertices, color, 0.78f,
                                x, y, z, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z);
                    }
                    if (isVisible(type, world.getBlockAtWorld(worldX, y + 1, worldZ))) {
                        face(vertices, color, 1.18f,
                                x, y + 1, z, x, y + 1, z + 1, x + 1, y + 1, z + 1, x + 1, y + 1, z);
                    }
                    if (isVisible(type, world.getBlockAtWorld(worldX, y - 1, worldZ))) {
                        face(vertices, color, 0.58f,
                                x, y, z, x + 1, y, z, x + 1, y, z + 1, x, y, z + 1);
                    }
                    if (isVisible(type, world.getBlockAtWorld(worldX, y, worldZ + 1))) {
                        face(vertices, color, 0.92f,
                                x, y, z + 1, x + 1, y, z + 1, x + 1, y + 1, z + 1, x, y + 1, z + 1);
                    }
                    if (isVisible(type, world.getBlockAtWorld(worldX, y, worldZ - 1))) {
                        face(vertices, color, 0.84f,
                                x, y, z, x, y + 1, z, x + 1, y + 1, z, x + 1, y, z);
                    }
                }
            }
        }
        return vertices.toArray();
    }

    private static boolean isVisible(BlockType current, int neighborId) {
        if (neighborId == BlockType.AIR.getId()) {
            return true;
        }
        BlockType neighbor = BlockType.fromId(neighborId);
        return neighbor == BlockType.WATER && current != BlockType.WATER;
    }

    private static float[] color(BlockType type) {
        return switch (type) {
            case GRASS -> new float[]{0.30f, 0.67f, 0.24f};
            case DIRT -> new float[]{0.48f, 0.31f, 0.18f};
            case STONE -> new float[]{0.48f, 0.50f, 0.53f};
            case SAND -> new float[]{0.78f, 0.70f, 0.43f};
            case WOOD -> new float[]{0.43f, 0.28f, 0.13f};
            case LEAVES -> new float[]{0.19f, 0.48f, 0.18f};
            case WATER -> new float[]{0.18f, 0.42f, 0.78f};
            case AIR -> new float[]{0.0f, 0.0f, 0.0f};
        };
    }

    private static void face(FloatBuilder out, float[] color, float shade,
            float x0, float y0, float z0, float x1, float y1, float z1,
            float x2, float y2, float z2, float x3, float y3, float z3) {
        float red = Math.min(color[0] * shade, 1.0f);
        float green = Math.min(color[1] * shade, 1.0f);
        float blue = Math.min(color[2] * shade, 1.0f);
        out.add(x0, y0, z0, red, green, blue);
        out.add(x1, y1, z1, red, green, blue);
        out.add(x2, y2, z2, red, green, blue);
        out.add(x0, y0, z0, red, green, blue);
        out.add(x2, y2, z2, red, green, blue);
        out.add(x3, y3, z3, red, green, blue);
    }

    private static final class FloatBuilder {
        private float[] values = new float[STRIDE * 1024];
        private int size;

        private void add(float x, float y, float z, float r, float g, float b) {
            ensureCapacity(size + STRIDE);
            values[size++] = x;
            values[size++] = y;
            values[size++] = z;
            values[size++] = r;
            values[size++] = g;
            values[size++] = b;
        }

        private void ensureCapacity(int required) {
            if (required > values.length) {
                values = Arrays.copyOf(values, Math.max(required, values.length * 2));
            }
        }

        private float[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
