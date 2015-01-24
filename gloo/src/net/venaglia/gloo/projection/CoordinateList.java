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

    Point set(int i, Point vertex);

    Vector set(int i, Vector normal);

    Color set(int i, Color color);

    TextureCoordinate set(int i, TextureCoordinate textureCoordinate);

    boolean has(Field field);

    int recordSize(Field field);

    int offset(Field field);

    int stride(Field field);

    ByteBuffer data(Field field);
}
