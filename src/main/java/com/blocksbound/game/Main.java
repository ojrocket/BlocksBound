package com.blocksbound.game;

import com.blocksbound.engine.core.Engine;
import com.blocksbound.engine.core.EngineConfig;
import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.input.Input;
import com.blocksbound.engine.player.Player;
import com.blocksbound.engine.render.Camera;
import com.blocksbound.engine.render.Renderer;
import com.blocksbound.engine.world.BlockHit;
import com.blocksbound.engine.world.BlockType;
import com.blocksbound.engine.world.Chunk;
import com.blocksbound.engine.world.Raycaster;
import com.blocksbound.engine.world.World;
import com.blocksbound.engine.world.WorldStorage;
import com.blocksbound.engine.world.WorldStorage.SavedWorld;
import java.io.IOException;
import java.util.Optional;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Main implements Game {
    private static final int LOAD_RADIUS = 5;
    private static final int RENDER_RADIUS = 4;
    private static final float REACH = 6.0f;
    private static final float BLOCK_BREAK_INTERVAL = 0.05f;

    private final Engine engine;
    private final Camera camera;
    private final Input input;
    private final Renderer renderer;
    private final java.nio.file.Path savePath = WorldStorage.defaultSavePath();
    private World world;
    private Player player;
    private final BlockBreakCooldown blockBreakCooldown = new BlockBreakCooldown(BLOCK_BREAK_INTERVAL);
    private int selectedBlock = BlockType.DIRT.getId();
    private int lastChunkX = Integer.MIN_VALUE;
    private int lastChunkZ = Integer.MIN_VALUE;

    public Main() {
        this.engine = new Engine(EngineConfig.defaults(), this);
        this.camera = engine.getRenderer().getCamera();
        this.input = engine.getInput();
        this.renderer = engine.getRenderer();
    }

    @Override
    public void init() {
        Optional<SavedWorld> saved;
        try {
            saved = WorldStorage.load(savePath);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load world save at " + savePath, exception);
        }

        if (saved.isPresent()) {
            world = saved.get().world();
            world.ensureChunksAround((int) Math.floor(saved.get().playerPosition().x),
                    (int) Math.floor(saved.get().playerPosition().z), LOAD_RADIUS);
            player = new Player(world, camera, input);
            player.setPosition(saved.get().playerPosition());
        } else {
            world = new World();
            world.ensureChunksAround(0, 0, LOAD_RADIUS);
            player = new Player(world, camera, input);
            player.setPosition(new Vector3f(0.5f, world.getSurfaceHeight(0, 0) + 2.62f, 0.5f));
        }

        input.setCursorMode(GLFW.GLFW_CURSOR_DISABLED);
        input.setCursorPosition(engine.getWindow().getWidth() / 2.0, engine.getWindow().getHeight() / 2.0);
        engine.getWindow().setTitle("BlocksBound | WASD move, Space jump, LMB break, RMB place, F5 save, F9 load");
    }

    @Override
    public void update(float deltaSeconds) {
        if (input.wasKeyPressed(GLFW.GLFW_KEY_ESCAPE)) {
            engine.getWindow().setWindowShouldClose(true);
        }
        if (input.wasKeyPressed(GLFW.GLFW_KEY_F5)) {
            saveWorld();
        }
        if (input.wasKeyPressed(GLFW.GLFW_KEY_F9)) {
            loadWorld();
        }

        selectBlock(GLFW.GLFW_KEY_1, BlockType.GRASS);
        selectBlock(GLFW.GLFW_KEY_2, BlockType.DIRT);
        selectBlock(GLFW.GLFW_KEY_3, BlockType.STONE);
        selectBlock(GLFW.GLFW_KEY_4, BlockType.SAND);
        selectBlock(GLFW.GLFW_KEY_5, BlockType.WOOD);
        selectBlock(GLFW.GLFW_KEY_6, BlockType.LEAVES);

        player.update(deltaSeconds);
        Vector3f position = player.getPosition();
        int chunkX = Math.floorDiv((int) Math.floor(position.x), Chunk.SIZE);
        int chunkZ = Math.floorDiv((int) Math.floor(position.z), Chunk.SIZE);
        if (chunkX != lastChunkX || chunkZ != lastChunkZ) {
            world.ensureChunksAround((int) Math.floor(position.x), (int) Math.floor(position.z), LOAD_RADIUS);
            lastChunkX = chunkX;
            lastChunkZ = chunkZ;
        }
        updateBlocks(deltaSeconds);
    }

    @Override
    public void render() {
        int centerX = Math.floorDiv((int) Math.floor(camera.getPosition().x), Chunk.SIZE);
        int centerZ = Math.floorDiv((int) Math.floor(camera.getPosition().z), Chunk.SIZE);
        Optional<BlockHit> targetedBlock = Raycaster.cast(world, camera.getPosition(), camera.getFront(), REACH);
        renderer.renderWorld(world, centerX, centerZ, RENDER_RADIUS, targetedBlock);
    }

    @Override
    public void shutdown() {
        saveWorld();
    }

    private void updateBlocks(float deltaSeconds) {
        Optional<BlockHit> hit = Raycaster.cast(world, camera.getPosition(), camera.getFront(), REACH);
        if (hit.isEmpty()) {
            blockBreakCooldown.shouldBreak(input.isMouseButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT),
                    input.wasMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT), false, deltaSeconds);
            return;
        }
        BlockHit target = hit.get();
        if (blockBreakCooldown.shouldBreak(input.isMouseButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT),
                input.wasMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_LEFT), true, deltaSeconds)) {
            world.setBlockAtWorld(target.x(), target.y(), target.z(), BlockType.AIR.getId());
        }
        if (input.wasMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                && !player.occupiesBlock(target.previousX(), target.previousY(), target.previousZ())) {
            world.setBlockAtWorld(target.previousX(), target.previousY(), target.previousZ(), selectedBlock);
        }
    }

    private void selectBlock(int key, BlockType type) {
        if (input.wasKeyPressed(key)) {
            selectedBlock = type.getId();
            engine.getWindow().setTitle("BlocksBound | Selected " + type.name()
                    + " | LMB break, RMB place, F5 save, F9 load");
        }
    }

    private void saveWorld() {
        try {
            WorldStorage.save(savePath, world, player.getPosition());
            engine.getWindow().setTitle("BlocksBound | World saved");
        } catch (IOException exception) {
            System.err.println("Could not save world to " + savePath + ": " + exception.getMessage());
        }
    }

    private void loadWorld() {
        try {
            Optional<SavedWorld> saved = WorldStorage.load(savePath);
            if (saved.isEmpty()) {
                engine.getWindow().setTitle("BlocksBound | No save found");
                return;
            }
            world = saved.get().world();
            player = new Player(world, camera, input);
            player.setPosition(saved.get().playerPosition());
            lastChunkX = Integer.MIN_VALUE;
            lastChunkZ = Integer.MIN_VALUE;
            world.ensureChunksAround((int) Math.floor(saved.get().playerPosition().x),
                    (int) Math.floor(saved.get().playerPosition().z), LOAD_RADIUS);
            engine.getWindow().setTitle("BlocksBound | World loaded");
        } catch (IOException exception) {
            System.err.println("Could not load world from " + savePath + ": " + exception.getMessage());
            engine.getWindow().setTitle("BlocksBound | Load failed (see console)");
        }
    }

    public static void main(String[] args) {
        new Main().engine.start();
    }
}
