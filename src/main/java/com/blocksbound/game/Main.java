package com.blocksbound.game;

import com.blocksbound.engine.core.Engine;
import com.blocksbound.engine.core.EngineConfig;
import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.input.Input;
import com.blocksbound.engine.render.Camera;
import com.blocksbound.engine.window.Window;
import org.lwjgl.glfw.GLFW;

public class Main implements Game {
    private final Engine engine;
    private final Camera camera;
    private final Input input;
    private float moveSpeed = 5.0f;

    public Main() {
        this.engine = new Engine(EngineConfig.defaults(), this);
        this.camera = engine.getRenderer().getCamera();
        this.input = engine.getInput();
    }

    @Override
    public void init() {
        input.setCursorMode(GLFW.GLFW_CURSOR_DISABLED);
        input.setCursorPosition(640, 360);
    }

    @Override
    public void update(float deltaSeconds) {
        if (input.wasKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            engine.getWindow().setWindowShouldClose(true);
        }

        double dx = input.getMouseDeltaX();
        double dy = input.getMouseDeltaY();
        camera.yaw += dx * 0.16f;
        camera.pitch -= dy * 0.16f;
        camera.pitch = Math.max(-89.0f, Math.min(89.0f, camera.pitch));
        camera.updateDirection();

        float speed = moveSpeed * deltaSeconds;
        if (input.isKeyDown(GLFW.GLFW_KEY_W)) {
            camera.getPosition().add((float) (camera.getFront().x * speed), (float) (camera.getFront().y * speed), (float) (camera.getFront().z * speed));
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_S)) {
            camera.getPosition().sub((float) (camera.getFront().x * speed), (float) (camera.getFront().y * speed), (float) (camera.getFront().z * speed));
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_A)) {
            var right = camera.getFront().cross(new org.joml.Vector3f(0.0f, 1.0f, 0.0f), new org.joml.Vector3f());
            camera.getPosition().sub(right.x * speed, right.y * speed, right.z * speed);
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_D)) {
            var right = camera.getFront().cross(new org.joml.Vector3f(0.0f, 1.0f, 0.0f), new org.joml.Vector3f());
            camera.getPosition().add(right.x * speed, right.y * speed, right.z * speed);
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            camera.getPosition().add(0.0f, speed, 0.0f);
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT)) {
            camera.getPosition().add(0.0f, -speed, 0.0f);
        }
    }

    @Override
    public void render() {
    }

    @Override
    public void shutdown() {
        System.out.println("BlocksBound shutting down");
    }

    public static void main(String[] args) {
        new Main().engine.start();
    }
}
