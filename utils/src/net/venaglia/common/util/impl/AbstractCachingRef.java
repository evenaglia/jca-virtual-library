package net.venaglia.common.util.impl;

import net.venaglia.common.util.Ref;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 2/27/13
 * Time: 6:11 PM
 */
public abstract class AbstractCachingRef<T> implements Ref<T> {

    private LazyRef<T> holder = new RefBeforeLoad<T>(this);

    public final T get() {
        return holder.get();
    }

    protected abstract T getImpl();

    abstract static class LazyRef<T> implements Ref<T> {
        final AbstractCachingRef<T> cachingRef;

        private LazyRef(AbstractCachingRef<T> cachingRef) {
            this.cachingRef = cachingRef;
        }
    }

    static class RefBeforeLoad<T> extends LazyRef<T> {

        private final Lock lock = new ReentrantLock();

        private boolean loaded = false;
        private T value;

        public RefBeforeLoad(AbstractCachingRef<T> cachingRef) {
            super(cachingRef);
        }

        public T get() {
            lock.lock();
            try {
                if (!loaded) {
                    loaded = true;
                    value = cachingRef.getImpl();
                    cachingRef.holder = new RefAfterLoad<T>(cachingRef, value);
                }
                return value;
            } finally {
                lock.unlock();
            }
        }

    }

    static class RefAfterLoad<T> extends LazyRef<T> {

        private final T value;

        private RefAfterLoad(AbstractCachingRef<T> cachingRef, T value) {
            super(cachingRef);
            this.value = value;
        }

        public T get() {
            return value;
        }
    }
}
