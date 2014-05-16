package net.venaglia.realms.common.map.data.binaries;

import net.venaglia.common.util.Predicate;
import net.venaglia.common.util.serializer.SerializerException;
import net.venaglia.common.util.serializer.SerializerStrategy;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 4/28/14
 * Time: 5:11 PM
 */
public class BinaryTypeDefinition<B> implements BinaryType {

    private static final Map<Class<?>,MetadataSerializer<?>> ALLOWED_TYPES;
    private static final MetadataSerializer<String> STRING_SERIALIZER = new StringMetadataSerializer();

    static {
        Map<Class<?>, MetadataSerializer<?>> map = new HashMap<Class<?>, MetadataSerializer<?>>();
        map.put(String.class, STRING_SERIALIZER);
        map.put(Integer.TYPE, new MetadataSerializer<Integer>() {

            public Integer cast(Object o) {
                return (Integer)o;
            }

            public Integer defaultValue() {
                return 0;
            }

            public String serialize(Integer value) {
                return value.toString();
            }

            public Integer deserialize(String value) {
                return Integer.decode(value);
            }
        });
        ALLOWED_TYPES = Collections.unmodifiableMap(map);
    }

    private final Class<B> type;
    private final String mimetype;
    private final SerializerStrategy<B> serializer;
    private final Map<String,MetadataAccessor<B,?>> metadataAccessors;
    private final Predicate<String> metadataFieldNamePredicate;

    private BinaryTypeDefinition(Class<B> type, String mimetype, SerializerStrategy<B> serializer) {
        if (type == null) throw new NullPointerException("type");
        if (mimetype == null) throw new NullPointerException("mimetype");
        if (serializer == null) throw new NullPointerException("serializer");
        this.type = type;
        this.mimetype = mimetype;
        this.serializer = serializer;
        this.metadataAccessors = new LinkedHashMap<String,MetadataAccessor<B,?>>();
        this.metadataFieldNamePredicate = new Predicate<String>() {
            public boolean allow(String value) {
                return metadataAccessors.containsKey(value);
            }
        };
        for (Field field : getAllFields(type)) {
            BinaryMetadata metadata = field.getAnnotation(BinaryMetadata.class);
            if (metadata != null) {
                Class<?> fieldType = field.getType();
                if (!ALLOWED_TYPES.containsKey(fieldType)) {
                    throw new IllegalArgumentException("Type not allowed on binary metadata: " + fieldType);
                }
                metadataAccessors.put(field.getName(), buildAccessor(field, fieldType));
            }
        }
    }

    private <F> MetadataAccessor<B,F> buildAccessor(Field field, Class<F> fieldType) {
        @SuppressWarnings("unchecked")
        F defaultValue = (F)ALLOWED_TYPES.get(fieldType).defaultValue();
        return new MetadataAccessor<B,F>(field, fieldType, defaultValue);
    }

    public Class<?> getJavaType() {
        return type;
    }

    public String mimeType() {
        return mimetype;
    }

