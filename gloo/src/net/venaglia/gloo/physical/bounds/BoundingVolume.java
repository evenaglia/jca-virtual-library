package net.venaglia.gloo.physical.bounds;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Element;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;

/**
 * User: ed
 * Date: 9/10/12
 * Time: 11:41 PM
 */
public interface BoundingVolume<T extends BoundingVolume<T>> extends SimpleBoundingVolume, Element<T>, Bounded {

    public enum Type {
        SPHERE {
            @Override
            public BoundingSphere cast(BoundingVolume<?> v) {
                return v.asSphere();
            }
        },
        BOX {
            @Override
            public BoundingBox cast(BoundingVolume<?> v) {
                return v.asBox();
            }
        };

        public abstract BoundingVolume<?> cast(BoundingVolume<?> v);
    }

    Type getBestFit();

    BoundingVolume<?> asBestFit();

    BoundingSphere asSphere();

    BoundingBox asBox();

    Shape<?> asShape(float minAccuracy);

    boolean intersects(double v, Axis axis);

    boolean intersects(BoundingVolume<?> bounds);

    boolean envelops(BoundingVolume<?> bounds);

    boolean isInfinite();

    boolean isNull();

    Point center();

    Point closestTo(Point p);

    Point closestTo(double x, double y, double z);

    double getLongestDimension();

    double volume();
}
