package com.blocksbound.game;

final class BlockBreakCooldown {
    private final float intervalSeconds;
    private float remainingSeconds;

    BlockBreakCooldown(float intervalSeconds) {
        if (!Float.isFinite(intervalSeconds) || intervalSeconds <= 0.0f) {
            throw new IllegalArgumentException("Break interval must be a positive finite duration");
        }
        this.intervalSeconds = intervalSeconds;
    }

    boolean shouldBreak(boolean buttonDown, boolean buttonPressed, boolean targetAvailable, float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException("Delta time must be a non-negative finite duration");
        }
        if (!buttonDown || !targetAvailable) {
            remainingSeconds = 0.0f;
            return false;
        }
        if (buttonPressed || remainingSeconds <= 0.0f) {
            remainingSeconds = intervalSeconds;
            return true;
        }
        remainingSeconds -= deltaSeconds;
        if (remainingSeconds <= 0.0f) {
            remainingSeconds = intervalSeconds;
            return true;
        }
        return false;
    }
}
