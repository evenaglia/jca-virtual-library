package net.venaglia.common.util;

import net.venaglia.common.util.impl.RangeIterator;

/**
 * User: ed
 * Date: 1/27/15
 * Time: 8:32 AM
 */
public interface IntIterator {

    IntIterator NULL = new RangeIterator(0,0);

    boolean hasNext();

    int next();

    void remove();
}
