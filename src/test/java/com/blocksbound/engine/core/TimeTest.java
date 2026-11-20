package com.blocksbound.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TimeTest {
    @Test
    void updateReturnsElapsedSecondsAndCapsLongFrames() {
        AtomicLong clock = new AtomicLong();
        Time time = new Time(clock::get);

        clock.set(100_000_000L);
        assertEquals(0.1, time.update(), 0.000001);

        clock.set(600_000_000L);
        assertEquals(0.25, time.update(), 0.000001);
    }

    @Test
    void updateIgnoresNegativeClockMovement() {
        AtomicLong clock = new AtomicLong(100L);
        Time time = new Time(clock::get);

        clock.set(50L);

        assertEquals(0.0, time.update());
    }

    @Test
    void reportsFramesPerSecondOncePerSecondWindowCompletes() {
        AtomicLong clock = new AtomicLong();
        Time time = new Time(clock::get);

        clock.set(500_000_000L);
        time.update();
        clock.set(1_000_000_000L);
        time.update();

        assertEquals(2.0, time.getFps(), 0.000001);
    }
}
