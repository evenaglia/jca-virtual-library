package net.venaglia.common.util.extensible;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 1/25/15
 * Time: 9:06 PM
 */
public class ExtensibleObjectRegistry<O extends ExtensibleObject> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Class<O> type;
    private final Map<ExtendedPropertyKey<?>,WeakReference<ExtendedPropertyProvider<O,?>>> providers;
    private final Collection<ExtendedPropertyKey<?>> keys;

    private ExtensibleObjectRegistry(Class<O> type) {
        this.type = type;
        this.providers = new WeakHashMap<ExtendedPropertyKey<?>,WeakReference<ExtendedPropertyProvider<O,?>>>(4);
        this.keys = Collections.unmodifiableSet(providers.keySet());
    }

    @SuppressWarnings("unchecked")
    private <V> ExtendedPropertyProvider<O,V> getProvider(ExtendedPropertyKey<V> key) {
        WeakReference<ExtendedPropertyProvider<O,?>> ref;
        lock.readLock().lock();
        try {
            ref = providers.get(key);
        } finally {
            lock.readLock().unlock();
        }
        ExtendedPropertyProvider<O,?> provider;
        if (ref == null) {
            lock.writeLock().lock();
            try {
                providers.remove(key);
            } finally {
                lock.writeLock().unlock();
            }
            return null;
        } else {
            provider = ref.get();
        }
        return (ExtendedPropertyProvider<O,V>)provider;
    }

    private <V> void addProvider(ExtendedPropertyProvider<O,V> provider) {
        WeakReference<ExtendedPropertyProvider<O,?>> ref = new WeakReference<ExtendedPropertyProvider<O,?>>(provider);
        lock.writeLock().lock();
        try {
            ref = providers.putIfAbsent(provider.getKey(), ref);
        } finally {
            lock.writeLock().unlock();
        }
        ExtendedPropertyProvider<O,?> prev = ref == null ? null : ref.get();
        if (prev != null && prev != provider) {
            throw new ExtendedPropertyProviderException("An ExtendedPropertyProvider is already registered for " + type.getSimpleName() + "." + provider.getKey());
        }
    }

    private Collection<ExtendedPropertyKey<?>> getKeys() {
        return keys;
    }

    enum WhenMissing {
        CREATE, FAIL, NULL
    }

    private static ConcurrentHashMap<Class<?>,ExtensibleObjectRegistry<?>> REGISTRY_BY_TYPE =
            new ConcurrentHashMap<Class<?>,ExtensibleObjectRegistry<?>>();

    @SuppressWarnings("unchecked")
    private static <O extends ExtensibleObject> ExtensibleObjectRegistry<O> getRegistry(Class<O> type, WhenMissing whenMissing) {
        ExtensibleObjectRegistry<?> registry = REGISTRY_BY_TYPE.get(type);
        if (registry == null) {
            switch (whenMissing) {
                case CREATE:
                    registry = new ExtensibleObjectRegistry<O>(type);
                    ExtensibleObjectRegistry<?> swap = REGISTRY_BY_TYPE.putIfAbsent(type, registry);
                    if (swap != null) {
                        registry = swap;
                    }
                    break;
                case FAIL:
                    throw new ExtendedPropertyProviderException(type.getSimpleName() + " is not registered as an ExtensibleObject");
                case NULL:
                    return null;
            }
        }
        return (ExtensibleObjectRegistry<O>)registry;
    }

    private static <O extends ExtensibleObject,V> ExtendedPropertyProvider<O,V> getProviderImpl(Class<O> type, ExtendedPropertyKey<V> key) {
        if (type == null) throw new NullPointerException("type");
        if (key == null) throw new NullPointerException("key");
        ExtendedPropertyProvider<O,V> provider = getRegistry(type, WhenMissing.FAIL).getProvider(key);
        if (provider == null) {
            throw new NoSuchPropertyException(key);
        }
        return provider;
    }

    public static <O extends ExtensibleObject> void register(ExtendedPropertyProvider<O,?> provider) {
        getRegistry(provider.getType(), WhenMissing.CREATE).addProvider(provider);
    }

    public static <O extends ExtensibleObject,V> ExtendedPropertyProvider<O,V> getProvider(Class<O> type, ExtendedPropertyKey<V> key) {
        return getProviderImpl(type, key);
    }

    public static <O extends ExtensibleObject> ExtendedPropertyProvider.IntProvider<O> getProvider(Class<O> type, ExtendedPropertyKey.IntKey key) {
        return (ExtendedPropertyProvider.IntProvider<O>)getProviderImpl(type, key);
    }

    public static <O extends ExtensibleObject> ExtendedPropertyProvider.LongProvider<O> getProvider(Class<O> type, ExtendedPropertyKey.LongKey key) {
        return (ExtendedPropertyProvider.LongProvider<O>)getProviderImpl(type, key);
    }

    public static <O extends ExtensibleObject> ExtendedPropertyProvider.FloatProvider<O> getProvider(Class<O> type, ExtendedPropertyKey.FloatKey key) {
        return (ExtendedPropertyProvider.FloatProvider<O>)getProviderImpl(type, key);
    }

    public static <O extends ExtensibleObject> ExtendedPropertyProvider.DoubleProvider<O> getProvider(Class<O> type, ExtendedPropertyKey.DoubleKey key) {
        return (ExtendedPropertyProvider.DoubleProvider<O>)getProviderImpl(type, key);
    }

    public static <O extends ExtensibleObject> ExtendedPropertyProvider.BooleanProvider<O> getProvider(Class<O> type, ExtendedPropertyKey.BooleanKey key) {
        return (ExtendedPropertyProvider.BooleanProvider<O>)getProviderImpl(type, key);
    }

    public static Collection<ExtendedPropertyKey<?>> getKeys(Class<? extends ExtensibleObject> type) {
        ExtensibleObjectRegistry<?> registry = getRegistry(type, WhenMissing.NULL);
        return registry == null ? Collections.emptySet() : registry.getKeys();
    }
}
