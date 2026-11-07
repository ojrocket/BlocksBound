package com.blocksbound.game;

import com.blocksbound.engine.core.Engine;
import com.blocksbound.engine.core.EngineConfig;
import com.blocksbound.engine.core.Game;
import com.blocksbound.engine.audio.SoundEffects;
import com.blocksbound.engine.audio.SoundEffects.Sound;
import com.blocksbound.engine.input.Input;
import com.blocksbound.engine.player.Player;
import com.blocksbound.engine.render.Camera;
import com.blocksbound.engine.render.Renderer;
import com.blocksbound.engine.world.BlockHit;
import com.blocksbound.engine.world.BlockDrop;
import com.blocksbound.engine.world.BlockType;
import com.blocksbound.engine.world.Chunk;
import com.blocksbound.engine.world.Raycaster;
import com.blocksbound.engine.world.World;
import com.blocksbound.engine.world.WorldStorage;
import com.blocksbound.engine.world.WorldStorage.SavedWorld;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.sound.sampled.LineUnavailableException;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class Main implements Game {
    private static final int LOAD_RADIUS = 5;
    private static final int RENDER_RADIUS = 4;
    private static final float REACH = 6.0f;
    private static final float BLOCK_BREAK_DURATION = 0.5f;
    private static final float DROP_PICKUP_RADIUS = 1.5f;
    private static final BlockType[] HOTBAR_BLOCKS = {
        BlockType.GRASS, BlockType.DIRT, BlockType.STONE, BlockType.SAND, BlockType.WOOD, BlockType.LEAVES,
        null, null, null, null
    };
    private static final int[] HOTBAR_KEYS = {
        GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
        GLFW.GLFW_KEY_6, GLFW.GLFW_KEY_7, GLFW.GLFW_KEY_8, GLFW.GLFW_KEY_9, GLFW.GLFW_KEY_0
    };

    private final Engine engine;
    private final Camera camera;
    private final Input input;
    private final Renderer renderer;
    private final java.nio.file.Path savePath = WorldStorage.defaultSavePath();
    private World world;
    private Player player;
    private SoundEffects soundEffects;
    private final BlockBreakProgress blockBreakProgress = new BlockBreakProgress(BLOCK_BREAK_DURATION);
    private int selectedHotbarSlot = 1;
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
            saved.get().inventory().forEach(player::addToInventory);
        } else {
            world = new World();
            world.ensureChunksAround(0, 0, LOAD_RADIUS);
            player = new Player(world, camera, input);
            player.setPosition(new Vector3f(0.5f, world.getSurfaceHeight(0, 0) + 2.62f, 0.5f));
        }

        try {
            soundEffects = new SoundEffects();
        } catch (LineUnavailableException | IllegalArgumentException exception) {
            System.err.println("Sound effects are unavailable: " + exception.getMessage());
        }

        input.setCursorMode(GLFW.GLFW_CURSOR_DISABLED);
        input.setCursorPosition(engine.getWindow().getWidth() / 2.0, engine.getWindow().getHeight() / 2.0);
        updateWindowTitle();
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

        selectHotbarSlot();

        boolean jumping = player.isGrounded() && input.wasKeyPressed(GLFW.GLFW_KEY_SPACE);
        player.update(deltaSeconds);
        if (jumping) {
            playSound(Sound.JUMP);
        }
        Vector3f position = player.getPosition();
        int chunkX = Math.floorDiv((int) Math.floor(position.x), Chunk.SIZE);
        int chunkZ = Math.floorDiv((int) Math.floor(position.z), Chunk.SIZE);
        if (chunkX != lastChunkX || chunkZ != lastChunkZ) {
            world.ensureChunksAround((int) Math.floor(position.x), (int) Math.floor(position.z), LOAD_RADIUS);
            lastChunkX = chunkX;
            lastChunkZ = chunkZ;
        }
        updateBlocks(deltaSeconds);
        collectNearbyDrops();
    }

    @Override
    public void render() {
        int centerX = Math.floorDiv((int) Math.floor(camera.getPosition().x), Chunk.SIZE);
        int centerZ = Math.floorDiv((int) Math.floor(camera.getPosition().z), Chunk.SIZE);
        Optional<BlockHit> targetedBlock = Raycaster.cast(world, camera.getPosition(), camera.getFront(), REACH);
        renderer.renderWorld(world, centerX, centerZ, RENDER_RADIUS, targetedBlock,
                blockBreakProgress.getProgress(), HOTBAR_BLOCKS, getHotbarCounts(), selectedHotbarSlot);
    }

    @Override
    public void shutdown() {
        try {
            if (world != null && player != null) {
                saveWorld();
            }
        } finally {
            if (soundEffects != null) {
                soundEffects.close();
                soundEffects = null;
            }
        }
    }

    private void updateBlocks(float deltaSeconds) {
        Optional<BlockHit> hit = Raycaster.cast(world, camera.getPosition(), camera.getFront(), REACH);
        if (hit.isEmpty()) {
            blockBreakProgress.update(input.isMouseButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT), null, deltaSeconds);
            return;
        }
        BlockHit target = hit.get();
        if (blockBreakProgress.update(input.isMouseButtonDown(GLFW.GLFW_MOUSE_BUTTON_LEFT),
                target, deltaSeconds)) {
            if (world.breakBlockAtWorld(target.x(), target.y(), target.z()).isPresent()) {
                playSound(Sound.BLOCK_BREAK);
            }
        }
        BlockType selectedBlock = HOTBAR_BLOCKS[selectedHotbarSlot];
        if (input.wasMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                && selectedBlock != null
                && world.getBlockAtWorld(target.previousX(), target.previousY(), target.previousZ())
                        == BlockType.AIR.getId()
                && !player.occupiesBlock(target.previousX(), target.previousY(), target.previousZ())
                && player.removeFromInventory(selectedBlock, 1)) {
            world.setBlockAtWorld(target.previousX(), target.previousY(), target.previousZ(), selectedBlock.getId());
            playSound(Sound.BLOCK_PLACE);
            updateWindowTitle();
        }
    }

    private void selectHotbarSlot() {
        for (int slot = 0; slot < HOTBAR_KEYS.length; slot++) {
            if (input.wasKeyPressed(HOTBAR_KEYS[slot])) {
                selectedHotbarSlot = slot;
                updateWindowTitle();
                return;
            }
        }
    }

    private int[] getHotbarCounts() {
        int[] counts = new int[HOTBAR_BLOCKS.length];
        for (int slot = 0; slot < HOTBAR_BLOCKS.length; slot++) {
            if (HOTBAR_BLOCKS[slot] != null) {
                counts[slot] = player.getInventoryCount(HOTBAR_BLOCKS[slot]);
            }
        }
        return counts;
    }

    private void collectNearbyDrops() {
        Vector3f position = player.getPosition();
        List<BlockDrop> collected = world.collectDroppedBlocksNear(
                position.x, position.y, position.z, DROP_PICKUP_RADIUS);
        if (collected.isEmpty()) {
            return;
        }
        for (BlockDrop drop : collected) {
            player.addToInventory(drop.type(), 1);
        }
        playSound(Sound.BLOCK_PICKUP);
        updateWindowTitle();
    }

    private void playSound(Sound sound) {
        if (soundEffects != null) {
            soundEffects.play(sound);
        }
    }

    private void updateWindowTitle() {
        BlockType selectedBlock = HOTBAR_BLOCKS[selectedHotbarSlot];
        String selection = selectedBlock == null ? "Empty" : selectedBlock.name();
        StringBuilder title = new StringBuilder("BlocksBound | Selected ")
                .append(selection).append(" | Inventory: ");
        boolean hasItems = false;
        for (BlockType type : BlockType.values()) {
            int count = player == null ? 0 : player.getInventoryCount(type);
            if (count > 0) {
                if (hasItems) {
                    title.append(", ");
                }
                title.append(type.name()).append(" x").append(count);
                hasItems = true;
            }
        }
        if (!hasItems) {
            title.append("empty");
        }
        title.append(" | WASD move, Space jump, LMB break, RMB place, F5 save, F9 load");
        engine.getWindow().setTitle(title.toString());
    }

    private void saveWorld() {
        try {
            WorldStorage.save(savePath, world, player.getPosition(), player.getInventory());
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
            saved.get().inventory().forEach(player::addToInventory);
            lastChunkX = Integer.MIN_VALUE;
            lastChunkZ = Integer.MIN_VALUE;
            world.ensureChunksAround((int) Math.floor(saved.get().playerPosition().x),
                    (int) Math.floor(saved.get().playerPosition().z), LOAD_RADIUS);
            updateWindowTitle();
        } catch (IOException exception) {
            System.err.println("Could not load world from " + savePath + ": " + exception.getMessage());
            engine.getWindow().setTitle("BlocksBound | Load failed (see console)");
        }
    }

    public static void main(String[] args) {
        new Main().engine.start();
    }
}
