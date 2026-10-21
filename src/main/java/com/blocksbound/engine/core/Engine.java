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

    public Engine(EngineConfig config, Game game) {
        this.config = config;
        this.game = game;
        this.window = new Window(config.title(), config.width(), config.height(), config.vsync());
        this.input = new Input(window.getHandle());
        this.renderer = new Renderer(window);
    }

    public void start() {
        game.init();
        renderer.init();
        GLFW.glfwSetWindowTitle(window.getHandle(), config.title());

        double fixedStep = config.fixedDeltaSeconds();
        while (!window.shouldClose()) {
            double deltaSeconds = time.update();
            accumulator += deltaSeconds;
            window.pollEvents();
            input.beginFrame();

            while (accumulator >= fixedStep) {
                game.update((float) fixedStep);
                accumulator -= fixedStep;
            }

            renderer.render(game, (float) (accumulator / fixedStep));
            window.swapBuffers();
        }

        shutdown();
    }

    public void shutdown() {
        game.shutdown();
        renderer.shutdown();
        window.close();
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
