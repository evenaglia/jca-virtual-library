package net.venaglia.gloo.projection.impl;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Coordinate;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.physical.texture.TextureCoordinate;

import java.nio.ByteBuffer;
import java.util.AbstractList;
import java.util.RandomAccess;
import java.util.Set;

/**
 * User: ed
 * Date: 3/8/13
 * Time: 8:06 AM
 */
public abstract class AbstractCoordinateList extends AbstractList<Coordinate> implements CoordinateList, RandomAccess {

    private int[] offsets = { -2, -2, -2, -2 };
    private int[] strides = { -2, -2, -2, -2 };

    @Override
    public Coordinate get(int i) {
        if (i < 0 || i >= size()) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        ByteBuffer data = data();
        Point vertex = null;
        Vector normal = null;
        Color color = null;
        TextureCoordinate textureCoordinate = null;
        for (Field field : Field.values()) {
            if (has(field)) {
                data.position(i * recordSize() + offset(field));
                switch (field) {
                    case VERTEX:
                        vertex = new Point(data.getDouble(), data.getDouble(), data.getDouble());
                        break;
                    case NORMAL:
                        normal = new Vector(data.getDouble(), data.getDouble(), data.getDouble());
                        break;
                    case COLOR:
                        color = new Color(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
                        break;
                    case TEXTURE_COORDINATE:
                        textureCoordinate = new TextureCoordinate(data.getFloat(), data.getFloat());
                        break;
                }
            }
        }
        return new Coordinate(vertex, normal, color, textureCoordinate);
    }

    protected abstract Set<Field> getFields();

    private int recordSize = -1;

    public boolean has(Field field) {
        return getFields().contains(field);
    }

    public int recordSize() {
        if (recordSize >= 0) {
            return recordSize;
        }
        int size = 0;
        for (Field field : getFields()) {
            size += field.size;
        }
        recordSize = size;
        return size;
    }

    public int offset(Field field) {
        int offset = offsets[field.ordinal()];
        if (offset == -2) {
            offset = -1;
            int sum = 0;
            for (Field f : getFields()) {
                if (f == field) {
                    offset = sum;
                    break;
                }
                sum += field.size;
            }
            offsets[field.ordinal()] = offset;
        }
        return offset;
    }

    public int stride(Field field) {
        int stride = strides[field.ordinal()];
        if (stride == -2) {
            stride = has(field) ? recordSize() - field.size : -1;
            strides[field.ordinal()] = stride;
        }
        return stride;
    }
}
