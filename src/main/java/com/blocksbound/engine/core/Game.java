package com.blocksbound.engine.core;

public interface Game {
    void init();

    void update(float deltaSeconds);

    void render();

    void shutdown();
}
