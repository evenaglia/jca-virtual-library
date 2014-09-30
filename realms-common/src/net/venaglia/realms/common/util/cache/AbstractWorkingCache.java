package net.venaglia.realms.common.util.cache;

import net.venaglia.realms.common.util.Identifiable;
import net.venaglia.common.util.Predicate;

import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 3/21/14
 * Time: 10:37 PM
 */
public abstract class AbstractWorkingCache<E extends Identifiable, N extends AbstractWorkingCache.Node<E>> implements Cache<E> {

    protected final Lock lock = new ReentrantLock();

    private final NavigableMap<Long,N> all;

    private volatile Node<E> head; // most recently used
    private volatile Node<E> tail; // least recently used
    private volatile int modCount = 0;

    public AbstractWorkingCache() {
        this.all = new ConcurrentSkipListMap<Long,N>();
    }

    public int size() {
        return all.size();
    }

    public Iterator<E> iterator() {
        final Iterator<N> iter = all.values().iterator();
        return new Iterator<E>() {

            private N current = null;

            public boolean hasNext() {
                return iter.hasNext();
            }

            public E next() {
                current = null; // in case the call to next() fails
                current = iter.next();
                return current.getValue();
            }

            public void remove() {
                if (current == null) {
                    throw new IllegalStateException();
                }
                lock.lock();
                try {
                    modCount++;
                    doEvict(current);
                    current = null;
                } finally {
                    lock.unlock();
                }
            }
        };
    }

    public int getModCount() {
        return modCount;
    }

    public E get(Long id) {
        N node = all.get(id);
        if (node != null) {
            doHit(node);
            return node.getValue();
        }
        node = doLoad(id);
        return node == null ? null : node.getValue();
    }

    public boolean seed(E value) {
        Long id = value.getId();
        if (id == null) {
            throw new IllegalArgumentException(value.toString());
        }
        lock.lock();
        try {
            N node = all.get(id);
            if (node == null) {
                modCount++;
                node = createEmptyNode();
                node.id = id;
                node.setValue(value);
                if (head != null) {
                    head.prev = node;
                    node.next = head;
                } else {
                    tail = node;
                }
                head = node;
                all.put(id, node);
                admit(node);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean seed(Collection<? extends E> values) {
        NavigableMap<Long,E> toAdd = new TreeMap<Long,E>();
        for (E value : values) {
            Long id = value.getId();
            if (id == null) {
                throw new IllegalArgumentException(value.toString());
            }
            toAdd.put(id, value);
        }
        int count = 0;
        lock.lock();
        try {
            toAdd.keySet().removeAll(all.keySet());
            count = toAdd.size();
            if (count > 0) {
                modCount++;
                N first = null;
                N last = null;
                for (E value : toAdd.values()) {
                    N next = createEmptyNode();
                    if (last != null) {
                        last.next = next;
                        next.prev = last;
                        last = next;
                    } else {
                        first = next;
                        last = next;
                    }
                    last.id = value.getId();
                    last.setValue(value);
                }
                if (head != null) {
                    assert last != null;
                    last.next = head;
                    head = first;
                } else {
                    head = first;
                    tail = last;
                }

            }
        } finally {
            lock.unlock();
        }
        return count > 0;
    }

    public boolean remove(E value) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        lock.lock();
        try {
            modCount++;
            head = null;
            tail = null;
            for (N node : all.values()) {
                node.prev = null;
                node.next = null;
                evict(node);
            }
        } finally {
            all.clear();
            lock.unlock();
        }
    }

    public void evict(E value) {
        lock.lock();
        try {
            N node = all.get(value.getId());
            if (node != null) {
                modCount++;
                doEvict(node);
            }
        } finally {
            lock.unlock();
        }
    }

    public void evictOldest() {
        lock.lock();
        try {
            if (tail != null) {
                modCount++;
                //noinspection unchecked
                doEvict((N)tail);
            }
        } finally {
            lock.unlock();
        }
    }

    public void evictOldest(Predicate<E> predicate) {

    }

    protected abstract N createEmptyNode();

    protected E miss(Long id) {
        return null;
    }

    protected void hit(N node) {
        // no-op
    }

    protected void admit(N node) {
        // no-op
    }

    protected void evict(N node) {
        // no-op
    }

    private N doLoad(Long id) {
        E thing = miss(id);
        if (thing == null || thing instanceof Volatile) {
            return null;
        }
        N node = createEmptyNode();
        node.id = id;
        node.setValue(thing);
        lock.lock();
        try {
            if (all.containsKey(id)) {
                return all.get(id);
            } else {
                all.put(id, node);
                node.next = head;
                head = node;
            }
        } finally {
            lock.unlock();
        }
        return node;
    }

    private void doHit(N node) {
        if (head != node) {
            lock.lock();
            try {
                if (node.next != null) {
                    node.next.prev = node.prev;
                } else if (tail == node) {
                    tail = node.prev;
                }
                if (node.prev != null) {
                    node.prev.next = node.next;
                }
                node.prev = null;
                node.next = head;
                hit(node);
            } finally {
                lock.unlock();
            }
        }
    }

    private void doEvict(N node) {
        if (all.containsKey(node.id)) {
            all.remove(node.id);
            if (node.next != null) {
                node.next.prev = node.prev;
            } else if (tail == node) {
                tail = node.prev;
            }
            if (node.prev != null) {
                node.prev.next = node.next;
            } else if (head == node) {
                head = node.next;
            }
            node.prev = null;
            node.next = null;
            evict(node);
        }
    }

    protected static class Node<E> {

        private Long id;
        private Node<E> prev;
        private Node<E> next;

        private E value;

        public E getValue() {
            return value;
        }

        public void setValue(E value) {
            this.value = value;
        }
    }
}
