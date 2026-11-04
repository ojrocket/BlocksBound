package com.blocksbound.game;

import com.blocksbound.engine.world.BlockHit;

final class BlockBreakProgress {
    private final float breakDurationSeconds;
    private float elapsedSeconds;
    private int targetX;
    private int targetY;
    private int targetZ;
    private boolean hasTarget;
    private boolean breakCompletedForPress;

    BlockBreakProgress(float breakDurationSeconds) {
        if (!Float.isFinite(breakDurationSeconds) || breakDurationSeconds <= 0.0f) {
            throw new IllegalArgumentException("Break duration must be a positive finite duration");
        }
        this.breakDurationSeconds = breakDurationSeconds;
    }

    boolean update(boolean buttonDown, BlockHit target, float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("Delta time must be a non-negative finite duration");
        }
        if (!buttonDown) {
            reset();
            return false;
        }
        if (breakCompletedForPress) {
            return false;
        }
        if (target == null) {
            clearTarget();
            return false;
        }
        if (!hasTarget || target.x() != targetX || target.y() != targetY || target.z() != targetZ) {
            targetX = target.x();
            targetY = target.y();
            targetZ = target.z();
            hasTarget = true;
            elapsedSeconds = 0.0f;
        }
        elapsedSeconds += deltaSeconds;
        if (elapsedSeconds < breakDurationSeconds) {
            return false;
        }
        breakCompletedForPress = true;
        elapsedSeconds = breakDurationSeconds;
        return true;
    }

    float getProgress() {
        return hasTarget && !breakCompletedForPress ? elapsedSeconds / breakDurationSeconds
                : breakCompletedForPress ? 1.0f : 0.0f;
    }

    private void clearTarget() {
        hasTarget = false;
        elapsedSeconds = 0.0f;
    }

    private void reset() {
        clearTarget();
        breakCompletedForPress = false;
    }
}
