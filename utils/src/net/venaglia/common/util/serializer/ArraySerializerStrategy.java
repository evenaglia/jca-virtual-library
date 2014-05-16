package net.venaglia.common.util.serializer;

import java.nio.ByteBuffer;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 5:51 PM
 */
public class ArraySerializerStrategy extends AbstractSerializerStrategy<Object> {

    public static final ArraySerializerStrategy INSTANCE = new ArraySerializerStrategy();

    public static final SerializerStrategy<boolean[]> BOOLEANS = INSTANCE.castFor(boolean[].class);
    public static final SerializerStrategy<byte[]> BYTES = INSTANCE.castFor(byte[].class);
    public static final SerializerStrategy<short[]> SHORTS = INSTANCE.castFor(short[].class);
    public static final SerializerStrategy<int[]> INTS = INSTANCE.castFor(int[].class);
    public static final SerializerStrategy<long[]> LONGS = INSTANCE.castFor(long[].class);
    public static final SerializerStrategy<float[]> FLOATS = INSTANCE.castFor(float[].class);
    public static final SerializerStrategy<double[]> DOUBLES = INSTANCE.castFor(double[].class);
    public static final SerializerStrategy<char[]> CHARS = INSTANCE.castFor(char[].class);

    private final Map<Class<?>,PrimitiveSerializer> primitiveSerializers;

