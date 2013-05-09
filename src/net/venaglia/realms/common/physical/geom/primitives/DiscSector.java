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

/**
 * User: ed
 * Date: 4/12/13
 * Time: 3:54 PM
 */
public class DiscSector extends AbstractShape<DiscSector> implements FlippableShape<DiscSector> {

    public final Point center;
    public final Vector normal;

    public DiscSector(int segments, double radius, double startAngle, double endAngle) {
        this(buildPoints(segments, radius, fixAngle(startAngle), fixAngle(endAngle)), Point.ORIGIN, Vector.Z);
    }

    private DiscSector(Point[] points, Point center, Vector normal) {
        super(assertMultiple(assertMinLength(points, 6), 2));
        this.center = center;
        this.normal = normal;
    }

    private static double fixAngle(double angle) {
        double TWO_PI = Math.PI * 2.0;
        while (angle < 0) {
            angle += TWO_PI;
        }
        while (angle > TWO_PI) {
            angle -= TWO_PI;
        }
        return angle;
    }

    private static Point[] buildPoints(int segments, double radius, double startAngle, double endAngle) {
        if (segments < 2) {
            throw new IllegalArgumentException("Segments must be at least 2");
        }
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be positive: " + radius);
        }
        if (startAngle == endAngle) {
            throw new IllegalArgumentException("Start and end angles cannot be the same: " + startAngle + " & " + endAngle);
        }
        if (endAngle < startAngle) {
            endAngle += Math.PI * 2.0;
        }
        double spanAngle = endAngle - startAngle;

        Point[] points = new Point[segments];
        for (int i = 0; i < segments; i++) {
            double a = spanAngle * i / (segments - 1) + startAngle;
            points[segments - i - 1] = new Point(Math.sin(a) * radius, Math.cos(a) * radius, 0);
        }
        return points;
    }

    @Override
    protected DiscSector build(Point[] points, XForm xForm) {
        Point newCenter = xForm.apply(center);
        Vector newNormal = Vector.betweenPoints(newCenter, xForm.apply(center.translate(normal))).normalize();
        return new DiscSector(points, newCenter, newNormal);
    }

    public DiscSector flip() {
        Point[] points = this.points.clone();
        Collections.reverse(Arrays.asList(points).subList(1, points.length));
        return new DiscSector(points, center, normal.reverse());
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        buffer.normal(normal);
        buffer.vertex(center);
        for (Point point : points) {
            buffer.vertex(point);
        }
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
        DiscSector disc = new DiscSector(64, 0.5, 2.2, 0.9).rotate(Axis.X, 0.85);
        new SingleShapeDemo(disc, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
