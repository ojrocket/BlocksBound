package com.blocksbound.engine.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorldSystemsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void terrainGenerationIsDeterministicForASeed() {
        Chunk first = TerrainGenerator.generate(1234L, -2, 7);
        Chunk second = TerrainGenerator.generate(1234L, -2, 7);

        assertArrayEquals(first.copyBlocks(), second.copyBlocks());
        assertTrue(first.getBlock(8, TerrainGenerator.heightAt(1234L, -24, 120), 8) != BlockType.AIR.getId());
    }

    @Test
    void chunkMesherEmitsOnlyExposedFaces() {
        World world = new World(5L);
        Chunk chunk = new Chunk();
        chunk.setBlock(4, 40, 4, BlockType.STONE.getId());
        chunk.setBlock(5, 40, 4, BlockType.STONE.getId());
        world.putLoadedChunk(0, 0, chunk);

        float[] vertices = ChunkMesher.build(world, 0, 0, chunk);

        assertEquals(60 * 6, vertices.length);
    }

    @Test
    void worldCoordinatesHandleNegativeChunksAndDirtyBorderMeshes() {
        World world = new World(7L);
        Chunk left = world.getChunk(-1, 0);
        Chunk right = world.getChunk(0, 0);
        left.markClean();
        right.markClean();
        int worldX = -1;
        int worldY = 40;
        int worldZ = 3;

        world.setBlockAtWorld(worldX, worldY, worldZ, BlockType.WOOD.getId());

        assertEquals(BlockType.WOOD.getId(), world.getBlockAtWorld(worldX, worldY, worldZ));
        assertTrue(left.isDirty());
        assertTrue(right.isDirty());
    }

    @Test
    void raycasterReturnsTheTargetAndPlacementCell() {
        World world = new World(9L);
        world.setBlockAtWorld(2, 50, 0, BlockType.STONE.getId());

        BlockHit hit = Raycaster.cast(world, new Vector3f(0.5f, 50.5f, 0.5f),
                new Vector3f(1.0f, 0.0f, 0.0f), 6.0f).orElseThrow();

        assertEquals(2, hit.x());
        assertEquals(1, hit.previousX());
        assertEquals(1.5f, hit.distance());
    }

    @Test
    void worldSaveRestoresSeedBlocksAndPlayerPosition() throws Exception {
        World world = new World(42L);
        world.setBlockAtWorld(-1, 40, 2, BlockType.LEAVES.getId());
        Vector3f playerPosition = new Vector3f(-4.5f, 31.25f, 12.0f);
        Path saveFile = temporaryDirectory.resolve("world.dat");

        WorldStorage.save(saveFile, world, playerPosition);
        WorldStorage.SavedWorld loaded = WorldStorage.load(saveFile).orElseThrow();

        assertEquals(42L, loaded.world().getSeed());
        assertEquals(BlockType.LEAVES.getId(), loaded.world().getBlockAtWorld(-1, 40, 2));
        assertEquals(playerPosition, loaded.playerPosition());
    }
}
