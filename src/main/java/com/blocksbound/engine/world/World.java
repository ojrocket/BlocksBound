package com.blocksbound.engine.world;

import java.util.HashMap;
import java.util.Map;

public class World {
    private final Map<Long, Chunk> chunks = new HashMap<>();

    public World() {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                chunks.put(packChunkKey(x, z), new Chunk());
            }
        }
    }

    public Chunk getChunk(int chunkX, int chunkZ) {
        return chunks.getOrDefault(packChunkKey(chunkX, chunkZ), new Chunk());
    }

    public int getBlockAtWorld(int worldX, int worldY, int worldZ) {
        int chunkX = Math.floorDiv(worldX, Chunk.getSize());
        int chunkZ = Math.floorDiv(worldZ, Chunk.getSize());
        int localX = Math.floorMod(worldX, Chunk.getSize());
        int localY = Math.floorMod(worldY, Chunk.getSize());
        int localZ = Math.floorMod(worldZ, Chunk.getSize());
        return getChunk(chunkX, chunkZ).getBlock(localX, localY, localZ);
    }

    public void setBlockAtWorld(int worldX, int worldY, int worldZ, int blockId) {
        int chunkX = Math.floorDiv(worldX, Chunk.getSize());
        int chunkZ = Math.floorDiv(worldZ, Chunk.getSize());
        int localX = Math.floorMod(worldX, Chunk.getSize());
        int localY = Math.floorMod(worldY, Chunk.getSize());
        int localZ = Math.floorMod(worldZ, Chunk.getSize());
        if (localY < 0 || localY >= Chunk.getSize()) {
            return;
        }
        getChunk(chunkX, chunkZ).setBlock(localX, localY, localZ, blockId);
    }

    public long packChunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }
}
