package com.blocksbound.game;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BlockBreakCooldownTest {
    @Test
    void holdingBreaksImmediatelyThenOnceEveryFiftyMilliseconds() {
        BlockBreakCooldown cooldown = new BlockBreakCooldown(0.05f);

        assertTrue(cooldown.shouldBreak(true, true, true, 1.0f / 60.0f));
        assertFalse(cooldown.shouldBreak(true, false, true, 1.0f / 60.0f));
        assertFalse(cooldown.shouldBreak(true, false, true, 1.0f / 60.0f));
        assertTrue(cooldown.shouldBreak(true, false, true, 1.0f / 60.0f));
        assertFalse(cooldown.shouldBreak(true, false, true, 1.0f / 60.0f));
        assertFalse(cooldown.shouldBreak(true, false, true, 1.0f / 60.0f));
        assertTrue(cooldown.shouldBreak(true, false, true, 1.0f / 60.0f));
    }

    @Test
    void releasingOrLosingTheTargetResetsTheCooldown() {
        BlockBreakCooldown cooldown = new BlockBreakCooldown(0.05f);

        assertTrue(cooldown.shouldBreak(true, true, true, 0.0f));
        assertFalse(cooldown.shouldBreak(true, false, true, 0.01f));
        assertFalse(cooldown.shouldBreak(false, false, true, 0.01f));
        assertTrue(cooldown.shouldBreak(true, true, true, 0.0f));
        assertFalse(cooldown.shouldBreak(true, false, false, 0.01f));
        assertTrue(cooldown.shouldBreak(true, false, true, 0.0f));
    }
}
