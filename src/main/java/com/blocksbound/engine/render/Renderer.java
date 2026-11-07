package com.blocksbound.engine.render;

import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.window.Window;
import com.blocksbound.engine.world.Chunk;
import com.blocksbound.engine.world.ChunkMesher;
import com.blocksbound.engine.world.BlockHit;
import com.blocksbound.engine.world.BlockDrop;
import com.blocksbound.engine.world.BlockType;
import com.blocksbound.engine.world.World;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class Renderer {
    private static final int HOTBAR_SLOT_COUNT = 10;
    private static final float HOTBAR_SLOT_SIZE = 0.105f;
    private static final float HOTBAR_SLOT_GAP = 0.008f;
    private static final float HOTBAR_BOTTOM = -0.94f;

    private final Window window;
    private final Camera camera = new Camera();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Map<Long, Mesh> chunkMeshes = new HashMap<>();
    private final Map<BlockType, Mesh> droppedBlockMeshes = new EnumMap<>(BlockType.class);
    private final Map<Integer, Mesh> digitMeshes = new HashMap<>();
    private Shader shader;
    private Mesh blockHighlight;
    private Mesh crosshair;
    private Mesh crosshairOutline;
    private Mesh breakProgressBackground;
    private Mesh breakProgressFill;
    private Mesh hotbarSlotBackground;
    private Mesh hotbarSelection;
    private BlockType[] hotbarBlocks = new BlockType[HOTBAR_SLOT_COUNT];
    private int[] hotbarCounts = new int[HOTBAR_SLOT_COUNT];
    private int selectedHotbarSlot;
    private int projectionLocation;
    private int viewLocation;
    private int modelLocation;
    private float breakProgress;
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
        for (BlockType type : BlockType.values()) {
            if (type.isSolid()) {
                droppedBlockMeshes.put(type, new Mesh(ChunkMesher.buildBlockCube(type)));
            }
        }
        crosshairOutline = new Mesh(createCrosshair(0.015f, 0.004f, 0.025f, 0.0f, 0.0f, 0.0f),
                GL11.GL_LINES);
        crosshair = new Mesh(createCrosshair(0.015f, 0.004f, 0.025f, 1.0f, 1.0f, 1.0f),
                GL11.GL_LINES);
        breakProgressBackground = new Mesh(createBar(-0.035f, -0.028f, 0.035f, -0.022f,
                0.08f, 0.08f, 0.08f));
        breakProgressFill = new Mesh(createBar(-0.035f, -0.028f, 0.035f, -0.022f,
                0.35f, 0.9f, 0.3f));
        hotbarSlotBackground = new Mesh(createBar(-0.5f, -0.5f, 0.5f, 0.5f,
                0.12f, 0.12f, 0.12f));
        hotbarSelection = new Mesh(createRectangleOutline(), GL11.GL_LINES);
        for (int digit = 0; digit <= 9; digit++) {
            digitMeshes.put(digit, new Mesh(createDigit(digit), GL11.GL_LINES));
        }
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
        renderCrosshair(width, height, breakProgress);
    }

    public void renderWorld(World world, int centerChunkX, int centerChunkZ, int renderRadius,
            Optional<BlockHit> highlightedBlock, float breakProgress, BlockType[] hotbarBlocks,
            int[] hotbarCounts, int selectedHotbarSlot) {
        if (hotbarBlocks.length != HOTBAR_SLOT_COUNT || hotbarCounts.length != HOTBAR_SLOT_COUNT
                || selectedHotbarSlot < 0 || selectedHotbarSlot >= HOTBAR_SLOT_COUNT) {
            throw new IllegalArgumentException("Hotbar must contain ten slots and a valid selection");
        }
        this.breakProgress = Math.max(0.0f, Math.min(1.0f, breakProgress));
        this.hotbarBlocks = hotbarBlocks.clone();
        this.hotbarCounts = hotbarCounts.clone();
        this.selectedHotbarSlot = selectedHotbarSlot;
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
        for (BlockDrop drop : world.getDroppedBlocks()) {
            int dropChunkX = Math.floorDiv((int) Math.floor(drop.x()), Chunk.SIZE);
            int dropChunkZ = Math.floorDiv((int) Math.floor(drop.z()), Chunk.SIZE);
            if (Math.abs(dropChunkX - centerChunkX) > renderRadius
                    || Math.abs(dropChunkZ - centerChunkZ) > renderRadius) {
                continue;
            }
            modelMatrix.identity().translation(drop.x() - 0.15f, drop.y() - 0.15f, drop.z() - 0.15f)
                    .scale(0.3f);
            GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
            droppedBlockMeshes.get(drop.type()).render();
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

    private void renderCrosshair(int width, int height, float breakProgress) {
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
        if (breakProgress > 0.0f) {
            modelMatrix.identity().scaling((float) height / width, 1.0f, 1.0f);
            GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
            breakProgressBackground.render();
            modelMatrix.identity()
                    .translation(-0.035f * ((float) height / width) * (1.0f - breakProgress), 0.0f, 0.0f)
                    .scaling((float) height / width * breakProgress, 1.0f, 1.0f);
            GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
            breakProgressFill.render();
        }
        renderHotbar(width);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private void renderHotbar(int width) {
        float aspectScale = (float) window.getHeight() / width;
        float totalWidth = HOTBAR_SLOT_COUNT * HOTBAR_SLOT_SIZE + (HOTBAR_SLOT_COUNT - 1) * HOTBAR_SLOT_GAP;
        float left = -totalWidth * 0.5f;
        float top = HOTBAR_BOTTOM + HOTBAR_SLOT_SIZE;
        for (int slot = 0; slot < HOTBAR_SLOT_COUNT; slot++) {
            float slotLeft = left + slot * (HOTBAR_SLOT_SIZE + HOTBAR_SLOT_GAP);
            float centerX = slotLeft + HOTBAR_SLOT_SIZE * 0.5f;
            float centerY = HOTBAR_BOTTOM + HOTBAR_SLOT_SIZE * 0.5f;
            modelMatrix.identity()
                    .translation(centerX * aspectScale, centerY, 0.0f)
                    .scale(HOTBAR_SLOT_SIZE * aspectScale, HOTBAR_SLOT_SIZE, 1.0f);
            GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
            hotbarSlotBackground.render();

            if (slot == selectedHotbarSlot) {
                modelMatrix.identity()
                        .translation(centerX * aspectScale, centerY, 0.0f)
                        .scale(HOTBAR_SLOT_SIZE * aspectScale, HOTBAR_SLOT_SIZE, 1.0f);
                GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
                GL11.glLineWidth(2.0f);
                hotbarSelection.render();
                GL11.glLineWidth(1.0f);
            }

            renderDigit(slot == 9 ? 0 : slot + 1, (slotLeft + 0.017f) * aspectScale,
                    top - 0.018f, 0.018f * aspectScale, 0.026f);

            BlockType block = hotbarBlocks[slot];
            if (block != null) {
                float iconSize = 0.038f;
                modelMatrix.identity()
                        .translation((centerX - iconSize * 0.5f) * aspectScale,
                                centerY - iconSize * 0.5f, 0.025f)
                        .scale(iconSize * aspectScale, iconSize, iconSize);
                GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
                droppedBlockMeshes.get(block).render();
                if (hotbarCounts[slot] > 0) {
                    renderCount(hotbarCounts[slot], (slotLeft + HOTBAR_SLOT_SIZE - 0.008f) * aspectScale,
                            HOTBAR_BOTTOM + 0.014f, aspectScale);
                }
            }
        }
    }

    private void renderCount(int count, float right, float centerY, float aspectScale) {
        String digits = Integer.toString(count);
        float digitWidth = 0.012f;
        float digitHeight = 0.020f;
        float gap = 0.001f;
        float totalWidth = digits.length() * digitWidth + (digits.length() - 1) * gap;
        float start = right - totalWidth * aspectScale;
        for (int index = 0; index < digits.length(); index++) {
            int digit = digits.charAt(index) - '0';
            renderDigit(digit, start + (index * (digitWidth + gap) + digitWidth * 0.5f) * aspectScale,
                    centerY, digitWidth * aspectScale, digitHeight);
        }
    }

    private void renderDigit(int digit, float centerX, float centerY, float width, float height) {
        modelMatrix.identity().translation(centerX, centerY, 0.0f).scale(width, height, 1.0f);
        GL20.glUniformMatrix4fv(modelLocation, false, modelMatrix.get(new float[16]));
        digitMeshes.get(digit).render();
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

    private static float[] createBar(float left, float bottom, float right, float top,
            float red, float green, float blue) {
        float[] vertices = new float[6 * 6];
        float[][] positions = {
            {left, bottom}, {right, bottom}, {right, top},
            {left, bottom}, {right, top}, {left, top}
        };
        for (int vertex = 0; vertex < positions.length; vertex++) {
            int offset = vertex * 6;
            vertices[offset] = positions[vertex][0];
            vertices[offset + 1] = positions[vertex][1];
            vertices[offset + 2] = 0.025f;
            vertices[offset + 3] = red;
            vertices[offset + 4] = green;
            vertices[offset + 5] = blue;
        }
        return vertices;
    }

    private static float[] createRectangleOutline() {
        float[] points = {
            -0.5f, -0.5f, 0.5f, -0.5f,
            0.5f, -0.5f, 0.5f, 0.5f,
            0.5f, 0.5f, -0.5f, 0.5f,
            -0.5f, 0.5f, -0.5f, -0.5f
        };
        float[] vertices = new float[points.length / 2 * 6];
        for (int vertex = 0; vertex < points.length / 2; vertex++) {
            int pointOffset = vertex * 2;
            int vertexOffset = vertex * 6;
            vertices[vertexOffset] = points[pointOffset];
            vertices[vertexOffset + 1] = points[pointOffset + 1];
            vertices[vertexOffset + 2] = 0.025f;
            vertices[vertexOffset + 3] = 1.0f;
            vertices[vertexOffset + 4] = 0.85f;
            vertices[vertexOffset + 5] = 0.12f;
        }
        return vertices;
    }

    private static float[] createDigit(int digit) {
        boolean[][] segments = {
            {true, true, true, true, true, true, false},
            {false, true, true, false, false, false, false},
            {true, true, false, true, true, false, true},
            {true, true, true, true, false, false, true},
            {false, true, true, false, false, true, true},
            {true, false, true, true, false, true, true},
            {true, false, true, true, true, true, true},
            {true, true, true, false, false, false, false},
            {true, true, true, true, true, true, true},
            {true, true, true, true, false, true, true}
        };
        float[][] endpoints = {
            {-0.38f, 0.45f, 0.38f, 0.45f},
            {0.38f, 0.45f, 0.38f, 0.04f},
            {0.38f, -0.04f, 0.38f, -0.45f},
            {-0.38f, -0.45f, 0.38f, -0.45f},
            {-0.38f, -0.04f, -0.38f, -0.45f},
            {-0.38f, 0.45f, -0.38f, 0.04f},
            {-0.38f, 0.0f, 0.38f, 0.0f}
        };
        float[] vertices = new float[7 * 2 * 6];
        int vertex = 0;
        for (int segment = 0; segment < 7; segment++) {
            if (!segments[digit][segment]) {
                continue;
            }
            for (int point = 0; point < 2; point++) {
                int endpointOffset = point * 2;
                int vertexOffset = vertex++ * 6;
                vertices[vertexOffset] = endpoints[segment][endpointOffset];
                vertices[vertexOffset + 1] = endpoints[segment][endpointOffset + 1];
                vertices[vertexOffset + 2] = 0.025f;
                vertices[vertexOffset + 3] = 1.0f;
                vertices[vertexOffset + 4] = 1.0f;
                vertices[vertexOffset + 5] = 1.0f;
            }
        }
        return java.util.Arrays.copyOf(vertices, vertex * 6);
    }

    public void shutdown() {
        for (Mesh mesh : chunkMeshes.values()) {
            mesh.close();
        }
        chunkMeshes.clear();
        for (Mesh mesh : droppedBlockMeshes.values()) {
            mesh.close();
        }
        droppedBlockMeshes.clear();
        if (blockHighlight != null) {
            blockHighlight.close();
        }
        if (crosshair != null) {
            crosshair.close();
        }
        if (crosshairOutline != null) {
            crosshairOutline.close();
        }
        if (breakProgressBackground != null) {
            breakProgressBackground.close();
        }
        if (breakProgressFill != null) {
            breakProgressFill.close();
        }
        if (hotbarSlotBackground != null) {
            hotbarSlotBackground.close();
        }
        if (hotbarSelection != null) {
            hotbarSelection.close();
        }
        for (Mesh mesh : digitMeshes.values()) {
            mesh.close();
        }
        digitMeshes.clear();
        if (shader != null) {
            shader.close();
        }
    }
}
