package com.blocksbound.engine.player;

import com.blocksbound.engine.input.Input;
import com.blocksbound.engine.render.Camera;
import com.blocksbound.engine.world.BlockType;
import com.blocksbound.engine.world.World;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Player {
    private static final float EYE_HEIGHT = 1.62f;
    private static final float BODY_HEIGHT = 1.8f;
    private static final float RADIUS = 0.30f;
    private static final float COLLISION_EPSILON = 0.01f;
    private static final float GRAVITY = 24.0f;
    private static final float JUMP_SPEED = 8.5f;

    private final World world;
    private final Camera camera;
    private final Input input;
    private final Vector3f position = new Vector3f();
    private float verticalVelocity;
    private boolean grounded;

    public Player(World world, Camera camera, Input input) {
        this.world = world;
        this.camera = camera;
        this.input = input;
        position.set(camera.getPosition());
    }

    public void update(float deltaSeconds) {
        camera.yaw += (float) input.getMouseDeltaX() * 0.16f;
        camera.pitch -= (float) input.getMouseDeltaY() * 0.16f;
        camera.pitch = Math.max(-89.0f, Math.min(89.0f, camera.pitch));
        camera.updateDirection();

        float forwardX = camera.getFront().x;
        float forwardZ = camera.getFront().z;
        float length = (float) Math.sqrt(forwardX * forwardX + forwardZ * forwardZ);
        if (length > 0.0f) {
            forwardX /= length;
            forwardZ /= length;
        }
        float rightX = -forwardZ;
        float rightZ = forwardX;
        float moveX = 0.0f;
        float moveZ = 0.0f;
        if (input.isKeyDown(GLFW.GLFW_KEY_W)) {
            moveX += forwardX;
            moveZ += forwardZ;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_S)) {
            moveX -= forwardX;
            moveZ -= forwardZ;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_D)) {
            moveX += rightX;
            moveZ += rightZ;
        }
        if (input.isKeyDown(GLFW.GLFW_KEY_A)) {
            moveX -= rightX;
            moveZ -= rightZ;
        }
        float moveLength = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
        if (moveLength > 0.0f) {
            float speed = input.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) ? 8.0f : 5.5f;
            moveAxis(moveX / moveLength * speed * deltaSeconds, true);
            moveAxis(moveZ / moveLength * speed * deltaSeconds, false);
        }

        if (grounded && input.wasKeyPressed(GLFW.GLFW_KEY_SPACE)) {
            verticalVelocity = JUMP_SPEED;
            grounded = false;
        }
        verticalVelocity -= GRAVITY * deltaSeconds;
        float nextY = position.y + verticalVelocity * deltaSeconds;
        position.y = nextY;
        if (intersectsWorld()) {
            position.y -= verticalVelocity * deltaSeconds;
            if (verticalVelocity < 0.0f) {
                grounded = true;
            }
            verticalVelocity = 0.0f;
        } else {
            grounded = false;
        }
        camera.getPosition().set(position);
    }

    public Vector3f getPosition() {
        return new Vector3f(position);
    }

    public void setPosition(Vector3f newPosition) {
        position.set(newPosition);
        verticalVelocity = 0.0f;
        grounded = intersectsWorld(world, new Vector3f(position).add(0.0f, -COLLISION_EPSILON * 2.0f, 0.0f));
        camera.getPosition().set(position);
    }

    public boolean occupiesBlock(int blockX, int blockY, int blockZ) {
        float feet = position.y - EYE_HEIGHT;
        float head = feet + BODY_HEIGHT;
        return position.x + RADIUS > blockX && position.x - RADIUS < blockX + 1
                && head > blockY && feet < blockY + 1
                && position.z + RADIUS > blockZ && position.z - RADIUS < blockZ + 1;
    }

    private void moveAxis(float amount, boolean xAxis) {
        if (xAxis) {
            position.x += amount;
        } else {
            position.z += amount;
        }
        if (intersectsWorld()) {
            if (xAxis) {
                position.x -= amount;
            } else {
                position.z -= amount;
            }
        }
    }

    private boolean intersectsWorld() {
        return intersectsWorld(world, position);
    }

    static boolean intersectsWorld(World world, Vector3f position) {
        float feet = position.y - EYE_HEIGHT;
        float head = feet + BODY_HEIGHT;
        int minX = (int) Math.floor(position.x - RADIUS + COLLISION_EPSILON);
        int maxX = (int) Math.floor(position.x + RADIUS - COLLISION_EPSILON);
        int minY = (int) Math.floor(feet + COLLISION_EPSILON);
        int maxY = (int) Math.floor(head - COLLISION_EPSILON);
        int minZ = (int) Math.floor(position.z - RADIUS + COLLISION_EPSILON);
        int maxZ = (int) Math.floor(position.z + RADIUS - COLLISION_EPSILON);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int block = world.getBlockAtWorld(x, y, z);
                    if (block != BlockType.AIR.getId() && BlockType.fromId(block).isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
