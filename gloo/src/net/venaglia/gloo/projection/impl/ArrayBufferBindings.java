package net.venaglia.gloo.projection.impl;

import static net.venaglia.gloo.projection.CoordinateList.Field;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.GL_NORMAL_ARRAY;
import static org.lwjgl.opengl.GL11.GL_COLOR_ARRAY;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_COORD_ARRAY;

import net.venaglia.gloo.projection.CoordinateList;
import org.lwjgl.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 12/26/14
 * Time: 1:33 PM
 */
public class ArrayBufferBindings {

    public static final ArrayBufferBindings ALL_DISABLED = new ArrayBufferBindings();

    private static final int BINDINGS = Type.values().length;

    protected final BoundTo[] activeBindings = new BoundTo[Type.values().length];

    public ArrayBufferBindings() {
        activeBindings[0] = new BoundTo(Type.VERTEX);
        activeBindings[1] = new BoundTo(Type.NORMAL);
        activeBindings[2] = new BoundTo(Type.COLOR);
        activeBindings[3] = new BoundTo(Type.TEXTURE_COORDINATE);
    }

    public ArrayBufferBindings(CoordinateList cl) {
        this();
        doSet(cl, activeBindings[0], Field.VERTEX);
        doSet(cl, activeBindings[1], Field.NORMAL);
        doSet(cl, activeBindings[2], Field.COLOR);
        doSet(cl, activeBindings[3], Field.TEXTURE_COORDINATE);
    }

    private void doSet(final CoordinateList cl, final BoundTo boundTo, final Field field) {
        if (cl.has(field)) {
            boundTo.set(cl.data(field), cl.offset(field), cl.stride(field), cl.recordSize(field));
        }
    }

    public void apply(ArrayBufferBindings bindings) {
        for (int i = 0; i < BINDINGS; i++) {
            activeBindings[i].applyImpl(bindings.activeBindings[i]);
        }
    }

    protected void doEnable(Type type) {
        // no-op
    }

    protected void doDisable(Type type) {
        // no-op
    }

    protected void doBind(Type type, ByteBuffer buffer, int offset, int stride) {
        // no-op
    }

    protected enum Type {
        VERTEX(GL_VERTEX_ARRAY),
        NORMAL(GL_NORMAL_ARRAY),
        COLOR(GL_COLOR_ARRAY),
        TEXTURE_COORDINATE(GL_TEXTURE_COORD_ARRAY);

        private final int code;

        private Type(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    protected class BoundTo {

        protected final Type type;

        public boolean enabled = false;
        public ByteBuffer buffer = null;
        public int offset = -1;
        public int stride = -1;
        public int size = -1;

        public BoundTo(Type type) {
            this.type = type;
        }

        public void apply(BoundTo boundTo) {
            if (type != boundTo.type) {
                throw new IllegalArgumentException("Cannot apply a binding for " + boundTo.type + " to " + type);
            }
            applyImpl(boundTo);
        }

        protected void applyImpl(BoundTo boundTo) {
            if (boundTo.enabled) {
                set(boundTo.buffer, boundTo.offset, boundTo.stride, boundTo.size);
            } else {
                unset();
            }
        }

        public void set(ByteBuffer buffer, int offset, int stride, int size) {
            if (!enabled || this.buffer != buffer || this.offset != offset || this.stride != stride || this.size != size) {
                if (!enabled) {
                    doEnable(type);
                    enabled = true;
                }
                doBind(type, buffer, offset, 0);
                this.buffer = buffer;
                this.offset = offset;
                this.size = size;
            }
        }

        public void unset() {
            if (enabled) {
                doDisable(type);
                enabled = false;
                this.buffer = null;
                this.size = -1;
            }
        }

        public boolean isSet() {
            return enabled;
        }

        @Override
        public String toString() {
            if (enabled) {
                long address = MemoryUtil.getAddress(buffer, 0);
                return String.format("ArrayBufferBindings.BoundTo<%s[]>@0x%x(%d)", type, address, size);
            } else {
                return String.format("ArrayBufferBindings.BoundTo<%s[]>@null", type);
            }
        }
    }
}
