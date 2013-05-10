package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.util.Series;
import net.venaglia.realms.common.util.SpatialMap;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 9/20/12
 * Time: 11:41 PM
 */
public abstract class AbstractSpatialMap<E> implements SpatialMap<E> {

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public E get(Point p) {
        return contains(p.x, p.y, p.z) ? get(p, Double.MIN_VALUE) : null;
    }

    public E get(double x, double y, double z) {
        return contains(x, y, z) ? get(new Point(x, y, z), Double.MIN_VALUE) : null;
    }

    public E get(Point p, double r) {
        final double x = p.x, y = p.y, z = p.z;
        final AtomicReference<E> result = new AtomicReference<E>();
        intersect(new BoundingSphere(p, r), new Consumer<E>() {
            double best = Double.MAX_VALUE;

            public void found(BasicEntry<E> entry, double i, double j, double k) {
                double d = Vector.computeDistance(i - x, j - y, k - z);
                if (d < best) {
                    result.set(entry.get());
                    best = d;
                }
            }

            public void found(Entry<E> entry, double x, double y, double z) {
                found((BasicEntry<E>)entry, x, y, z);
            }
        });
        return result.get();
    }

    public E get(final double x, final double y, final double z, double r) {
        return get(new Point(x, y, z), r);
    }

    public int intersect(BoundingVolume<?> region, final BasicConsumer<E> consumer) {
        return intersect(region, new Consumer<E>() {
            public void found(Entry<E> entry, double x, double y, double z) {
                consumer.found(entry, x, y, z);
            }
        });
    }

    public Series<E> asSeries() {
        return new Series<E>() {
            public int size() {
                return AbstractSpatialMap.this.size();
            }

            public Iterator<E> iterator() {
                final Iterator<Entry<E>> delegate = AbstractSpatialMap.this.iterator();
                return new Iterator<E>() {
                    public boolean hasNext() {
                        return delegate.hasNext();
                    }

                    public E next() {
                        return delegate.next().get();
                    }

                    public void remove() {
                        delegate.remove();
                    }
                };
            }
        };
    }

    public boolean add(E object, Point p) {
        return add(object, p.x, p.y, p.z);
    }

    public boolean add(E obj, double x, double y, double z) {
        return addImpl(createEntry(obj, x, y, z));
    }

    protected AbstractEntry<E> createEntry(E obj, double x, double y, double z) {
        return new EntryImpl<E>(obj, x, y, z);
    }

    protected boolean addImpl(AbstractEntry<E> entry) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    protected boolean removeImpl(AbstractEntry<E> entry) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    protected boolean moveImpl(AbstractEntry<E> entry, double x, double y, double z) throws IndexOutOfBoundsException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    protected static <E,T extends AbstractSpatialMap<E>> T getParent(AbstractEntry<E> entry) {
        return (T)entry.parent;
    }

    protected static <E,T extends AbstractSpatialMap<E>> AbstractEntry<E> setParent(AbstractEntry<E> entry, T parent) {
        entry.parent = parent;
        return entry;
    }

    protected static <E> AbstractEntry<E> clearParent(AbstractEntry<E> entry, AbstractSpatialMap<?> expectedParent) {
        if (entry.parent == expectedParent) {
            entry.parent = null;
        }
        return entry;
    }

    protected static boolean isParent(AbstractEntry<?> entry, AbstractSpatialMap<?> expectedParent) {
         return entry.parent == expectedParent;
    }

    protected static <E> void consume(Entry<E> entry, BasicConsumer<E> consumer) {
        if (entry instanceof AbstractEntry) {
            AbstractEntry<E> entryImpl = (AbstractEntry<E>)entry;
            consume(entryImpl, consumer);
        } else {
            consumer.found(entry, entry.getAxis(Axis.X), entry.getAxis(Axis.Y), entry.getAxis(Axis.Z));
        }
    }

    protected static <E> void consume(Entry<E> entry, Consumer<E> consumer) {
        if (entry instanceof AbstractEntry) {
            AbstractEntry<E> entryImpl = (AbstractEntry<E>)entry;
            consume(entryImpl, consumer);
        } else {
            consumer.found(entry, entry.getAxis(Axis.X), entry.getAxis(Axis.Y), entry.getAxis(Axis.Z));
        }
    }

    protected static <E> void consume(AbstractEntry<E> entry, BasicConsumer<E> consumer) {
        consumer.found(entry, entry.x, entry.y, entry.z);
    }

    protected static <E> void consume(AbstractEntry<E> entry, Consumer<E> consumer) {
        consumer.found(entry, entry.x, entry.y, entry.z);
    }

    protected static boolean includes(BoundingVolume<?> bounds, Entry<?> entry) {
        if (entry instanceof AbstractEntry) {
            AbstractEntry<?> entryImpl = (AbstractEntry<?>)entry;
            return includes(bounds, entryImpl);
        } else {
            return bounds.includes(entry.getAxis(Axis.X), entry.getAxis(Axis.Y), entry.getAxis(Axis.Z));
        }
    }

    protected static boolean includes(BoundingVolume<?> bounds, AbstractEntry<?> entry) {
        return bounds.includes(entry.x, entry.y, entry.z);
    }

    protected static <E> AbstractEntry<E> setPosition(AbstractEntry<E> entry, double x, double y, double z) {
        entry.x = x;
        entry.y = y;
        entry.z = z;
        return entry;
    }

    private static final Consumer<?> DUMMY_CONSUMER = new Consumer<Object>() {
        public void found(Entry<Object> entry, double x, double y, double z) {
        }
    };

    protected static <T> Consumer<T> dummyConsumer() {
        return dummyConsumer((Consumer<T>)null);
    }

    @SuppressWarnings("unchecked")
    protected static <T> Consumer<T> dummyConsumer(Consumer<T> consumer) {
        return consumer != null ? consumer : (Consumer<T>)DUMMY_CONSUMER;
    }

    protected static abstract class AbstractEntry<S> implements Entry<S> {

        protected double x;
        protected double y;
        protected double z;
        protected AbstractSpatialMap<S> parent;

        protected AbstractEntry(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public abstract S get();

        public boolean move(Point p) throws IndexOutOfBoundsException {
            return move(p.x, p.y, p.z);
        }

        public boolean move(double x, double y, double z) throws IndexOutOfBoundsException {
            if (isOrphan()) {
                throw new IllegalStateException("This entity has been orphaned and longer belongs to a spatial map");
            }
            if (parent.moveImpl(this, x, y, z)) {
                setPosition(this, x, y, z);
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

        public double getAxis(Axis axis) {
            return axis.of(x,y,z);
        }

        public boolean isOrphan() {
            return parent == null;
        }

        @Override
        public String toString() {
            return String.format("Entry[%.4f,%.4f,%.4f] -> (%s)", x, y, z, get());
        }
    }

    protected static class EntryImpl<S> extends AbstractEntry<S> {

        protected final S object;

        EntryImpl(S object, double x, double y, double z) {
            super(x, y, z);
            this.object = object;
        }

        @Override
        public S get() {
            return object;
        }

    }
}