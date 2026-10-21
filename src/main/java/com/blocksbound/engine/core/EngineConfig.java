package com.blocksbound.engine.core;

public record EngineConfig(
        String title,
        int width,
        int height,
        boolean vsync,
        double fixedDeltaSeconds
) {
    public static EngineConfig defaults() {
        return new EngineConfig("BlocksBound", 1280, 720, true, 1.0 / 60.0);
    }
}
