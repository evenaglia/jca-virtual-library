package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:35 PM
 */
public final class Line extends AbstractShape<Line> {

    public Line(Point a, Point b) {
        super(toArray(a, b));
    }

    public Line(Point a, Point b, Point... points) {
        super(toArray(a, b, points));
    }

    public Line(Iterable<Point> points) {
        super(assertMinLength(toArray(points), 2));
    }

    private Line(Point[] points) {
        super(points);
    }

    @Override
    protected Line build(Point[] points, XForm xForm) {
        return new Line(points);
    }

    public Vector getVector(int i) {
        Point a = points[i];
        Point b = points[i + 1];
        return new Vector(b.x - a.x, b.y - a.y, b.z - a.z);
    }

    public Vector getNormal(int i) {
        return null;
    }

    public void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.LINE_STRIP);
        for (Point p : points) {
            buffer.vertex(p);
        }
        buffer.end();
    }
}
