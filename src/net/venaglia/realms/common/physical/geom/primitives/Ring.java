package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.FlippableShape;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 3:54 PM
 */
public class Ring extends AbstractShape<Ring> implements FlippableShape<Ring> {

    public final Point center;
    public final Vector normal;

    public Ring(int segments, double radius1, double radius2) {
        this(buildPoints(segments, Math.min(radius1, radius2), Math.max(radius1, radius2)), Point.ORIGIN, Vector.Z);
    }

    private Ring(Point[] points, Point center, Vector normal) {
        super(assertMinLength(points, 6));
        this.center = center;
        this.normal = normal;
    }

    private static Point[] buildPoints(int segments, double radius1, double radius2) {
        if (segments < 3) {
            throw new IllegalArgumentException("Segments must be at least 3");
        }
        if (radius1 < 0 || radius2 < 0) {
            throw new IllegalArgumentException("Radius values must be positive: " + radius1 + " & " + radius2);
        }
        if (radius1 == radius2) {
            throw new IllegalArgumentException("Radius values cannot be the same: " + radius1 + " & " + radius2);
        }
        Point[] points = new Point[segments * 2];
        int j = 0;
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            double x = Math.sin(a);
            double y = Math.cos(a);
            points[j++] = new Point(x * radius2, y * radius2, 0);
            points[j++] = new Point(x * radius1, y * radius1, 0);
        }
        return points;
    }

    public Ring flip() {
        Point[] points = this.points.clone();
        List<Point> list = Arrays.asList(points);
        for (int i = 0, l = points.length; i < l; i += 2) {
            Collections.reverse(list.subList(i, i + 2));
        }
        Collections.reverse(list);
        return new Ring(points, center, normal.reverse());
    }

    @Override
    protected Ring build(Point[] points, XForm xForm) {
        Point newCenter = xForm.apply(center);
        Vector newNormal = Vector.betweenPoints(newCenter, xForm.apply(center.translate(normal))).normalize();
        return new Ring(points, newCenter, newNormal);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        buffer.normal(normal);
        for (Point point : points) {
            buffer.vertex(point);
        }
        buffer.vertex(points[0]);
        buffer.vertex(points[1]);
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
        Ring ring = new Ring(64, 0.5, 0.3).rotate(Axis.X, 0.85);
        new SingleShapeDemo(ring, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
