package com.blocksbound.engine.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blocksbound.engine.render.Camera;
import com.blocksbound.engine.world.BlockType;
import com.blocksbound.engine.world.Chunk;
import com.blocksbound.engine.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PlayerCollisionTest {
    @Test
    void inventoryStacksCanExceedSixtyFourAndDisappearWhenEmptied() {
        Player player = new Player(new World(1L), new Camera(), null);

        player.addToInventory(BlockType.STONE, 100);

        assertEquals(100, player.getInventoryCount(BlockType.STONE));
        assertTrue(player.removeFromInventory(BlockType.STONE, 100));
        assertEquals(0, player.getInventoryCount(BlockType.STONE));
        assertFalse(player.removeFromInventory(BlockType.STONE, 1));
    }

    @Test
    void standingOnGroundDoesNotCountAsOverlappingTheGroundBlock() {
        World world = emptyWorld();
        world.setBlockAtWorld(0, 20, 0, BlockType.STONE.getId());

        assertFalse(Player.intersectsWorld(world, new Vector3f(0.5f, 20.0f + 1.62f + 1.0f, 0.5f)));
    }

    @Test
    void smallGroundContactPenetrationDoesNotBlockHorizontalMovement() {
        World world = emptyWorld();
        world.setBlockAtWorld(0, 20, 0, BlockType.STONE.getId());

        assertFalse(Player.intersectsWorld(world, new Vector3f(0.5f, 20.0f + 1.62f + 0.995f, 0.5f)));
    }

    @Test
    void realPenetrationIntoGroundStillCollides() {
        World world = emptyWorld();
        world.setBlockAtWorld(0, 20, 0, BlockType.STONE.getId());

        assertTrue(Player.intersectsWorld(world, new Vector3f(0.5f, 20.0f + 1.62f + 0.95f, 0.5f)));
    }

    private static World emptyWorld() {
        World world = new World(1L);
        world.putLoadedChunk(0, 0, new Chunk());
        return world;
    }
}
