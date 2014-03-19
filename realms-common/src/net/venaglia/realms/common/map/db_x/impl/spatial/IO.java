package net.venaglia.realms.common.map.db_x.impl.spatial;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 8/2/13
 * Time: 9:11 PM
 */
abstract class IO<V> {

    boolean readonly() {
        return true;
    }

    abstract void read(Node node);

    void write(Node node) {
        throw new UnsupportedOperationException();
    }

    ReadWriteLock getLock(Integer nodeIndex) {
        primaryLock.lock();
        try {
            WeakReference<ReadWriteLock> lockRef = locks.get(nodeIndex);
            ReadWriteLock lock = lockRef == null ? null : lockRef.get();
            if (lock == null) {
                lock = new ReentrantReadWriteLock();
                locks.put(nodeIndex, new WeakReference<ReadWriteLock>(lock));
            }
            return lock;
        } finally {
            primaryLock.unlock();
        }
    }

    void ensureWritable() {
        if (readonly()) {
            throw new UnsupportedOperationException();
        }
    }

    abstract Entry<V> entryFor(ValueNode node, boolean highValue);

    abstract NullNode<V> appendNullNode();

    abstract void queueDirty(Node<V> node);

    // ---------------------------------------------------------------------------- private methods

    private final Lock primaryLock = new ReentrantLock();
    private final Map<Integer,WeakReference<ReadWriteLock>> locks = new TreeMap<Integer,WeakReference<ReadWriteLock>>();

}
