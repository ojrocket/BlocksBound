package com.blocksbound.engine.render;

import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.window.Window;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

public class Renderer {
    private final Window window;
    private final Camera camera = new Camera();
    private Shader shader;
    private Mesh mesh;

    public Renderer(Window window) {
        this.window = window;
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glClearColor(0.2f, 0.4f, 0.8f, 1.0f);
    }

    public void init() {
        String vertexSource = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec2 aTex;
            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform mat4 uModel;
            out vec2 vTex;
            void main() {
                vTex = aTex;
                gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
            }
            """;

        String fragmentSource = """
            #version 330 core
            in vec2 vTex;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vTex.x, vTex.y, 0.5, 1.0);
            }
            """;

        shader = new Shader(vertexSource, fragmentSource);
        float[] vertices = {
            -0.5f, -0.5f, 0.5f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.5f, 1.0f, 0.0f,
             0.5f,  0.5f, 0.5f, 1.0f, 1.0f,
            -0.5f,  0.5f, 0.5f, 0.0f, 1.0f,

            -0.5f, -0.5f, -0.5f, 0.0f, 0.0f,
             0.5f, -0.5f, -0.5f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f, 1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f, 0.0f, 1.0f
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0,
            1, 5, 6, 6, 2, 1,
            5, 4, 7, 7, 6, 5,
            4, 0, 3, 3, 7, 4,
            3, 2, 6, 6, 7, 3,
            4, 5, 1, 1, 0, 4
        };

        mesh = new Mesh(vertices, indices);
    }

    public void render(Game game, float alpha) {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        int width = window.getWidth();
        int height = window.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        GL11.glViewport(0, 0, width, height);
        shader.use();
        int projLoc = shader.getUniformLocation("uProjection");
        int viewLoc = shader.getUniformLocation("uView");
        int modelLoc = shader.getUniformLocation("uModel");
        camera.updateProjection(width, height);
        camera.updateView();
        org.lwjgl.opengl.GL20.glUniformMatrix4fv(projLoc, false, camera.getProjectionMatrix().get(new float[16]));
        org.lwjgl.opengl.GL20.glUniformMatrix4fv(viewLoc, false, camera.getViewMatrix().get(new float[16]));
        org.lwjgl.opengl.GL20.glUniformMatrix4fv(modelLoc, false, new Matrix4f().get(new float[16]));
        mesh.render();
        game.render();
    }

    public Camera getCamera() {
        return camera;
    }

    public void shutdown() {
        if (mesh != null) {
            mesh.close();
        }
        if (shader != null) {
            shader.close();
        }
    }
}
