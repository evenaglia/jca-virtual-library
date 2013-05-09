package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 4:11 PM
 */
public class Scroll extends AbstractShape<Scroll> {

    /*
     * n = segments;
     * points[0..3] = scroll end centers
     * points[4..4n+3] = scroll end circumference
     * points[4n+4..6n+6] = page edge
     */

    protected final Vector[] normals;
    protected final int segments;

    public Scroll(DetailLevel detailLevel) {
        this(Angle.buildAngles(detailLevel.steps));
    }

    private Scroll(Angle[] angles) {
        this(assertLength(buildPoints(angles), angles.length * 6), buildNormals(angles), angles.length - 1);
    }

    private Scroll(Point[] points, Vector[] normals, int segments) {
        super(assertMultiple(assertMinLength(points, 3), 2));
        this.normals = normals;
        this.segments = segments;
    }

    private static Point[] buildPoints(Angle[] angles) {
        int segments = angles.length - 1;
        List<Point> points = new ArrayList<Point>((segments + 1) * 6);
        Point basePoints[] = new Point[segments + 1];
        basePoints[0] = Point.ORIGIN;
        for (int i = 0; i < segments; i++) {
            Angle angle = angles[i];
            basePoints[segments - i] = new Point(0, angle.sin * 0.125, angle.cos * 0.125);
        }
        Vector[] xlateVectors = {
                new Vector(0.5, 0.625, 0),
                new Vector(-0.5, 0.625, 0),
                new Vector(0.5, -0.625, 0),
                new Vector(-0.5, -0.625, 0)
        };
        for (Point point : basePoints) {
            if (point == null) continue;
            for (Vector xlate : xlateVectors) {
                points.add(point.translate(xlate));
            }
        }
        Angle baseAngle = new Angle(2, 12, 0);
        Point cp1 = new Point(0, baseAngle.cos * 0.125, baseAngle.sin * 0.125 - 0.625);
        Point cp2 = new Point(0, baseAngle.cos * 0.125 + (baseAngle.sin * -0.5), baseAngle.sin * 0.125 - 0.625 + (baseAngle.cos * 0.5));
        Point cp3 = new Point(0, baseAngle.cos * 0.125 + (baseAngle.sin * 0.5), baseAngle.sin * 0.125 + 0.625 + (baseAngle.cos * -0.5));
        Point cp4 = new Point(0, baseAngle.cos * 0.125, baseAngle.sin * 0.125 + 0.625);
        for (int i = 0; i <= segments; i++) {
            Angle angle = angles[i];
            double p = angle.fraction;
            double q = 1.0 - p;
            double y = ((cp1.y * p + cp2.y * q) * p + (cp2.y * p + cp3.y * q) * q) * p +
                       ((cp2.y * p + cp3.y * q) * p + (cp3.y * p + cp4.y * q) * q) * q;
            double z = ((cp1.z * p + cp2.z * q) * p + (cp2.z * p + cp3.z * q) * q) * p +
                       ((cp2.z * p + cp3.z * q) * p + (cp3.z * p + cp4.z * q) * q) * q;
            basePoints[i] = new Point(0, z, y);
        }
        xlateVectors = new Vector[]{
                new Vector(0.5, 0, 0),
                new Vector(-0.5, 0, 0)
        };
        for (Point point : basePoints) {
            for (Vector xlate : xlateVectors) {
                points.add(point.translate(xlate));
            }
        }

        return points.toArray(new Point[points.size()]);
    }

