package com.blocksbound.engine.world;

import java.util.Optional;
import org.joml.Vector3f;

public final class Raycaster {
    private Raycaster() {
    }

    public static Optional<BlockHit> cast(World world, Vector3f origin, Vector3f direction, float maxDistance) {
        int x = floor(origin.x);
        int y = floor(origin.y);
        int z = floor(origin.z);
        if (isSolid(world, x, y, z)) {
            return Optional.of(new BlockHit(x, y, z, x, y, z, 0.0f));
        }

        int stepX = Float.compare(direction.x, 0.0f);
        int stepY = Float.compare(direction.y, 0.0f);
        int stepZ = Float.compare(direction.z, 0.0f);
        float deltaX = stepX == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / direction.x);
        float deltaY = stepY == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / direction.y);
        float deltaZ = stepZ == 0 ? Float.POSITIVE_INFINITY : Math.abs(1.0f / direction.z);
        float maxX = initialBoundary(origin.x, x, direction.x, stepX, deltaX);
        float maxY = initialBoundary(origin.y, y, direction.y, stepY, deltaY);
        float maxZ = initialBoundary(origin.z, z, direction.z, stepZ, deltaZ);
        float distance = 0.0f;

        while (distance <= maxDistance) {
            int previousX = x;
            int previousY = y;
            int previousZ = z;
            if (maxX <= maxY && maxX <= maxZ) {
                distance = maxX;
                maxX += deltaX;
                x += stepX;
            } else if (maxY <= maxZ) {
                distance = maxY;
                maxY += deltaY;
                y += stepY;
            } else {
                distance = maxZ;
                maxZ += deltaZ;
                z += stepZ;
            }
            if (distance > maxDistance) {
                break;
            }
            if (isSolid(world, x, y, z)) {
                return Optional.of(new BlockHit(x, y, z, previousX, previousY, previousZ, distance));
            }
        }
        return Optional.empty();
    }

    private static boolean isSolid(World world, int x, int y, int z) {
        int block = world.getBlockAtWorld(x, y, z);
        return block != BlockType.AIR.getId() && BlockType.fromId(block).isSolid();
    }

    private static float initialBoundary(float origin, int cell, float direction, int step, float delta) {
        if (step > 0) {
            return (cell + 1.0f - origin) * delta;
        }
        if (step < 0) {
            return (origin - cell) * delta;
        }
        return Float.POSITIVE_INFINITY;
    }

    private static int floor(float value) {
        return (int) Math.floor(value);
    }
}
