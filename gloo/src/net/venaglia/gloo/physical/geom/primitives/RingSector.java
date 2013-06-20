package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 3:54 PM
 */
public class RingSector extends AbstractShape<RingSector> {

    public final Point center;
    public final Vector normal;

    public RingSector(int segments, double radius1, double radius2, double startAngle, double endAngle) {
        this(buildPoints(segments, Math.min(radius1, radius2), Math.max(radius1, radius2), fixAngle(startAngle), fixAngle(endAngle)), Point.ORIGIN, Vector.Z);
    }

    private RingSector(Point[] points, Point center, Vector normal) {
        super(assertMinLength(points, 6));
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

    private static Point[] buildPoints(int segments, double radius1, double radius2, double startAngle, double endAngle) {
        if (segments < 2) {
            throw new IllegalArgumentException("Segments must be at least 2");
        }
        if (radius1 <= 0 || radius2 <= 0) {
            throw new IllegalArgumentException("Radius values must be positive: " + radius1 + " & " + radius2);
        }
        if (radius1 == radius2) {
            throw new IllegalArgumentException("Radius values cannot be the same: " + radius1 + " & " + radius2);
        }
        if (startAngle == endAngle) {
            throw new IllegalArgumentException("Start and end angles cannot be the same: " + startAngle + " & " + endAngle);
        }
        if (endAngle < startAngle) {
            endAngle += Math.PI * 2.0;
        }
        double spanAngle = endAngle - startAngle;

        Point[] points = new Point[segments * 2];
        int j = 0;
        for (int i = 0; i < segments; i++) {
            double a = spanAngle * i / (segments - 1) + startAngle;
            double x = Math.sin(a);
            double y = Math.cos(a);
            points[j++] = new Point(x * radius2, y * radius2, 0);
            points[j++] = new Point(x * radius1, y * radius1, 0);
        }
        return points;
    }

    @Override
    protected RingSector build(Point[] points, XForm xForm) {
        Point newCenter = xForm.apply(center);
        Vector newNormal = Vector.betweenPoints(newCenter, xForm.apply(center.translate(normal))).normalize();
        return new RingSector(points, newCenter, newNormal);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        buffer.normal(normal);
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
        RingSector ring = new RingSector(64, 0.5, 0.3, 2.2, 0.9).rotate(Axis.X, 0.85);
        new SingleShapeDemo(ring, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
