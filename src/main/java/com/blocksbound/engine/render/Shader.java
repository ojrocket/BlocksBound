package com.blocksbound.engine.render;

import org.lwjgl.opengl.GL20;

public class Shader implements AutoCloseable {
    private final int programId;

    public Shader(String vertexSource, String fragmentSource) {
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSource);
        GL20.glCompileShader(vertexShader);
        checkCompileStatus(vertexShader, "vertex");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);
        checkCompileStatus(fragmentShader, "fragment");

        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShader);
        GL20.glAttachShader(programId, fragmentShader);
        GL20.glLinkProgram(programId);
        checkLinkStatus(programId);

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    public void use() {
        GL20.glUseProgram(programId);
    }

    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(programId, name);
    }

    private void checkCompileStatus(int shaderId, String type) {
        int result = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (result == GL20.GL_FALSE) {
            throw new IllegalStateException(type + " shader compilation failed: " + GL20.glGetShaderInfoLog(shaderId));
        }
    }

    private void checkLinkStatus(int programId) {
        int result = GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS);
        if (result == GL20.GL_FALSE) {
            throw new IllegalStateException("Shader program link failed: " + GL20.glGetProgramInfoLog(programId));
        }
    }

    @Override
    public void close() {
        GL20.glDeleteProgram(programId);
    }
}
