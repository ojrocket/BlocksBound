package com.blocksbound.engine.core;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EngineConfigTest {
    @Test
    void acceptsValidConfiguration() {
        assertDoesNotThrow(() -> new EngineConfig("BlocksBound", 1280, 720, true, 1.0 / 60.0));
    }

    @Test
    void rejectsBlankTitlesAndNonPositiveDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineConfig(" ", 1280, 720, true, 1.0 / 60.0));
        assertThrows(IllegalArgumentException.class,
                () -> new EngineConfig("BlocksBound", 0, 720, true, 1.0 / 60.0));
        assertThrows(IllegalArgumentException.class,
                () -> new EngineConfig("BlocksBound", 1280, -1, true, 1.0 / 60.0));
    }

    @Test
    void rejectsInvalidFixedStepDurations() {
        assertThrows(IllegalArgumentException.class,
                () -> new EngineConfig("BlocksBound", 1280, 720, true, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> new EngineConfig("BlocksBound", 1280, 720, true, Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> new EngineConfig("BlocksBound", 1280, 720, true, Double.POSITIVE_INFINITY));
    }
}
