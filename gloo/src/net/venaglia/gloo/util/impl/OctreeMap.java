package net.venaglia.gloo.util.impl;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.bounds.SimpleBoundingVolume;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.complex.BoundingShape;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.util.OctreeVoxel;

import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 10/8/12
 * Time: 12:11 PM
 */
public class OctreeMap<E> extends AbstractSpatialMap<E> implements Projectable {

    private final OctreeMap<E> parent;
    private final int depth;
    private final int divideThreshold;
    private final int unifyThreshold;
    private final double x0;
    private final double x_;
    private final double x1;
    private final double y0;
    private final double y_;
    private final double y1;
    private final double z0;
    private final double z_;
    private final double z1;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private boolean containsNodes = true;
    private Object[] children = new Object[4];
    private int totalEntryCount = 0;
    private int modCount = 0;

    private OctreeMap(OctreeMap<E> parent, int depth,
                      int divideThreshold, int unifyThreshold,
                      double x0, double x1,
                      double y0, double y1,
                      double z0, double z1) {
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
        this.z0 = z0;
        this.z_ = (z0 + z1) / 2.0;
        this.z1 = z1;
        if (divideThreshold < 2) {
            throw new IllegalArgumentException("divideThreshold must be at least 2");
        }
        if (unifyThreshold >= divideThreshold) {
            throw new IllegalArgumentException("unifyThreshold (" + unifyThreshold + ") must be greater than divideThreshold (" + divideThreshold + ")");
        }
    }

    public OctreeMap(BoundingVolume<?> bounds, int divideThreshold, int unifyThreshold) {
        this(null, 0,
             divideThreshold, unifyThreshold,
             bounds.min(Axis.X), bounds.max(Axis.X),
             bounds.min(Axis.Y), bounds.max(Axis.Y),
             bounds.min(Axis.Z), bounds.max(Axis.Z));
    }

    public OctreeMap(BoundingVolume<?> bounds, int divideThreshold) {
        this(bounds, divideThreshold, divideThreshold - (divideThreshold / 8) - 1);
    }

    public OctreeMap(BoundingVolume<?> bounds) {
        this(bounds, 6);
    }

    public boolean contains(double x, double y, double z) {
        if (withinBounds(x, y, z)) {
            lock.readLock().lock();
            try {
                if (containsNodes) {
                    for (int i = 0; i < totalEntryCount; i++) {
                        Entry<?> child = (Entry<?>)children[i];
                        if (child.getAxis(Axis.X) == x && child.getAxis(Axis.Y) == y && child.getAxis(Axis.Z) == z) {
                            return true;
                        }
                    }
                } else {
                    OctreeMap<E> child = whichChild(x, y, z, false);
                    return child != null && child.contains(x, y, z);
                }
            } finally {
                lock.readLock().unlock();
            }
        }
        return false;
    }

