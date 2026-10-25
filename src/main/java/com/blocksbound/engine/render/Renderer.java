package com.blocksbound.engine.render;

import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.window.Window;
import com.blocksbound.engine.world.Chunk;
import com.blocksbound.engine.world.ChunkMesher;
import com.blocksbound.engine.world.World;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class Renderer {
    private final Window window;
    private final Camera camera = new Camera();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Map<Long, Mesh> chunkMeshes = new HashMap<>();
    private Shader shader;
    private int projectionLocation;
    private int viewLocation;
    private int modelLocation;
    private World renderedWorld;

    public Renderer(Window window) {
        this.window = window;
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glClearColor(0.48f, 0.72f, 0.91f, 1.0f);
    }

    public void init() {
        String vertexSource = """
            #version 330 core
            layout(location = 0) in vec3 aPos;
            layout(location = 1) in vec3 aColor;
            uniform mat4 uProjection;
            uniform mat4 uView;
            uniform mat4 uModel;
            out vec3 vColor;
            void main() {
                vColor = aColor;
                gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
            }
            """;

        String fragmentSource = """
            #version 330 core
            in vec3 vColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vColor, 1.0);
            }
            """;

        shader = new Shader(vertexSource, fragmentSource);
        projectionLocation = shader.getUniformLocation("uProjection");
        viewLocation = shader.getUniformLocation("uView");
        modelLocation = shader.getUniformLocation("uModel");
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
        camera.updateProjection(width, height);
        camera.updateView();
        GL20.glUniformMatrix4fv(projectionLocation, false, camera.getProjectionMatrix().get(new float[16]));
        GL20.glUniformMatrix4fv(viewLocation, false, camera.getViewMatrix().get(new float[16]));
        game.render();
    }

    public void renderWorld(World world, int centerChunkX, int centerChunkZ, int renderRadius) {
        shader.use();
        if (renderedWorld != world) {
            for (Mesh mesh : chunkMeshes.values()) {
                mesh.close();
            }
            chunkMeshes.clear();
            renderedWorld = world;
        }
        Iterator<Map.Entry<Long, Mesh>> iterator = chunkMeshes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Mesh> entry = iterator.next();
            int chunkX = World.unpackChunkX(entry.getKey());
            int chunkZ = World.unpackChunkZ(entry.getKey());
            if (Math.abs(chunkX - centerChunkX) > renderRadius
                    || Math.abs(chunkZ - centerChunkZ) > renderRadius) {
                entry.getValue().close();
                iterator.remove();
            }
        }
        for (Map.Entry<Long, Chunk> entry : world.getLoadedChunks()) {
            long key = entry.getKey();
            int chunkX = World.unpackChunkX(key);
            int chunkZ = World.unpackChunkZ(key);
            if (Math.abs(chunkX - centerChunkX) > renderRadius
                    || Math.abs(chunkZ - centerChunkZ) > renderRadius) {
                continue;
            }
            Chunk chunk = entry.getValue();
            Mesh mesh = chunkMeshes.get(key);
            if (chunk.isDirty() || mesh == null) {
                if (mesh != null) {
                    mesh.close();
                }
                mesh = new Mesh(ChunkMesher.build(world, chunkX, chunkZ, chunk));
                chunkMeshes.put(key, mesh);
                chunk.markClean();
            }
            modelMatrix.identity().translation(chunkX * (float) Chunk.SIZE, 0.0f,
                    chunkZ * (float) Chunk.SIZE);
            GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
            mesh.render();
        }
    }

    public Camera getCamera() {
        return camera;
    }

    public void shutdown() {
        for (Mesh mesh : chunkMeshes.values()) {
            mesh.close();
        }
        chunkMeshes.clear();
        if (shader != null) {
            shader.close();
        }
    }
}
