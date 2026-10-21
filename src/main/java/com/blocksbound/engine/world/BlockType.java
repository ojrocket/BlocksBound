package com.blocksbound.engine.world;

public enum BlockType {
    AIR(0),
    GRASS(1),
    DIRT(2),
    STONE(3),
    SAND(4),
    WOOD(5),
    LEAVES(6),
    WATER(7);

    private final int id;

    BlockType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
