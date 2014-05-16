package net.venaglia.common.util.impl;

import net.venaglia.common.util.Factory;
import net.venaglia.common.util.RecycleBuffer;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 4/11/14
 * Time: 5:41 PM
 */
public abstract class RecycleBufferImpl<E> implements RecycleBuffer<E> {

    private final Object[] unused;
    private final int upperCapacity;
    private final int retainedCapacity;
    private final Lock lock = new ReentrantLock();

    private volatile int top = 0;

    public RecycleBufferImpl(int upperCapacity) {
        if (upperCapacity < 16) {
            throw new IllegalArgumentException("Upper capacity is too small, must be at least 16: " + upperCapacity);
        }
        this.upperCapacity = upperCapacity;
        this.retainedCapacity = Math.round(upperCapacity * 0.75f);
        this.unused = new Object[upperCapacity];
    }

    public E get() {
        E value = getImpl();
        return value == null ? bufferUnderflow() : value;
    }

    @SuppressWarnings("unchecked")
    private E getImpl() {
        lock.lock();
        try {
            return (top <= 0) ? null : (E)unused[--top];
        } finally {
            lock.unlock();
        }
    }

    protected abstract E bufferUnderflow();

    public void recycle(E value) {
        lock.lock();
        try {
            if (top >= upperCapacity) {
                for (int i = retainedCapacity; i < upperCapacity; i++) {
                    unused[i] = null;
                }
                top = retainedCapacity;
            }
            unused[top++] = value;
        } finally {
            lock.unlock();
        }
    }

    public static <T> RecycleBuffer<T> forType(final Class<T> type, int upperCapacity) {
        RecycleBufferImpl<T> recycleBuffer = new RecycleBufferImpl<T>(upperCapacity) {
            @Override
            protected T bufferUnderflow() {
                try {
                    return type.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // fail-fast
        T obj = recycleBuffer.bufferUnderflow();
        if (!type.isInstance(obj)) {
            throw new ClassCastException("Type mismatch; cannot cast object of type " + obj.getClass() + " to " + type.getClass());
        }
        return recycleBuffer;
    }

    @SuppressWarnings("unchecked")
    public static <T> RecycleBuffer<T> forParameterizedType(final Class<?> type, int upperCapacity) {
        return (RecycleBuffer<T>)forType(type, upperCapacity);
    }

    public static <T> RecycleBuffer<T> forSource(final Factory<T> factory, int upperCapacity) {
        RecycleBufferImpl<T> recycleBuffer = new RecycleBufferImpl<T>(upperCapacity) {
            @Override
            protected T bufferUnderflow() {
                return factory.createEmpty();
            }
        };
        recycleBuffer.bufferUnderflow(); // fail-fast check
        return recycleBuffer;
    }
}
