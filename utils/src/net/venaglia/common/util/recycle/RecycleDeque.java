package net.venaglia.common.util.recycle;

import net.venaglia.common.util.Series;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 10/9/14
 * Time: 9:34 PM
 *
 * A non-blocking thread safe Deque like collection suitable for storing
 * recycled objects which allows multiple threads to add or remove items
 * concurrently. The internal data structure is an array with head and tail
 * pointers. Since the array is finite in size and it may grow, operations that
 * cause the array to grow block ALL other operations until the resize
 * completes.
 */
public class RecycleDeque<E> implements Series<E> {

    private static final int MIN_INITIAL_CAPACITY = 8;

    private E[] buffer;
    private int mask;

    private final ReadWriteLock lock = new ReentrantReadWriteLock(false);
    private final AtomicInteger head = new AtomicInteger();
    private final AtomicInteger tail = new AtomicInteger();
    private final AtomicInteger size = new AtomicInteger();

    @SuppressWarnings("unchecked")
    private E[] allocateElements(int numElements) {
        int initialCapacity = MIN_INITIAL_CAPACITY;
        // Find the best power of two to hold elements.
        // Tests "<=" because arrays aren't kept full.
        if (numElements >= initialCapacity) {
            initialCapacity = numElements;
            initialCapacity |= (initialCapacity >>>  1);
            initialCapacity |= (initialCapacity >>>  2);
            initialCapacity |= (initialCapacity >>>  4);
            initialCapacity |= (initialCapacity >>>  8);
            initialCapacity |= (initialCapacity >>> 16);
            initialCapacity++;

            if (initialCapacity < 0)   // Too many elements, must back off
                initialCapacity >>>= 1;// Good luck allocating 2 ^ 30 elements
        }
        return (E[]) new Object[initialCapacity];
    }

    private void arrayCopy(E[] src, int srcMask, E[] dest, int destMask, int tail, int head) {
        int count = head - tail;
        int srcA = tail & srcMask;
        int srcB = head & srcMask;
        int destA = tail & destMask;
        if (srcA > srcB) {
            System.arraycopy(src, srcA, dest, destA, count - srcA);
            System.arraycopy(src, 0, dest, (head - srcB) & destMask, srcB);
        } else {
            if (srcA + count >= src.length) {
                throw new AssertionError();
            }
            System.arraycopy(src, srcA, dest, destA, count);
        }
    }

    public RecycleDeque() {
        this(MIN_INITIAL_CAPACITY);
    }

    public RecycleDeque(int initialCapacity) {
        buffer = allocateElements(initialCapacity);
        mask = buffer.length - 1;
    }

    public int size() {
        return size.get();
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {

            private final int expectedHead = head.get();
            private final int expectedTail = tail.get();

            private int i = expectedTail;

            private void checkForConcurrentModification() {
                if (head.get() != expectedHead || tail.get() != expectedTail) {
                    throw new ConcurrentModificationException();
                }
            }

            public boolean hasNext() {
                checkForConcurrentModification();
                return i != expectedHead;
            }

            public E next() {
                lock.readLock().lock();
                try {
                    checkForConcurrentModification();
                    if (i == expectedHead) {
                        throw new NoSuchElementException();
                    }
                    return buffer[i & mask];
                } finally {
                    i++;
                    lock.readLock().unlock();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public void add(E value) {
        do {
            int s = size.get();
            int t = s + 1;
            if (t >= buffer.length) {
                if (buffer.length << 2 < 0) {
                    throw new IllegalStateException("Too many elements in RecycleDeque");
                }
                lock.writeLock().lock();
                byte revert = 0;
                try {
                    if (size.compareAndSet(s, t)) {
                        revert = 1; // if we don't succeed, rollback the size
                        E[] newBuffer = allocateElements(buffer.length);
                        int newLength = newBuffer.length;
                        int newMask = newLength - 1;
                        int tail = this.tail.get();
                        int head = this.head.getAndIncrement();
                        revert = 2; // rollback the head too
                        if (head > tail) {
                            arrayCopy(buffer, mask, newBuffer, newMask, tail, head);
                        }
                        newBuffer[head & newMask] = value;
                        revert = 0; // looks like it's going to work
                        buffer = newBuffer;
                        mask = newMask;
                        return;
                    }
                } finally {
                    switch (revert) {
                        case 2: head.decrementAndGet();
                        case 1: size.decrementAndGet();
                    }
                    lock.writeLock().unlock();
                }
            } else {
                lock.readLock().lock();
                try {
                    if (size.compareAndSet(s, t)) {
                        buffer[head.getAndIncrement() & mask] = value;
                        return;
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }
        } while (true);
    }

    public E pollFirst() {
        int i;
        do {
            i = size.get();
            if (i == 0) {
                return null;
            }
        } while (!size.compareAndSet(i, i - 1));

        // got one, let's take it out
        lock.readLock().lock();
        try {
            int j = tail.getAndIncrement() & mask;
            try {
                return buffer[j];
            } finally {
                buffer[j] = null; // null out the reference
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    public E pollLast() {
        int i;
        do {
            i = size.get();
            if (i == 0) {
                return null;
            }
        } while (!size.compareAndSet(i, i - 1));

        // got one, let's take it out
        lock.readLock().lock();
        try {
            int j = head.decrementAndGet() & mask;
            try {
                return buffer[j];
            } finally {
                buffer[j] = null; // null out the reference
            }
        } finally {
            lock.readLock().lock();
        }
    }
}
