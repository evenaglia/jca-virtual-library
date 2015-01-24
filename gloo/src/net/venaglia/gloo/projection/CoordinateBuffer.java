package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 4:59 PM
 */
public interface CoordinateBuffer extends ColorBuffer {

    void vertex(Point point);

    void vertex(double x, double y, double z);

    void normal(Vector normal);

    void normal(double i, double j, double k);

    void coordinate(Coordinate coordinate);

    void coordinates(Iterable<Coordinate> coordinates);
}
