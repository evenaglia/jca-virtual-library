package net.venaglia.common.util.serializer;

import net.venaglia.common.util.ThreadSingletonSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * User: ed
 * Date: 4/4/14
 * Time: 6:33 PM
 */
public class ObjectSerializer {

    public static final ObjectSerializer INSTANCE = new ObjectSerializer();

    public static final ThreadSingletonSource<ByteBuffer> BUFFERS = new ThreadSingletonSource<ByteBuffer>() {
        @Override
        protected ByteBuffer newInstance() {
            ByteBuffer buffer = ByteBuffer.allocate(65536);
            buffer.order(ByteOrder.BIG_ENDIAN);
            return buffer;
        }
    };

    private final AbstractSerializerStrategy<Object> impl = new AbstractSerializerStrategy<Object>() {
        public void serialize(Object value, ByteBuffer out) {
            serializeObject(value, out);
        }

        public Object deserialize(ByteBuffer in) {
            return deserializeObject(in);
        }
    };

    private ObjectSerializer() {
        // use the ThreadSingletonSource
    }

    public void serialize(Object value, ByteBuffer out) {
        impl.serialize(value, out);
    }

    public byte[] serialize(Object value) {
        ByteBuffer buffer = BUFFERS.get();
        try {
            impl.serialize(value, buffer);
            byte[] data = new byte[buffer.position()];
            buffer.flip();
            buffer.get(data);
            return data;
        } finally {
            buffer.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T deserialize(ByteBuffer in) {
        return (T)impl.deserialize(in);
    }

    public <T> T deserialize(byte[] data) {
        ByteBuffer buffer = BUFFERS.get();
        try {
            buffer.put(data);
            buffer.flip();
            return deserialize(buffer);
        } finally {
            buffer.clear();
        }
    }
}
