package net.venaglia.common.util.serializer;

import net.venaglia.common.util.Predicate;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 5:48 PM
 */
public abstract class AbstractSerializerStrategy<T> implements SerializerStrategy<T> {

    protected final Class<T> type;
    protected final char typeMarker;

    protected AbstractSerializerStrategy() {
        this.type = null;
        this.typeMarker = 127;
    }

    protected AbstractSerializerStrategy(Class<T> type, char typeMarker) {
        this.type = type;
        this.typeMarker = typeMarker;
        SerializerRegistry.add(this);
    }

    public Class<? extends T> getJavaType() {
        return type;
    }

    public char getTypeMarker() {
        return typeMarker;
    }

    public boolean accept(Object o) {
        return o != null && accept(o.getClass());
    }

    public boolean accept(Class<?> type) {
        return this.type.isAssignableFrom(type);
    }

    public void deserializePartial(ByteBuffer in, Predicate<String> filter, Map<String, Object> out) {
        throw new UnsupportedOperationException();
    }

    protected final void serializeTypeMarker(SerializerStrategy<?> serializer, ByteBuffer out) {
        out.put((byte)serializer.getTypeMarker());
    }

    protected final void serializeBoolean(boolean v, ByteBuffer out) {
        out.put((byte)(v ? 1 : 0));
    }

    protected final void serializeByte(byte v, ByteBuffer out) {
        out.put(v);
    }

    protected final void serializeShort(short v, ByteBuffer out) {
        out.putShort(v);
    }

    protected final void serializeInt(int v, ByteBuffer out) {
        out.putInt(v);
    }

    protected final void serializeLong(long v, ByteBuffer out) {
        out.putLong(v);
    }

    protected final void serializeFloat(float v, ByteBuffer out) {
        out.putFloat(v);
    }

    protected final void serializeDouble(double v, ByteBuffer out) {
        out.putDouble(v);
    }

    protected final void serializeChar(char c, ByteBuffer out) {
        out.putChar(c);
    }

    protected final SizeMarker serializeSize(final ByteBuffer out) {
        out.putInt(0);
        final int start = out.position();
        return new SizeMarker() {
            public void close() {
                int size = out.position() - start;
                out.putInt(start, size);
            }
        };
    }

    protected final void serializeSmallNonNegativeInteger(long v, ByteBuffer out) {
        if (v < 0 || v > Integer.MAX_VALUE) {
            serializeByte((byte)-3, out);
            serializeInt((int)v, out);
        } else if (v > Short.MAX_VALUE) {
            serializeByte((byte)-2, out);
            serializeInt((int)v, out);
        } else if (v > Byte.MAX_VALUE) {
            serializeByte((byte)-1, out);
            serializeShort((short)v, out);
        } else {
            serializeByte((byte)v, out);
        }

    }

    protected void serializeString(String value, ByteBuffer out) {
        byte[] bytes = value.getBytes(Charset.forName("UTF-8"));
        serializeSmallNonNegativeInteger(bytes.length, out);
        out.put(bytes);
    }

    protected void serializeType(Class<?> value, ByteBuffer out) {
        serializeString(value.getName(), out);
    }

    protected <T> void serializeObject(T obj, ByteBuffer out) {
        if (obj != null) {
            serializeBoolean(false, out);
            SerializerStrategy<? super T> strategy = SerializerRegistry.forObject(obj);
            serializeTypeMarker(strategy, out);
            strategy.serialize(obj, out);
        } else {
            serializeBoolean(true, out);
        }
    }

    protected final <T> SerializerStrategy<T> deserializeTypeMarker(ByteBuffer in) {
        return SerializerRegistry.forTypeMarker((char)in.get());
    }

    protected final boolean deserializeBoolean(ByteBuffer in) {
        return in.get() != 0;
    }

    protected final byte deserializeByte(ByteBuffer in) {
        return in.get();
    }

    protected final short deserializeShort(ByteBuffer in) {
        return in.getShort();
    }

    protected final int deserializeInt(ByteBuffer in) {
        return in.getInt();
    }

    protected final long deserializeLong(ByteBuffer in) {
        return in.getLong();
    }

    protected final float deserializeFloat(ByteBuffer in) {
        return in.getFloat();
    }

    protected final double deserializeDouble(ByteBuffer in) {
        return in.getDouble();
    }

    protected final char deserializeChar(ByteBuffer in) {
        return in.getChar();
    }

    protected final int deserializeSize(ByteBuffer in) {
        return in.getInt();
    }

    protected final int deserializeSizeAndSkip(ByteBuffer in) {
        int size = in.getInt();
        skip(size, in);
        return size;
    }

    protected final long deserializeSmallNonNegativeInteger(ByteBuffer in) {
        long v = deserializeByte(in);
        if (v == -1) {
            v = deserializeShort(in);
        } else if (v == -2) {
            v = deserializeInt(in);
        } else if (v == -3) {
            v = deserializeLong(in);
        }
        return v;
    }

    protected final String deserializeString(ByteBuffer in) {
        int l = deserializeByte(in);
        if (l == -1) {
            l = deserializeShort(in);
        } else if (l == -2) {
            l = deserializeInt(in);
        }
        byte[] buffer = new byte[l];
        in.get(buffer);
        return new String(buffer, Charset.forName("UTF-8"));
    }

    protected final Class<?> deserializeType(ByteBuffer in) {
        try {
            return Class.forName(deserializeString(in));
        } catch (ClassNotFoundException e) {
            throw new SerializerException(e);
        }
    }

    protected final <T> T deserializeObject(ByteBuffer in) {
        boolean isNull = deserializeBoolean(in);
        if (isNull) {
            return null;
        }
        SerializerStrategy<T> strategy = deserializeTypeMarker(in);
        return strategy.deserialize(in);
    }

    protected final void skip(int size, ByteBuffer in) {
        if (size < 0) {
            throw new IllegalArgumentException("Size is negative: " + size);
        }
        if (size > in.remaining()) {
            throw new IllegalArgumentException("Size is greater than what remains in the buffer: " + size + " > " + in.remaining());
        }
        in.position(in.position() + size);
    }

    protected interface SizeMarker {
        void close();
    }
}
