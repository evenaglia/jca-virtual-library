package net.venaglia.realms.common.map.db_x.impl.spatial;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 8/6/13
 * Time: 5:49 PM
 */
class FreeIndexBuffer<V> {

    private final IO<V> io;
    private final Lock lock = new ReentrantLock();
    private final RootNode<V> root;

    private FreeNode<V> head;
    private FreeNode<V> tail;
    private FreeNode<V> a;
    private FreeNode<V> b;

    FreeIndexBuffer(RootNode<V> root, FreeNode<V> node, IO<V> io) {
        this.root = root;
        head = node;
        this.io = io;
        while (node.getNext() != null) {
            // read the entire chain
            node = node.getNext();
        }
        tail = node;
    }

    Integer take() {
        lock.lock();
        FreeNode<V> mark = a;
        try {
            while (mark.isEmpty()) {
                mark = mark.getNext();
                if (mark == null) {
                    mark = head;
                }
                if (mark == a) {
                    return io.appendNullNode().getIndex();
                }
            }
            return mark.take();
        } finally {
            a = mark;
            lock.unlock();
        }
    }

    void offer(Integer index) {
        lock.lock();
        FreeNode<V> mark = b;
        try {
            while (mark.isFull()) {
                mark = mark.getNext();
                if (mark == null) {
                    mark = head;
                }
                if (mark == b) {
                    mark = tail.allocateNew(take(), root);
                    tail = mark;
                }
            }
            mark.offer(index);
        } finally {
            b = mark;
            lock.unlock();
        }
    }
}
