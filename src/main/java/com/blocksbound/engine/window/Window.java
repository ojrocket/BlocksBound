package com.blocksbound.engine.window;

import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

public class Window implements AutoCloseable {
    private final long handle;
    private int width;
    private int height;
    private final String title;
    private final GLFWErrorCallback errorCallback;
    private final GLFWFramebufferSizeCallback framebufferSizeCallback;
    private final GLCapabilities caps;

    public Window(String title, int width, int height, boolean vsync) {
        this.title = title;
        this.width = width;
        this.height = height;
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        errorCallback = GLFWErrorCallback.createPrint(System.err);
        GLFW.glfwSetErrorCallback(errorCallback);
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        handle = GLFW.glfwCreateWindow(width, height, title, 0L, 0L);
        if (handle == 0L) {
            throw new IllegalStateException("Unable to create GLFW window");
        }

        GLFW.glfwMakeContextCurrent(handle);
        GLFW.glfwSwapInterval(vsync ? 1 : 0);
        GLFW.glfwShowWindow(handle);
        caps = GL.createCapabilities();
        if (caps == null) {
            throw new IllegalStateException("OpenGL capabilities were not created");
        }
        framebufferSizeCallback = GLFWFramebufferSizeCallback.create((windowHandle, framebufferWidth, framebufferHeight) -> {
            this.width = framebufferWidth;
            this.height = framebufferHeight;
            GL11.glViewport(0, 0, framebufferWidth, framebufferHeight);
        });
        GLFW.glfwSetFramebufferSizeCallback(handle, framebufferSizeCallback);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer framebufferWidth = stack.mallocInt(1);
            IntBuffer framebufferHeight = stack.mallocInt(1);
            GLFW.glfwGetFramebufferSize(handle, framebufferWidth, framebufferHeight);
            this.width = framebufferWidth.get(0);
            this.height = framebufferHeight.get(0);
            GL11.glViewport(0, 0, this.width, this.height);
        }
    }

    public long getHandle() {
        return handle;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getTitle() {
        return title;
    }

    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(handle);
    }

    public void swapBuffers() {
        GLFW.glfwSwapBuffers(handle);
    }

    public void setWindowShouldClose(boolean shouldClose) {
        GLFW.glfwSetWindowShouldClose(handle, shouldClose);
    }

    @Override
    public void close() {
        if (handle != 0L) {
            GLFW.glfwDestroyWindow(handle);
        }
        if (framebufferSizeCallback != null) {
            framebufferSizeCallback.close();
        }
        if (errorCallback != null) {
            errorCallback.close();
        }
        GLFW.glfwTerminate();
    }
}
