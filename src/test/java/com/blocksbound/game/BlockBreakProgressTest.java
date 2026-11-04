package com.blocksbound.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.blocksbound.engine.world.BlockHit;
import org.junit.jupiter.api.Test;

class BlockBreakProgressTest {
    private static final BlockHit FIRST_BLOCK = new BlockHit(1, 2, 3, 0, 2, 3, 1.0f);
    private static final BlockHit SECOND_BLOCK = new BlockHit(2, 2, 3, 1, 2, 3, 1.0f);

    @Test
    void holdingBreakCompletesOneBlockAndRequiresReleaseForAnother() {
        BlockBreakProgress progress = new BlockBreakProgress(0.5f);

        assertFalse(progress.update(true, FIRST_BLOCK, 0.2f));
        assertEquals(0.4f, progress.getProgress(), 0.0001f);
        assertTrue(progress.update(true, FIRST_BLOCK, 0.3f));
        assertEquals(1.0f, progress.getProgress());
        assertFalse(progress.update(true, SECOND_BLOCK, 0.5f));

        assertFalse(progress.update(false, null, 0.0f));
        assertEquals(0.0f, progress.getProgress());
        assertFalse(progress.update(true, SECOND_BLOCK, 0.49f));
        assertTrue(progress.update(true, SECOND_BLOCK, 0.01f));
    }

    @Test
    void changingOrLosingTargetResetsProgress() {
        BlockBreakProgress progress = new BlockBreakProgress(0.5f);

        assertFalse(progress.update(true, FIRST_BLOCK, 0.3f));
        assertFalse(progress.update(true, SECOND_BLOCK, 0.2f));
        assertEquals(0.4f, progress.getProgress(), 0.0001f);
        assertFalse(progress.update(true, null, 0.1f));
        assertEquals(0.0f, progress.getProgress());
        assertFalse(progress.update(true, SECOND_BLOCK, 0.49f));
        assertTrue(progress.update(true, SECOND_BLOCK, 0.01f));
    }
}
