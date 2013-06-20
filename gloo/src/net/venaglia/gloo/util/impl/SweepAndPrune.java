package net.venaglia.gloo.util.impl;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 9/20/12
 * Time: 10:39 PM
 */
public class SweepAndPrune<E> extends AbstractSpatialMap<E> {

    private static Integer[] NIL = {};

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final BoundingVolume<?> bounds;

    private List<AbstractEntry<E>> entries = new ArrayList<AbstractEntry<E>>();
    private volatile boolean orderValid = true;
    private Integer[] ordered = NIL;
    private Axis primaryAxis = Axis.X;
    private PrimarySweepComparator sweepComparator = new PrimarySweepComparator();

    public SweepAndPrune() {
        this(null);
    }

    public SweepAndPrune(BoundingVolume<?> bounds) {
        this.bounds = bounds == null || bounds.isInfinite() ? null : bounds;
        if (bounds != null && bounds.isNull()) {
            throw new IllegalArgumentException("The passed bounding volume has an undefined volume: " + bounds);
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            entries.clear();
            orderValid = true;
            primaryAxis = Axis.X;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        return entries.size();
    }

    public boolean contains(double x, double y, double z) {
        sort();
        lock.readLock().lock();
        try {
            Comparator<Integer> toFind = sweepComparator.toFind(primaryAxis.of(x, y, z));
            int min = binarySearch(ordered, 0, ordered.length - 1, -1, toFind, false);
            int max = binarySearch(ordered, 0, ordered.length - 1, -1, toFind, true);
            if (min <= max) {
                max++;
                Integer[] subset = new Integer[max - min];
                System.arraycopy(ordered, min, subset, 0, subset.length);
                SweepComparator secondary = sweepComparator.secondary;
                Arrays.sort(subset, secondary);
                toFind = secondary.toFind(secondary.axis().of(x,y,z));
                min = binarySearch(subset, 0, subset.length - 1, -1, toFind, false);
                max = binarySearch(subset, 0, subset.length - 1, -1, toFind, true);
                if (min <= max) {
                    SweepComparator tertiary = sweepComparator.tertiary;
                    Arrays.sort(subset, min, max, tertiary);
                    toFind = tertiary.toFind(tertiary.axis().of(x,y,z));
                    int _min = binarySearch(subset, min, max, -1, toFind, false);
                    int _max = binarySearch(subset, min, max, -1, toFind, true);
                    return _min <= _max;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return false;
    }

    public int intersect(BoundingVolume<?> region, Consumer<E> consumer) {
        sort();
        int hits = 0;
        lock.readLock().lock();
        consumer = dummyConsumer(consumer);
        try {
            int min = binarySearch(ordered, 0, ordered.length - 1, -1, sweepComparator.toFind(region.min(primaryAxis)), false);
            int max = binarySearch(ordered, 0, ordered.length - 1, -1, sweepComparator.toFind(region.max(primaryAxis)), true);
            if (min <= max) {
                max++;
                Integer[] subset = new Integer[max - min];
                System.arraycopy(ordered, min, subset, 0, subset.length);
                SweepComparator secondary = sweepComparator.secondary;
                Arrays.sort(subset, secondary);
                min = binarySearch(subset, 0, subset.length - 1, -1, secondary.toFind(region.min(secondary.axis())), false);
                max = binarySearch(subset, 0, subset.length - 1, -1, secondary.toFind(region.max(secondary.axis())), true);
                if (min <= max) {
                    SweepComparator tertiary = sweepComparator.tertiary;
                    Arrays.sort(subset, min, max, tertiary);
                    int _min = binarySearch(subset, min, max, -1, tertiary.toFind(region.min(tertiary.axis())), false);
                    int _max = binarySearch(subset, min, max, -1, tertiary.toFind(region.max(tertiary.axis())), true);
                    for (int i = _min; i <= _max; i++) {
                        AbstractEntry<E> entry = entries.get(subset[i]);
                        if (includes(region, entry)) {
                            hits++;
                            consume(entry, consumer);
                        }
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return hits;
    }

    public BoundingVolume<?> getBounds() {
        return bounds;
    }

    protected int binarySearch(Integer[] entryOrder, int low, int high, Integer find, Comparator<Integer> comparator, boolean findFloor) {
        int mid;
        while (low <= high) {
            mid = (low + high) / 2;
            int cmp = comparator.compare(entryOrder[mid], find);
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else if (findFloor) {
                if (low == mid) {
                    return comparator.compare(entryOrder[high], find) == 0 ? high : mid;
                }
                low = mid;
            } else {
                if (high == mid) return mid;
                high = mid;
            }
        }
        return findFloor ? high : low;
    }

    protected void sort() {
        sort(null);
    }

    protected void sort(Axis preferredAxis) {
        if (!orderValid || (preferredAxis != null && primaryAxis != preferredAxis)) {
            lock.writeLock().lock();
            try {
                synchronized (this) {
                    if (!orderValid || (preferredAxis != null && primaryAxis != preferredAxis)) {
                        sortImpl(preferredAxis);
                    }
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    private void sortImpl(Axis preferredAxis) {
        if (preferredAxis == null) {
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            if (entries.isEmpty()) {
                ordered = NIL;
                return;
            }
            if (ordered.length != entries.size()) {
                ordered = genOrdered(entries.size());
            }
            for (AbstractEntry<E> entry : entries) {
                double axis;
                axis = entry.getAxis(Axis.X);
                minX = Math.min(minX, axis);
                maxX = Math.max(maxX, axis);
                axis = entry.getAxis(Axis.Y);
                minY = Math.min(minY, axis);
                maxY = Math.max(maxY, axis);
                axis = entry.getAxis(Axis.Z);
                minZ = Math.min(minZ, axis);
                maxZ = Math.max(maxZ, axis);
            }
            double dx = maxX - minX;
            double dy = maxY - minY;
            double dz = maxZ - minZ;
            if (dx > dy) {
                if (dx > dz) {
                    primaryAxis = Axis.X;
                } else {
                    primaryAxis = Axis.Z;
                }
            } else {
                if (dy > dz) {
                    primaryAxis = Axis.Y;
                } else {
                    primaryAxis = Axis.Z;
                }
            }
        } else {
            primaryAxis = preferredAxis;
        }
        Arrays.sort(ordered, sweepComparator);
        orderValid = true;
    }

    private Integer[] genOrdered(int size) {
        Integer[] result = new Integer[size];
        for (int i = 0; i < size; i++) {
            result[i] = i;
        }
        return result;
    }

    @Override
    protected boolean addImpl(AbstractEntry<E> entry) throws UnsupportedOperationException {
        lock.writeLock().lock();
        try {
            boolean added = entries.add(entry);
            if (added) {
                orderValid = false;
                primaryAxis = null;
                setParent(entry, this);
            }
            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected boolean removeImpl(AbstractEntry<E> entry) throws UnsupportedOperationException {
        lock.writeLock().lock();
        try {
            boolean removed = entries.remove(entry);
            if (removed) {
                orderValid = false;
                primaryAxis = null;
                clearParent(entry, null);
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected boolean moveImpl(AbstractEntry<E> entry, double x, double y, double z)
            throws IndexOutOfBoundsException, UnsupportedOperationException {
        if (isParent(entry, this)) {
            lock.writeLock().lock();
            try {
                orderValid = false;
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public Iterator<Entry<E>> iterator() {
        Iterator<?> iterator = entries.iterator();
        return (Iterator<Entry<E>>)iterator;
    }

    public List<Entry<E>> toList(Axis axis) {
        sort(axis);
        lock.readLock().lock();
        try {
            return toList(ordered);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Entry<E>> toList(Integer[] indices) {
        int size = indices.length;
        List<Entry<E>> list = new ArrayList<Entry<E>>(size);
        for (Integer index : indices) {
            list.add(entries.get(index));
        }
        return list;
    }

    private abstract class SweepComparator implements Comparator<Integer> {

        public int compare(Integer left, Integer right) {
            Axis axis = axis();
            double l = entries.get(left).getAxis(axis);
            double r = entries.get(right).getAxis(axis);
            if (l == r) {
                axis = axis.next();
                l = entries.get(left).getAxis(axis);
                r = entries.get(right).getAxis(axis);
                if (l == r) {
                    axis = axis.next();
                    l = entries.get(left).getAxis(axis);
                    r = entries.get(right).getAxis(axis);
                    return l == r ? 0 : (l < r ? -1 : 1);
                } else {
                    return l < r ? -1 : 1;
                }
            } else {
                return l < r ? -1 : 1;
            }
        }

        public abstract Axis axis();

        protected Comparator<Integer> toFind(final double value) {
            return new Comparator<Integer>() {
                public int compare(Integer left, Integer right) {
                    Axis axis = axis();
                    double l = left == -1 ? value : entries.get(left).getAxis(axis);
                    double r = right == -1 ? value : entries.get(right).getAxis(axis);
                    return l == r ? 0 : (l < r ? -1 : 1);
                }
            };
        }
    }

    protected class PrimarySweepComparator extends SweepComparator {

        protected final SweepComparator secondary = new SweepComparator() {
            @Override
            public Axis axis() {
                return primaryAxis.next();
            }
        };

        protected final SweepComparator tertiary = new SweepComparator() {
            @Override
            public Axis axis() {
                return primaryAxis.prev();
            }
        };

        public Axis axis() {
            return primaryAxis;
        }
    }

    private static boolean simple = true;

    public static void main(String[] args) {
        SweepAndPrune<String> map = new SweepAndPrune<String>();
        List<Point> points = new ArrayList<Point>();
        for (int i = -5; i <= 5; i++) {
            for (int j = -3; j <= 3; j++) {
                for (int k = -3; k <= 3; k++) {
                    Point p = new Point(i, j, k);
                    points.add(p);
                    map.add(p.toString(), p);
                }
            }
        }
        for (Point p : points) {
            assert p.toString().equals(map.get(p));
        }
    }

    public static void main3(String[] args) {
        SweepAndPrune<String> map = new SweepAndPrune<String>();
        String[] source =
               ("-0.083333,-999.999968,-0.240562\n" +
                "-0.250000,-999.999940,-0.240562\n" +
                "-0.416667,-999.999884,-0.240562\n" +
                "-0.583333,-999.999801,-0.240562\n" +
                "-0.166667,-999.999968,-0.192450\n" +
                "-0.333333,-999.999926,-0.192450\n" +
                "-0.500000,-999.999856,-0.192450\n" +
                "-0.250000,-999.999958,-0.144337\n" +
                "-0.416667,-999.999903,-0.144337\n" +
                "-0.166667,-999.999981,-0.096225\n" +
                "-0.333333,-999.999940,-0.096225\n" +
                "-0.500000,-999.999870,-0.096225\n" +
                "-0.250000,-999.999968,-0.048112\n" +
                "-0.416667,-999.999912,-0.048112\n" +
                "-0.333333,-999.999944,0.000000\n" +
                "-0.250000,-999.999968,0.048113\n" +
                "-0.416667,-999.999912,0.048113\n" +
                "-0.333333,-999.999940,0.096225\n" +
                "-0.333333,-999.999926,0.192450\n" +
                "-0.166667,-999.999944,-0.288675\n" +
                "-0.333333,-999.999903,-0.288675\n" +
                "-0.500000,-999.999833,-0.288675\n" +
                "-0.250000,-999.999958,0.144338\n" +
                "-0.166667,-999.999986,0.000000\n" +
                "-0.083333,-999.999986,-0.144337").split("\n");
        Point test = new Point(0.000000,-999.999958,-0.288675);
        for (String point : source) {
            String[] coords = point.split(",");
            map.add(point, Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
        }
        map.add("test", test);
        assert "test".equals(map.get(test));
    }

    public static void main2(String[] args) {
        SweepAndPrune<String> map = new SweepAndPrune<String>();
        String[] names;
        if (simple) {
            names = new String[]{ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        } else {
            names = new String[10000];
            for (int i = 0; i < names.length; i++) {
                names[i] = String.format("%4d", i);
            }
        }
        double[] values = new double[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = (i / 3) * 1.0;
        }
        Random rand = new Random();
        for (int l = names.length, i = l * 3; i >= 0; i--) {
            int left = rand.nextInt(l);
            int right = rand.nextInt(l);
            String name = names[left];
            names[left] = names[right];
            names[right] = name;
            double value = values[left];
            values[left] = values[right];
            values[right] = value;
        }
        for (int i = 0; i < names.length; i++) {
            map.add(names[i], values[i], values[i], values[i]);
        }

        Comparator<Integer> comparator = map.sweepComparator.toFind(1.5);
        map.sort();
        int index = map.binarySearch(map.ordered, 0, map.ordered.length - 1, -1, comparator, false);
        System.out.println(index);
    }
}
