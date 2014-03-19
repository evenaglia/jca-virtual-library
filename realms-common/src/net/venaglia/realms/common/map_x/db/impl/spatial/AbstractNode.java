package net.venaglia.realms.common.map_x.db.impl.spatial;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * User: ed
 * Date: 8/3/13
 * Time: 11:41 AM
 */
abstract class AbstractNode<V> implements Node<V> {

    private static final byte STATE_UNLOADED   = 0;
    private static final byte STATE_LOADING    = 1;
    private static final byte STATE_LOADED     = 2;
    private static final byte STATE_DIRTY      = 3;
    private static final byte STATE_LOAD_ERROR = 4;

    protected final IO<V> io;
    protected final Integer nodeIndex;
    protected final VoxelNode<V> parent;
    protected final MutableDirty dirty;

    private volatile byte state = 0;

    protected AbstractNode(Integer nodeIndex, VoxelNode<V> parent) {
        this(nodeIndex, parent, parent.io);
    }

    protected AbstractNode(Integer nodeIndex, VoxelNode<V> parent, IO<V> io) {
        this.nodeIndex = nodeIndex;
        this.parent = parent;
        this.io = io;
        dirty = new MutableDirty() {
            public boolean isDirty() {
                return state == STATE_DIRTY;
            }

            public void setDirty() {
                if ((state & ((byte)2)) != STATE_LOADED) {
                    throw new IllegalStateException();
                }
                if (state != STATE_DIRTY) {
                    AbstractNode.this.io.queueDirty(AbstractNode.this);
                    state = STATE_DIRTY;
                }
            }

            public void clearDirty() {
                if ((state & ((byte)2)) != STATE_LOADED) {
                    throw new IllegalStateException();
                }
                state = STATE_LOADED;
            }
        };
    }

    public VoxelNode<V> getParent() {
        return parent;
    }

    public Integer getIndex() {
        return nodeIndex;
    }

    protected void ensureLoaded() {
        while (state == STATE_LOADING) {
            Thread.yield();
        }
        if ((state & ((byte)2)) == STATE_LOADED) {
            return; // loaded or dirty
        }
        synchronized (this) {
            if (state == STATE_UNLOADED) { // double check lock
                state = STATE_LOADING;
                loadImpl();
            }
        }
        if (state == STATE_LOAD_ERROR) {
            throw new IllegalStateException(getType() +  " node @ " + nodeIndex + " could not be loaded from disk");
        }
    }

    protected void loadWithInitialData() {
        synchronized (this) {
            if (state != STATE_UNLOADED) {
                throw new IllegalStateException("cannot load with initial data, after having already been loaded");
            }
            state = STATE_LOADING;
            ReadWriteLock lock = io.getLock(nodeIndex);
            lock.writeLock().lock();
            try {
                readFrom(ByteBuffer.wrap(getType().initialData()));
                state = STATE_LOADED;
            } finally {
                lock.writeLock().unlock();
                if (state == STATE_LOADING) {
                    state = STATE_LOAD_ERROR; // load failure
                }
            }
        }
    }

    private void loadImpl() {
        ReadWriteLock lock = io.getLock(nodeIndex);
        lock.writeLock().lock();
        try {
            io.read(this);
            state = STATE_LOADED;
        } finally {
            lock.writeLock().unlock();
            if (state == STATE_LOADING) {
                state = STATE_LOAD_ERROR; // load failure
            }
        }
    }

    protected void flush() {
        if (state == STATE_DIRTY) {
            ReadWriteLock lock = io.getLock(nodeIndex);
            lock.readLock().lock();
            try {
                io.write(this);
                state = STATE_LOADED;
            } finally {
                lock.readLock().unlock();
            }
        }
    }
}
