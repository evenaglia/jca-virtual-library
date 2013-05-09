package net.venaglia.realms.common.physical.geom;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 9:42 AM
 */
public interface FlippableShape<T extends FlippableShape<T>> extends Shape<T> {

    T flip();
}