    private static Vector[] buildNormals(Angle[] angles) {
        int segments = angles.length - 1;
        Vector[] normals = new Vector[(segments + 1) * 6];
        normals[0] = normals[2] = Vector.X; // scroll ends - right
        normals[1] = normals[3] = Vector.X.reverse(); // scroll ends - left
        for (int i = 0, j = 4; i < segments; i++) {
            Angle angle = angles[i];
            Vector normal = new Vector(0, angle.sin, angle.cos);
            normals[j++] = normal;
            normals[j++] = normal;
            normals[j++] = normal;
            normals[j++] = normal;
        }
        Angle baseAngle = new Angle(2, 12, 0);
        Point cp1 = new Point(0, baseAngle.cos * 0.125, baseAngle.sin * 0.125 - 0.625);
        Point cp2 = new Point(0, baseAngle.cos * 0.125 + (baseAngle.sin * -0.5), baseAngle.sin * 0.125 - 0.625 + (baseAngle.cos * 0.5));
        Point cp3 = new Point(0, baseAngle.cos * 0.125 + (baseAngle.sin * 0.5), baseAngle.sin * 0.125 + 0.625 + (baseAngle.cos * -0.5));
        Point cp4 = new Point(0, baseAngle.cos * 0.125, baseAngle.sin * 0.125 + 0.625);
        for (int i = 0, j = (segments + 1) * 4; i <= segments; i++) {
            Angle angle = angles[i];
            double p = angle.fraction;
            double q = 1.0 - p;
            double y = (cp1.z * p + cp2.z * q) * p + (cp2.z * p + cp3.z * q) * q -
                       (cp2.z * p + cp3.z * q) * p - (cp3.z * p + cp4.z * q) * q;
            double z = (cp2.y * p + cp3.y * q) * p + (cp3.y * p + cp4.y * q) * q -
                       (cp1.y * p + cp2.y * q) * p - (cp2.y * p + cp3.y * q) * q;
            Vector normal = new Vector(0, y, z).normalize();
            normals[j++] = normal.reverse();
            normals[j++] = normal;
        }
        return normals;
    }

    @Override
    protected Scroll build(Point[] points, XForm xForm) {
        return new Scroll(points, xForm.apply(normals), segments);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        for (int k = 0; k < 4; k += 2) {
            buffer.start(GeometryBuffer.GeometrySequence.POLYGON);
            buffer.normal(normals[k]);
            for (int i = 0, j = k + 4; i < segments; i++, j += 4) {
                buffer.vertex(points[j]);
            }
            buffer.end();
        }

        for (int k = 1; k < 4; k += 2) {
            buffer.start(GeometryBuffer.GeometrySequence.POLYGON);
            buffer.normal(normals[k]);
            for (int i = 0, j = k + (segments) * 4; i < segments; i++, j -= 4) {
                buffer.vertex(points[j]);
            }
            buffer.vertex(points[k + (segments) * 4]);
            buffer.end();
        }

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0, j = 4; i < segments; i++, j += 4) {
            buffer.normal(normals[j]);
            buffer.vertex(points[j]);
            buffer.vertex(points[j + 1]);
        }
        buffer.normal(normals[4]);
        buffer.vertex(points[4]);
        buffer.vertex(points[5]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0, j = 6; i < segments; i++, j += 4) {
            buffer.normal(normals[j]);
            buffer.vertex(points[j]);
            buffer.vertex(points[j + 1]);
        }
        buffer.normal(normals[6]);
        buffer.vertex(points[6]);
        buffer.vertex(points[7]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0, j = segments * 4 + 4; i <= segments; i++, j += 2) {
            buffer.normal(normals[j]);
            buffer.vertex(points[j]);
            buffer.vertex(points[j + 1]);
        }
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0, j = segments * 6 + 6 - 1; i <= segments; i++, j -= 2) {
            buffer.normal(normals[j]);
            buffer.vertex(points[j - 1]);
            buffer.vertex(points[j]);
        }
        buffer.end();

    }

    public Vector getNormal(int index) {
        return normals[index >> 1];
    }

    private static class Angle {

        public final double angle;
        public final double fraction;
        public final double sin;
        public final double cos;

        Angle(int step, int total, double offset) {
            fraction = ((double)step) / total;
            angle = Math.PI * 2.0 * fraction + offset;
            double a = Math.PI * 2.0 * (fraction + offset);
            sin = Math.sin(a);
            cos = Math.cos(a);
        }

        private static Angle[] buildAngles(int segments) {
            if (segments < 4) {
                throw new IllegalArgumentException("Segments must be at least 4");
            }
            Angle[] angles = new Angle[segments + 1];
            for (int i = 0; i <= segments; i++) {
                angles[i] = new Angle(i, segments, 0.166666666666666667);
            }
            return angles;
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Scroll scroll = new Scroll(DetailLevel.HIGH).rotate(Axis.X, 0.85);
//        Scroll scroll = new Scroll(BezierPatch.DetailLevel.MEDIUM_LOW).rotate(Axis.X, 0.85);
//        Scroll scroll = new Scroll(BezierPatch.DetailLevel.LOW).rotate(Axis.X, 0.85);
        new SingleShapeDemo(scroll, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
