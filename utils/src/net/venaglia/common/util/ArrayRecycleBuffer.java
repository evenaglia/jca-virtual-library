package net.venaglia.common.util;

/**
 * User: ed
 * Date: 5/9/14
 * Time: 12:54 PM
 */
public interface ArrayRecycleBuffer<A> {

    A get(int minimumLength);

    void recycle(A value);
}
