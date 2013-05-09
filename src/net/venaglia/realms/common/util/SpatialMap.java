package net.venaglia.realms.common.util;

import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Point;

/**
 * User: ed
 * Date: 9/10/12
 * Time: 9:57 PM
 */
public interface SpatialMap<E> extends BasicSpatialMap<E>, Series<SpatialMap.Entry<E>> {

    void clear();

    boolean add(E obj, Point p) throws UnsupportedOperationException;

    boolean add(E obj, double x, double y, double z) throws UnsupportedOperationException;

    int intersect(BoundingVolume<?> region, Consumer<E> consumer);

    public interface Entry<S> extends BasicSpatialMap.BasicEntry<S> {

        public boolean move(Point p) throws IndexOutOfBoundsException, UnsupportedOperationException;

        public boolean move(double x, double y, double z) throws IndexOutOfBoundsException, UnsupportedOperationException;

        public boolean remove() throws UnsupportedOperationException;
    }

    public interface Consumer<S> {
        void found(Entry<S> entry, double x, double y, double z);
    }
}