    private ArraySerializerStrategy() {
        super(Object.class, '[');
        Map<Class<?>,PrimitiveSerializer> primitiveSerializers = new HashMap<Class<?>,PrimitiveSerializer>();
        primitiveSerializers.put(boolean[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeBooleans((boolean[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(byte[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeBytes((byte[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(short[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeShorts((short[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(int[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeInts((int[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(long[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeLongs((long[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(float[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeFloats((float[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(double[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeDoubles((double[])primitiveArray, out);
            }
        });
        primitiveSerializers.put(char[].class, new PrimitiveSerializer() {
            public void serialize(Object primitiveArray, ByteBuffer out) {
                serializeChars((char[])primitiveArray, out);
            }
        });
        this.primitiveSerializers = Collections.unmodifiableMap(primitiveSerializers);
    }

    @Override
    public boolean accept(Class<?> type) {
        return type.isArray() && SerializerRegistry.canHandle(type.getComponentType());
    }

    public void serialize(Object value, ByteBuffer out) {
        if (value instanceof Object[]) {
            serializeObjects((Object[])value, out);
            return;
        }
        PrimitiveSerializer primitiveSerializer = primitiveSerializers.get(value.getClass());
        if (primitiveSerializer == null) {
            throw new SerializerException("Unable to serialize: " + value.getClass());
        }
        primitiveSerializer.serialize(value, out);
    }

    public static <A> SerializerStrategy<A[]> getHomogenousArraySerializer(final Class<A> componentType, final boolean mayContainNulls) {
        final SerializerStrategy<? super A> serializer = SerializerRegistry.forObjectType(componentType);
        if (serializer == null || !serializer.getJavaType().equals(componentType)) {
            throw new SerializerException("No serializer for " + componentType);
        }
        return new AbstractSerializerStrategy<A[]>() {

            @SuppressWarnings("unchecked")
            private A[] createArray(int length) {
                return (A[])Array.newInstance(componentType, length);
            }

            public void serialize(A[] value, ByteBuffer out) {
                if (value == null) {
                    serializeInt(-1, out);
                } else {
                    serializeInt(value.length, out);
                    for (A a : value) {
                        boolean isNull = mayContainNulls && a == null;
                        if (mayContainNulls) {
                            serializeBoolean(isNull, out);
                        }
                        if (!isNull) {
                            serializer.serialize(a, out);
                        }
                    }
                }
            }

            public A[] deserialize(ByteBuffer in) {
                int length = deserializeInt(in);
                A[] result = null;
                if (length >= 0) {
                    result = createArray(length);
                    for (int i = 0; i < length; i++) {
                        boolean isNull = mayContainNulls && deserializeBoolean(in);
                        if (!isNull) {
                            result[i] = componentType.cast(serializer.deserialize(in));
                        }
                    }
                }
                return result;
            }
        };
    }

    private <C> void serializeObjects(C[] value, ByteBuffer out) {
        @SuppressWarnings("unchecked")
        Class<C> componentType = (Class<C>)value.getClass().getComponentType();
        SerializerStrategy<? super C> elementStrategy = SerializerRegistry.forObjectType(componentType);
        int length = value.length;

        serializeTypeMarker(elementStrategy, out);
        serializeSmallNonNegativeInteger(length, out);
        serializeType(componentType, out);
        for (C c : value) {
            if (c != null) {
                serializeBoolean(false, out);
                elementStrategy.serialize(c, out);
            } else {
                serializeBoolean(true, out);
            }
        }
    }

    private void serializeBooleans(boolean[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Boolean.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        for (int i = 0; i < length; i++) {
            serializeBoolean(value[i], out);
        }
    }

    private void serializeBytes(byte[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Byte.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        out.put(value);
    }

    private void serializeShorts(short[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Short.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        for (int i = 0; i < length; i++) {
            serializeShort(value[i], out);
        }
    }

    private void serializeInts(int[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Integer.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        for (int i = 0; i < length; i++) {
            serializeInt(value[i], out);
        }
    }

    private void serializeLongs(long[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Long.TYPE, out);
        for (int i = 0; i < length; i++) {
            serializeLong(value[i], out);
        }
    }

    private void serializeFloats(float[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Float.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        for (int i = 0; i < length; i++) {
            serializeFloat(value[i], out);
        }
    }

    private void serializeDoubles(double[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Double.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        for (int i = 0; i < length; i++) {
            serializeDouble(value[i], out);
        }
    }

    private void serializeChars(char[] value, ByteBuffer out) {
        int length = Array.getLength(value);
        serializeType(Character.TYPE, out);
        serializeSmallNonNegativeInteger(length, out);
        for (int i = 0; i < length; i++) {
            serializeChar(value[i], out);
        }
    }

    public Object deserialize(ByteBuffer in) {
        Class<?> componentType = deserializeType(in);
        int length = (int)deserializeSmallNonNegativeInteger(in);
        SerializerStrategy<?> elementStrategy = deserializeTypeMarker(in);
        switch (elementStrategy.getTypeMarker()) {
            case '0':
                return deserializeBooleans(in, length);
            case 'b':
                return deserializeBytes(in, length);
            case 's':
                return deserializeShorts(in, length);
            case 'i':
                return deserializeInts(in, length);
            case 'l':
                return deserializeLongs(in, length);
            case 'f':
                return deserializeFloats(in, length);
            case 'd':
                return deserializeDoubles(in, length);
        }
        Object[] result = (Object[])Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            boolean isNull = deserializeBoolean(in);
            if (!isNull) {
                result[i] = elementStrategy.deserialize(in);
            }
        }
        return result;
    }

    private boolean[] deserializeBooleans(ByteBuffer in, int length) {
        boolean[] value = new boolean[length];
        for (int i = 0; i < length; i++) {
            value[i] = deserializeBoolean(in);
        }
        return value;
    }

    private byte[] deserializeBytes(ByteBuffer in, int length) {
        byte[] value = new byte[length];
        in.get(value);
        return value;
    }

    private short[] deserializeShorts(ByteBuffer in, int length) {
        short[] value = new short[length];
        for (int i = 0; i < length; i++) {
            value[i] = in.getShort();
        }
        return value;
    }

    private int[] deserializeInts(ByteBuffer in, int length) {
        int[] value = new int[length];
        for (int i = 0; i < length; i++) {
            value[i] = in.getInt();
        }
        return value;
    }

    private long[] deserializeLongs(ByteBuffer in, int length) {
        long[] value = new long[length];
        for (int i = 0; i < length; i++) {
            value[i] = in.getLong();
        }
        return value;
    }

    private float[] deserializeFloats(ByteBuffer in, int length) {
        float[] value = new float[length];
        for (int i = 0; i < length; i++) {
            value[i] = in.getFloat();
        }
        return value;
    }

    private double[] deserializeDoubles(ByteBuffer in, int length) {
        double[] value = new double[length];
        for (int i = 0; i < length; i++) {
            value[i] = in.getDouble();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private <A> SerializerStrategy<A> castFor(Class<A> type) {
        if (primitiveSerializers.containsKey(type) ||
            type.isArray() && SerializerRegistry.canHandle(type.getComponentType())) {
            return (SerializerStrategy<A>)this;
        }
        throw new ClassCastException("ArraySerializerStrategy cannot handle " + type);
    }

    interface PrimitiveSerializer {
        void serialize(Object primitiveArray, ByteBuffer out);
    }

    static void init() {
        // no-op, just here to force this class to initialize
    }
}
