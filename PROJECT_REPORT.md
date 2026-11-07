# BlocksBound Project Report

## Overview

BlocksBound is a Java 21 first-person voxel sandbox and a custom game-engine project. The playable slice currently focuses on exploring generated terrain, editing blocks, collecting drops, and saving a local world.

## Architecture

- **Core:** `Engine` owns the fixed-step game loop, timing, input updates, rendering, and lifecycle. `EngineConfig` describes and validates window and timing settings.
- **Window and input:** GLFW creates the resizable window and reports keyboard, mouse-button, and cursor events.
- **Rendering:** LWJGL/OpenGL draws the 3D world and HUD. JOML supplies camera and matrix math. Chunk meshes are built from visible block faces and cached until edits dirty them.
- **World:** `World` stores 16 × 64 × 16 chunks, generated deterministically from a world seed. World coordinates map across positive and negative chunk positions.
- **Gameplay:** `Player` implements first-person movement, sprinting, gravity, jumping, collision, and inventory. `Raycaster` targets blocks; `BlockBreakProgress` tracks mining; the hotbar supports selecting, placing, and collecting blocks.
- **Audio:** Short sound effects for jumping, block breaking and placement, and pickups are synthesized locally using Java Sound.
- **Persistence:** `WorldStorage` writes the seed, loaded chunks, player position, drops, and inventory to a versioned binary save at `~/.blocksbound/world.dat`, replacing the file atomically where supported.

## Current Controls

| Input | Action |
|---|---|
| WASD | Move |
| Mouse | Look |
| Left Ctrl | Sprint |
| Space | Jump |
| Left mouse button | Hold for 0.5 seconds to break one block; release before breaking another |
| Right mouse button | Place the selected block if one is in inventory |
| 1–6 | Select grass, dirt, stone, sand, wood, or leaves |
| F5 / F9 | Save / load |
| Escape | Exit and save |

## Build and Tests

- **Build tool:** Gradle (wrapper included)
- **Runtime:** Java 21
- **Main class:** `com.blocksbound.game.Main`
- **Run:** `.\gradlew.bat run`
- **Test:** `.\gradlew.bat test`
- **Coverage in the current test suite:** player collision, seeded terrain, chunk meshing, negative-coordinate chunk borders, block drops and pickups, raycasting, save/load, block-break progress, and engine configuration validation.

## Review Findings and Changes

- Engine configurations now reject blank titles, non-positive window dimensions, and invalid fixed-step durations. Invalid fixed-step values could otherwise prevent the fixed-update loop from making progress.
- Engine shutdown now runs when initialization or the main loop throws, closes initialized resources in order, and avoids masking an existing failure with a cleanup exception.
- Game shutdown no longer attempts to save if initialization failed before the world and player were created.
- The README's block-break timing and behavior now match the implemented 0.5-second, one-block-per-press mechanic.

## Scope and Follow-up

This is an early playable slice, not the full planned game. Crafting, enemies, health, biomes, textures, a menu/settings flow, and JDBC/MySQL persistence are not implemented. The current save system is local binary storage. The existing tests cover core logic, but graphical rendering, audio output, and full interactive gameplay still require manual testing on a machine with compatible OpenGL and audio devices.
