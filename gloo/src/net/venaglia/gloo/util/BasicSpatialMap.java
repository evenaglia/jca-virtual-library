package net.venaglia.gloo.util;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.bounds.SimpleBoundingVolume;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.Series;

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

    int intersect(SimpleBoundingVolume region, BasicConsumer<E> consumer);

    BoundingVolume<?> getBounds();

    Series<E> asSeries();

    public interface BasicEntry<S> extends Ref<S> {
        public double getAxis(Axis axis);
    }

    public interface BasicConsumer<S> {
        void found(BasicEntry<S> entry, double x, double y, double z);
    }
}
