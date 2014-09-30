package net.venaglia.common.util.serializer;

import net.venaglia.common.util.Pair;

import java.nio.ByteBuffer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 4/4/14
 * Time: 9:15 AM
 */
public class CollectionSerializerStrategy extends AbstractSerializerStrategy<Collection<?>> {

    private static final Class<? extends Collection> UNMODIFIABLE_COLLECTION_TYPE =
            Collections.unmodifiableCollection(new HashSet<Object>()).getClass();
    private static final Class<? extends Set> UNMODIFIABLE_SET_TYPE =
            Collections.unmodifiableSet(new HashSet<Object>()).getClass();
    private static final Class<? extends List> UNMODIFIABLE_LIST_TYPE =
            Collections.unmodifiableList(new ArrayList<Object>()).getClass();
    private static final Field INTERNAL_COLLECTION_FIELD;

    static {
        try {
            INTERNAL_COLLECTION_FIELD = UNMODIFIABLE_COLLECTION_TYPE.getDeclaredField("c");
            INTERNAL_COLLECTION_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final CollectionSerializerStrategy INSTANCE = new CollectionSerializerStrategy();

    public enum StandardCollectionType {
        HashSet('h'),
        LinkedHashSet('s'),
        ArrayList('a'),
        LinkedList('l'),
        UnmodifiableHashSet('H'),
        UnmodifiableLinkedHashSet('S'),
        UnmodifiableArrayList('A'),
        UnmodifiableLinkedList('L');

        private final Character characterKey;

        private StandardCollectionType(Character characterKey) {
            this.characterKey = characterKey;
        }

        public Character getCharacterKey() {
            return characterKey;
        }
    }

    private final Map<CollectionFactoryKey,CollectionFactory> supportedCollectionTypes;
    private final Map<Character,CollectionFactory> factoriesByKey;

    private CollectionSerializerStrategy() {
        //noinspection unchecked
        super((Class<Collection<?>>)(Class)Collection.class, '*');

        Map<CollectionFactoryKey,CollectionFactory> supportedMapTypes =
                new HashMap<CollectionFactoryKey,CollectionFactory>();
        CollectionFactory hashSetFactory = new CollectionFactory('h', HashSet.class);
        CollectionFactory linkedHashSetFactory = new CollectionFactory('s', LinkedHashSet.class);
        CollectionFactory arrayListFactory = new CollectionFactory('a', ArrayList.class);
        CollectionFactory linkedListFactory = new CollectionFactory('l', LinkedList.class);
        CollectionFactory unmodifiableHashSetFactory = new CollectionFactory('H', UNMODIFIABLE_SET_TYPE, hashSetFactory);
        CollectionFactory unmodifiableLinkedHashSetFactory = new CollectionFactory('S', UNMODIFIABLE_SET_TYPE, linkedHashSetFactory);
        CollectionFactory unmodifableArrayListFactory = new CollectionFactory('A', UNMODIFIABLE_LIST_TYPE, arrayListFactory);
        CollectionFactory unmodifiableLinkedListFactory = new CollectionFactory('L', UNMODIFIABLE_LIST_TYPE, linkedListFactory);
        supportedMapTypes.put(forMutableCollection(HashSet.class), hashSetFactory);
        supportedMapTypes.put(forMutableCollection(LinkedHashSet.class), linkedHashSetFactory);
        supportedMapTypes.put(forMutableCollection(ArrayList.class), arrayListFactory);
        supportedMapTypes.put(forMutableCollection(LinkedList.class), linkedListFactory);
        supportedMapTypes.put(forUnmodifiableCollection(HashSet.class), new CollectionFactory('1', UNMODIFIABLE_COLLECTION_TYPE, hashSetFactory));
        supportedMapTypes.put(forUnmodifiableCollection(LinkedHashSet.class), new CollectionFactory('2', UNMODIFIABLE_COLLECTION_TYPE, linkedHashSetFactory));
        supportedMapTypes.put(forUnmodifiableCollection(ArrayList.class), new CollectionFactory('3', UNMODIFIABLE_COLLECTION_TYPE, arrayListFactory));
        supportedMapTypes.put(forUnmodifiableCollection(LinkedList.class), new CollectionFactory('4', UNMODIFIABLE_COLLECTION_TYPE, linkedListFactory));
        supportedMapTypes.put(forUnmodifiableSet(HashSet.class), unmodifiableHashSetFactory);
        supportedMapTypes.put(forUnmodifiableSet(LinkedHashSet.class), unmodifiableLinkedHashSetFactory);
        supportedMapTypes.put(forUnmodifiableList(ArrayList.class), unmodifableArrayListFactory);
        supportedMapTypes.put(forUnmodifiableCollection(LinkedList.class), unmodifiableLinkedListFactory);
        Class<? extends Collection> emptySetType = Collections.emptySet().getClass();
        supportedMapTypes.put(forMutableCollection(emptySetType), new CollectionFactory('-', emptySetType) {
            @Override
            Set<Object> createCollection(int size) {
                return Collections.emptySet();
            }
        });
        Class<? extends Collection> emptyListType = Collections.emptyList().getClass();
        supportedMapTypes.put(forMutableCollection(emptyListType), new CollectionFactory('_', emptyListType) {
            @Override
            List<Object> createCollection(int size) {
                return Collections.emptyList();
            }
        });
        this.supportedCollectionTypes = Collections.unmodifiableMap(supportedMapTypes);

        Map<Character,CollectionFactory> factoriesByKey = new HashMap<Character,CollectionFactory>();
        for (CollectionFactory factory : supportedMapTypes.values()) {
            factoriesByKey.put(factory.getKey(), factory);
        }
        this.factoriesByKey = Collections.unmodifiableMap(factoriesByKey);

    }

    private CollectionFactoryKey forMutableCollection(Class<? extends Collection> mapType) {
        return new CollectionFactoryKey(mapType, null);
    }

    private CollectionFactoryKey forUnmodifiableCollection(Class<? extends Collection> mapType) {
        return new CollectionFactoryKey(UNMODIFIABLE_COLLECTION_TYPE, mapType);
    }

    private CollectionFactoryKey forUnmodifiableSet(Class<? extends Set> mapType) {
        return new CollectionFactoryKey(UNMODIFIABLE_SET_TYPE, mapType);
    }

    private CollectionFactoryKey forUnmodifiableList(Class<? extends List> mapType) {
        return new CollectionFactoryKey(UNMODIFIABLE_LIST_TYPE, mapType);
    }

    @Override
    public boolean accept(Object o) {
        if (o instanceof Collection) {
            CollectionFactoryKey key = getKey((Collection<?>)o);
            if (supportedCollectionTypes.containsKey(key) && supportedCollectionTypes.get(key).accept((Collection<?>)o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean accept(Class<?> type) {
        return Collection.class.isAssignableFrom(type) &&
               supportedCollectionTypes.containsKey(new CollectionFactoryKey(type, null));
    }

    public void serialize(Collection<?> value, ByteBuffer out) {
        CollectionFactory collectionFactory = supportedCollectionTypes.get(getKey(value));
        if (collectionFactory == null) {
            throw new SerializerException("No CollectionFactory found for collection: " + value.getClass());
        }
        serializeChar("collection<type>", collectionFactory.getKey(), out);
        serializeSmallNonNegativeInteger("collection.size", value.size(), out);
        SerializerDebugger.Marker marker = SerializerDebugger.start("collection");
        try {
            for (Object o : value) {
                serializeObject("collection.item(n)", o, out);
            }
        } finally {
            marker.close();
        }
    }

    public void serialize(Collection<?> value, ByteBuffer out, StandardCollectionType typeOverride) {
        CollectionFactory collectionFactory = typeOverride == null
                                              ? null
                                              : factoriesByKey.get(typeOverride.getCharacterKey());
        if (collectionFactory == null) {
            throw new SerializerException("No CollectionFactory found for collection: " + value.getClass());
        }
        serializeChar("collection<type>", collectionFactory.getKey(), out);
        serializeSmallNonNegativeInteger("collection.size", value.size(), out);
        SerializerDebugger.Marker marker = SerializerDebugger.start("collection");
        try {
            for (Object o : value) {
                serializeObject("collection.item(n)", o, out);
            }
        } finally {
            marker.close();
        }
    }

    public Collection<?> deserialize(ByteBuffer in) {
        char key = deserializeChar("collection<type>", in);
        int size = (int)deserializeSmallNonNegativeInteger("collection.size", in);
        CollectionFactory factory = factoriesByKey.get(key);
        Collection<Object> buffer = factory.createCollection(size);
        SerializerDebugger.Marker marker = SerializerDebugger.start("collection");
        try {
            for (int i = 0; i < size; i++) {
                Object v = deserializeObject("collection.item(n)", in);
                buffer.add(v);
            }
            return factory.finalizeCollection(buffer);
        } finally {
            marker.close();
        }
    }

    public <V> void deserialize(ByteBuffer in, Collection<V> buffer) {
        deserializeChar("collection<type>", in); // ignored, but exists for compatibility
        int size = (int)deserializeSmallNonNegativeInteger("collection.size", in);
        SerializerDebugger.Marker marker = SerializerDebugger.start("collection");
        try {
            for (int i = 0; i < size; i++) {
                V v = deserializeObject("collection.itme(n)", in);
                buffer.add(v);
            }
        } finally {
            marker.close();
        }
    }

    private static Collection<?> getWrapped(Collection<?> unmodifiableMap) {
        if (UNMODIFIABLE_COLLECTION_TYPE.isInstance(unmodifiableMap)) {
            try {
                return (Collection<?>)INTERNAL_COLLECTION_FIELD.get(unmodifiableMap);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private static CollectionFactoryKey getKey(Collection<?> collection) {
        if (UNMODIFIABLE_COLLECTION_TYPE.isInstance(collection)) {
            return new CollectionFactoryKey(UNMODIFIABLE_COLLECTION_TYPE, getWrapped(collection).getClass());
        }
        return new CollectionFactoryKey(collection.getClass(), null);
    }

    class CollectionFactory {

        private final char key;
        private final Class<Collection<?>> type;
        private final CollectionFactory mutableCollectionFactory;

        protected CollectionFactory(char key, Class<? extends Collection> type) {
            this(key, type, null);
        }

        @SuppressWarnings("unchecked")
        protected CollectionFactory(char key, Class<? extends Collection> type, CollectionFactory mutableCollectionFactory) {
            this.key = key;
            this.type = (Class<Collection<?>>)(Class)type;
            this.mutableCollectionFactory = mutableCollectionFactory;
        }

        char getKey() {
            return key;
        }

        @SuppressWarnings("unchecked")
        Collection<Object> createCollection(int size) {
            if (mutableCollectionFactory != null) {
                return mutableCollectionFactory.createCollection(size);
            }
            try {
                return (Collection<Object>)type.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        <V> Collection<V> finalizeCollection(Collection<?> map) {
            if (mutableCollectionFactory != null) {
                return Collections.unmodifiableCollection((Collection<? extends V>)mutableCollectionFactory.finalizeCollection(
                        map));
            } else {
                return (Collection<V>)map;
            }
        }

        boolean accept(Collection<?> o) {
            if (o == null) {
                return false;
            }
            if (mutableCollectionFactory == null) {
                return type.equals(o.getClass());
            } else {
                return type.equals(o.getClass()) && CollectionSerializerStrategy.this.accept(getWrapped(o));
            }
        }
    }

    static class CollectionFactoryKey extends Pair<Class<? extends Collection>,Class<? extends Collection>> {

        @SuppressWarnings("unchecked")
        CollectionFactoryKey(Class<?> mapType1, Class<?> mapType2) {
            super((Class<Collection>)mapType1, (Class<Collection>)mapType2);
        }
    }

    static void init() {
        // no-op, just here to force this class to initialize
    }
}
