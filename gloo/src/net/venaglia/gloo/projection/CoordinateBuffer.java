package net.venaglia.gloo.projection;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 4:59 PM
 */
public interface CoordinateBuffer {

    void vertex(Point point);

    void vertex(double x, double y, double z);

    void normal(Vector normal);

    void normal(double i, double j, double k);

    void color(Color color);

    void color(float r, float g, float b);

    void colorAndAlpha(Color color);

    void colorAndAlpha(float r, float g, float b, float a);

    void coordinate(Coordinate coordinate);

    void coordinates(Iterable<Coordinate> coordinates);
}
