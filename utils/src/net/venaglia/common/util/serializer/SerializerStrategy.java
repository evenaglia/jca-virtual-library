package net.venaglia.common.util.serializer;

import net.venaglia.common.util.Predicate;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 8:33 AM
 */
public interface SerializerStrategy<T> {

    Class<? extends T> getJavaType();

    char getTypeMarker();

    boolean accept(Object o);

    boolean accept(Class<?> type);

    void serialize(T value, ByteBuffer out);

    T deserialize(ByteBuffer in);

    T deserializePartial(ByteBuffer in, Predicate<? super String> filter);

    void deserializePartial(ByteBuffer in, Predicate<? super String> filter, Map<String, Object> out);
}
