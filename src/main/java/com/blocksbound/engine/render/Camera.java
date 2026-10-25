package com.blocksbound.engine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private final Vector3f position = new Vector3f(0.0f, 1.6f, 5.0f);
    private final Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f);
    private final Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    public float yaw = -90.0f;
    public float pitch = 0.0f;
    public float fov = 70.0f;
    public float nearPlane = 0.1f;
    public float farPlane = 100.0f;

    public void updateProjection(int width, int height) {
        projectionMatrix.identity().perspective((float) Math.toRadians(fov), (float) width / (float) height, nearPlane, farPlane);
    }

    public void updateView() {
        viewMatrix.identity().lookAt(position, position.add(front, new Vector3f()), up);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getFront() {
        return front;
    }

    public void updateDirection() {
        front.x = (float) (Math.cos(Math.toRadians(pitch)) * Math.cos(Math.toRadians(yaw)));
        front.y = (float) Math.sin(Math.toRadians(pitch));
        front.z = (float) (Math.cos(Math.toRadians(pitch)) * Math.sin(Math.toRadians(yaw)));
        front.normalize();
    }
}
