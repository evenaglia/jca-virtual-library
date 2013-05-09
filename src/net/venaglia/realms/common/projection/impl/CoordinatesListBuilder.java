package net.venaglia.realms.common.projection.impl;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.projection.Coordinate;
import net.venaglia.realms.common.projection.CoordinateBuffer;
import net.venaglia.realms.common.projection.CoordinateList;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.physical.texture.TextureMapping;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 9:17 AM
 */
public class CoordinatesListBuilder implements CoordinateBuffer {

    private int recordSize;
    private boolean normals;
    private boolean colors;
    private boolean texture;
    private int limit;
    private int count;
    private ByteBuffer buffer;
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
        int recordSize = (Double.SIZE >> 3) * 3; // vertices
        for (CoordinateList.Field field : extraFields) {
            recordSize = field == CoordinateList.Field.VERTEX ? 0 : getFieldSize(field);
        }
        this.recordSize = recordSize;
        this.normals = o.contains(CoordinateList.Field.NORMAL);
        this.colors = o.contains(CoordinateList.Field.COLOR);
        this.texture = o.contains(CoordinateList.Field.TEXTURE_COORDINATE);
        if (texture) st = new float[2];
        limit = 16;
        count = 0;
        buffer = ByteBuffer.allocate(limit * recordSize);
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

    public CoordinatesListBuilder setTextureMapping(TextureMapping textureMapping) {
        this.textureMapping = textureMapping;
        return this;
    }

    public void vertex(Point point) {
        vertex(point.x, point.y, point.z);
    }

    public void vertex(double x, double y, double z) {
        ensureCapacity();
        buffer.putDouble(x);
        buffer.putDouble(y);
        buffer.putDouble(z);
        if (normals) {
            buffer.putDouble(i);
            buffer.putDouble(j);
            buffer.putDouble(k);
        }
        if (colors) {
            buffer.putFloat(r);
            buffer.putFloat(g);
            buffer.putFloat(b);
            buffer.putFloat(a);
        }
        if (texture) {
            textureMapping.unwrap(x, y, z, st);
            buffer.putFloat(st[0]);
            buffer.putFloat(st[1]);
        }
        count++;
    }

    private void ensureCapacity() {
        if (count >= limit) {
            int newLimit = limit <<= 1;
            ByteBuffer newBuffer = ByteBuffer.allocate(newLimit * recordSize);
            buffer.flip();
            newBuffer.put(buffer);
            buffer = newBuffer;
            limit = newLimit;
        }
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
        final int size = count;
        final int recordSize = this.recordSize;
        final ByteBuffer data = ByteBuffer.allocateDirect(buffer.limit());
        data.put(buffer);
        data.flip();
        final Set<CoordinateList.Field> fields = EnumSet.of(CoordinateList.Field.VERTEX);
        if (normals) {
            fields.add(CoordinateList.Field.NORMAL);
        }
        if (colors) {
            fields.add(CoordinateList.Field.COLOR);
        }
        if (texture) {
            fields.add(CoordinateList.Field.TEXTURE_COORDINATE);
        }
        return new AbstractCoordinateList() {
            public int size() {
                return size;
            }

            @Override
            protected Set<Field> getFields() {
                return fields;
            }

            public int recordSize() {
                return recordSize;
            }

            public ByteBuffer data() {
                return data;
            }
        };
    }
//    public ByteBuffer applyTextureMapping(TextureMapping mapping) {
//        float[] c = {0,0};
//        for (int i = 0; i < size; i++) {
//            buffer.position(STRIDE * i);
//            double x = buffer.getDouble();
//            double y = buffer.getDouble();
//            double z = buffer.getDouble();
//            buffer.getDouble(); // i
//            buffer.getDouble(); // j
//            buffer.getDouble(); // k
//            mapping.unwrap(x, y, z, c);
//            buffer.putFloat(c[0]);
//            buffer.putFloat(c[1]);
//        }
//        return buffer.asReadOnlyBuffer();
//    }
}
