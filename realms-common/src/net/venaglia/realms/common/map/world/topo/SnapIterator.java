package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.RangeBasedLongSet;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 6/10/14
 * Time: 9:34 PM
 */
class SnapIterator implements Iterator<Long> {

    private final RangeBasedLongSet idSet;
    private final long snap;

    private long next = 0L;

    public SnapIterator(RangeBasedLongSet idSet) {
        this(idSet, 1L);
    }

    public SnapIterator(RangeBasedLongSet idSet, long snap) {
        if (idSet == null) throw new NullPointerException("idSet");
        if (snap < 1) {
            throw new IllegalArgumentException("snap must be a positive integer: " + snap);
        }
        this.idSet = idSet;
        this.snap = snap;
    }

    public boolean hasNext() {
        try {
            idSet.getNext(next);
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public Long next() {
        long n = idSet.getNext(next);
        try {
            if (snap > 1) {
                next = n - n % snap;
            }
            return next;
        } finally {
            next += snap;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
