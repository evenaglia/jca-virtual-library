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

/**
 * User: ed
 * Date: 4/12/13
 * Time: 4:11 PM
 */
public class Tube extends AbstractShape<Tube> implements FlippableShape<Tube> {

    protected final Vector[] normals;

    public Tube(int segments, double radius, double height) {
        this(buildPoints(segments, radius, height / 2.0), buildNormals(segments));
    }

    private Tube(Point[] points, Vector[] normals) {
        super(assertMultiple(assertMinLength(points, 3), 2));
        this.normals = normals;
    }

    public Tube flip() {
        Point[] points = this.points.clone();
        Vector[] normals = this.normals.clone();
        for (int i = 0, j = 1, l = points.length; i < l; i += 2, j += 2) {
            Point p = points[i];
            points[i] = points[j];
            points[j] = p;
        }
        for (int i = 0, l = normals.length; i < l; i++) {
            normals[i] = normals[i].reverse();
        }
        return new Tube(points, normals);
    }

    private static Point[] buildPoints(int segments, double radius, double top) {
        if (segments < 3) {
            throw new IllegalArgumentException("Segments must be at least 3");
        }
        if (top <= 0) {
            throw new IllegalArgumentException("Height must be > 0: " + (top * 2.0));
        }
        Point[] points = new Point[segments * 2];
        double bottom = -top;
        int j = 0;
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            points[j++] = new Point(Math.sin(a) * radius, Math.cos(a) * radius, bottom);
            points[j++] = new Point(Math.sin(a) * radius, Math.cos(a) * radius, top);
        }
        return points;
    }

    private static Vector[] buildNormals(int segments) {
        Vector[] normals = new Vector[segments];
        for (int i = 0; i < segments; i++) {
            double a = Math.PI * 2.0 * i / segments;
            double x = Math.sin(a);
            double y = Math.cos(a);
            normals[i] = new Vector(x, y, 0);
        }
        return normals;
    }

    @Override
    protected Tube build(Point[] points, XForm xForm) {
        return new Tube(points, xForm.apply(normals));
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0, l = points.length; i < l; i++) {
            if ((i & 1) == 0) {
                buffer.normal(normals[i >> 1]);
            }
            buffer.vertex(points[i]);
        }
        buffer.normal(normals[0]);
        buffer.vertex(points[0]);
        buffer.vertex(points[1]);
        buffer.end();
    }

    public Vector getNormal(int index) {
        return normals[index >> 1];
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Tube tube = new Tube(64, 0.5, 1.25).flip().rotate(Axis.X, 0.85);
        new SingleShapeDemo(tube, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
