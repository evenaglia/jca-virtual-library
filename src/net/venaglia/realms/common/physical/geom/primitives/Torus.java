package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Axis;
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
public class Torus extends AbstractShape<Torus> {

    public final Point center;
    public final Vector[] normals;

    protected final int segmentsMajor;
    protected final int segmentsMinor;

    public Torus(int segments, double majorRadius, double minorRadius, boolean inside) {
        this(buildAngles(segments, 0), buildAngles(segments, 0.5), majorRadius, minorRadius, inside);
    }

    public Torus(int segmentsMajor, int segmentsMinor, double majorRadius, double minorRadius, boolean inside) {
        this(buildAngles(segmentsMajor, 0), buildAngles(segmentsMinor, 0.5), majorRadius, minorRadius, inside);
    }

    private Torus(Angle[] anglesMajor, Angle[] anglesMinor, double majorRadius, double minorRadius, boolean inside) {
        this(buildPoints(anglesMajor, anglesMinor, majorRadius, minorRadius, inside),
             buildNormals(anglesMajor, anglesMinor, inside),
             Point.ORIGIN,
             anglesMajor.length,
             anglesMinor.length);
    }

    private Torus(Point[] points, Vector[] normals, Point center, int segmentsMajor, int segmentsMinor) {
        super(assertLength(assertMinLength(points, 9), segmentsMajor * segmentsMinor));
        this.center = center;
        this.normals = normals;
        this.segmentsMajor = segmentsMajor;
        this.segmentsMinor = segmentsMinor;
    }

    private static Angle[] buildAngles(int segments, double offset) {
        if (segments < 3) {
            throw new IllegalArgumentException("Segments must be at least 3");
        }
        Angle[] angles = new Angle[segments];
        for (int i = 0; i < segments; i++) {
            angles[i] = new Angle(i + offset, segments);
        }
        return angles;
    }

    private static Point[] buildPoints(Angle[] anglesMajor, Angle[] anglesMinor, double majorRadius, double minorRadius, boolean inside) {
        Point[] points = new Point[anglesMajor.length * anglesMinor.length];
        int k = 0;
        for (Angle a : anglesMajor) {
            for (Angle b : anglesMinor) {
                double r = majorRadius - b.cos * minorRadius;
                points[k++] = new Point(a.sin * r, a.cos * r, b.sin * minorRadius);
            }
        }
        if (inside) {
            List<Point> list = Arrays.asList(points);
            for (int i = 0, j = 0; i < anglesMajor.length; i++, j += anglesMinor.length) {
                Collections.reverse(list.subList(j, j + anglesMinor.length));
            }
        }
        return points;
    }

    private static Vector[] buildNormals(Angle[] anglesMajor, Angle[] anglesMinor, boolean inside) {
        Vector[] normals = new Vector[anglesMajor.length * anglesMinor.length];
        double reverse = inside ? -1.0 : 1.0;
        int k = 0;
        for (Angle a : anglesMajor) {
            for (Angle b : anglesMinor) {
                normals[k++] = new Vector(a.sin * -b.cos, a.cos * -b.cos, b.sin).normalize(reverse);
            }
        }
        return normals;
    }

    @Override
    protected Torus build(Point[] points, XForm xForm) {
        return new Torus(points, xForm.apply(normals), xForm.apply(center), segmentsMajor, segmentsMinor);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        int l = points.length;
        for (int i = 0, a = 0; i < segmentsMajor; i++, a += segmentsMinor) {
            int b = (a + segmentsMinor) % l;
            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            for (int j = 0, s = a, t = b; j < segmentsMinor; j++, s++, t++) {
                buffer.normal(normals[s]);
                buffer.vertex(points[s]);
                buffer.normal(normals[t]);
                buffer.vertex(points[t]);
            }
            buffer.normal(normals[a]);
            buffer.vertex(points[a]);
            buffer.normal(normals[b]);
            buffer.vertex(points[b]);
            buffer.end();
        }
    }

    public Vector getNormal(int index) {
        return normals[index];
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
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
//        Torus torus = new Torus(64, 1.0, 0.25, false).rotate(Axis.X, 0.85);
        Torus torus = new Torus(64, 4, 1.0, 0.25, false).rotate(Axis.X, 0.85);
        new SingleShapeDemo(torus, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
