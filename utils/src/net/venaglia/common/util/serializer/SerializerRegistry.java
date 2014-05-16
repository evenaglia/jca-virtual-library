package net.venaglia.common.util.serializer;

import net.venaglia.common.util.Secondary;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 5:54 PM
 */
public class SerializerRegistry {

    private static final SerializerRegistry INSTANCE = new SerializerRegistry();

    static {
        new AbstractSerializerStrategy<Void>(Void.class, '\0') {
            public void serialize(Void value, ByteBuffer out) {
                // no-op
            }

            public Void deserialize(ByteBuffer in) {
                return null;
            }
        };
        PrimitiveSerializerStrategy.init();
        StringSerializerStrategy.init();
        ArraySerializerStrategy.init();
        CollectionSerializerStrategy.init();
        MapSerializerStrategy.init();
    }

    private final ConcurrentMap<Character,SerializerStrategy<?>> byTypeMarker = new ConcurrentHashMap<Character,SerializerStrategy<?>>();
    private final ConcurrentMap<Class<?>,SerializerStrategy<?>> byType = new ConcurrentHashMap<Class<?>,SerializerStrategy<?>>();

    private SerializerRegistry() {
    }

    private void addImpl(SerializerStrategy<?> strategy) {
        if (byType.containsKey(strategy.getJavaType())) {
            if (strategy.getClass().getAnnotation(Secondary.class) == null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot add SerializerStrategy<%s>, another strategy already registered for type '%s': SerializerStrategy<%s>",
                        strategy.getJavaType().getSimpleName(),
                        strategy.getJavaType(),
                        byType.get(strategy.getJavaType()).getJavaType().getSimpleName()));
            }
        }
        if (byTypeMarker.containsKey(strategy.getTypeMarker())) {
            if (strategy.getClass().getAnnotation(Secondary.class) == null) {
                throw new IllegalArgumentException(String.format(
                        "Cannot add SerializerStrategy<%s>, another strategy already registered for marker '%s': SerializerStrategy<%s>",
                        strategy.getJavaType().getSimpleName(),
                        strategy.getTypeMarker(),
                        byTypeMarker.get(strategy.getTypeMarker()).getJavaType().getSimpleName()));
            }
        }
        if (!byType.containsKey(strategy.getJavaType())) {
            byType.put(strategy.getJavaType(), strategy);
        }
        if (!byTypeMarker.containsKey(strategy.getTypeMarker())) {
            byTypeMarker.put(strategy.getTypeMarker(), strategy);
        }
    }

    private <T> SerializerStrategy<T> forObjectImpl(Object o) throws SerializerException {
        SerializerStrategy<?> result = null;
        if (o == null) {
            result = byTypeMarker.get('\0');
        } else {
            Class<?> type = o.getClass();
            while (result == null && type != Object.class) {
                result = byType.get(type);
                if (result != null && !result.accept(o)) {
                    result = null;
                }
                if (result == null) {
                    type = type.getSuperclass();
                }
            }
            if (result == null) {
                for (Class<?> c : o.getClass().getInterfaces()) {
                    result = byType.get(c);
                    if (result != null && !result.accept(o)) {
                        result = null;
                    }
                }
            } else if (result.getJavaType() != type) {
                byType.put(type, result);
            }
        }
        if (result == null && o != null) {
            throw new SerializerException("Cannot find serializer for class " + o.getClass());
        }
        //noinspection unchecked
        return (SerializerStrategy<T>)result;
    }

    private <T> SerializerStrategy<T> forObjectTypeImpl(Class<?> t) throws SerializerException {
        Class<?> type = t;
        SerializerStrategy<?> result = null;
        while (result == null && type != Object.class) {
            result = byType.get(type);
            if (result != null && !result.accept(t)) {
                result = null;
            }
            if (result == null) {
                type = type.getSuperclass();
            }
        }
        if (result == null) {
            for (Class<?> c : t.getInterfaces()) {
                result = byType.get(c);
                if (result != null && !result.accept(t)) {
                    result = null;
                }
            }
        } else if (result.getJavaType() != type) {
            byType.put(type, result);
        }
        if (result == null) {
            throw new SerializerException("Cannot find serializer for class " + type);
        }
        //noinspection unchecked
        return (SerializerStrategy<T>)result;
    }

    private <T> SerializerStrategy<T> forTypeMarkerImpl(char typeMarker) throws SerializerException {
        SerializerStrategy<?> strategy = byTypeMarker.get(typeMarker);
        if (strategy == null) {
            throw new SerializerException("Cannot find serialzer for type marker '" + typeMarker + "'");
        }
        //noinspection unchecked
        return (SerializerStrategy<T>)strategy;
    }

    public static void add(SerializerStrategy<?> strategy) {
        INSTANCE.addImpl(strategy);
    }

    public static <T> SerializerStrategy<? super T> forObject(T obejct) throws SerializerException {
        return INSTANCE.forObjectImpl(obejct);
    }

    public static <T> SerializerStrategy<? super T> forObjectType(Class<T> type) throws SerializerException {
        return INSTANCE.forObjectTypeImpl(type);
    }

    public static <T> SerializerStrategy<T> forTypeMarker(char typeMarker) throws SerializerException {
        return INSTANCE.forTypeMarkerImpl(typeMarker);
    }

    public static boolean canHandle(Object object) {
        try {
            forObject(object);
            return true;
        } catch (SerializerException e) {
            return false;
        }
    }

    public static boolean canHandle(Class<?> type) {
        try {
            forObjectType(type);
            return true;
        } catch (SerializerException e) {
            return false;
        }
    }
}
