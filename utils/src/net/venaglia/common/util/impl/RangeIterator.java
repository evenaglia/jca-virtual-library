package net.venaglia.common.util.impl;

import net.venaglia.common.util.IntIterator;

/**
 * User: ed
 * Date: 1/28/15
 * Time: 8:08 PM
 */
public class RangeIterator implements IntIterator {

    private final int end;

    private int n;

    /**
     * @param start value to start with (inclusive)
     * @param end value to end with (exclusive)
     * @throws IllegalArgumentException if start >= end
     */
    public RangeIterator(int start, int end) {
        if (start >= end) {
            throw new IllegalArgumentException("Start must be less than end: start=" + start + ", end=" + end);
        }
        this.end = end;
        this.n = start;
    }

    @Override
    public boolean hasNext() {
        return n < end;
    }

    @Override
    public int next() {
        return n++;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
