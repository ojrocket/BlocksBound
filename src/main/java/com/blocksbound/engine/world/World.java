package com.blocksbound.engine.world;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class World {
    private final Map<Long, Chunk> chunks = new HashMap<>();
    private final List<BlockDrop> droppedBlocks = new ArrayList<>();
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

    public List<BlockDrop> getDroppedBlocks() {
        return Collections.unmodifiableList(droppedBlocks);
    }

    public Optional<BlockType> breakBlockAtWorld(int worldX, int worldY, int worldZ) {
        if (worldY < 0 || worldY >= Chunk.HEIGHT) {
            return Optional.empty();
        }
        int blockId = getBlockAtWorld(worldX, worldY, worldZ);
        if (blockId == BlockType.AIR.getId()) {
            return Optional.empty();
        }
        BlockType type = BlockType.fromId(blockId);
        if (!type.isSolid()) {
            return Optional.empty();
        }
        setBlockAtWorld(worldX, worldY, worldZ, BlockType.AIR.getId());
        droppedBlocks.add(new BlockDrop(type, worldX + 0.5f, worldY + 0.5f, worldZ + 0.5f));
        return Optional.of(type);
    }

    public List<BlockDrop> collectDroppedBlocksNear(float x, float y, float z, float radius) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
                || !Float.isFinite(radius) || radius < 0.0f) {
            throw new IllegalArgumentException("Pickup position and radius must be finite and radius non-negative");
        }
        float radiusSquared = radius * radius;
        List<BlockDrop> collected = new ArrayList<>();
        Iterator<BlockDrop> iterator = droppedBlocks.iterator();
        while (iterator.hasNext()) {
            BlockDrop drop = iterator.next();
            float dx = drop.x() - x;
            float dy = drop.y() - y;
            float dz = drop.z() - z;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                collected.add(drop);
                iterator.remove();
            }
        }
        return collected;
    }

    public void addDroppedBlock(BlockDrop drop) {
        if (drop == null || drop.type() == null || !drop.type().isSolid()
                || !Float.isFinite(drop.x()) || !Float.isFinite(drop.y()) || !Float.isFinite(drop.z())) {
            throw new IllegalArgumentException("Dropped block must have a solid type and finite position");
        }
        droppedBlocks.add(drop);
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
