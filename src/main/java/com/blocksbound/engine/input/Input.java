package com.blocksbound.engine.input;

import org.lwjgl.glfw.GLFW;

public class Input {
    private final long windowHandle;
    private final boolean[] keys = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] pressed = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] released = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private final boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];
    private final boolean[] mousePressed = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST + 1];

    private double mouseX;
    private double mouseY;
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
        GLFW.glfwSetMouseButtonCallback(windowHandle, (win, button, action, mods) -> {
            if (button >= 0 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST) {
                if (action == GLFW.GLFW_PRESS) {
                    mouseButtons[button] = true;
                    mousePressed[button] = true;
                } else if (action == GLFW.GLFW_RELEASE) {
                    mouseButtons[button] = false;
                }
            }
        });

        GLFW.glfwSetCursorPosCallback(windowHandle, (win, xPos, yPos) -> {
            mouseDeltaX += xPos - mouseX;
            mouseDeltaY += yPos - mouseY;
            mouseX = xPos;
            mouseY = yPos;
        });
    }

    public void endFrame() {
        for (int i = 0; i < keys.length; i++) {
            pressed[i] = false;
            released[i] = false;
        }
        for (int i = 0; i < mousePressed.length; i++) {
            mousePressed[i] = false;
        }
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
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

    public boolean isMouseButtonDown(int button) {
        return button >= 0 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST && mouseButtons[button];
    }

    public boolean wasMouseButtonPressed(int button) {
        return button >= 0 && button <= GLFW.GLFW_MOUSE_BUTTON_LAST && mousePressed[button];
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
        mouseDeltaX = 0.0;
        mouseDeltaY = 0.0;
    }
}
