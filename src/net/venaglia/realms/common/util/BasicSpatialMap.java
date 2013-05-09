package net.venaglia.realms.common.util;

import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;

/**
 * User: ed
 * Date: 3/5/13
 * Time: 9:52 PM
 */
public interface BasicSpatialMap<E> {

    boolean isEmpty();

    boolean contains(double x, double y, double z);

    E get(Point p);

    E get(double x, double y, double z);

    E get(Point p, double r);

    E get(double x, double y, double z, double r);

    int intersect(BoundingVolume<?> region, BasicConsumer<E> consumer);

    BoundingVolume<?> getBounds();

    Series<E> asSeries();

    public interface BasicEntry<S> extends Ref<S> {
        public double getAxis(Axis axis);
    }

    public interface BasicConsumer<S> {
        void found(BasicEntry<S> entry, double x, double y, double z);
    }
}
