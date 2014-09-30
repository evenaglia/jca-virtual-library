package net.venaglia.realms.builder.geoform;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * User: ed
 * Date: 9/5/14
 * Time: 11:09 AM
 */
public class VertexCounter {

    public static final long UPPER_LIMIT = 0x7FFFFFFFFFFFFFL;
    public static final int VERTICES_PER_ENTRY = 1 << 16;
    public static final int ARRAY_SIZE = VERTICES_PER_ENTRY >> 4;

    private static final long ONE_TO_ALL    = 0x1111111111111111L;
    private static final long TOP_SLOT_MASK = 0xF000000000000000L;

    private ConcurrentSkipListMap<Integer,AtomicLongArray> counts;
    private CountAccessorImpl accessor = new CountAccessorImpl();

    public VertexCounter() {
        counts = new ConcurrentSkipListMap<Integer,AtomicLongArray>();
    }

    public CountAccessor at(long vertexId) {
        if (vertexId < 0 || vertexId > UPPER_LIMIT) {
            throw new ArrayIndexOutOfBoundsException(String.valueOf(vertexId));
        }
        return accessor.setVertexId(vertexId);
    }

    public void incrementRange(long startVertexId, int count) {
        long end = startVertexId + count - 1;
        if (startVertexId < 0 || end > UPPER_LIMIT) {
            throw new ArrayIndexOutOfBoundsException(String.valueOf(startVertexId));
        }
        long vertexId = startVertexId;
        accessor.setVertexId(vertexId);
        while (vertexId <= end && (vertexId & 0xF) != 0) {
            accessor.incrementCount();
            accessor.nextVertex();
            vertexId++;
        }
        if (vertexId >= end) {
            return;
        }
        int key = (int)(vertexId >> 16);
        int toKey = (int)(end >> 16);
        int index = (int)(vertexId >> 4) & 0xFFF;
        int toIndex = (int)(end >> 4) & 0xFFF;
        AtomicLongArray counts = getCounts(key);
        while (key < toKey || index < toIndex) {
            boolean done = false;
            while (!done) {
                long l = counts.get(index);
                long m = l + ONE_TO_ALL;
                if ((l & TOP_SLOT_MASK) != TOP_SLOT_MASK && (m & ONE_TO_ALL ^ ONE_TO_ALL) == (l & ONE_TO_ALL)) {
                    if (counts.compareAndSet(index, l, m)) {
                        vertexId += 16;
                        done = true;
                    }
                } else {
                    for (int i = 0; i < 16; i++) {
                        accessor.setVertexId(vertexId++).incrementCount();
                    }
                    done = true;
                }
            }
            index++;
            if (index >= ARRAY_SIZE) {
                index = 0;
                key++;
                counts = getCounts(key);
            }
        }
        while (vertexId <= end) {
            accessor.setVertexId(vertexId++).incrementCount();
        }
    }

    public long[] tallyCounts() {
        long[] results = new long[16];
        for (AtomicLongArray counts : this.counts.values()) {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                long l = counts.get(i);
                for (int j = 0; j < 16 && l > 0; j++, l >>= 4) {
                    int count = (int)(l & 0xF);
                    if (count > 0) {
                        results[count]++;
                    }
                }
            }
        }
        return results;
    }

    public void clear() {
        // counts.clear() is faster, but this method is all about helping the GC
        for (Iterator<Integer> i = counts.keySet().iterator(); i.hasNext(); ) {
            i.next();
            i.remove();
        }
        counts = null;
        accessor.clear();
        accessor = null;
    }

    private AtomicLongArray getCounts(Integer key) {
        AtomicLongArray counts = this.counts.get(key);
        if (counts == null) {
            counts = new AtomicLongArray(ARRAY_SIZE);
            AtomicLongArray previous = this.counts.putIfAbsent(key, counts);
            return previous == null ? counts : previous;
        }
        return counts;
    }

    public interface CountAccessor {
        long getVertexId();
        void nextVertexNotZero();
        void incrementCount();
        int getCount();
    }

    private class CountAccessorImpl implements CountAccessor {

        private AtomicLongArray counts;
        private int key = -1;
        private long vertexId;
        private int index;
        private long mask;
        private int shift;
        private long one;

        CountAccessorImpl setVertexId(long vertexId) {
            int key = (int)(vertexId >> 16);
            if (this.key != key) {
                this.key = key;
                this.counts = getCounts(key);
            }
            this.vertexId = vertexId;
            long localId = vertexId & 0xFFFFL;
            this.index = (int)(localId >> 4); // 16 counts per long
            this.shift = (int)(localId & 0xFL) * 4;
            this.mask = 0xFL << shift;
            this.one = 1L << shift;
            return this;
        }

        public long getVertexId() {
            return vertexId;
        }

        public void nextVertex() {
            long next = vertexId + 1;
            if (next >= UPPER_LIMIT) {
                throw new NoSuchElementException();
            }
            setVertexId(next);
        }

        public void nextVertexNotZero() {
            if (!nextVertexNotZeroImpl()) {
                throw new NoSuchElementException();
            }
        }

        private boolean nextVertexNotZeroImpl() {
            if (nextVertexNotZeroInThisIndex()) {
                return true;
            }
            if (nextVertexNotZeroInThisArray()) {
                return true;
            }

            // save state
            AtomicLongArray counts = this.counts;
            int index = this.index;
            this.index = -1;

            Integer key = (int)(vertexId >> 16);
            do {
                key = VertexCounter.this.counts.higherKey(key);
                if (key == null) {
                    // restore state
                    this.counts = counts;
                    this.index = index;
                    return false;
                }
                this.counts = getCounts(key);
            } while (!nextVertexNotZeroInThisArray());
            return true;
        }

        private boolean nextVertexNotZeroInThisArray() {
            int index = this.index + 1;
            while (index < ARRAY_SIZE && counts.get(index) == 0L) {
                index++;
            }
            if (index >= ARRAY_SIZE) {
                return false;
            }
            this.index = index;
            this.vertexId = ((long)index) << 4;
            this.shift = -4;
            if (!nextVertexNotZeroInThisIndex()) {
                throw new IllegalStateException("buffer is zero... but is wasn't a moment ago... huh...");
            }
            return true;
        }

        private boolean nextVertexNotZeroInThisIndex() {
            long vertexId = this.vertexId;
            int shift = this.shift + 4;
            long l = counts.get(index) >> shift;
            if (l == 0L) {
                return false;
            }
            while ((l & 0xFL) == 0) {
                vertexId++;
                shift += 4;
                l >>= 4;
            }
            this.vertexId = vertexId;
            this.shift = shift;
            this.mask = 0xFL << shift;
            this.one = 1L << shift;
            return true;
        }

        public void incrementCount() {
            boolean done = false;
            while (!done) {
                long l = counts.get(index);
                if ((l & mask) == mask) {
                    // count is already full! (>=15)
                    return;
                }
                done = counts.compareAndSet(index, l, l + one);
            }
        }

        public int getCount() {
            if (index >= UPPER_LIMIT) {
                return 0;
            }
            long l = counts.get(index);
            return (int)((l & mask) >> shift) & 0xF;
        }

        void clear() {
            counts = null;
        }
    }
}
