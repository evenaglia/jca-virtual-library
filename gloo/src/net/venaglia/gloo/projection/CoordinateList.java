package net.venaglia.gloo.projection;

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

    boolean has(Field field);

    int recordSize();

    int offset(Field field);

    int stride(Field field);

    ByteBuffer data();
}
