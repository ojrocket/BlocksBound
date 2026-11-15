package com.blocksbound.engine.world;

public final class TerrainGenerator {
    private static final int SEA_LEVEL = 14;
    private static final int TREE_SPACING = 8;

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
        generateTrees(seed, chunkX, chunkZ, chunk);
        return chunk;
    }

    public static int heightAt(long seed, int worldX, int worldZ) {
        double broad = valueNoise(seed, worldX * 0.012, worldZ * 0.012);
        double detail = valueNoise(seed ^ 0x9E3779B97F4A7C15L, worldX * 0.06, worldZ * 0.06);
        double mountainNoise = (valueNoise(seed ^ 0xD1B54A32D192ED03L,
                worldX * 0.007, worldZ * 0.007) + 1.0) * 0.5;
        double mountainMask = smoothstep(0.34, 0.62, mountainNoise);
        double ridgeNoise = 1.0 - Math.abs(valueNoise(seed ^ 0x94D049BB133111EBL,
                worldX * 0.025, worldZ * 0.025));
        double mountain = Math.pow(ridgeNoise, 1.4) * mountainMask * 33.0;
        int height = 17 + (int) Math.round(broad * 7.0 + detail * 2.5 + mountain);
        return Math.max(4, Math.min(Chunk.HEIGHT - 2, height));
    }

    private static void generateTrees(long seed, int chunkX, int chunkZ, Chunk chunk) {
        int originX = chunkX * Chunk.SIZE;
        int originZ = chunkZ * Chunk.SIZE;
        int minCellX = Math.floorDiv(originX - 2, TREE_SPACING);
        int maxCellX = Math.floorDiv(originX + Chunk.SIZE + 1, TREE_SPACING);
        int minCellZ = Math.floorDiv(originZ - 2, TREE_SPACING);
        int maxCellZ = Math.floorDiv(originZ + Chunk.SIZE + 1, TREE_SPACING);
        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                if (hash(seed ^ 0xA0761D6478BD642FL, cellX, cellZ) < -0.2) {
                    continue;
                }
                int treeX = cellX * TREE_SPACING
                        + (int) ((hash(seed ^ 0xE7037ED1A0B428DBL, cellX, cellZ) + 1.0)
                                * 0.5 * TREE_SPACING);
                int treeZ = cellZ * TREE_SPACING
                        + (int) ((hash(seed ^ 0x8EBC6AF09C88C6E3L, cellX, cellZ) + 1.0)
                                * 0.5 * TREE_SPACING);
                int surface = heightAt(seed, treeX, treeZ);
                if (surface <= SEA_LEVEL + 1 || surface > 34) {
                    continue;
                }
                placeTree(chunk, originX, originZ, treeX, surface, treeZ);
            }
        }
    }

    private static void placeTree(Chunk chunk, int originX, int originZ, int treeX, int surface, int treeZ) {
        int trunkHeight = 4;
        int canopyBase = surface + trunkHeight - 1;
        for (int layer = 0; layer < 3; layer++) {
            int y = canopyBase + layer;
            int radius = layer == 2 ? 1 : 2;
            for (int offsetX = -radius; offsetX <= radius; offsetX++) {
                for (int offsetZ = -radius; offsetZ <= radius; offsetZ++) {
                    if (Math.abs(offsetX) == radius && Math.abs(offsetZ) == radius && radius == 2) {
                        continue;
                    }
                    setChunkBlockIfAir(chunk, originX, originZ,
                            treeX + offsetX, y, treeZ + offsetZ, BlockType.LEAVES);
                }
            }
        }
        for (int y = surface + 1; y <= surface + trunkHeight; y++) {
            setChunkBlock(chunk, originX, originZ, treeX, y, treeZ, BlockType.WOOD);
        }
    }

    private static void setChunkBlockIfAir(Chunk chunk, int originX, int originZ,
            int worldX, int y, int worldZ, BlockType type) {
        int localX = worldX - originX;
        int localZ = worldZ - originZ;
        if (localX >= 0 && localX < Chunk.SIZE && localZ >= 0 && localZ < Chunk.SIZE
                && y >= 0 && y < Chunk.HEIGHT && chunk.getBlock(localX, y, localZ) == BlockType.AIR.getId()) {
            chunk.setBlock(localX, y, localZ, type.getId());
        }
    }

    private static void setChunkBlock(Chunk chunk, int originX, int originZ,
            int worldX, int y, int worldZ, BlockType type) {
        int localX = worldX - originX;
        int localZ = worldZ - originZ;
        if (localX >= 0 && localX < Chunk.SIZE && localZ >= 0 && localZ < Chunk.SIZE
                && y >= 0 && y < Chunk.HEIGHT) {
            chunk.setBlock(localX, y, localZ, type.getId());
        }
    }

    private static double smoothstep(double edge0, double edge1, double value) {
        double amount = Math.max(0.0, Math.min(1.0, (value - edge0) / (edge1 - edge0)));
        return amount * amount * (3.0 - 2.0 * amount);
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
