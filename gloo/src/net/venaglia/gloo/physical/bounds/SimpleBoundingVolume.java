package net.venaglia.gloo.physical.bounds;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;

/**
 * User: ed
 * Date: 9/6/14
 * Time: 12:47 AM
 */
public interface SimpleBoundingVolume {

    double min(Axis axis);

    double max(Axis axis);

    boolean includes(Point point);

    boolean includes(double x, double y, double z);

}
