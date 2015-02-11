package net.venaglia.common.util;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 1/27/15
 * Time: 8:05 AM
 */
public class IntSet implements Cloneable {

    public static int MAXIMUM_CAPACITY = 1 << 30;
    public static int NO_VALUE = Integer.MIN_VALUE;

    private final float loadFactor;

    private int size;
    private int capacity;
    private int keyMask;
    private int modCount;
    private int[] values;

    public IntSet() {
        this(8, 0.7f);
    }

    public IntSet(int initialCapacity) {
        this(initialCapacity, 0.7f);
    }

    public IntSet(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal load factor: " + loadFactor);
        }
        this.size = 0;
        this.capacity = tableSizeFor(initialCapacity);
        this.loadFactor = loadFactor;
        this.keyMask = capacity - 1;
        this.values = new int[capacity];
        clear();
    }

    private IntSet(IntSet that) {
        this.loadFactor = that.loadFactor;
        this.size = that.size;
        this.capacity = that.capacity;
        this.keyMask = that.keyMask;
        this.values = that.values.clone();
    }

    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    @Override
    public IntSet clone() {
        return new IntSet(this);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(int v) {
        if (v == NO_VALUE) return false;
        for (int k = keyFrom(v) & keyMask, n = 0; n < capacity; k = (k + 1) & keyMask, n++) {
            if (values[k] == v) return true;
        }
        return false;
    }

    public boolean containsAll(IntSet set) {
        if (set.size() > size) {
            return false;
        }
        for (IntIterator iterator = set.iterator(); iterator .hasNext();) {
            if (!this.contains(iterator.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean add(int v) {
        if (v == NO_VALUE) {
            throw new IllegalArgumentException();
        }
        if (!contains(v)) {
            ensureCapacity(size + 1);
            int k = keyFrom(v) & keyMask;
            int n;
            for (n = 0; n < capacity; n++, k = k + 1 & keyMask) {
                if (values[k] == NO_VALUE) {
                    values[k] = v;
                    size++;
                    modCount++;
                    break;
                }
            }
            assert n < capacity;
            return true;
        }
        return false;
    }

    public boolean addAll(IntSet set) {
        boolean changed = false;
        for (IntIterator iterator = set.iterator(); iterator .hasNext();) {
            changed |= add(iterator.next());
        }
        return changed;
    }

    public boolean remove(int v) {
        if (contains(v)) {
            int k = keyFrom(v) & keyMask;
            int n;
            for (n = 0; n < capacity; n++, k = k + 1 & keyMask) {
                if (values[k] == v) {
                    values[k] = NO_VALUE;
                    modCount++;
                    size--;
                    break;
                }
            }
            assert n < capacity;
            return true;
        }
        return false;
    }

    public boolean removeAll(IntSet set) {
        boolean changed = false;
        for (IntIterator iterator = set.iterator(); iterator .hasNext();) {
            changed |= remove(iterator.next());
        }
        return changed;
    }

    public boolean retainAll(IntSet set) {
        boolean changed = false;
        for (IntIterator iterator = iterator(); iterator .hasNext();) {
            if (!set.contains(iterator.next())) {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    public void clear() {
        size = 0;
        Arrays.fill(values, NO_VALUE);
        modCount++;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int value : values) {
            if (value != NO_VALUE) {
                h += keyFrom(value);
            }
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof IntSet) || ((IntSet)obj).size() != size) {
            return false;
        }
        IntSet set = (IntSet)obj;
        for (int value : values) {
            if (value != NO_VALUE && !set.contains(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(size * 4);
        boolean first = true;
        buffer.append('[');
        for (int value : values) {
            if (value != NO_VALUE) {
                if (first) {
                    first = false;
                } else {
                    buffer.append(',');
                }
                buffer.append(value);
            }
        }
        buffer.append(']');
        return buffer.toString();
    }

    public IntIterator iterator() {
        return new IntIterator() {

            private int k = -1;
            private int next = -1;
            private boolean removed = true;
            private int expectedModCount = modCount;

            private void checkForModification() {
                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }

            @Override
            public boolean hasNext() {
                checkForModification();
                if (next >= 0) return next < capacity;
                for (next = k + 1; next < capacity; next++) {
                    if (values[next] != NO_VALUE) return true;
                }
                return false;
            }

            @Override
            public int next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                try {
                    return values[next];
                } finally {
                    k = next;
                    removed = false;
                    next = -1;
                }
            }

            @Override
            public void remove() {
                checkForModification();
                if (removed) {
                    throw new IllegalStateException();
                }
                values[k] = NO_VALUE;
                size--;
                expectedModCount = ++modCount;
                removed = true;
            }
        };
    }

    public int[] toArray() {
        int[] array = new int[size];
        int i = 0;
        for (IntIterator iterator = iterator(); iterator .hasNext();) {
            array[i++] = iterator.next();
        }
        return array;
    }

    protected void ensureCapacity(int ensure) {
        if (ensure > Math.round(capacity * loadFactor)) {
            int newCapacity = tableSizeFor(capacity << 2);
            if (newCapacity == capacity) {
                throw new IllegalStateException("Not enough room");
            }
            int newSize = 0;
            int newKeyMask = newCapacity - 1;
            int[] newValues = new int[newCapacity];
            Arrays.fill(newValues, NO_VALUE);
            for (int i = 0; i < capacity; i++) {
                int v = values[i];
                if (v == NO_VALUE) {
                    continue;
                }
                int k = keyFrom(v) & newKeyMask;
                int n;
                for (n = 0; n < newCapacity; n++, k = k + 1 & newKeyMask) {
                    if (newValues[k] == NO_VALUE) {
                        newValues[k] = v;
                        newSize++;
                        break;
                    }
                }
                assert n < newCapacity;
            }
            modCount++;
            values = newValues;
            keyMask = newKeyMask;
            capacity = newCapacity;
            size = newSize;
        }
    }

    protected int keyFrom(int value) {
        value ^= 0x5b9c212e;
        int even = value & 0xAAAAAAAA;
        int odd = Integer.rotateRight(value & 0x55555555, 8);
        return (even | odd);
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    private static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
}
