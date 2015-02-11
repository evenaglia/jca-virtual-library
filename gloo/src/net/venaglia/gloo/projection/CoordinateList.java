package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.texture.TextureCoordinate;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 11:58 AM
 */
public interface CoordinateList extends List<Coordinate> {

    enum Field {
        VERTEX((Double.SIZE >> 3) * 3),
        NORMAL((Double.SIZE >> 3) * 3),
        COLOR((Float.SIZE >> 3) * 4),
        TEXTURE_COORDINATE((Float.SIZE >> 3) * 2);

        public final int size;

        private Field(int size) {
            this.size = size;
        }
    }

    Point set(int n, Point vertex);

    void setVertex(int n, double x, double y, double z);

    Vector set(int n, Vector normal);

    void setNormal(int n, double i, double j, double k);

    Color set(int n, Color color);

    void setColor(int n, float r, float g, float b);

    void setColor(int n, float r, float g, float b, float a);

    TextureCoordinate set(int n, TextureCoordinate textureCoordinate);

    void setTextureCoordinate(int n, float s, float t);

    boolean has(Field field);

    int recordSize(Field field);

    int offset(Field field);

    int stride(Field field);

    ByteBuffer data(Field field);
}
