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
import java.util.List;
import java.util.Optional;
import org.joml.Vector3f;

public final class WorldStorage {
    private static final int MAGIC = 0x42424C4B;
    private static final int VERSION = 1;
    private static final int MAX_CHUNKS = 100_000;

    private WorldStorage() {
    }

    public static Path defaultSavePath() {
        return Path.of(System.getProperty("user.home"), ".blocksbound", "world.dat");
    }

    public static void save(Path path, World world, Vector3f playerPosition) throws IOException {
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
            if (version != VERSION) {
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
            return Optional.of(new SavedWorld(world, playerPosition));
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid world save: " + path, exception);
        }
    }

    public record SavedWorld(World world, Vector3f playerPosition) {
    }
}
