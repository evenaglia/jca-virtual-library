package net.venaglia.common.util.serializer;

import net.venaglia.common.util.Predicate;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 9:02 PM
 */
@SuppressWarnings("UnusedDeclaration")
public abstract class PrimitiveSerializerStrategy<T> extends AbstractSerializerStrategy<T>  {

    public static final Map<String, Class<?>> PRIMITIVE_TYPES_BY_NAME;

    static {
        Map<String,Class<?>> types = new LinkedHashMap<String,Class<?>>();
        types.put("boolean", boolean.class);
        types.put("byte", byte.class);
        types.put("short", short.class);
        types.put("int", int.class);
        types.put("long", long.class);
        types.put("float", float.class);
        types.put("double", double.class);
        types.put("char", char.class);
        PRIMITIVE_TYPES_BY_NAME = Collections.unmodifiableMap(types);
    }

    @SuppressWarnings("unchecked")
    private PrimitiveSerializerStrategy(Class<T> type, char typeMarker) {
        super(type, typeMarker);
    }

    private SerializerStrategy<T> forPrimitive(final char typeMarker) {
        final Class<T> primitiveType;
        try {
            //noinspection unchecked
            primitiveType = (Class<T>)type.getField("TYPE").get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        SerializerStrategy<T> result = new SerializerStrategy<T>() {
            @SuppressWarnings("unchecked")
            public Class<T> getJavaType() {
                return primitiveType;
            }

            public char getTypeMarker() {
                return typeMarker;
            }

            public boolean accept(Object o) {
                return PrimitiveSerializerStrategy.this.accept(o);
            }

            public boolean accept(Class<?> type) {
                return PrimitiveSerializerStrategy.this.type.equals(type);
            }

            public void serialize(T value, ByteBuffer out) {
                PrimitiveSerializerStrategy.this.serialize(value, out);
            }

            public T deserialize(ByteBuffer in) {
                return PrimitiveSerializerStrategy.this.deserialize(in);
            }

            public void deserializePartial(ByteBuffer in, Predicate<String> filter, Map<String,Object> out) {
                throw new UnsupportedOperationException();
            }
        };
        SerializerRegistry.add(result);
        return result;
    }

    public static final PrimitiveSerializerStrategy<Boolean> BOOLEAN_OBJ = new PrimitiveSerializerStrategy<Boolean>(Boolean.class, '1') {
        public void serialize(Boolean value, ByteBuffer out) {
            serializeBoolean(null, value, out);
        }

        public Boolean deserialize(ByteBuffer in) {
            return deserializeBoolean(null, in);
        }
    };

    public static final SerializerStrategy<Boolean> BOOLEAN = BOOLEAN_OBJ.forPrimitive('0');

    public static final PrimitiveSerializerStrategy<Byte> BYTE_OBJ = new PrimitiveSerializerStrategy<Byte>(Byte.class, 'B') {
        public void serialize(Byte value, ByteBuffer out) {
            serializeByte(null, value, out);
        }

        public Byte deserialize(ByteBuffer in) {
            return deserializeByte(null, in);
        }
    };

    public static final SerializerStrategy<Byte> BYTE = BYTE_OBJ.forPrimitive('b');

    public static final PrimitiveSerializerStrategy<Short> SHORT_OBJ = new PrimitiveSerializerStrategy<Short>(Short.class, 'S') {
        public void serialize(Short value, ByteBuffer out) {
            serializeShort(null, value, out);
        }

        public Short deserialize(ByteBuffer in) {
            return deserializeShort(null, in);
        }
    };

    public static final SerializerStrategy<Short> SHORT = SHORT_OBJ.forPrimitive('s');

    public static final PrimitiveSerializerStrategy<Integer> INTEGER_OBJ = new PrimitiveSerializerStrategy<Integer>(Integer.class, 'I') {
        public void serialize(Integer value, ByteBuffer out) {
            serializeInt(null, value, out);
        }

        public Integer deserialize(ByteBuffer in) {
            return deserializeInt(null, in);
        }
    };

    public static final SerializerStrategy<Integer> INTEGER = INTEGER_OBJ.forPrimitive('i');

    public static final PrimitiveSerializerStrategy<Long> LONG_OBJ = new PrimitiveSerializerStrategy<Long>(Long.class, 'L') {
        public void serialize(Long value, ByteBuffer out) {
            serializeLong(null, value, out);
        }

        public Long deserialize(ByteBuffer in) {
            return deserializeLong(null, in);
        }
    };

    public static final SerializerStrategy<Long> LONG = LONG_OBJ.forPrimitive('l');

    public static final PrimitiveSerializerStrategy<Float> FLOAT_OBJ = new PrimitiveSerializerStrategy<Float>(Float.class, 'F') {
        public void serialize(Float value, ByteBuffer out) {
            serializeFloat(null, value, out);
        }

        public Float deserialize(ByteBuffer in) {
            return deserializeFloat(null, in);
        }
    };

    public static final SerializerStrategy<Float> FLOAT = FLOAT_OBJ.forPrimitive('f');

    public static final PrimitiveSerializerStrategy<Double> DOUBLE_OBJ = new PrimitiveSerializerStrategy<Double>(Double.class, 'D') {
        public void serialize(Double value, ByteBuffer out) {
            serializeDouble(null, value, out);
        }

        public Double deserialize(ByteBuffer in) {
            return deserializeDouble(null, in);
        }
    };

    public static final SerializerStrategy<Double> DOUBLE = DOUBLE_OBJ.forPrimitive('d');

    public static final PrimitiveSerializerStrategy<Character> CHAR_OBJ = new PrimitiveSerializerStrategy<Character>(Character.class, 'C') {
        public void serialize(Character value, ByteBuffer out) {
            serializeChar(null, value, out);
        }

        public Character deserialize(ByteBuffer in) {
            return deserializeChar(null, in);
        }
    };

    public static final SerializerStrategy<Character> CHAR = CHAR_OBJ.forPrimitive('c');

    static void init() {
        // no-op, just here to force this class to initialize
    }
}
