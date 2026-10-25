package com.blocksbound.engine.world;

public record BlockHit(int x, int y, int z, int previousX, int previousY, int previousZ, float distance) {
}
