package com.blocksbound.engine.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Mesh implements AutoCloseable {
    private static final int STRIDE = 6;

    private final int vaoId;
    private final int vboId;
    private final int vertexCount;

    public Mesh(float[] vertices) {
        if (vertices.length % STRIDE != 0) {
            throw new IllegalArgumentException("Vertex data must contain position and color values");
        }
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, STRIDE * Float.BYTES, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 3, GL20.GL_FLOAT, false, STRIDE * Float.BYTES, 3L * Float.BYTES);

        GL30.glBindVertexArray(0);
        vertexCount = vertices.length / STRIDE;
    }

    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
    }

    @Override
    public void close() {
        GL15.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
