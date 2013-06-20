package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.common.util.Pair;
import net.venaglia.gloo.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 5/10/13
 * Time: 6:22 PM
 */
public class Cylinder extends AbstractShape<Cylinder> {

    private Vector[] normals;
    private Point center;

    public Cylinder(double radius, double height, DetailLevel detailLevel) {
        this(build(radius, height, detailLevel));
    }

    private Cylinder(Pair<Point[],Vector[]> build) {
        this(build.getA(), build.getB(), Point.ORIGIN);
    }

    private Cylinder(Point[] points, Vector[] normals, Point center) {
        super(points);
        this.normals = normals;
        this.center = center;
    }

    private static Pair<Point[],Vector[]> build(double radius, double height, DetailLevel detailLevel) {
        Angle[] angles = Angle.buildAngles(detailLevel.steps * 2);
        List<Point> points = new ArrayList<Point>(detailLevel.steps * 4);
        List<Vector> normals = new ArrayList<Vector>(detailLevel.steps * 2 + 2);
        double h = height * 0.5;
        for (Angle angle : angles) {
            double x = angle.sin * radius;
            double y = angle.cos * radius;
            points.add(new Point(x, y, -h));
            points.add(new Point(x, y, h));
            normals.add(new Vector(x, y, 0));
        }
        normals.add(Vector.Z);           // top normal
        normals.add(Vector.Z.reverse()); // bottom normal
        return new Pair<Point[], Vector[]>(
                points.toArray(new Point[points.size()]),
                normals.toArray(new Vector[normals.size()])
        );
    }

    public Cylinder flip() {
        Point[] points = this.points.clone();
        for (int i = 0; i < points.length; i += 2) {
            Point p = points[i];
            points[i] = points[i + 1];
            points[i + 1] = p;
        }
        Vector[] normals = this.normals.clone();
        for (int i = 0; i < normals.length; i++) {
            normals[i] = normals[i].reverse();
        }
        return new Cylinder(points, normals, center);
    }

    protected Cylinder build(Point[] points, XForm xForm) {
        return new Cylinder(points, xForm.apply(normals), xForm.apply(center));
    }

    public Vector getNormal(int index) {
        if (index < 0 || index > points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normals[index >> 1];
    }

    protected void project(GeometryBuffer buffer) {
        int l = points.length;
        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0; i < l; i += 2) {
            buffer.normal(normals[i >> 1]);
            buffer.vertex(points[i]);
            buffer.vertex(points[i + 1]);
        }
        buffer.normal(normals[0]);
        buffer.vertex(points[0]);
        buffer.vertex(points[1]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        buffer.normal(normals[normals.length - 2]); // top normal
        for (int i = 0; i < l; i += 2) {
            buffer.vertex(points[i]);
        }
        buffer.vertex(points[0]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        buffer.normal(normals[normals.length - 1]); // bottom normal
        for (int i = l - 1; i > 0; i -= 2) {
            buffer.vertex(points[i]);
        }
        buffer.vertex(points[l - 1]);
        buffer.end();
    }

    private static class Angle {

        public final double angle;
        public final double sin;
        public final double cos;

        Angle(double step, int total) {
            angle = Math.PI * 2.0 * step / total;
            sin = Math.sin(angle);
            cos = Math.cos(angle);
        }

        public static Angle[] buildAngles(int segments) {
            if (segments < 3) {
                throw new IllegalArgumentException("Segments must be at least 3");
            }
            Angle[] angles = new Angle[segments];
            for (int i = 0; i < segments; i++) {
                angles[i] = new Angle(i, segments);
            }
            return angles;
        }
    }

    public static void main(String[] args) {
        new SingleShapeDemo(new Cylinder(0.5, 1.25, DetailLevel.HIGH).rotate(Axis.X, 0.85), new Color(1.0f, 0.6f, 0.0f), SingleShapeDemo.Mode.SHADED).start();
    }
}
