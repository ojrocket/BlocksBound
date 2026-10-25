package com.blocksbound.engine.world;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class World {
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private long seed;

    public World() {
        this(new java.util.Random().nextLong());
    }

    public World(long seed) {
        this.seed = seed;
    }

    public long getSeed() {
        return seed;
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.computeIfAbsent(packChunkKey(chunkX, chunkZ),
                ignored -> TerrainGenerator.generate(seed, chunkX, chunkZ));
    }

    public Chunk getLoadedChunk(int chunkX, int chunkZ) {
        return chunks.get(packChunkKey(chunkX, chunkZ));
    }

    public Collection<Map.Entry<Long, Chunk>> getLoadedChunks() {
        return Collections.unmodifiableCollection(chunks.entrySet());
    }

    public void ensureChunksAround(int worldX, int worldZ, int radius) {
        int centerX = Math.floorDiv(worldX, Chunk.SIZE);
        int centerZ = Math.floorDiv(worldZ, Chunk.SIZE);
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                getChunk(x, z);
            }
        }
    }

    public int getBlockAtWorld(int worldX, int worldY, int worldZ) {
        if (worldY < 0) {
            return BlockType.STONE.getId();
        }
        if (worldY >= Chunk.HEIGHT) {
            return BlockType.AIR.getId();
        }
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);
        Chunk chunk = getLoadedChunk(chunkX, chunkZ);
        if (chunk == null) {
            return BlockType.AIR.getId();
        }
        return chunk.getBlock(Math.floorMod(worldX, Chunk.SIZE), worldY, Math.floorMod(worldZ, Chunk.SIZE));
    }

    public void setBlockAtWorld(int worldX, int worldY, int worldZ, int blockId) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return;
        }
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);
        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);
        Chunk chunk = getChunk(chunkX, chunkZ);
        chunk.setBlock(localX, worldY, localZ, blockId);
        if (localX == 0) {
            markDirty(chunkX - 1, chunkZ);
        } else if (localX == Chunk.SIZE - 1) {
            markDirty(chunkX + 1, chunkZ);
        }
        if (localZ == 0) {
            markDirty(chunkX, chunkZ - 1);
        } else if (localZ == Chunk.SIZE - 1) {
            markDirty(chunkX, chunkZ + 1);
        }
    }

    public int getSurfaceHeight(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.SIZE);
        int chunkZ = Math.floorDiv(worldZ, Chunk.SIZE);
        Chunk chunk = getChunk(chunkX, chunkZ);
        int localX = Math.floorMod(worldX, Chunk.SIZE);
        int localZ = Math.floorMod(worldZ, Chunk.SIZE);
        for (int y = Chunk.HEIGHT - 1; y >= 0; y--) {
            int block = chunk.getBlock(localX, y, localZ);
            if (block != BlockType.AIR.getId() && block != BlockType.WATER.getId()) {
                return y;
            }
        }
        return 0;
    }

    public void putLoadedChunk(int chunkX, int chunkZ, Chunk chunk) {
        chunks.put(packChunkKey(chunkX, chunkZ), chunk);
    }

    public long packChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    public static int unpackChunkX(long key) {
        return (int) (key >> 32);
    }

    public static int unpackChunkZ(long key) {
        return (int) key;
    }

    private void markDirty(int chunkX, int chunkZ) {
        Chunk neighbor = getLoadedChunk(chunkX, chunkZ);
        if (neighbor != null) {
            neighbor.markDirty();
        }
    }
}
