package com.blocksbound.engine.input;

import org.lwjgl.glfw.GLFW;

public class Input {
    private final long windowHandle;
    private final boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] pressed = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] released = new boolean[GLFW.GLFW_KEY_LAST + 1];

    private double mouseX;
    private double mouseY;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    public Input(long windowHandle) {
        this.windowHandle = windowHandle;
        GLFW.glfwSetKeyCallback(windowHandle, (win, key, scancode, action, mods) -> {
            if (key >= 0 && key <= GLFW.GLFW_KEY_LAST) {
                if (action == GLFW.GLFW_PRESS) {
                    keys[key] = true;
                    pressed[key] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    keys[key] = false;
                    released[key] = true;
                }
            }
        });

        GLFW.glfwSetCursorPosCallback(windowHandle, (win, xPos, yPos) -> {
            mouseX = xPos;
            mouseY = yPos;
        });
    }

    public void beginFrame() {
        for (int i = 0; i < keys.length; i++) {
            pressed[i] = false;
            released[i] = false;
        }
        mouseDeltaX = mouseX - lastMouseX;
        mouseDeltaY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    public boolean isKeyDown(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW.GLFW_KEY_LAST && keys[keyCode];
    }

    public boolean wasKeyPressed(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW.GLFW_KEY_LAST && pressed[keyCode];
    }

    public boolean wasKeyReleased(int keyCode) {
        return keyCode >= 0 && keyCode <= GLFW.GLFW_KEY_LAST && released[keyCode];
    }

    public double getMouseDeltaX() {
        return mouseDeltaX;
    }

    public double getMouseDeltaY() {
        return mouseDeltaY;
    }

    public void setCursorMode(int cursorMode) {
        GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_CURSOR, cursorMode);
    }

    public void setCursorPosition(double x, double y) {
        GLFW.glfwSetCursorPos(windowHandle, x, y);
        mouseX = x;
        mouseY = y;
        lastMouseX = x;
        lastMouseY = y;
    }
}
