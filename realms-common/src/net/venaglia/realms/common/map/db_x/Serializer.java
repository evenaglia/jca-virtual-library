package net.venaglia.realms.common.map.db_x;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 2/24/13
 * Time: 9:19 AM
 */
public interface Serializer<T> {

    T deserialize(ByteBuffer buffer);

    T deserialize(byte[] buffer);

    byte[] serialize(T obj) throws IOException;
}
