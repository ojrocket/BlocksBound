package com.blocksbound.engine.core;

public record EngineConfig(
        String title,
        int width,
        int height,
        boolean vsync,
        double fixedDeltaSeconds
) {
    public EngineConfig {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Window title must not be blank");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Window dimensions must be positive");
        }
        if (!Double.isFinite(fixedDeltaSeconds) || fixedDeltaSeconds <= 0.0) {
            throw new IllegalArgumentException("Fixed delta must be a positive finite duration");
        }
    }

    public static EngineConfig defaults() {
        return new EngineConfig("BlocksBound", 1280, 720, true, 1.0 / 60.0);
    }
}
