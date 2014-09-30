package net.venaglia.gloo.util.impl;

import net.venaglia.gloo.util.AreaMap;
import net.venaglia.gloo.util.Bounds2D;

import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 7/15/14
 * Time: 8:02 AM
 */
public class QuadMap<E> implements AreaMap<E> {

    private final QuadMap<E> parent;
    private final int depth;
    private final int divideThreshold;
    private final int unifyThreshold;
    private final double x0;
    private final double x_;
    private final double x1;
    private final double y0;
    private final double y_;
    private final double y1;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private boolean containsNodes = true;
    private Object[] children = new Object[4];
    private int totalEntryCount = 0;
    private int modCount = 0;

    private QuadMap(QuadMap<E> parent, int depth,
                    int divideThreshold, int unifyThreshold,
                    double x0, double x1,
                    double y0, double y1) {
        this.parent = parent;
        this.depth = depth;
        this.divideThreshold = divideThreshold;
        this.unifyThreshold = unifyThreshold;
        this.x0 = x0;
        this.x_ = (x0 + x1) / 2.0;
        this.x1 = x1;
        this.y0 = y0;
        this.y_ = (y0 + y1) / 2.0;
        this.y1 = y1;
        if (divideThreshold < 2) {
            throw new IllegalArgumentException("divideThreshold must be at least 2");
        }
        if (unifyThreshold >= divideThreshold) {
            throw new IllegalArgumentException("unifyThreshold (" + unifyThreshold + ") must be greater than divideThreshold (" + divideThreshold + ")");
        }
    }

    public QuadMap(Bounds2D bounds, int divideThreshold, int unifyThreshold) {
        this(null, 0,
             divideThreshold, unifyThreshold,
             bounds.x1, bounds.x2,
             bounds.y1, bounds.y2);
    }

    public QuadMap(Bounds2D bounds, int divideThreshold) {
        this(bounds, divideThreshold, divideThreshold - (divideThreshold / 8) - 1);
    }

