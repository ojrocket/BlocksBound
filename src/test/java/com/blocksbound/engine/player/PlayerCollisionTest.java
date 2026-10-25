package com.blocksbound.engine.player;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blocksbound.engine.world.BlockType;
import com.blocksbound.engine.world.World;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class PlayerCollisionTest {
    @Test
    void standingOnGroundDoesNotCountAsOverlappingTheGroundBlock() {
        World world = new World(1L);
        world.setBlockAtWorld(0, 20, 0, BlockType.STONE.getId());

        assertFalse(Player.intersectsWorld(world, new Vector3f(0.5f, 20.0f + 1.62f + 1.0f, 0.5f)));
    }

    @Test
    void smallGroundContactPenetrationDoesNotBlockHorizontalMovement() {
        World world = new World(1L);
        world.setBlockAtWorld(0, 20, 0, BlockType.STONE.getId());

        assertFalse(Player.intersectsWorld(world, new Vector3f(0.5f, 20.0f + 1.62f + 0.995f, 0.5f)));
    }

    @Test
    void realPenetrationIntoGroundStillCollides() {
        World world = new World(1L);
        world.setBlockAtWorld(0, 20, 0, BlockType.STONE.getId());

        assertTrue(Player.intersectsWorld(world, new Vector3f(0.5f, 20.0f + 1.62f + 0.95f, 0.5f)));
    }
}
