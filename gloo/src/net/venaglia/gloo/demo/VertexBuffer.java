package net.venaglia.gloo.demo;

import net.venaglia.gloo.projection.AbstractLoadableElement;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL15.*;

/**
 * User: ed
 * Date: 7/14/12
 * Time: 9:56 AM
 */
public class VertexBuffer extends AbstractLoadableElement {

    private final float[] positions;

    private int buffer = Integer.MIN_VALUE;

    public VertexBuffer(float... positions) {
        this.positions = positions;
    }

    @Override
    protected void loadImpl() {
        FloatBuffer positions = BufferUtils.createFloatBuffer(this.positions.length);
        positions.put(this.positions);
        positions.flip();

        buffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
        glBufferData(GL_ARRAY_BUFFER, positions, GL_STATIC_DRAW); // or GL_DYNAMIC_DRAW
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    @Override
    protected void unloadImpl() {
        glDeleteBuffers(buffer);
        buffer = Integer.MIN_VALUE;
    }

    public void bind() {
        ensureLoaded();
        glBindBuffer(GL_ARRAY_BUFFER, buffer);
    }
}