    public Map<String,Object> generateMetadata(byte[] data) {
        if (metadataAccessors.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String,Object> metadata = new HashMap<String,Object>();
        try {
            serializer.deserializePartial(ByteBuffer.wrap(data), metadataFieldNamePredicate, metadata);
        } catch (UnsupportedOperationException e) {
            B object = serializer.deserialize(ByteBuffer.wrap(data));
            for (MetadataAccessor<B,?> accessor : metadataAccessors.values()) {
                metadata.put(accessor.getName(), accessor.get(object));
            }
        }
        return metadata;
    }

    public Map<String,Object> decodeMetadata(String encoded) {
        if (metadataAccessors.isEmpty() || encoded == null || encoded.length() == 0) {
            return Collections.emptyMap();
        }
        Map<String,Object> metadata = new HashMap<String,Object>();
        for (String nameValue : encoded.split("&")) {
            String[] nv = nameValue.split("=", 2);
            if (nv.length != 2) {
                throw new SerializerException("Malformed name/value pair: " + nameValue);
            }
            String name = nv[0];
            MetadataAccessor<B,?> accessor = metadataAccessors.get(name);
            MetadataSerializer<?> serializer = accessor == null ? null : ALLOWED_TYPES.get(accessor.getType());
            if (serializer != null) {
                metadata.put(name, serializer.deserialize(nv[1]));
            }
        }
        return metadata;
    }

    public String encodeMetadata(Map<String,Object> metadata) {
        if (metadataAccessors.isEmpty()) {
            return "";
        }
        StringBuilder buffer = new StringBuilder();
        for (Map.Entry<String,Object> entry : metadata.entrySet()) {
            String name = entry.getKey();
            serializeEntry(buffer, name, entry.getValue());
        }
        return buffer.toString();
    }

    public SerializerStrategy<B> getSerializer() {
        return serializer;
    }

    private <P> void serializeEntry(StringBuilder buffer, String name, Object value) {
        @SuppressWarnings("unchecked")
        MetadataAccessor<B,P> accessor = (MetadataAccessor<B,P>)metadataAccessors.get(name);
        if (accessor != null) {
            @SuppressWarnings("unchecked")
            MetadataSerializer<P> serializer = (MetadataSerializer<P>)(ALLOWED_TYPES.get(accessor.getType()));
            if (serializer != null) {
                if (buffer.length() > 0) {
                    buffer.append("&");
                }
                buffer.append(STRING_SERIALIZER.serialize(name))
                      .append("=")
                      .append(serializer.serialize(serializer.cast(value)));
            }
        }
    }

    private static Collection<Field> getAllFields(Class<?> type) {
        Map<String,Field> allFields = new LinkedHashMap<String,Field>();
        while (!Object.class.equals(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (!allFields.containsKey(field.getName())) {
                    allFields.put(field.getName(), field);
                }
            }
            type = type.getSuperclass();
        }
        return allFields.values();
    }

    public static <B> BinaryTypeDefinition<B> build(Class<B> type, String mimetype, SerializerStrategy<B> serializer) {
        return new BinaryTypeDefinition<B>(type, mimetype, serializer);
    }

    private static class MetadataAccessor<O,F> {
        private final Field field;
        private final Class<F> type;
        private final F defaultValue;

        private MetadataAccessor(Field field, Class<F> type, F defaultValue) {
            this.field = field;
            this.type = type;
            this.defaultValue = defaultValue;
        }

        String getName() {
            return field.getName();
        }

        Class<F> getType() {
            return type;
        }

        void set(O object, F value) {
            try {
                field.set(object, value);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        F get(O object) {
            Object value = null;
            try {
                value = field.get(object);
                return value == null ? defaultValue : type.cast(value);
            } catch (ClassCastException e) {
                if (value == null) value = Void.TYPE;
                throw new RuntimeException("Unable to convert " + value + " <" + value.getClass().getSimpleName() + "> to " + type);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    private interface MetadataSerializer<P> {
        P cast(Object o);
        P defaultValue();
        String serialize(P value);
        P deserialize(String value);
    }

    private static class StringMetadataSerializer implements MetadataSerializer<String> {

        public String cast(Object o) {
            return (String)o;
        }

        public String defaultValue() {
            return null;
        }

        public String serialize(String value) {
            StringBuilder buffer = new StringBuilder(value.length());
            for (int i = 0, l = value.length(); i < l; i++) {
                char c = value.charAt(i);
                switch (i) {
                    case '\r':
                        buffer.append("\\r");
                        break;
                    case '\n':
                        buffer.append("\\n");
                        break;
                    case '\f':
                        buffer.append("\\f");
                        break;
                    case '\t':
                        buffer.append("\\t");
                        break;
                    case '\\':
                        buffer.append("\\\\");
                        break;
                    default:
                        if (c >= ' ' && c < '0') {
                            buffer.append(String.format("%%%02x", (int)c));
                        } else if (c >= '0' && c <= '~') {
                            buffer.append(c);
                        } else {
                            buffer.append(String.format("\\u%04x", (int)c));
                        }
                }
            }
            return buffer.toString();
        }

        public String deserialize(String value) {
            StringBuilder buffer = new StringBuilder(value.length());
            for (int i = 0, l = value.length(); i < l;) {
                char c = value.charAt(i++);
                if (c == '\\' && (i+1) < l) {
                    c = value.charAt(i++);
                    switch (c) {
                        case 'r':
                            buffer.append('\r');
                            break;
                        case 'n':
                            buffer.append('\n');
                            break;
                        case 'f':
                            buffer.append('\f');
                            break;
                        case 't':
                            buffer.append('\t');
                            break;
                        case 'u':
                            if ((i+4) < l) {
                                buffer.append((char)Integer.parseInt(value.substring(i,i+4), 16));
                                i += 4;
                                break;
                            }
                        default:
                            throw new RuntimeException("Bad string escape '\\" + c + "' in \"" + value + "\"");

                    }
                } else if (c == '%' && (i+2) < l) {
                    buffer.append((char)Integer.parseInt(value.substring(i,i+2), 16));
                    i += 2;
                } else if (c >= '0' && c <= '~') {
                    buffer.append(c);
                } else {
                    throw new RuntimeException("Bad character in string '" + c + "' in \"" + value + "\"");
                }
            }
            return buffer.toString();
        }
    }
}
