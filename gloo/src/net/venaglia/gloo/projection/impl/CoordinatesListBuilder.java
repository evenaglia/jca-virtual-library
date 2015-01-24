package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Coordinate;
import net.venaglia.gloo.projection.CoordinateBuffer;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.TextureMapping;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 9:17 AM
 */
public class CoordinatesListBuilder implements CoordinateBuffer {

    private boolean mutable;
    private ByteBuffer vertices;
    private ByteBuffer normals;
    private ByteBuffer colors;
    private ByteBuffer texture;
    private int limit;
    private int count;
    private TextureMapping textureMapping;
    private double i, j, k;
    private float r, g, b, a;
    private float[] st;

    public CoordinatesListBuilder() {
        this(CoordinateList.Field.values());
    }

    public CoordinatesListBuilder(CoordinateList.Field... extraFields) {
        reset(extraFields);
    }

    public void reset(CoordinateList.Field... extraFields) {
        Set<CoordinateList.Field> o = EnumSet.noneOf(CoordinateList.Field.class);
        Collections.addAll(o, extraFields);
        assert o.contains(CoordinateList.Field.VERTEX);
        this.limit = 16;
        this.count = 0;
        this.mutable = false;
        this.vertices = ByteBuffer.allocate(limit * 24).order(ByteOrder.nativeOrder());
        this.normals = o.contains(CoordinateList.Field.NORMAL)
                       ? ByteBuffer.allocate(limit * 24).order(ByteOrder.nativeOrder())
                       : null;
        this.colors = o.contains(CoordinateList.Field.COLOR)
                      ? ByteBuffer.allocate(limit * 16).order(ByteOrder.nativeOrder())
                      : null;
        this.texture = o.contains(CoordinateList.Field.TEXTURE_COORDINATE)
                       ? ByteBuffer.allocate(limit * 8).order(ByteOrder.nativeOrder())
                       : null;
        if (texture != null) {
            this.st = new float[2];
        }
    }

    private static int getFieldSize(CoordinateList.Field field) {
        switch (field) {
            case COLOR:
                return (Float.SIZE >> 3) * 4;
            case VERTEX:
            case NORMAL:
                return (Double.SIZE >> 3) * 3;
            case TEXTURE_COORDINATE:
                return (Float.SIZE >> 3) * 2;
            default:
                return 0;
        }
    }

    public CoordinatesListBuilder setMutable(boolean mutable) {
        this.mutable = mutable;
        return this;
    }

    public CoordinatesListBuilder setTextureMapping(TextureMapping textureMapping) {
        this.textureMapping = textureMapping;
        return this;
    }

    public void vertex(Point point) {
        vertex(point.x, point.y, point.z);
    }

    public void vertex(double x, double y, double z) {
        ensureCapacity();
        vertices.putDouble(x);
        vertices.putDouble(y);
        vertices.putDouble(z);
        if (normals != null) {
            normals.putDouble(i);
            normals.putDouble(j);
            normals.putDouble(k);
        }
        if (colors != null) {
            colors.putFloat(r);
            colors.putFloat(g);
            colors.putFloat(b);
            colors.putFloat(a);
        }
        if (texture != null) {
            textureMapping.unwrap(x, y, z, st);
            texture.putFloat(st[0]);
            texture.putFloat(st[1]);
        }
        count++;
    }

    private void ensureCapacity() {
        if (count >= limit) {
            int newLimit = limit <<= 1;
            vertices = growBuffer(vertices, newLimit, 24);
            normals = growBuffer(normals, newLimit, 24);
            colors = growBuffer(colors, newLimit, 16);
            texture = growBuffer(texture, newLimit, 8);
            limit = newLimit;
        }
    }

    private ByteBuffer growBuffer(ByteBuffer buffer, int newLimit, int size) {
        if (buffer == null) {
            return null;
        }
        ByteBuffer newBuffer = ByteBuffer.allocate(newLimit * size).order(ByteOrder.nativeOrder());
        buffer.flip();
        newBuffer.put(buffer);
        return newBuffer;
    }

