package net.venaglia.common.util.serializer;

import net.venaglia.common.util.Pair;

import java.nio.ByteBuffer;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: ed
 * Date: 4/4/14
 * Time: 9:15 AM
 */
public class MapSerializerStrategy extends AbstractSerializerStrategy<Map<?,?>> {

    private static final Class<? extends Map> UNMODIFIABLE_MAP_TYPE =
            Collections.unmodifiableMap(new HashMap<Object,Object>()).getClass();
    private static final Field INTERNAL_MAP_FIELD;

    static {
        try {
            INTERNAL_MAP_FIELD = UNMODIFIABLE_MAP_TYPE.getDeclaredField("m");
            INTERNAL_MAP_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final MapSerializerStrategy INSTANCE = new MapSerializerStrategy();

    public enum StandardMapType {
        HashMap('h'),
        LinkedHashMap('l'),
        ConcurrentHashMap('c'),
        UnmodifiableHashMap('H'),
        UnmodifiableLinkedHashMap('L'),
        UnmodifiableConcurrentHashMap('C');

        private final Character characterKey;

        private StandardMapType(Character characterKey) {
            this.characterKey = characterKey;
        }

        public Character getCharacterKey() {
            return characterKey;
        }
    }

    private final Map<MapFactoryKey,MapFactory> supportedMapTypes;
    private final Map<Character,MapFactory> factoriesByKey;

    private MapSerializerStrategy() {
        //noinspection unchecked
        super((Class<Map<?,?>>)(Class)Map.class, 'M');

        Map<MapFactoryKey,MapFactory> supportedMapTypes =
                new HashMap<MapFactoryKey,MapFactory>();
        MapFactory hashMapFactory = new MapFactory('h', HashMap.class);
        MapFactory linkedHashMapFactory = new MapFactory('l', LinkedHashMap.class);
        MapFactory concurrentHashMapFactory = new MapFactory('c', ConcurrentHashMap.class);
        MapFactory unmodifiableHashMapFactory = new MapFactory('H', UNMODIFIABLE_MAP_TYPE, hashMapFactory);
        MapFactory unmodifiableLinkedHashMapFactory = new MapFactory('L', UNMODIFIABLE_MAP_TYPE, linkedHashMapFactory);
        MapFactory unmodifiableConcurrentHashMapFactory = new MapFactory('C', UNMODIFIABLE_MAP_TYPE, concurrentHashMapFactory);
        supportedMapTypes.put(forMutableMap(HashMap.class), hashMapFactory);
        supportedMapTypes.put(forMutableMap(LinkedHashMap.class), linkedHashMapFactory);
        supportedMapTypes.put(forMutableMap(ConcurrentHashMap.class), concurrentHashMapFactory);
        supportedMapTypes.put(forUnmodifiableMap(HashMap.class), unmodifiableHashMapFactory);
        supportedMapTypes.put(forUnmodifiableMap(LinkedHashMap.class), unmodifiableLinkedHashMapFactory);
        supportedMapTypes.put(forUnmodifiableMap(ConcurrentHashMap.class), unmodifiableConcurrentHashMapFactory);
        Class<? extends Map> emptyMapType = Collections.emptyMap().getClass();
        supportedMapTypes.put(forMutableMap(emptyMapType), new MapFactory(' ', emptyMapType) {
            @Override
            Map<Object,Object> createMap(int size) {
                return Collections.emptyMap();
            }
        });
        this.supportedMapTypes = Collections.unmodifiableMap(supportedMapTypes);

        Map<Character,MapFactory> factoriesByKey = new HashMap<Character,MapFactory>();
        for (MapFactory factory : supportedMapTypes.values()) {
            factoriesByKey.put(factory.getKey(), factory);
        }
        this.factoriesByKey = Collections.unmodifiableMap(factoriesByKey);
    }

    private MapFactoryKey forMutableMap(Class<? extends Map> mapType) {
        return new MapFactoryKey(mapType, null);
    }

    private MapFactoryKey forUnmodifiableMap(Class<? extends Map> mapType) {
        return new MapFactoryKey(UNMODIFIABLE_MAP_TYPE, mapType);
    }

    @Override
    public boolean accept(Object o) {
        if (o instanceof Map) {
            MapFactoryKey key = getKey((Map<?,?>)o);
            if (supportedMapTypes.containsKey(key) && supportedMapTypes.get(key).accept((Map<?,?>)o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean accept(Class<?> type) {
        return Map.class.isAssignableFrom(type) &&
               supportedMapTypes.containsKey(new MapFactoryKey(type, null));
    }

    public void serialize(Map value, ByteBuffer out) {
        MapFactory mapFactory = supportedMapTypes.get(getKey(value));
        if (mapFactory == null) {
            throw new SerializerException("No MapFactory found for map: " + value.getClass());
        }
        serializeChar("map<type>", mapFactory.getKey(), out);
        serializeSmallNonNegativeInteger("map.size", value.size(), out);
        SerializerDebugger.Marker marker = SerializerDebugger.start("map");
        try {
            for (Map.Entry<?,?> o : ((Map<?,?>)value).entrySet()) {
                serializeObject("map.item(n).key", o.getKey(), out);
                serializeObject("map.item(n).value", o.getValue(), out);
            }
        } finally {
            marker.close();
        }
    }

    public void serialize(Map value, ByteBuffer out, StandardMapType typeOverride) {
        MapFactory mapFactory = typeOverride == null
                                ? null
                                : factoriesByKey.get(typeOverride.getCharacterKey());
        if (mapFactory == null) {
            throw new SerializerException("No MapFactory found for type override: " + typeOverride);
        }
        serializeChar("map<type>", mapFactory.getKey(), out);
        serializeSmallNonNegativeInteger("map.size", value.size(), out);
        SerializerDebugger.Marker marker = SerializerDebugger.start("map");
        try {
            for (Map.Entry<?,?> o : ((Map<?,?>)value).entrySet()) {
                serializeObject("map.item(n).key", o.getKey(), out);
                serializeObject("map.item(n).value", o.getValue(), out);
            }
        } finally {
            marker.close();
        }
    }

    public Map<?,?> deserialize(ByteBuffer in) {
        char key = deserializeChar("map<type>", in);
        int size = (int)deserializeSmallNonNegativeInteger("map.size", in);
        MapFactory factory = factoriesByKey.get(key);
        Map<Object,Object> buffer = factory.createMap(size);
        SerializerDebugger.Marker marker = SerializerDebugger.start("map");
        try {
            for (int i = 0; i < size; i++) {
                Object k = deserializeObject("map.item(n).key", in);
                Object v = deserializeObject("map.item(n).value", in);
                buffer.put(k, v);
            }
            return factory.finalizeMap(buffer);
        } finally {
            marker.close();
        }
    }

    public <K,V> void deserialize(ByteBuffer in, Map<K,V> buffer) {
        deserializeChar("map<type>", in); // ignored, but exists for compatibility
        int size = (int)deserializeSmallNonNegativeInteger("map.size", in);
        SerializerDebugger.Marker marker = SerializerDebugger.start("map");
        try {
            for (int i = 0; i < size; i++) {
                K k = deserializeObject("map.item(n).key", in);
                V v = deserializeObject("map.item(n).value", in);
                buffer.put(k, v);
            }
        } finally {
            marker.close();
        }
    }

    private static Map<?,?> getWrapped(Map<?,?> unmodifiableMap) {
        if (UNMODIFIABLE_MAP_TYPE.isInstance(unmodifiableMap)) {
            try {
                return (Map<?,?>)INTERNAL_MAP_FIELD.get(unmodifiableMap);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static MapFactoryKey getKey(Map<?,?> map) {
        if (UNMODIFIABLE_MAP_TYPE.isInstance(map)) {
            return new MapFactoryKey(UNMODIFIABLE_MAP_TYPE, getWrapped(map).getClass());
        }
        return new MapFactoryKey(map.getClass(), null);
    }

    class MapFactory {

        private final char key;
        private final Class<Map<?,?>> type;
        private final MapFactory mutableMapFactory;

        protected MapFactory(char key, Class<? extends Map> type) {
            this(key, type, null);
        }

        @SuppressWarnings("unchecked")
        protected MapFactory(char key, Class<? extends Map> type, MapFactory mutableMapFactory) {
            this.key = key;
            this.type = (Class<Map<?,?>>)(Class)type;
            this.mutableMapFactory = mutableMapFactory;
        }

        char getKey() {
            return key;
        }

        @SuppressWarnings("unchecked")
        Map<Object,Object> createMap(int size) {
            if (mutableMapFactory != null) {
                return mutableMapFactory.createMap(size);
            }
            try {
                return (Map<Object,Object>)type.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        <K,V> Map<K,V> finalizeMap(Map<?,?> map) {
            if (mutableMapFactory != null) {
                return Collections.unmodifiableMap((Map<? extends K, ? extends V>)mutableMapFactory.finalizeMap(map));
            } else {
                return (Map<K,V>)map;
            }
        }

        boolean accept(Map<?,?> o) {
            if (o == null) {
                return false;
            }
            if (mutableMapFactory == null) {
                return type.equals(o.getClass());
            } else {
                return type.equals(o.getClass()) && MapSerializerStrategy.this.accept(getWrapped(o));
            }
        }
    }

    static class MapFactoryKey extends Pair<Class<? extends Map>,Class<? extends Map>> {

        @SuppressWarnings("unchecked")
        MapFactoryKey(Class<?> mapType1, Class<?> mapType2) {
            super((Class<Map>)mapType1, (Class<Map>)mapType2);
        }
    }

    static void init() {
        // no-op, just here to force this class to initialize
    }
}
