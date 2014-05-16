package net.venaglia.common.util.impl;

import net.venaglia.common.util.ArrayRecycleBuffer;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 5/9/14
 * Time: 12:55 PM
 */
public class ArrayRecycleBufferImpl<A> implements ArrayRecycleBuffer<A> {

    private static final NavigableSet<Integer> SIZES;
    private static final BlockingQueue<Runnable> RECLAIM = new LinkedBlockingDeque<Runnable>();
    private static final Map<Class<?>,Character> PRIMITIVE_TYPES;

    static {
        SIZES = new TreeSet<Integer>();
        for (int i = 8; i < Integer.MAX_VALUE; i *= 2) {
            SIZES.add(i);
        }
        SIZES.add(Integer.MAX_VALUE);
        PRIMITIVE_TYPES = new HashMap<Class<?>, Character>();
        PRIMITIVE_TYPES.put(boolean[].class, 'b');
        PRIMITIVE_TYPES.put(char[].class, 'c');
        PRIMITIVE_TYPES.put(byte[].class, 'y');
        PRIMITIVE_TYPES.put(short[].class, 's');
        PRIMITIVE_TYPES.put(int[].class, 'i');
        PRIMITIVE_TYPES.put(long[].class, 'l');
        PRIMITIVE_TYPES.put(float[].class, 'f');
        PRIMITIVE_TYPES.put(double[].class, 'd');
        Thread reclaim = new Thread(new Runnable() {
            @SuppressWarnings("InfiniteLoopStatement")
            public void run() {
                while (true) {
                    Thread t = Thread.currentThread();
                    try {
                        RECLAIM.take().run();
                    } catch (Exception e) {
                        t.getUncaughtExceptionHandler().uncaughtException(t, e);
                    }
                }
            }
        }, "Array Recycle Worker");
        reclaim.setDaemon(true);
        reclaim.start();
    }

    private final A zero;
    private final char type;
    private final NavigableMap<Integer,Integer> sizeLimits;
    private final Class<?> arrayElementType;
    private final Map<Integer,Queue<A>> unused = new HashMap<Integer,Queue<A>>();
    private final Lock lock = new ReentrantLock();

    @SuppressWarnings("unchecked")
    private ArrayRecycleBufferImpl(Class<A> arrayType, int maxCachedSize, int limit) {
        if (!arrayType.isArray()) {
            throw new IllegalArgumentException("Not an array type: " + arrayType);
        }
        this.zero = create(0);
        this.type = PRIMITIVE_TYPES.containsKey(arrayType) ? PRIMITIVE_TYPES.get(arrayType) : 'O';
        this.sizeLimits = new TreeMap<Integer,Integer>();
        int currentLimit = limit;
        for (Integer size : SIZES) {
            if (size <= maxCachedSize) {
                this.sizeLimits.put(size, currentLimit);
                currentLimit = Math.max(Math.round(size * 0.75f), 1);
            }
        }
        if (!this.sizeLimits.containsKey(maxCachedSize)) {
            this.sizeLimits.put(maxCachedSize, currentLimit);
        }
        this.arrayElementType = arrayType.getComponentType();
    }

    @SuppressWarnings("unchecked")
    protected A create(int length) {
        return (A)Array.newInstance(arrayElementType, length);
    }

    protected int cacheLimit(int length) {
        Integer limit = this.sizeLimits.get(length);
        return limit == null ? 0 : limit;
    }

    protected void clear(A array) {
        switch (type) {
            case 'b':
                Arrays.fill((boolean[])array, false);
                break;
            case 'c':
                Arrays.fill((char[])array, '\0');
                break;
            case 'y':
                Arrays.fill((byte[])array, (byte)0);
                break;
            case 's':
                Arrays.fill((short[])array, (short)0);
                break;
            case 'i':
                Arrays.fill((int[])array, 0);
                break;
            case 'l':
                Arrays.fill((long[])array, 0L);
                break;
            case 'f':
                Arrays.fill((float[])array, 0.0f);
                break;
            case 'd':
                Arrays.fill((double[])array, 0.0);
                break;
            case 'O':
                Arrays.fill((Object[])array, null);
                break;
        }
    }

    public A get(int minimumLength) {
        if (minimumLength == 0) {
            return zero;
        }
        Integer key = sizeLimits.ceilingKey(minimumLength);
        if (key == null) {
            return create(minimumLength); // too big to cache;
        }
        lock.lock();
        A result;
        try {
            Queue<A> bucket = getBucket(key);
            result = bucket.poll();
        } finally {
            lock.unlock();
        }
        return result == null ? create(key) : result;
    }

    private Queue<A> getBucket(Integer key) {
        Queue<A> bucket = unused.get(key);
        if (bucket == null) {
            bucket = new ArrayDeque<A>(4);
            unused.put(key, bucket);
        }
        return bucket;
    }

    public void recycle(final A value) {
        if (value == null) {
            return;
        }
        final Integer key = Array.getLength(value);
        if (sizeLimits.containsKey(key)) {
            RECLAIM.add(new Runnable() {
                public void run() {
                    clear(value);
                    lock.lock();
                    try {
                        Queue<A> bucket;
                        int limit = cacheLimit(key);
                        if (limit > 0 && (bucket = getBucket(key)).size() < limit) {
                            bucket.add(value);
                        }
                    } finally {
                        lock.unlock();
                    }
                }
            });
        }
    }

    public static <A> ArrayRecycleBufferImpl<A> forArray(Class<A> arrayType) {
        return new ArrayRecycleBufferImpl<A>(arrayType, 1024 * 1024, 768);
    }

    public static <A> ArrayRecycleBufferImpl<A> forArray(Class<A> arrayType, int maxCachedSize) {
        return new ArrayRecycleBufferImpl<A>(arrayType, maxCachedSize, Math.max(192, maxCachedSize >> 12));
    }
}
