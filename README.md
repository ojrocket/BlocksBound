# BlocksBound

A Java-based voxel game engine built with LWJGL 3, OpenGL 3.3, and JOML. It follows the architecture and gameplay foundations described in the attached research notes so it can grow into a Minecraft-style sandbox prototype.

## What this project includes

- Engine lifecycle: window, input, time, renderer, and camera
- OpenGL 3.3 core-profile rendering setup
- Voxel world foundation with block IDs and chunk coordinate handling
- First-person fly camera controls for testing and scene exploration
- Gradle build system with generated wrapper files

## Current status

This is a working engine foundation rather than a full Minecraft clone. It is designed to be extended with:

- chunk meshing and greedy meshing optimization
- terrain generation and noise-based world generation
- block placement and breaking
- collision and physics systems
- entity and inventory systems
- save/load persistence

## Requirements

- Java 21 LTS or newer
- Windows, Linux, or macOS with OpenGL 3.3-capable graphics drivers
- A desktop environment that supports a GLFW window

## Running the project

From the project root:

### Linux / macOS

```bash
./gradlew run
```

### Windows

```powershell
./gradlew.bat run
```

## Explicit JDK configuration

If Gradle does not pick the correct Java installation:

### Linux / macOS

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew run
```

### Windows PowerShell

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
.\gradlew.bat run
```

## Building the project

```bash
./gradlew build
```

## Controls

- W / S / A / D: movement
- Space / Left Shift: vertical movement
- Mouse: look around
- Escape: quit

## Project structure

```text
BlocksBound/
├── build.gradle
├── gradlew
├── gradlew.bat
├── settings.gradle
├── src/
│   ├── main/java/com/blocksbound/
│   │   ├── engine/
│   │   │   ├── core/
│   │   │   ├── input/
│   │   │   ├── render/
│   │   │   └── world/
│   │   └── game/
│   └── test/java/
├── README.md
├── .gitignore
└── build/
```

## Why this is a solid Minecraft-style base

This project follows the architecture recommended by the research notes:

- engine and game concepts are separated cleanly
- fixed-step update loop is included
- world and chunk layout is designed for voxel data
- camera and render pipeline are ready for extension
- OpenGL setup is already written for a modern core profile

This gives you a dependable base for expanding into chunk streaming, terrain generation, chunk meshing, raycasting, physics, and block interaction.

## License

This repo is a prototype engine foundation intended for learning and game-development experimentation.
