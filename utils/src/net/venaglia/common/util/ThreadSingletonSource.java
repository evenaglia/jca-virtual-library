package net.venaglia.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 4/2/14
 * Time: 9:20 AM
 */
public abstract class ThreadSingletonSource<T> implements Ref<T> {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();
    private static final ThreadLocal<Holder> THREAD_LOCAL = new ThreadLocal<Holder>() {
        @Override
        protected Holder initialValue() {
            return new Holder();
        }
    };

    private final Integer seq = SEQUENCE.getAndIncrement();

    public T get() {
        return THREAD_LOCAL.get().getImpl(this, seq);
    }

    protected abstract T newInstance();

    public static <T> ThreadSingletonSource<T> forType(final Class<T> type) {
        ThreadSingletonSource<T> source = new ThreadSingletonSource<T>() {
            @Override
            protected T newInstance() {
                try {
                    return type.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        source.newInstance(); // fail-fast check
        return source;
    }

    public static <T> ThreadSingletonSource<T> forType(final Factory<T> factory) {
        ThreadSingletonSource<T> source = new ThreadSingletonSource<T>() {
            @Override
            protected T newInstance() {
                return factory.createEmpty();
            }
        };
        source.newInstance(); // fail-fast check
        return source;
    }

    private static class Holder {

        private final Map<Integer,Object> cache = new HashMap<Integer,Object>();

        private <T> T getImpl(ThreadSingletonSource<T> source, Integer qualifier) {
            @SuppressWarnings("unchecked")
            T instance = (T)cache.get(qualifier);
            if (instance == null) {
                instance = source.newInstance();
                cache.put(qualifier, instance);
            }
            return instance;
        }

    }
}
