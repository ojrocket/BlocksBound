package com.blocksbound.engine.core;

public class Time {
    private double lastFrameTimeNanos;
    private double accumulator;
    private double lastFpsCheck;
    private int frames;

    public Time() {
        lastFrameTimeNanos = System.nanoTime();
        lastFpsCheck = lastFrameTimeNanos;
    }

    public double update() {
        double now = System.nanoTime();
        double deltaSeconds = (now - lastFrameTimeNanos) / 1_000_000_000.0;
        lastFrameTimeNanos = now;
        return Math.min(deltaSeconds, 0.25);
    }

    public double getAccumulator() {
        return accumulator;
    }

    public void addAccumulator(double deltaSeconds) {
        accumulator += deltaSeconds;
    }

    public void consumeFixedStep(double fixedDeltaSeconds) {
        accumulator -= fixedDeltaSeconds;
    }

    public double getFps() {
        return frames;
    }

    public void updateFrameCounter(double currentSeconds) {
        frames++;
        if (currentSeconds - lastFpsCheck >= 1.0) {
            lastFpsCheck = currentSeconds;
            frames = 0;
        }
    }
}
