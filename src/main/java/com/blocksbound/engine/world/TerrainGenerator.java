package com.blocksbound.engine.world;

public final class TerrainGenerator {
    private static final int SEA_LEVEL = 14;

    private TerrainGenerator() {
    }

    public static Chunk generate(long seed, int chunkX, int chunkZ) {
        Chunk chunk = new Chunk();
        int originX = chunkX * Chunk.SIZE;
        int originZ = chunkZ * Chunk.SIZE;
        for (int x = 0; x < Chunk.SIZE; x++) {
            for (int z = 0; z < Chunk.SIZE; z++) {
                int worldX = originX + x;
                int worldZ = originZ + z;
                int surface = heightAt(seed, worldX, worldZ);
                for (int y = 0; y <= surface; y++) {
                    int block;
                    if (y == surface) {
                        block = surface <= SEA_LEVEL + 1 ? BlockType.SAND.getId() : BlockType.GRASS.getId();
                    } else if (y >= surface - 3) {
                        block = surface <= SEA_LEVEL + 1 ? BlockType.SAND.getId() : BlockType.DIRT.getId();
                    } else {
                        block = BlockType.STONE.getId();
                    }
                    chunk.setBlock(x, y, z, block);
                }
                for (int y = surface + 1; y <= SEA_LEVEL && y < Chunk.HEIGHT; y++) {
                    chunk.setBlock(x, y, z, BlockType.WATER.getId());
                }
            }
        }
        return chunk;
    }

    public static int heightAt(long seed, int worldX, int worldZ) {
        double broad = valueNoise(seed, worldX * 0.035, worldZ * 0.035);
        double detail = valueNoise(seed ^ 0x9E3779B97F4A7C15L, worldX * 0.12, worldZ * 0.12);
        return Math.max(4, Math.min(Chunk.HEIGHT - 2, 19 + (int) Math.round(broad * 9.0 + detail * 2.5)));
    }

    private static double valueNoise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double tx = fade(x - x0);
        double tz = fade(z - z0);
        double a = lerp(hash(seed, x0, z0), hash(seed, x0 + 1, z0), tx);
        double b = lerp(hash(seed, x0, z0 + 1), hash(seed, x0 + 1, z0 + 1), tx);
        return lerp(a, b, tz);
    }

    private static double hash(long seed, int x, int z) {
        long value = seed ^ (x * 0x632BE59BD9B4E019L) ^ (z * 0x9E3779B97F4A7C15L);
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return ((value >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
