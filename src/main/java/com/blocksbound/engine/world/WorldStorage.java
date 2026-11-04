package com.blocksbound.engine.world;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.joml.Vector3f;

public final class WorldStorage {
    private static final int MAGIC = 0x42424C4B;
    private static final int VERSION = 2;
    private static final int MAX_CHUNKS = 100_000;
    private static final int MAX_DROPS = 1_000_000;

    private WorldStorage() {
    }

    public static Path defaultSavePath() {
        return Path.of(System.getProperty("user.home"), ".blocksbound", "world.dat");
    }

    public static void save(Path path, World world, Vector3f playerPosition) throws IOException {
        save(path, world, playerPosition, Map.of());
    }

    public static void save(Path path, World world, Vector3f playerPosition,
            Map<BlockType, Integer> inventory) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            try (DataOutputStream output = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                output.writeInt(MAGIC);
                output.writeInt(VERSION);
                output.writeLong(world.getSeed());
                output.writeFloat(playerPosition.x);
                output.writeFloat(playerPosition.y);
                output.writeFloat(playerPosition.z);

                List<java.util.Map.Entry<Long, Chunk>> chunks = new ArrayList<>(world.getLoadedChunks());
                chunks.sort(Comparator.comparingLong(java.util.Map.Entry::getKey));
                output.writeInt(chunks.size());
                for (java.util.Map.Entry<Long, Chunk> entry : chunks) {
                    output.writeInt(World.unpackChunkX(entry.getKey()));
                    output.writeInt(World.unpackChunkZ(entry.getKey()));
                    for (short block : entry.getValue().copyBlocks()) {
                        output.writeShort(block);
                    }
                }
                if (world.getDroppedBlocks().size() > MAX_DROPS) {
                    throw new IOException("Too many dropped blocks to save");
                }
                output.writeInt(world.getDroppedBlocks().size());
                for (BlockDrop drop : world.getDroppedBlocks()) {
                    output.writeInt(drop.type().getId());
                    output.writeFloat(drop.x());
                    output.writeFloat(drop.y());
                    output.writeFloat(drop.z());
                }
                int inventorySize = 0;
                for (Map.Entry<BlockType, Integer> entry : inventory.entrySet()) {
                    if (entry.getKey() == null || !entry.getKey().isSolid()
                            || entry.getValue() == null || entry.getValue() < 0) {
                        throw new IOException("Invalid inventory entry");
                    }
                    if (entry.getValue() > 0) {
                        inventorySize++;
                    }
                }
                output.writeInt(inventorySize);
                for (Map.Entry<BlockType, Integer> entry : inventory.entrySet()) {
                    if (entry.getValue() == 0) {
                        continue;
                    }
                    output.writeInt(entry.getKey().getId());
                    output.writeInt(entry.getValue());
                }
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Optional<SavedWorld> load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try (DataInputStream input = new DataInputStream(
                new BufferedInputStream(Files.newInputStream(path)))) {
            if (input.readInt() != MAGIC) {
                throw new IOException("Not a BlocksBound world save: " + path);
            }
            int version = input.readInt();
            if (version < 1 || version > VERSION) {
                throw new IOException("Unsupported world save version: " + version);
            }
            long seed = input.readLong();
            Vector3f playerPosition = new Vector3f(input.readFloat(), input.readFloat(), input.readFloat());
            if (!Float.isFinite(playerPosition.x) || !Float.isFinite(playerPosition.y)
                    || !Float.isFinite(playerPosition.z)) {
                throw new IOException("Invalid player position in world save");
            }
            int chunkCount = input.readInt();
            if (chunkCount < 0 || chunkCount > MAX_CHUNKS) {
                throw new IOException("Invalid chunk count in world save: " + chunkCount);
            }

            World world = new World(seed);
            for (int index = 0; index < chunkCount; index++) {
                int chunkX = input.readInt();
                int chunkZ = input.readInt();
                Chunk chunk = new Chunk();
                short[] blocks = new short[Chunk.SIZE * Chunk.SIZE * Chunk.HEIGHT];
                for (int blockIndex = 0; blockIndex < blocks.length; blockIndex++) {
                    short block = input.readShort();
                    try {
                        BlockType.fromId(block & 0xFFFF);
                    } catch (IllegalArgumentException exception) {
                        throw new IOException("Invalid block id in world save", exception);
                    }
                    blocks[blockIndex] = block;
                }
                chunk.loadBlocks(blocks);
                if (world.getLoadedChunk(chunkX, chunkZ) != null) {
                    throw new IOException("Duplicate chunk in world save");
                }
                world.putLoadedChunk(chunkX, chunkZ, chunk);
            }
            Map<BlockType, Integer> inventory = new EnumMap<>(BlockType.class);
            if (version >= 2) {
                int dropCount = input.readInt();
                if (dropCount < 0 || dropCount > MAX_DROPS) {
                    throw new IOException("Invalid dropped block count in world save: " + dropCount);
                }
                for (int index = 0; index < dropCount; index++) {
                    int blockId = input.readInt();
                    BlockType type = readSolidBlockType(blockId);
                    float x = input.readFloat();
                    float y = input.readFloat();
                    float z = input.readFloat();
                    try {
                        world.addDroppedBlock(new BlockDrop(type, x, y, z));
                    } catch (IllegalArgumentException exception) {
                        throw new IOException("Invalid dropped block in world save", exception);
                    }
                }
                int inventorySize = input.readInt();
                if (inventorySize < 0 || inventorySize > BlockType.values().length) {
                    throw new IOException("Invalid inventory size in world save: " + inventorySize);
                }
                for (int index = 0; index < inventorySize; index++) {
                    BlockType type = readSolidBlockType(input.readInt());
                    int count = input.readInt();
                    if (count <= 0 || inventory.putIfAbsent(type, count) != null) {
                        throw new IOException("Invalid inventory entry in world save");
                    }
                }
            }
            return Optional.of(new SavedWorld(world, playerPosition, inventory));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid world save: " + path, exception);
        }
    }

    private static BlockType readSolidBlockType(int blockId) throws IOException {
        try {
            BlockType type = BlockType.fromId(blockId);
            if (!type.isSolid()) {
                throw new IOException("Invalid non-solid block type in world save");
            }
            return type;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid block id in world save", exception);
        }
    }

    public record SavedWorld(World world, Vector3f playerPosition, Map<BlockType, Integer> inventory) {
        public SavedWorld {
            inventory = Map.copyOf(inventory);
        }
    }
}