    public int intersect(SimpleBoundingVolume region, Consumer<E> consumer) {
        double maxX = region.max(Axis.X);
        double minX = region.min(Axis.X);
        double maxY = region.max(Axis.Y);
        double minY = region.min(Axis.Y);
        double maxZ = region.max(Axis.Z);
        double minZ = region.min(Axis.Z);
        if (maxX < x0 || minX >= x1 || maxY < y0 || minY >= y1 || maxZ < z0 || minZ >= z1) {
            return 0; // not without the bounds of this node;
        }
        int hits = 0;
        lock.readLock().lock();
        try {
            if (containsNodes) {
                for (int i = 0; i < totalEntryCount; i++) {
                    @SuppressWarnings("unchecked")
                    AbstractEntry<E> child = (AbstractEntry<E>)children[i];
                    if (region.includes(child.getAxis(Axis.X), child.getAxis(Axis.Y), child.getAxis(Axis.Z))) {
                        hits++;
                        consume(child, consumer);
                    }
                }
            } else {
                int cells = 0xFF; // bit mask
                cells &= intersect(x_, minX, maxX, 0x55, 0xAA);
                cells &= intersect(y_, minY, maxY, 0x33, 0xCC);
                cells &= intersect(z_, minZ, maxZ, 0x0F, 0xF0);
                for (int i = 0; i < 8; i++) {
                    if ((cells & 1) == 1) {
                        @SuppressWarnings("unchecked")
                        OctreeMap<E> child = (OctreeMap<E>)children[i];
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

    public BoundingVolume<?> getBounds() {
        return new BoundingBox(new Point(x0, y0, z0), new Point(x1, y1, z1));
    }

    private int intersect(double n, double min, double max, int lower, int upper) {
        if (n >= max) return lower;
        if (n < min) return upper;
        return lower | upper;
    }

    @Override
    protected boolean addImpl(AbstractEntry<E> entry) throws UnsupportedOperationException {
        double x = entry.getAxis(Axis.X);
        double y = entry.getAxis(Axis.Y);
        double z = entry.getAxis(Axis.Z);
        return addImpl(entry, x, y, z);
    }

    protected boolean addImpl(AbstractEntry<E> entry, double x, double y, double z) throws UnsupportedOperationException {
        if (!withinBounds(x, y, z)) {
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
                setParent(entry, this);
                modCount++;
                return true;
            } else if (containsNodes) {
                // divide spaces
                Object[] previousChildren = children;
                children = new Object[8];
                containsNodes = false;
                for (int i = 0; i < totalEntryCount; i++) {
                    @SuppressWarnings("unchecked")
                    AbstractEntry<E> e = (AbstractEntry<E>)previousChildren[i];
                    double a = e.getAxis(Axis.X);
                    double b = e.getAxis(Axis.Y);
                    double c = e.getAxis(Axis.Z);
                    whichChild(a, b, c, true).addImpl(e, a, b, c);
                }
                containsNodes = false;
            }
            if (whichChild(x, y, z, true).addImpl(entry, x, y, z)) {
                totalEntryCount++;
                modCount++;
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    protected boolean removeImpl(AbstractEntry<E> entry) throws UnsupportedOperationException {
        lock.writeLock().lock();
        try {
            if (containsNodes) {
                int i = indexOf(entry);
                if (i >= 0) {
                    children[i] = children[--totalEntryCount];
                    clearParent(entry, this);
                    modCount++;
                    return true;
                }
                return false;
            }
            OctreeMap<E> child = whichChild(entry.getAxis(Axis.X), entry.getAxis(Axis.Y), entry.getAxis(Axis.Z), true);
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

    private int indexOf(AbstractEntry<E> entry) {
        int i = -1;
        for (int j = 0; j < totalEntryCount; j++) {
            if (children[j].equals(entry)) {
                i = j;
                break;
            }
        }
        return i;
    }

    @Override
    protected boolean moveImpl(AbstractEntry<E> entry, double x, double y, double z)
            throws IndexOutOfBoundsException, UnsupportedOperationException {
        OctreeMap<E> ancestor = findAncestorThatContains(x, y, z);
        assert containsNodes;
        if (ancestor != this) {
            ancestor.lock.writeLock().lock();
            try {
                return ancestor.removeImpl(entry) && ancestor.addImpl(entry, x, y, z);
            } finally {
                ancestor.lock.writeLock().unlock();
            }
        } else {
            return (indexOf(entry) >= 0);
        }
    }

    private OctreeMap<E> whichChild(double x, double y, double z, boolean createIfMissing) {
        // child 0 : x0 <= x < x & y0 <=
        int i = (x < x_ ? 0 : 1) | (y < y_ ? 0 : 2) | (z < z_ ? 0 : 4);
        //noinspection unchecked
        OctreeMap<E> child = (OctreeMap<E>)children[i];
        if (child == null&& createIfMissing) {
            child = createChild(i);
            children[i] = child;
        }
        return child;
    }

    private OctreeMap<E> createChild(int i) {
        return new OctreeMap<E>(this, depth + 1, divideThreshold, unifyThreshold,
                                ((i & 1) == 1 ? x_ : x0), ((i & 1) == 1 ? x1 : x_),
                                ((i & 2) == 2 ? y_ : y0), ((i & 2) == 2 ? y1 : y_),
                                ((i & 4) == 4 ? z_ : z0), ((i & 4) == 4 ? z1 : z_));
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
                    OctreeMap<E> sbuMap = (OctreeMap<E>)child;
                    sbuMap.clear();
                }
                Arrays.fill(children, null);
                totalEntryCount = 0;
                containsNodes = true;
                modCount++;
            } else {
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

                public boolean hasNext() {
                    return childIndex < totalEntryCount;
                }

                public Entry<E> next() {
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
                        OctreeMap<E> nextChild = childIndex < 8 ? (OctreeMap<E>)children[childIndex++] : null;
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

    private OctreeMap<E> findAncestorThatContains(double x, double y, double z) {
        for (OctreeMap<E> node = this; node != null; node = node.parent) {
            if (node.withinBounds(x, y, z)) {
                return node;
            }
        }
        throw new IndexOutOfBoundsException(new Point(x,y,z).toString());
    }

    private boolean withinBounds(double x, double y, double z) {
        return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
    }

    public int getMaxElementsPerVoxel() {
        return divideThreshold;
    }

    public NodeView<E> getNodeView() {
        if (!containsNodes) {
            return new NodeViewImpl<E>() {

                public boolean hasChildNodes() {
                    checkForConcurrentModification();
                    return true;
                }

                @SuppressWarnings("unchecked")
                public NodeView<E> getChildNode(OctreeVoxel voxel) {
                    checkForConcurrentModification();
                    Object child = children[voxel.ordinal()];
                    if (child == null) {
                        return (NodeView<E>)EMPTY_VIEW;
                    }
                    return ((OctreeMap<E>)child).getNodeView();
                }
            };
        } else {
            return new NodeViewImpl<E>() {
                public int getEntryCount() {
                    checkForConcurrentModification();
                    return totalEntryCount;
                }

                @SuppressWarnings("unchecked")
                public Entry<E> getEntry(int i) {
                    checkForConcurrentModification();
                    return (Entry<E>)children[i];
                }
            };
        }
    }

    protected void reconstruct(Iterator<ReconstructOperation<E>> operations) {
        lock.writeLock().lock();
        Object[] children = this.children;
        boolean containsNodes = this.containsNodes;
        int totalEntryCount = this.totalEntryCount;
        this.children = new Object[4];
        this.containsNodes = true;
        this.totalEntryCount = 0;
        try {
            children = reconstructImpl(operations, true);
            containsNodes = this.containsNodes;
            totalEntryCount = this.totalEntryCount;
            modCount++;
        } finally {
            this.children = children;
            this.containsNodes = containsNodes;
            this.totalEntryCount = totalEntryCount;
            lock.writeLock().unlock();
        }
    }

    private Object[] reconstructImpl(Iterator<ReconstructOperation<E>> operations, boolean top) {
        int nodeSeq = 0;
        while (operations.hasNext()) {
            ReconstructOperation<E> operation = operations.next();
            switch (operation.getCode()) {
                case ReconstructOperation.OPERATION_CODE_ADD_ENTRY:
                    if (!containsNodes) {
                        throw new IllegalStateException("Cannot add an entry to a voxel that contains other voxels");
                    }
                    if (nodeSeq > divideThreshold) {
                        throw new IllegalArgumentException("Too many entries");
                    }
                    if (nodeSeq >= children.length) {
                        Object[] c = new Object[divideThreshold];
                        System.arraycopy(children, 0, c, 0, children.length);
                        children = c;
                    }
                    Entry<E> entry = operation.getEntry();
                    if (entry == null) {
                        throw new NullPointerException("Entry cannot be null");
                    }
                    children[nodeSeq++] = entry;
                    break;
                case ReconstructOperation.OPERATION_CODE_MOVE_TO_NEW_CHILD:
                    if (containsNodes && nodeSeq == 0) {
                        children = new Object[8];
                        containsNodes = false;
                    }
                    if (containsNodes) {
                        throw new IllegalStateException("Cannot add a child to a voxel that contains entries");
                    }
                    int index = operation.getVoxel().ordinal();
                    if (children[index] != null) {
                        throw new IllegalStateException("Cannot add a child to a voxel where one already exists");
                    }
                    OctreeMap<E> child = createChild(index);
                    children[index] = child;
                    child.reconstructImpl(operations, false);
                    break;
                case ReconstructOperation.OPERATION_CODE_MOVE_TO_PARENT:
                    if (top) {
                        throw new IllegalStateException("Unexpected code \"move to parent\"");
                    }
                    closeReconstructedNode(nodeSeq, top);
                    return children;
                case ReconstructOperation.OPERATION_CODE_END:
                    if (!top) {
                        throw new IllegalStateException("Unexpected code \"end\"");
                    }
                    closeReconstructedNode(nodeSeq, top);
                    return children;
                default:
                    throw new IllegalArgumentException("Unrecognized code: 0x" + Integer.toHexString(operation.getCode()));
            }
        }
        throw new IllegalStateException("Unexpected end of operations");
    }

    private void closeReconstructedNode(int nodeSeq, boolean top) {
        if (containsNodes) {
            if (nodeSeq == 0 && !top) {
                throw new IllegalStateException("Cannot create a child without any entries");
            }
            totalEntryCount = nodeSeq;
        } else {
            for (Object o : children) {
                if (o instanceof OctreeMap) {
                    totalEntryCount += ((OctreeMap<?>)o).totalEntryCount;
                }
            }
        }

    }

    public boolean isStatic() {
        return false;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        buffer.pushBrush();
        Brush brush = new Brush();
        brush.setLighting(false);
        brush.setDepth(null);
        buffer.applyBrush(brush);
        projectImpl(nowMS, buffer);
        buffer.popBrush();
    }

    public void projectImpl(long nowMS, GeometryBuffer buffer) {
        lock.readLock().lock();
        try {
            if (!containsNodes) {
                for (int i = 0; i < 8; i++) {
                    if (children[i] != null) {
                        ((OctreeMap<?>)children[i]).projectImpl(nowMS, buffer);
                    }
                }
            }
            buffer.color(new Color(colorSine(depth, 0.0), colorSine(depth, 1.0), colorSine(depth, 2.0), 0.5f));
            BoundingShape.drawBox(buffer, new Point(x0, y0, z0), new Point(x1, y1, z1));
        } finally {
            lock.readLock().unlock();
        }
    }

    private float colorSine(int depth, double part) {
        return (float)(Math.sin((depth * -1.0 + Math.PI / 2.0) + Math.PI * part * 0.6666666667) * 0.5 + 0.5);
    }

    public interface NodeView<V> {

        NodeView EMPTY_VIEW = new NodeViewAdapter();

        boolean isEmpty();

        boolean hasChildNodes();

        NodeView<V> getChildNode(OctreeVoxel voxel);

        int getEntryCount();

        Entry<V> getEntry(int i);
    }

    protected interface ReconstructOperation<E> {

        char OPERATION_CODE_MOVE_TO_NEW_CHILD = 'd';
        char OPERATION_CODE_MOVE_TO_PARENT = 'u';
        char OPERATION_CODE_ADD_ENTRY = '+';
        char OPERATION_CODE_END = '\0';

        char getCode();

        OctreeVoxel getVoxel();

        Entry<E> getEntry();
    }

    private static class NodeViewAdapter<V> implements NodeView<V> {

        public boolean isEmpty() {
            return true;
        }

        public boolean hasChildNodes() {
            return false;
        }

        public NodeView<V> getChildNode(OctreeVoxel voxel) {
            throw new NoSuchElementException();
        }

        public int getEntryCount() {
            return 0;
        }

        public Entry<V> getEntry(int i) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    private class NodeViewImpl<V> extends NodeViewAdapter<V> {

        private final int expectedModCount = modCount;

        public boolean isEmpty() {
            return totalEntryCount == 0;
        }

        protected void checkForConcurrentModification() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    public static void main(String[] args) {
        int divideThreshold = 6;
        int unifyThreshold = divideThreshold - (divideThreshold / 8) - 1;
        System.out.println(unifyThreshold);
    }
}
