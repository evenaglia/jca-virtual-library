package net.venaglia.realms.common.map.world;

import net.venaglia.common.util.IntIterator;
import net.venaglia.common.util.IntSet;
import net.venaglia.common.util.impl.IntArrayIterator;
import net.venaglia.realms.spec.GeoSpec;

import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 1/27/15
 * Time: 6:33 PM
 */
public class AcreIdSet implements Cloneable {

    public static final AcreIdSet ALL_ACRES;
    public static final AcreIdSet EMPTY;

    static {
        int count = GeoSpec.ACRES.iGet();
        ALL_ACRES = new ImmutableAcreIdSet(count);
        ALL_ACRES.bitset.set(0, count);
        ALL_ACRES.size = count;
        EMPTY = new ImmutableAcreIdSet(0);
    }

    protected final int acres;
    protected final int upSizeAt;
    protected final int downSizeAt;
    protected final AtomicInteger modCount = new AtomicInteger();

    protected int size = 0;
    protected Storage storage;
    protected BitSet bitset;
    protected IntSet intset;

    private enum Storage {
        BIT_SET, INT_SET, CUSTOM
    }

    public AcreIdSet() {
        this(null, 16);
    }

    public AcreIdSet(int initialCapacity) {
        this(null, initialCapacity);
    }

    private AcreIdSet(AcreIdSet that) {
        this(that.storage, 0);
        switch (that.storage) {
            case BIT_SET:
                this.bitset = (BitSet)that.bitset.clone();
                this.size = bitset.cardinality();
                break;
            case INT_SET:
                this.intset = that.intset.clone();
                this.size = intset.size();
                break;
            default:
                throw new Error("unsupported storage: " + storage);
        }
    }

    private AcreIdSet(Storage storage, int initialCapacity) {
        this.acres = GeoSpec.ACRES.iGet();
        this.upSizeAt = acres / 64;
        this.downSizeAt = acres / 72;
        if (storage != null) {
            this.storage = storage;
        } else if (initialCapacity > acres) {
            throw new IllegalArgumentException();
        } else if (initialCapacity >= upSizeAt) {
            this.storage = Storage.BIT_SET;
            this.bitset = new BitSet(acres);
        } else {
            this.storage = Storage.INT_SET;
            this.intset = new IntSet(initialCapacity, 0.5f);
        }
    }


    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    @Override
    public AcreIdSet clone() {
        return new AcreIdSet(this);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        switch (storage) {
            case BIT_SET:
                bitset.clear();
                size = 0;
                modCount.incrementAndGet();
                break;
            case INT_SET:
                intset.clear();
                size = 0;
                modCount.incrementAndGet();
                break;
            default:
                throw new Error("unsupported storage: " + storage);
        }
    }

    public IntIterator iterator() {
        switch (storage) {
            case BIT_SET:
                return new BitSetIntIterator(bitset, modCount);
            case INT_SET:
                return intset.iterator();
        }
        throw new Error("unsupported storage: " + storage);
    }

    public boolean contains(int id) {
        switch (storage) {
            case BIT_SET:
                return bitset.get(id);
            case INT_SET:
                return intset.contains(id);
        }
        throw new Error("unsupported storage: " + storage);
    }

    public boolean add(int id) {
        if (!contains(id)) {
            resize(1);
            modCount.getAndIncrement();
            switch (storage) {
                case BIT_SET:
                    bitset.set(id);
                    size++;
                    break;
                case INT_SET:
                    intset.add(id);
                    size++;
                    break;
                default:
                    throw new Error("unsupported storage: " + storage);
            }
            return true;
        }
        return false;
    }

    public boolean remove(int id) {
        if (contains(id)) {
            switch (storage) {
                case BIT_SET:
                    bitset.clear(id);
                    size--;
                    break;
                case INT_SET:
                    intset.remove(id);
                    size--;
                    break;
                default:
                    throw new Error("unsupported storage: " + storage);
            }
            resize(-1);
            modCount.getAndIncrement();
            return true;
        }
        return false;
    }

    public static AcreIdSet wrap(int[] array) {
        assert array != null;
        return new ArrayAcreIdSet(array, 0, array.length);
    }

