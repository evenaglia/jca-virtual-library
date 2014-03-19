package net.venaglia.realms.common.map_x.db.impl.spatial;

import net.venaglia.common.util.Series;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 8/7/13
 * Time: 5:51 PM
 */
class ValueSeries<V> implements Series<Entry<V>> {

    private final Map<Integer,ValueNode<V>> nodes = new HashMap<Integer,ValueNode<V>>(4);
    private final VoxelNode<V> parent;
    private final Octant octant;
    private final ReadWriteLock lock;

    private volatile boolean writeInit = false;
    private Map<Integer,ValueNode<V>> nodesByEntryKey;
    private Set<Integer> nodesWithFreeSpace;

    private ValueNode<V> head;
    private ValueNode<V> tail;
    private int count = 0;
    private int modcount = 0;

    public ValueSeries(VoxelNode<V> parent, Octant octant, IO<V> io, ValueNode<V> head) {
        this.parent = parent;
        this.octant = octant;
        this.lock = io.readonly() ? null : new ReentrantReadWriteLock();
        if (head != null) {
            this.head = head;
            ValueNode<V> node = head;
            while (node != null) {
                nodes.put(node.getIndex(), node);
                if (node.hasEntry1()) count++;
                if (node.hasEntry2()) count++;
                tail = node;
                node = node.getNext();
            }
        }
    }

    public int size() {
        return count;
    }

    public Iterator<Entry<V>> iterator() {
        return new Iterator<Entry<V>>() {

            private int expectedModCount = modcount;
            private ValueNode<V> node = head;
            private boolean highValue = false;
            private Entry<V> nextValue = null;
            private ValueNode<V> nextValueNode = null;
            private Entry<V> lastValue = null;

            public boolean hasNext() {
                checkForConcurrentModification();
                while (nextValue == null && node != null) {
                    nextValue = highValue ? node.getEntry2() : node.getEntry1();
                    nextValueNode = node;
                    if (highValue) {
                        highValue = false;
                        node = node.getNext();
                    } else {
                        highValue = true;
                    }
                }
                return nextValue != null;
            }

            public Entry<V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                if (nodesByEntryKey != null) {
                    nodesByEntryKey.put(nextValue.getKey(), nextValueNode);
                }
                try {
                    return nextValue;
                } finally {
                    lastValue = nextValue;
                    nextValue = null;
                }
            }

            public void remove() {
                ValueSeries.this.remove(lastValue);
            }

            private void checkForConcurrentModification() {
                if (modcount != expectedModCount) {
                    throw new ConcurrentModificationException();
                }
            }
        };
    }

    protected void initForWrite() {
        if (!writeInit) {
            nodesByEntryKey = new HashMap<Integer,ValueNode<V>>(count);
            nodesWithFreeSpace = new HashSet<Integer>(count);
            for (ValueNode<V> node : nodes.values()) {
                byte mask = (byte)((node.hasEntry1() ? 1 : 0) | (node.hasEntry2() ? 2 : 0));
                switch (mask) {
                    case 0: // both empty
                        nodesWithFreeSpace.add(node.getIndex());
                        break;
                    case 1: // 1 has a value, 2 is empty
                        nodesWithFreeSpace.add(node.getIndex());
                        nodesByEntryKey.put(node.getEntry1().getKey(), node);
                        break;
                    case 2: // 2 has a value, 1 is empty
                        nodesWithFreeSpace.add(node.getIndex());
                        nodesByEntryKey.put(node.getEntry2().getKey(), node);
                        break;
                    case 3: // both have a value
                        nodesByEntryKey.put(node.getEntry1().getKey(), node);
                        nodesByEntryKey.put(node.getEntry2().getKey(), node);
                        break;
                }
            }
            writeInit = true;
        }
    }

    void add(Entry<V> entry) {
        if (lock == null) {
            throw new UnsupportedOperationException();
        }
        if (entry == null) throw new NullPointerException("entry");
        lock.writeLock().lock();
        try {
            initForWrite();
            if (!nodesByEntryKey.containsKey(entry.getKey())) {
                addTo(getNodeWithFreeSpace(), entry);
            }
            // todo: check for voxel split
        } finally {
            lock.writeLock().unlock();
        }
    }

    private ValueNode<V> getNodeWithFreeSpace() {
        if (nodesWithFreeSpace.isEmpty()) {
            ValueNode<V> node = new ValueNode<V>(parent.getFreeIndexBuffer().take(), parent, tail);
            node.dirty.setDirty();
            if (tail != null) {
                tail.setNext(node);
            }
            tail = node;
            if (head == null) {
                head = node;
            }
            nodes.put(node.getIndex(), node);
            nodesWithFreeSpace.add(node.getIndex());
        }
        Integer index = nodesWithFreeSpace.iterator().next();
        return nodes.get(index);
    }

    private void addTo(ValueNode<V> valueNode, Entry<V> entry) {
        if (!valueNode.hasEntry1()) {
            modcount++;
            valueNode.setEntry1(entry);
            nodesByEntryKey.put(entry.getKey(), valueNode);
            if (valueNode.hasEntry2()) {
                nodesWithFreeSpace.remove(valueNode.getIndex());
            }
            count++;
        } else if (!valueNode.hasEntry2()) {
            modcount++;
            valueNode.setEntry2(entry);
            nodesByEntryKey.put(entry.getKey(), valueNode);
            nodesWithFreeSpace.remove(valueNode.getIndex());
            count++;
        } else {
            throw new IllegalArgumentException();
        }
    }

    void remove(Entry<V> entry) {
        if (lock == null) {
            throw new UnsupportedOperationException();
        }
        if (entry == null) throw new NullPointerException("entry");
        lock.writeLock().lock();
        try {
            initForWrite();
            ValueNode<V> valueNode = nodesByEntryKey.remove(entry.getKey());
            if (valueNode != null) {
                if (valueNode.hasEntry1() && valueNode.getEntry1().getKey().equals(entry.getKey())) {
                    modcount++;
                    valueNode.setEntry1(null);
                    nodesWithFreeSpace.add(valueNode.getIndex());
                    count--;
                } else if (valueNode.hasEntry2() && valueNode.getEntry2().getKey().equals(entry.getKey())) {
                    modcount++;
                    valueNode.setEntry2(null);
                    nodesWithFreeSpace.add(valueNode.getIndex());
                    count--;
                }
            }
            // todo: check for voxel join
        } finally {
            lock.writeLock().unlock();
        }
    }
}
