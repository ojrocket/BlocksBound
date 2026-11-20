package com.blocksbound.engine.core;

import java.util.Objects;
import java.util.function.LongSupplier;

public class Time {
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;
    private static final double MAX_DELTA_SECONDS = 0.25;
    private static final long FPS_UPDATE_INTERVAL_NANOS = 1_000_000_000L;

    private final LongSupplier nanoTime;
    private long previousFrameNanos;
    private long fpsWindowStartNanos;
    private int framesInFpsWindow;
    private double fps;

    public Time() {
        this(System::nanoTime);
    }

    Time(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        previousFrameNanos = nanoTime.getAsLong();
        fpsWindowStartNanos = previousFrameNanos;
    }

    public double update() {
        long now = nanoTime.getAsLong();
        long elapsedNanos = Math.max(0L, now - previousFrameNanos);
        previousFrameNanos = now;
        updateFps(now);
        return Math.min(elapsedNanos / NANOS_PER_SECOND, MAX_DELTA_SECONDS);
    }

    public double getFps() {
        return fps;
    }

    private void updateFps(long now) {
        framesInFpsWindow++;
        long elapsedNanos = now - fpsWindowStartNanos;
        if (elapsedNanos >= FPS_UPDATE_INTERVAL_NANOS) {
            fps = framesInFpsWindow * NANOS_PER_SECOND / elapsedNanos;
            framesInFpsWindow = 0;
            fpsWindowStartNanos = now;
        }
    }
}
