package com.blocksbound.engine.core;

import com.blocksbound.engine.input.Input;
import com.blocksbound.engine.render.Renderer;
import com.blocksbound.engine.window.Window;
import org.lwjgl.glfw.GLFW;

public class Engine {
    private final EngineConfig config;
    private final Window window;
    private final Input input;
    private final Renderer renderer;
    private final Time time = new Time();
    private final Game game;
    private double accumulator;
    private boolean gameInitializationAttempted;
    private boolean rendererInitializationAttempted;
    private boolean shutdown;

    public Engine(EngineConfig config, Game game) {
        this.config = config;
        this.game = game;
        this.window = new Window(config.title(), config.width(), config.height(), config.vsync());
        this.input = new Input(window.getHandle());
        this.renderer = new Renderer(window);
    }

    public void start() {
        if (shutdown) {
            throw new IllegalStateException("Engine has already shut down");
        }
        Throwable failure = null;
        try {
            gameInitializationAttempted = true;
            game.init();
            rendererInitializationAttempted = true;
            renderer.init();
            GLFW.glfwSetWindowTitle(window.getHandle(), config.title());

            double fixedStep = config.fixedDeltaSeconds();
            while (!window.shouldClose()) {
                double deltaSeconds = time.update();
                accumulator += deltaSeconds;
                window.pollEvents();

                boolean inputConsumed = false;
                while (accumulator >= fixedStep) {
                    game.update((float) fixedStep);
                    if (!inputConsumed) {
                        input.endFrame();
                        inputConsumed = true;
                    }
                    accumulator -= fixedStep;
                }

                renderer.render(game, (float) (accumulator / fixedStep));
                window.swapBuffers();
            }
        } catch (RuntimeException | Error exception) {
            failure = exception;
            throw exception;
        } finally {
            try {
                shutdown();
            } catch (RuntimeException | Error exception) {
                if (failure != null) {
                    failure.addSuppressed(exception);
                } else {
                    throw exception;
                }
            }
        }
    }

    public void shutdown() {
        if (shutdown) {
            return;
        }
        shutdown = true;
        try {
            if (gameInitializationAttempted) {
                game.shutdown();
            }
        } finally {
            try {
                if (rendererInitializationAttempted) {
                    renderer.shutdown();
                }
            } finally {
                window.close();
            }
        }
    }

    public Window getWindow() {
        return window;
    }

    public Input getInput() {
        return input;
    }

    public Renderer getRenderer() {
        return renderer;
    }
}
