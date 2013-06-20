package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

import java.util.Arrays;
import java.util.Collections;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 3:54 PM
 */
public class Disc extends AbstractShape<Disc> {

    public final Point center;
    public final Vector normal;

    public Disc (int segments, double radius) {
        this(buildPoints(segments, radius), Point.ORIGIN, Vector.Z);
    }

    private Disc(Point[] points, Point center, Vector normal) {
        super(assertMultiple(assertMinLength(points, 6), 2));
        this.center = center;
        this.normal = normal;
    }

    private static Point[] buildPoints(int segments, double radius) {
        if (segments < 3) {
            throw new IllegalArgumentException("Segments must be at least 3");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + radius);
        }
        Point[] points = new Point[segments];
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            points[segments - i - 1] = new Point(Math.sin(a) * radius, Math.cos(a) * radius, 0);
        }
        return points;
    }

    public Disc flip() {
        Point[] points = this.points.clone();
        Collections.reverse(Arrays.asList(points));
        return new Disc(points, center, normal.reverse());
    }

    @Override
    protected Disc build(Point[] points, XForm xForm) {
        Point newCenter = xForm.apply(center);
        Vector newNormal = Vector.betweenPoints(newCenter, xForm.apply(center.translate(normal))).normalize();
        return new Disc(points, newCenter, newNormal);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        buffer.normal(normal);
        buffer.vertex(center);
        for (Point point : points) {
            buffer.vertex(point);
        }
        buffer.vertex(points[0]);
        buffer.end();
    }

    public Vector getNormal(int index) {
        if (index < 0 || index >= points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normal;
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Disc disc = new Disc(64, 0.5).rotate(Axis.X, 0.85);
        new SingleShapeDemo(disc, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
