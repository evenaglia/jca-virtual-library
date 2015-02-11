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

    @Override
    public T deserializePartial(ByteBuffer in, Predicate<? super String> filter) {
        throw new UnsupportedOperationException();
    }

    public void deserializePartial(ByteBuffer in, Predicate<? super String> filter, Map<String, Object> out) {
        throw new UnsupportedOperationException();
    }

    protected final void serializeTypeMarker(SerializerStrategy<?> serializer, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("<strategy>");
        try {
            out.put((byte)serializer.getTypeMarker());
        } finally {
            marker.close();
        }
    }

    protected final void serializeBoolean(String field, boolean v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.put((byte)(v ? 1 : 0));
        } finally {
            marker.close();
        }
    }

    protected final void serializeByte(String field, byte v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.put(v);
        } finally {
            marker.close();
        }
    }

    protected final void serializeShort(String field, short v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.putShort(v);
        } finally {
            marker.close();
        }
    }

    protected final void serializeInt(String field, int v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.putInt(v);
        } finally {
            marker.close();
        }
    }

    protected final void serializeLong(String field, long v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.putLong(v);
        } finally {
            marker.close();
        }
    }

    protected final void serializeFloat(String field, float v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.putFloat(v);
        } finally {
            marker.close();
        }
    }

    protected final void serializeDouble(String field, double v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.putDouble(v);
        } finally {
            marker.close();
        }
    }

    protected final void serializeChar(String field, char c, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            out.putChar(c);
        } finally {
            marker.close();
        }
    }

    protected final SizeMarker serializeSize(final ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("<size>");
        try {
            final int sizePosition = out.position();
            out.putInt(Integer.MIN_VALUE + 123);
            final int start = out.position();
            return new SizeMarker() {
                public void close() {
                    int size = out.position() - start;
                    assert out.getInt(sizePosition) == Integer.MIN_VALUE + 123;
                    out.putInt(sizePosition, size);
                }
            };
        } finally {
            marker.close();
        }
    }

    protected final void serializeSmallNonNegativeInteger(String field, long v, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("(snni)" + field);
        try {
            if (v < 0 || v > Integer.MAX_VALUE) {
                serializeByte(field, (byte)-3, out);
                serializeLong("(long)" + field, (int)v, out);
            } else if (v > Short.MAX_VALUE) {
                serializeByte(field, (byte)-2, out);
                serializeInt("(int)" + field, (int)v, out);
            } else if (v > Byte.MAX_VALUE) {
                serializeByte(field, (byte)-1, out);
                serializeShort("(short)" + field, (short)v, out);
            } else {
                serializeByte(field, (byte)v, out);
            }
        } finally {
            marker.close();
        }
    }

    protected void serializeString(String field, String value, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("(String)" + field);
        try {
            byte[] bytes = value.getBytes(Charset.forName("UTF-8"));
            serializeSmallNonNegativeInteger(field + ".length", bytes.length, out);
            out.put(bytes);
        } finally {
            marker.close();
        }
    }

    protected void serializeType(Class<?> value, ByteBuffer out) {
        serializeString("<type>", value.getName(), out);
    }

    protected <T> void serializeObject(String field, T obj, ByteBuffer out) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("(Object)" + field);
        try {
            if (obj != null) {
                serializeBoolean(field + ".isNull", false, out);
                SerializerStrategy<? super T> strategy = SerializerRegistry.forObject(obj);
                serializeTypeMarker(strategy, out);
                SerializerDebugger.Marker marker2 = SerializerDebugger.start(field);
                try {
                    strategy.serialize(obj, out);
                } finally {
                 marker2.close();
                }
            } else {
                serializeBoolean(field + ".isNull", true, out);
            }
        } finally {
            marker.close();
        }
    }

    protected final <T> SerializerStrategy<T> deserializeTypeMarker(ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("<strategy>");
        try {
            return SerializerRegistry.forTypeMarker((char)in.get());
        } finally {
            marker.close();
        }
    }

    protected final boolean deserializeBoolean(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.get() != 0;
        } finally {
            marker.close();
        }
    }

    protected final byte deserializeByte(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.get();
        } finally {
            marker.close();
        }
    }

    protected final short deserializeShort(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getShort();
        } finally {
            marker.close();
        }
    }

    protected final int deserializeInt(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getInt();
        } finally {
            marker.close();
        }
    }

    protected final long deserializeLong(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getLong();
        } finally {
            marker.close();
        }
    }

    protected final float deserializeFloat(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getFloat();
        } finally {
            marker.close();
        }
    }

    protected final double deserializeDouble(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getDouble();
        } finally {
            marker.close();
        }
    }

    protected final char deserializeChar(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getChar();
        } finally {
            marker.close();
        }
    }

    protected final int deserializeSize(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            return in.getInt();
        } finally {
            marker.close();
        }
    }

    protected final int deserializeSizeAndSkip(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start(field);
        try {
            int size = in.getInt();
            skip(size, in);
            return size;
        } finally {
            marker.close();
        }
    }

    protected final long deserializeSmallNonNegativeInteger(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("(snni)" + field);
        try {
            long v = deserializeByte(field, in);
            if (v == -1) {
                v = deserializeShort("(short)" + field, in);
            } else if (v == -2) {
                v = deserializeInt("(int)" + field, in);
            } else if (v == -3) {
                v = deserializeLong("(long)", in);
            }
            return v;
        } finally {
            marker.close();

        }
    }

    protected final String deserializeString(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("(String)" + field);
        try {
            int l = (int)deserializeSmallNonNegativeInteger(field + ".length", in);
            byte[] buffer = new byte[l];
            in.get(buffer);
            return new String(buffer, Charset.forName("UTF-8"));
        } finally {
            marker.close();
        }
    }

    protected final Class<?> deserializeType(ByteBuffer in) {
        try {
            String type = deserializeString("<type>", in);
            if (PrimitiveSerializerStrategy.PRIMITIVE_TYPES_BY_NAME.containsKey(type)) {
                return PrimitiveSerializerStrategy.PRIMITIVE_TYPES_BY_NAME.get(type);
            }
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new SerializerException(e);
        }
    }

    protected final <T> T deserializeObject(String field, ByteBuffer in) {
        SerializerDebugger.Marker marker = SerializerDebugger.start("(Object)" + field);
        try {
            boolean isNull = deserializeBoolean(field + ".isNull", in);
            if (isNull) {
                return null;
            }
            SerializerStrategy<T> strategy = deserializeTypeMarker(in);
            return strategy.deserialize(in);
        } finally {
            marker.close();
        }
    }

    protected final void skip(int size, ByteBuffer in) {
        if (size < 0) {
            throw new IllegalArgumentException("Size is negative: " + size);
        }
        if (size > in.remaining()) {
            throw new IllegalArgumentException("Size is greater than what remains in the buffer: " + size + " > " + in.remaining());
        }
        SerializerDebugger.Marker marker = SerializerDebugger.start("<skip>");
        try {
            in.position(in.position() + size);
        } finally {
            marker.close();
        }
    }

    protected interface SizeMarker {
        void close();
    }
}
