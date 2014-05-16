package net.venaglia.common.util;

/**
 * User: ed
 * Date: 4/11/14
 * Time: 5:39 PM
 */
public interface RecycleBuffer<E> {

    E get();

    void recycle(E value);
}