    public static AcreIdSet wrap(int[] array, int off, int len) {
        assert array != null;
        return new ArrayAcreIdSet(array, off, len);
    }

    private void resize(int delta) {
        switch (storage) {
            case BIT_SET:
                if (delta < 0 && size + delta <= downSizeAt) {
                    intset = new IntSet(upSizeAt, 0.5f);
                    for (IntIterator iterator = new BitSetIntIterator(bitset, modCount); iterator.hasNext();) {
                        intset.add(iterator.next());
                    }
                    storage = Storage.INT_SET;
                    assert setsAreEqual(bitset, intset);
                    modCount.getAndIncrement();
                    bitset = null;
                }
                break;
            case INT_SET:
                if (delta > 0 && size + delta >= upSizeAt) {
                    bitset = new BitSet(acres);
                    for (IntIterator iterator = intset.iterator(); iterator.hasNext();) {
                        bitset.set(iterator.next());
                    }
                    storage = Storage.BIT_SET;
                    modCount.getAndIncrement();
                    intset = null;
                }
                break;
            default:
                throw new Error("unsupported storage: " + storage);
        }
    }

    private boolean setsAreEqual(BitSet bitset, IntSet intset) {
        if (bitset.cardinality() != intset.size()) {
            return false;
        }
        int size = 0;
        for (IntIterator i = intset.iterator(); i.hasNext(); size++) {
            if (!bitset.get(i.next())) return false;
        }
        assert size == intset.size();
        return true;
    }

    private class BitSetIntIterator implements IntIterator {

        private final BitSet bitset;
        private final AtomicInteger modCount;
        private final int expectedModCount;

        private int i = -1;
        private int n = -2;
        private boolean removed = true;

        public BitSetIntIterator(BitSet bitset, AtomicInteger modCount) {
            this.bitset = bitset;
            this.modCount = modCount;
            this.expectedModCount = modCount.get();
        }

        @Override
        public boolean hasNext() {
            checkForModification();
            if (n >= 0) {
                return true;
            }
            if (n == -1) {
                return false;
            }
            n = bitset.nextSetBit(i + 1);
            return n >= 0;
        }

        @Override
        public int next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            try {
                return n;
            } finally {
                i = n;
                n = -2;
                removed = false;
            }
        }

        @Override
        public void remove() {
            checkForModification();
            if (!removed) {
                if (bitset.get(i)) {
                    bitset.clear(i);
                    size--;
                }
                removed = true;
            }
        }

        private void checkForModification() {
            if (expectedModCount != modCount.get()) {
                throw new ConcurrentModificationException();
            }
        }
    }

    private static class ImmutableAcreIdSet extends AcreIdSet {

        public ImmutableAcreIdSet() {
            super();
        }

        private ImmutableAcreIdSet(int initialCapacity) {
            super(initialCapacity);
        }

        private ImmutableAcreIdSet(Storage storage, int initialCapacity) {
            super(storage, initialCapacity);
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("CloneDoesntCallSuperClone")
        @Override
        public AcreIdSet clone() {
            return this; // the set is immutable
        }

        @Override
        public IntIterator iterator() {
            final IntIterator iterator = super.iterator();
            return new IntIterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public int next() {
                    return iterator.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public boolean add(int id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(int id) {
            throw new UnsupportedOperationException();
        }
    }

    private static class ArrayAcreIdSet extends ImmutableAcreIdSet {

        private final int[] acreIds;
        private final int off;
        private final int len;

        public ArrayAcreIdSet(int[] acreIds, int off, int len) {
            super(Storage.CUSTOM, 0);
            if (off < 0) {
                throw new ArrayIndexOutOfBoundsException(off);
            }
            if (off + len > acreIds.length) {
                throw new ArrayIndexOutOfBoundsException(off + len);
            }
            this.acreIds = acreIds;
            this.off = off;
            this.len = len;
            this.size = acreIds.length;
        }

        @Override
        public IntIterator iterator() {
            return new IntArrayIterator(acreIds, off, len);
        }

        @Override
        public boolean contains(int id) {
            for (int acreId : acreIds) {
                if (acreId == id) return true;
            }
            return false;
        }
    }
}