    public QuadMap(Bounds2D bounds) {
        this(bounds, 6);
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public E get(final double x, final double y) {
        return contains(x, y) ? get(x, y, Double.MIN_VALUE) : null;
    }

    public E get(final double x, final double y, double r) {
        final AtomicReference<E> result = new AtomicReference<E>();
        intersect(Bounds2D.createCircle(x, y, r), new Consumer<E>() {
            double best = Double.MAX_VALUE;

            public void found(Entry<E> entry, double i, double j) {
                double d = computeDistanceSquared(i - x, j - y);
                if (d < best) {
                    result.set(entry.get());
                    best = d;
                }
            }
        });
        return result.get();
    }

    private double computeDistanceSquared(double a, double b) {
        return a * a + b * b;
    }

    public boolean contains(double x, double y) {
        if (withinBounds(x, y)) {
            lock.readLock().lock();
            try {
                if (containsNodes) {
                    for (int i = 0; i < totalEntryCount; i++) {
                        Entry<?> child = (Entry<?>)children[i];
                        if (child.getX() == x && child.getY() == y) {
                            return true;
                        }
                    }
                } else {
                    QuadMap<E> child = whichChild(x, y, false);
                    return child != null && child.contains(x, y);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
        return false;
    }

    public int intersect(Bounds2D region, Consumer<E> consumer) {
        double maxX = region.x2;
        double minX = region.x1;
        double maxY = region.y2;
        double minY = region.y1;
        if (maxX < x0 || minX >= x1 || maxY < y0 || minY >= y1) {
            return 0; // not without the bounds of this node;
        }
        int hits = 0;
        lock.readLock().lock();
        try {
            if (containsNodes) {
                for (int i = 0; i < totalEntryCount; i++) {
                    @SuppressWarnings("unchecked")
                    Entry<E> child = (Entry<E>)children[i];
                    if (region.includes(child.getX(), child.getY())) {
                        hits++;
                        consumer.found(child, child.getX(), child.getY());
                    }
                }
            } else {
                int cells = 0xFF; // bit mask
                cells &= intersect(x_, minX, maxX, 0x55, 0xAA);
                cells &= intersect(y_, minY, maxY, 0x33, 0xCC);
                for (int i = 0; i < 8; i++) {
                    if ((cells & 1) == 1) {
                        @SuppressWarnings("unchecked")
                        QuadMap<E> child = (QuadMap<E>)children[i];
                        if (child != null) {
                            hits += child.intersect(region, consumer);
                        }
                    }
                    cells >>= 1;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return hits;
    }

    public Bounds2D getBounds() {
        return Bounds2D.createRectangle(x0, y0, x1, y1);
    }

    private int intersect(double n, double min, double max, int lower, int upper) {
        if (n >= max) return lower;
        if (n < min) return upper;
        return lower | upper;
    }

    public boolean add(E obj, double x, double y) {
        return addImpl(createEntry(obj, x, y));
    }

    protected Entry<E> createEntry(E obj, double x, double y) {
        return new EntryImpl<E>(obj, x, y);
    }

    protected boolean addImpl(Entry<E> entry) throws UnsupportedOperationException {
        double x = entry.getX();
        double y = entry.getY();
        return addImpl(entry, x, y);
    }

    protected boolean addImpl(Entry<E> entry, double x, double y) throws UnsupportedOperationException {
        if (!withinBounds(x, y)) {
            throw new IndexOutOfBoundsException();
        }
        lock.writeLock().lock();
        try {
            if (containsNodes && totalEntryCount + 1 < divideThreshold) {
                if (children.length <= totalEntryCount) {
                    Object[] c = children;
                    children = new Object[Math.min(c.length * 2, divideThreshold)];
                    System.arraycopy(c, 0, children, 0, c.length);
                }
                children[totalEntryCount++] = entry;
                ((EntryImpl<E>)entry).parent = this;
                modCount++;
                return true;
            } else if (containsNodes) {
                // divide spaces
                Object[] previousChildren = children;
                children = new Object[4];
                containsNodes = false;
                for (int i = 0; i < totalEntryCount; i++) {
                    @SuppressWarnings("unchecked")
                    Entry<E> e = (Entry<E>)previousChildren[i];
                    double a = e.getX();
                    double b = e.getY();
                    whichChild(a, b, true).addImpl(e, a, b);
                }
                containsNodes = false;
            }
            if (whichChild(x, y, true).addImpl(entry, x, y)) {
                totalEntryCount++;
                modCount++;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected boolean removeImpl(Entry<E> entry) throws UnsupportedOperationException {
        lock.writeLock().lock();
        try {
            if (containsNodes) {
                int i = indexOf(entry);
                if (i >= 0) {
                    children[i] = children[--totalEntryCount];
                    if (((EntryImpl<E>)entry).parent == this) {
                        ((EntryImpl<E>)entry).parent = null;
                    }
                    modCount++;
                    return true;
                }
                return false;
            }
            QuadMap<E> child = whichChild(entry.getX(), entry.getY(), true);
            if (child != null && child.removeImpl(entry)) {
                totalEntryCount--;
                if (totalEntryCount <= unifyThreshold) {
                    Object[] c = new Object[divideThreshold];
                    int i = 0;
                    for (Entry<E> e : this) {
                        c[i++] = e;
                    }
                    children = c;
                    modCount++;
                    containsNodes = true;
                }
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private int indexOf(Entry<E> entry) {
        int i = -1;
        for (int j = 0; j < totalEntryCount; j++) {
            if (children[j].equals(entry)) {
                i = j;
                break;
            }
        }
        return i;
    }

    protected boolean moveImpl(Entry<E> entry, double x, double y)
            throws IndexOutOfBoundsException, UnsupportedOperationException {
        QuadMap<E> ancestor = findAncestorThatContains(x, y);
        assert containsNodes;
        if (ancestor != this) {
            ancestor.lock.writeLock().lock();
            try {
                return ancestor.removeImpl(entry) && ancestor.addImpl(entry, x, y);
            } finally {
                ancestor.lock.writeLock().unlock();
            }
        } else {
            return (indexOf(entry) >= 0);
        }
    }

    private QuadMap<E> whichChild(double x, double y, boolean createIfMissing) {
        // child 0 : x0 <= x < x & y0 <=
        int i = (x < x_ ? 0 : 1) | (y < y_ ? 0 : 2);
        //noinspection unchecked
        QuadMap<E> child = (QuadMap<E>)children[i];
        if (child == null&& createIfMissing) {
            child = createChild(i);
            children[i] = child;
        }
        return child;
    }

    private QuadMap<E> createChild(int i) {
        return new QuadMap<E>(this, depth + 1, divideThreshold, unifyThreshold,
                              ((i & 1) == 1 ? x_ : x0), ((i & 1) == 1 ? x1 : x_),
                              ((i & 2) == 2 ? y_ : y0), ((i & 2) == 2 ? y1 : y_));
    }

    public int size() {
        return totalEntryCount;
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            if (!containsNodes) {
                for (Object child : children) {
                    @SuppressWarnings("unchecked")
                    QuadMap<E> sbuMap = (QuadMap<E>)child;
                    sbuMap.clear();
                }
                Arrays.fill(children, null);
                totalEntryCount = 0;
                containsNodes = true;
                modCount++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Iterator<Entry<E>> iterator() {
        if (containsNodes) {
            return new Iterator<Entry<E>>() {

                private int childIndex = 0;
                private Entry<E> last;
                private int expectedModCount = modCount;

                public boolean hasNext() {
                    checkForConcurrentModification();
                    return childIndex < totalEntryCount;
                }

                private void checkForConcurrentModification() {
                    if (expectedModCount != modCount) {
                        throw new ConcurrentModificationException();
                    }
                }

                public Entry<E> next() {
                    checkForConcurrentModification();
                    if (childIndex >= totalEntryCount) {
                        throw new NoSuchElementException();
                    }
                    //noinspection unchecked
                    last = (Entry<E>)children[childIndex++];
                    return last;
                }

                public void remove() {
                    if (last == null) {
                        throw new NoSuchElementException();
                    }
                    checkForConcurrentModification();
                    last.remove();
                    last = null;
                    childIndex--;
                }
            };
        } else {
            return new Iterator<Entry<E>>() {

                private int childIndex = 0;
                private Iterator<Entry<E>> active = Collections.<Entry<E>>emptySet().iterator();
                private Entry<E> next;
                private Entry<E> last;

                public boolean hasNext() {
                    if (next != null) {
                        return true;
                    }
                    if (active == null) {
                        return false;
                    }
                    while (next == null) {
                        if (active != null && active.hasNext()) {
                            next = active.next();
                            continue;
                        }
                        active = null;
                        @SuppressWarnings("unchecked")
                        QuadMap<E> nextChild = childIndex < 8 ? (QuadMap<E>)children[childIndex++] : null;
                        if (nextChild == null) {
                            if (childIndex >= 8) {
                                return false;
                            }
                        } else {
                            active = nextChild.iterator();
                            while (active.hasNext() && next == null) {
                                next = active.next();
                            }
                        }
                    }
                    return true;
                }

                public Entry<E> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    last = next;
                    next = null;
                    return last;
                }

                public void remove() {
                    if (last == null) {
                        throw new NoSuchElementException();
                    }
                    last.remove();
                    last = null;
                }
            };
        }
    }

    private QuadMap<E> findAncestorThatContains(double x, double y) {
        for (QuadMap<E> node = this; node != null; node = node.parent) {
            if (node.withinBounds(x, y)) {
                return node;
            }
        }
        throw new IndexOutOfBoundsException(String.format("[%.4f,%.4f]", x, y));
    }

    private boolean withinBounds(double x, double y) {
        return x >= x0 && x < x1 && y >= y0 && y < y1;
    }

    protected static class EntryImpl<E> implements Entry<E> {

        double x;
        double y;
        QuadMap<E> parent;
        E value;

        public EntryImpl(E value, double x, double y) {
            this.value = value;
            this.x = x;
            this.y = y;
        }

        public E get() {
            return value;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public boolean move(double x, double y) throws IndexOutOfBoundsException {
            if (isOrphan()) {
                throw new IllegalStateException("This entity has been orphaned and longer belongs to a spatial map");
            }
            if (parent.moveImpl(this, x, y)) {
                this.x = x;
                this.y = y;
                return true;
            }
            return false;
        }

        public boolean remove() {
            if (isOrphan()) {
                throw new IllegalStateException("This entity has been orphaned and longer belongs to a spatial map");
            }
            return parent.removeImpl(this);
        }

        public boolean isOrphan() {
            return parent == null;
        }
    }

}
