package net.venaglia.realms.common.physical.bounds;

import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Element;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.projection.Projectable;

/**
 * User: ed
 * Date: 9/10/12
 * Time: 11:41 PM
 */
public interface BoundingVolume<T extends BoundingVolume<T>> extends Element<T>, Bounded {

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

    double min(Axis axis);

    double max(Axis axis);

    boolean includes(Point point);

    boolean includes(double x, double y, double z);

    boolean intersects(double v, Axis axis);

    boolean intersects(BoundingVolume<?> bounds);

    boolean envelops(BoundingVolume<?> bounds);

    boolean isInfinite();

    boolean isNull();

    Point center();

    double volume();
}
