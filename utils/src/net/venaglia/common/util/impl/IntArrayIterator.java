package net.venaglia.common.util.impl;

import net.venaglia.common.util.IntIterator;

import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 2/3/15
 * Time: 7:28 PM
 */
public class IntArrayIterator implements IntIterator {

    private final int[] values;
    private final int n;

    private int i = 0;

    public IntArrayIterator(int[] values) {
        this(values, 0, values == null ? 0 : values.length);
    }

    public IntArrayIterator(int[] values, int off, int len) {
        assert values != null;
        this.values = values;
        this.i = off;
        this.n = off + len;
        if (this.i < 0) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        if (this.n > values.length) {
            throw new ArrayIndexOutOfBoundsException(n);
        }
    }

    @Override
    public boolean hasNext() {
        return i < n;
    }

    @Override
    public int next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return values[i++];
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
