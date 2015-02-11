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

    protected final boolean mutable;

    protected AbstractCoordinateList(boolean mutable) {
        this.mutable = mutable;
    }

    @Override
    public Coordinate get(int i) {
        if (i < 0 || i >= size()) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        Point vertex = null;
        Vector normal = null;
        Color color = null;
        TextureCoordinate textureCoordinate = null;
        for (Field field : Field.values()) {
            if (has(field)) {
                ByteBuffer data = data(field);
                data.position(i * recordSize(field));
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

    private void ensureMutable() {
        if (!mutable) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Coordinate set(int i, Coordinate coordinate) {
        ensureMutable();
        if (coordinate == null) {
            throw new NullPointerException("coordinate");
        }
        Point vertex = null;
        Vector normal = null;
        Color color = null;
        TextureCoordinate textureCoordinate = null;
        for (Field field : getFields()) {
            int position = i * recordSize(field);
            ByteBuffer data = data(field);
            data.position(position);
            switch (field) {
                case VERTEX:
                    try {
                        vertex = new Point(data.getDouble(), data.getDouble(), data.getDouble());
                    } finally {
                        Point p = coordinate.getVertex();
                        if (p != null) { // should always be true
                            data.position(position);
                            data.putDouble(p.x).putDouble(p.y).putDouble(p.z);
                        }
                    }
                    break;
                case NORMAL:
                    try {
                        normal = new Vector(data.getDouble(), data.getDouble(), data.getDouble());
                    } finally {
                        Vector v = coordinate.getNormal();
                        if (v != null) {
                            data.position(position);
                            data.putDouble(v.i).putDouble(v.j).putDouble(v.k);
                        }
                    }
                    break;
                case COLOR:
                    try {
                        color = new Color(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
                    } finally {
                        Color c = coordinate.getColor();
                        if (c != null) {
                            data.position(position);
                            data.putFloat(c.r).putFloat(c.g).putFloat(c.b).putFloat(c.a);
                        }
                    }
                    break;
                case TEXTURE_COORDINATE:
                    try {
                        textureCoordinate = new TextureCoordinate(data.getFloat(), data.getFloat());
                    } finally {
                        TextureCoordinate t = coordinate.getTextureCoordinate();
                        if (t != null) {
                            data.position(position);
                            data.putFloat(t.s).putFloat(t.t);
                        }
                    }
                    break;
            }
        }
        return new Coordinate(vertex, normal, color, textureCoordinate);
    }

    @Override
    public Point set(int n, Point vertex) {
        ensureMutable();
        if (vertex == null) {
            throw new NullPointerException("vertex");
        }
        ByteBuffer data = data(Field.VERTEX);
        int position = n * recordSize(Field.VERTEX);
        data.position(position);
        try {
            return new Point(data.getDouble(), data.getDouble(), data.getDouble());
        } finally {
            data.position(position);
            data.putDouble(vertex.x).putDouble(vertex.y).putDouble(vertex.z);
        }
    }

    @Override
    public void setVertex(int n, double x, double y, double z) {
        ensureMutable();
        ByteBuffer data = data(Field.VERTEX);
        int position = n * recordSize(Field.VERTEX);
        data.position(position);
        data.putDouble(x).putDouble(y).putDouble(z);
    }

    @Override
    public Vector set(int n, Vector normal) {
        ensureMutable();
        if (normal == null) {
            throw new NullPointerException("normal");
        }
        ByteBuffer data = data(Field.NORMAL);
        int position = n * recordSize(Field.NORMAL);
        data.position(position);
        try {
            return new Vector(data.getDouble(), data.getDouble(), data.getDouble());
        } finally {
            data.position(position);
            data.putDouble(normal.i).putDouble(normal.j).putDouble(normal.k);
        }
    }

    @Override
    public void setNormal(int n, double i, double j, double k) {
        ensureMutable();
        ByteBuffer data = data(Field.NORMAL);
        int position = n * recordSize(Field.NORMAL);
        data.position(position);
        data.putDouble(i).putDouble(j).putDouble(k);
    }

    @Override
    public Color set(int n, Color color) {
        ensureMutable();
        if (color == null) {
            throw new NullPointerException("color");
        }
        ByteBuffer data = data(Field.COLOR);
        int position = n * recordSize(Field.COLOR);
        data.position(position);
        try {
            return new Color(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
        } finally {
            data.position(position);
            data.putFloat(color.r).putFloat(color.g).putFloat(color.b).putFloat(color.a);
        }
    }

    @Override
    public void setColor(int n, float r, float g, float b) {
        ensureMutable();
        ByteBuffer data = data(Field.COLOR);
        int position = n * recordSize(Field.COLOR);
        data.position(position);
        data.putFloat(r).putFloat(g).putFloat(b);
    }

    @Override
    public void setColor(int n, float r, float g, float b, float a) {
        ensureMutable();
        ByteBuffer data = data(Field.COLOR);
        int position = n * recordSize(Field.COLOR);
        data.position(position);
        data.putFloat(r).putFloat(g).putFloat(b).putFloat(a);
    }

    @Override
    public TextureCoordinate set(int n, TextureCoordinate textureCoordinate) {
        ensureMutable();
        if (textureCoordinate == null) {
            throw new NullPointerException("textureCoordinate");
        }
        ByteBuffer data = data(Field.TEXTURE_COORDINATE);
        int position = n * recordSize(Field.TEXTURE_COORDINATE);
        data.position(position);
        try {
            return new TextureCoordinate(data.getFloat(), data.getFloat());
        } finally {
            data.position(position);
            data.putFloat(textureCoordinate.s).putFloat(textureCoordinate.t);
        }
    }

    @Override
    public void setTextureCoordinate(int n, float s, float t) {
        ensureMutable();
        ByteBuffer data = data(Field.TEXTURE_COORDINATE);
        int position = n * recordSize(Field.TEXTURE_COORDINATE);
        data.position(position);
        data.putFloat(s).putFloat(t);
    }

    protected abstract Set<Field> getFields();

    public boolean has(Field field) {
        return getFields().contains(field);
    }

    public int recordSize(Field field) {
        switch (field) {
            case VERTEX:
                return 24;
            case NORMAL:
                return 24;
            case COLOR:
                return 16;
            case TEXTURE_COORDINATE:
                return 8;
        }
        return 0;
    }

    public int offset(Field field) {
        return 0;
    }

    public int stride(Field field) {
        return 0;
    }
}
