package net.venaglia.realms.common.map_x.db.impl;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInputStream;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import net.venaglia.realms.common.map_x.db.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 2/24/13
 * Time: 9:21 AM
 */
public class KryoSerializer<T> implements Serializer<T> {

    private static final ThreadLocal<Kryo> KRYO = new ThreadLocal<Kryo>() ;

    private final Class<T> type;

    public KryoSerializer(Class<T> type) {
        this.type = type;
    }

    protected Kryo getKryo() {
        Kryo kryo = KRYO.get();
        if (kryo == null) {
            kryo = new Kryo();
            configureKryo(kryo);
            KRYO.set(kryo);
        }
        return kryo;
    }

    protected void configureKryo(Kryo kryo) {
        // no-op
    }

    public T deserialize(ByteBuffer buffer) {
        return deserializeImpl(new Input(new ByteBufferInputStream(buffer)));
    }

    public T deserialize(byte[] buffer) {
        return deserializeImpl(new Input(new ByteArrayInputStream(buffer)));
    }

    protected T deserializeImpl(Input input) {
        return intercept(getKryo().readObject(input, type));
    }

    public byte[] serialize(T obj) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        getKryo().writeObject(new Output(buffer), obj);
        return buffer.toByteArray();
    }

    protected T intercept(T value) {
        return value;
    }
}
