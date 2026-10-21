package com.blocksbound.engine.render;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Mesh implements AutoCloseable {
    private final int vaoId;
    private final int vboId;
    private final int eboId;
    private final int vertexCount;

    public Mesh(float[] vertices, int[] indices) {
        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();
        eboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);

        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, eboId);
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);

        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 5 * Float.BYTES, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 5 * Float.BYTES, 3L * Float.BYTES);

        GL30.glBindVertexArray(0);
        vertexCount = indices.length;
    }

    public void render() {
        GL30.glBindVertexArray(vaoId);
        GL20.glDrawElements(GL20.GL_TRIANGLES, vertexCount, GL20.GL_UNSIGNED_INT, 0L);
    }

    @Override
    public void close() {
        GL15.glDeleteBuffers(vboId);
        GL15.glDeleteBuffers(eboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
