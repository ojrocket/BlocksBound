package com.blocksbound.engine.render;

import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.window.Window;
import com.blocksbound.engine.world.Chunk;
import com.blocksbound.engine.world.ChunkMesher;
import com.blocksbound.engine.world.BlockHit;
import com.blocksbound.engine.world.World;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class Renderer {
    private final Window window;
    private final Camera camera = new Camera();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Map<Long, Mesh> chunkMeshes = new HashMap<>();
    private Shader shader;
    private Mesh blockHighlight;
    private Mesh crosshair;
    private Mesh crosshairOutline;
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
        blockHighlight = new Mesh(createBlockOutline(), GL11.GL_LINES);
        crosshairOutline = new Mesh(createCrosshair(0.015f, 0.004f, 0.025f, 0.0f, 0.0f, 0.0f),
                GL11.GL_LINES);
        crosshair = new Mesh(createCrosshair(0.015f, 0.004f, 0.025f, 1.0f, 1.0f, 1.0f),
                GL11.GL_LINES);
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
        renderCrosshair(width, height);
    }

    public void renderWorld(World world, int centerChunkX, int centerChunkZ, int renderRadius,
            Optional<BlockHit> highlightedBlock) {
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
        highlightedBlock.ifPresent(this::renderBlockHighlight);
    }

    public Camera getCamera() {
        return camera;
    }

    private void renderBlockHighlight(BlockHit hit) {
        shader.use();
        float expansion = 0.003f;
        modelMatrix.identity()
                .translation(hit.x() - expansion, hit.y() - expansion, hit.z() - expansion)
                .scale(1.0f + expansion * 2.0f);
        GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
        GL11.glLineWidth(2.0f);
        blockHighlight.render();
        GL11.glLineWidth(1.0f);
    }

    private void renderCrosshair(int width, int height) {
        shader.use();
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL20.glUniformMatrix4fv(projectionLocation, false, new Matrix4f().get(new float[16]));
        GL20.glUniformMatrix4fv(viewLocation, false, new Matrix4f().get(new float[16]));
        modelMatrix.identity().scaling((float) height / width, 1.0f, 1.0f);
        GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
        GL11.glLineWidth(4.0f);
        crosshairOutline.render();
        GL11.glLineWidth(2.0f);
        crosshair.render();
        GL11.glLineWidth(1.0f);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private static float[] createBlockOutline() {
        float[] vertices = new float[12 * 2 * 6];
        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},
            {4, 5}, {5, 6}, {6, 7}, {7, 4},
            {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        float[][] corners = {
            {0.0f, 0.0f, 0.0f}, {1.0f, 0.0f, 0.0f},
            {1.0f, 1.0f, 0.0f}, {0.0f, 1.0f, 0.0f},
            {0.0f, 0.0f, 1.0f}, {1.0f, 0.0f, 1.0f},
            {1.0f, 1.0f, 1.0f}, {0.0f, 1.0f, 1.0f}
        };
        int offset = 0;
        for (int[] edge : edges) {
            for (int cornerIndex : edge) {
                float[] corner = corners[cornerIndex];
                vertices[offset++] = corner[0];
                vertices[offset++] = corner[1];
                vertices[offset++] = corner[2];
                vertices[offset++] = 1.0f;
                vertices[offset++] = 0.88f;
                vertices[offset++] = 0.12f;
            }
        }
        return vertices;
    }

    private static float[] createCrosshair(float arm, float gap, float z, float red, float green, float blue) {
        float[] positions = {
            -arm, 0.0f, z, -gap, 0.0f, z,
            gap, 0.0f, z, arm, 0.0f, z,
            0.0f, -arm, z, 0.0f, -gap, z,
            0.0f, gap, z, 0.0f, arm, z
        };
        float[] vertices = new float[positions.length / 3 * 6];
        for (int vertex = 0; vertex < positions.length / 3; vertex++) {
            int positionOffset = vertex * 3;
            int vertexOffset = vertex * 6;
            vertices[vertexOffset] = positions[positionOffset];
            vertices[vertexOffset + 1] = positions[positionOffset + 1];
            vertices[vertexOffset + 2] = positions[positionOffset + 2];
            vertices[vertexOffset + 3] = red;
            vertices[vertexOffset + 4] = green;
            vertices[vertexOffset + 5] = blue;
        }
        return vertices;
    }

    public void shutdown() {
        for (Mesh mesh : chunkMeshes.values()) {
            mesh.close();
        }
        chunkMeshes.clear();
        if (blockHighlight != null) {
            blockHighlight.close();
        }
        if (crosshair != null) {
            crosshair.close();
        }
        if (crosshairOutline != null) {
            crosshairOutline.close();
        }
        if (shader != null) {
            shader.close();
        }
    }
}