    public void normal(Vector normal) {
        normal(normal.i, normal.j, normal.k);
    }

    public void normal(double i, double j, double k) {
        this.i = i;
        this.j = j;
        this.k = k;
    }

    public void color(Color color) {
        colorAndAlpha(color.r, color.g, color.b, 0);
    }

    public void color(float r, float g, float b) {
        colorAndAlpha(r,g, b, 1);
    }

    public void colorAndAlpha(Color color) {
        colorAndAlpha(color.r, color.g, color.b, 1);
    }

    public void colorAndAlpha(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public void coordinate(Coordinate coordinate) {
        coordinateImpl(coordinate);
    }

    public void coordinates(Iterable<Coordinate> coordinates) {
        for (Coordinate coordinate : coordinates) {
            coordinateImpl(coordinate);
        }
    }

    private void coordinateImpl(Coordinate coordinate) {
        Point vertex = coordinate.getVertex();
        Vector normal = coordinate.getNormal();
        Color color = coordinate.getColor();
        TextureCoordinate textureCoordinate = coordinate.getTextureCoordinate();
        if (normal != null) {
            i = normal.i;
            j = normal.j;
            k = normal.k;
        }
        if (color != null) {
            r = color.r;
            g = color.g;
            b = color.b;
            a = color.a;
        }
        if (textureCoordinate != null) {
            st[0] = textureCoordinate.s;
            st[1] = textureCoordinate.t;
        }
        vertex(vertex);
    }

    public CoordinateList build() {
        final boolean mutable = this.mutable;
        final int size = count;
        final ByteBuffer vertices = toDirectByteBuffer(this.vertices);
        final ByteBuffer normals = toDirectByteBuffer(this.normals);
        final ByteBuffer colors = toDirectByteBuffer(this.colors);
        final ByteBuffer texture = toDirectByteBuffer(this.texture);
        final Set<CoordinateList.Field> fields = EnumSet.of(CoordinateList.Field.VERTEX);
        if (normals != null) {
            fields.add(CoordinateList.Field.NORMAL);
        }
        if (colors != null) {
            fields.add(CoordinateList.Field.COLOR);
        }
        if (texture != null) {
            fields.add(CoordinateList.Field.TEXTURE_COORDINATE);
        }
        return new CoordinateListImpl(mutable, size, fields, vertices, normals, colors, texture);
    }

    private ByteBuffer toDirectByteBuffer(ByteBuffer buffer) {
        if (buffer == null) {
            return null;
        }
        final ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.position()).order(ByteOrder.nativeOrder());
        buffer.flip();
        newBuffer.put(buffer);
        assert buffer.limit() == buffer.position();
        assert newBuffer.limit() == newBuffer.position();
        newBuffer.flip();
        return newBuffer;
    }

    private static class CoordinateListImpl extends AbstractCoordinateList {

        private final int size;
        private final Set<Field> fields;
        private final ByteBuffer vertices;
        private final ByteBuffer normals;
        private final ByteBuffer colors;
        private final ByteBuffer texture;

        public CoordinateListImpl(boolean mutable,
                                  int size,
                                  Set<Field> fields,
                                  ByteBuffer vertices,
                                  ByteBuffer normals,
                                  ByteBuffer colors,
                                  ByteBuffer texture) {
            super(mutable);
            this.size = size;
            this.fields = fields;
            this.vertices = vertices;
            this.normals = normals;
            this.colors = colors;
            this.texture = texture;
        }

        public int size() {
            return size;
        }

        @Override
        protected Set<Field> getFields() {
            return fields;
        }

        public ByteBuffer data(Field field) {
            switch (field) {
                case VERTEX:
                    return vertices;
                case NORMAL:
                    return normals;
                case COLOR:
                    return colors;
                case TEXTURE_COORDINATE:
                    return texture;
            }
            return null;
        }
    }
}
