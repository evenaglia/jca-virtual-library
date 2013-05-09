package net.venaglia.realms.common.physical.geom.primitives;

import static net.venaglia.realms.common.projection.GeometryBuffer.GeometrySequence.TRIANGLE_FAN;
import static net.venaglia.realms.common.projection.GeometryBuffer.GeometrySequence.QUAD_STRIP;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.FlippableShape;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.matrix.Matrix_1x4;
import net.venaglia.realms.common.util.matrix.Matrix_4x4;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * User: ed
 * Date: 4/18/13
 * Time: 8:40 AM
 */
public class Dome extends AbstractShape<Dome> implements FlippableShape<Dome> {

    private final Vector[] normals;
    private final int segments;
    private final int layers;

    public Dome(Point top, Point edge, Point onSphere1, Point onSphere2, int segments) {
        this(buildPoints(top, edge, onSphere1, onSphere2, segments));
    }

    private Dome(BuildResult buildResult) {
        this(buildResult.points, buildResult.normals, buildResult.segments, buildResult.layers);
    }

    private Dome(Point[] points, Vector[] noramls, int segments, int layers) {
        super(points);
        this.normals = noramls;
        this.segments = segments;
        this.layers = layers;
    }

    private static BuildResult buildPoints(Point top,
                                           Point edge,
                                           Point onSphere1,
                                           Point onSphere2,
                                           int segments) {
        if (segments < 4) {
            throw new IllegalArgumentException("Too few segments, must be at least 4: " + segments);
        }
        BoundingSphere sphere = new BoundingSphere(top, edge, onSphere1, onSphere2);
        assert Math.abs(Vector.computeDistance(top, sphere.center) - sphere.radius) < 0.00001;
        assert Math.abs(Vector.computeDistance(edge, sphere.center) - sphere.radius) < 0.00001;
        assert Math.abs(Vector.computeDistance(onSphere1, sphere.center) - sphere.radius) < 0.00001;
        assert Math.abs(Vector.computeDistance(onSphere2, sphere.center) - sphere.radius) < 0.00001;
        Vector centerToEdge = Vector.betweenPoints(sphere.center, edge);
        Vector relativeZ = Vector.betweenPoints(sphere.center, top);
        Vector relativeY = centerToEdge.cross(relativeZ).normalize(relativeZ.l);
        Vector relativeX = relativeZ.cross(relativeY).normalize(relativeZ.l);
        Matrix_4x4 xform = Matrix_4x4.rotate(relativeX, relativeY, relativeZ);
        xform.product(Matrix_4x4.translate(Vector.X.scale(sphere.radius)));
        double chordAngle = Math.asin(Vector.computeDistance(top, edge) * 0.5 / sphere.radius) * 2;
        Angle[] halfAngles = Angle.generate(segments * 2);
        Angle[] angles = new Angle[segments];
        for (int i = 0; i < segments; i++) {
            angles[i] = halfAngles[i << 1];
        }
        Point xformTop = xform.product(0, 0, 1, Matrix_1x4.View.POINT);
        xform = Matrix_4x4.translate(Vector.betweenPoints(xformTop, top)).product(xform);
        Vector xlate = Vector.betweenPoints(xformTop, top);

        List<Point> points = new ArrayList<Point>();
        points.add(top);
        int layers = 1;
        for (Angle elevation : halfAngles) {
            if (elevation.angle == 0) continue;
            if (elevation.angle > chordAngle) {
                elevation = new Angle(chordAngle);
            }
            double z = elevation.cos;
            for (Angle angle : angles) {
                double x = angle.sin * elevation.sin;
                double y = angle.cos * elevation.sin;
                points.add(xform.product(x, y, z, Matrix_1x4.View.POINT).translate(xlate));
            }
            if (elevation.angle >= chordAngle) {
                break; // last layer
            }
            layers++;
        }

        Vector[] normals = new Vector[points.size()];
        Point center = xform.product(0, 0, 0, Matrix_1x4.View.POINT);
        for (int i = 0, l = points.size(); i < l; i++) {
            normals[i] = Vector.betweenPoints(points.get(i), center).normalize();
        }

        return new BuildResult(points.toArray(new Point[points.size()]),
                               normals,
                               segments,
                               layers);
    }

    @Override
    protected Dome build(Point[] points, XForm xForm) {
        return new Dome(points, xForm.apply(normals), segments, layers);
    }

    public Dome flip() {
        int l = this.points.length;
        Point[] points = this.points.clone();
        List<Point> pointList = Arrays.asList(points);
        for (int i = 0, j = 1; i < layers; i++, j += segments) {
            Collections.reverse(pointList.subList(j, j + segments));
        }
        Vector[] normals = this.normals.clone();
        for (int i = 0; i < l; i++) {
            normals[i] = normals[i].reverse();
        }
        return new Dome(points, normals, segments, layers);
    }

    public Vector getNormal(int index) {
        return normals[index];
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(TRIANGLE_FAN);
        buffer.normal(normals[0]);
        buffer.vertex(points[0]);
        int a = 1;
        for (int i = 0; i < segments; i++) {
            buffer.normal(normals[segments - i]);
            buffer.vertex(points[segments - i]);
            a++;
        }
        buffer.normal(normals[segments]);
        buffer.vertex(points[segments]);
        buffer.end();

        int b = 1;
        for (int layer = 1; layer < layers; layer++) {
            buffer.start(QUAD_STRIP);
            int A = a;
            int B = b;
            for (int i = 0; i < segments; i++) {
                buffer.normal(normals[a]);
                buffer.vertex(points[a]);
                a++;
                buffer.normal(normals[b]);
                buffer.vertex(points[b]);
                b++;
            }
            buffer.normal(normals[A]);
            buffer.vertex(points[A]);
            buffer.normal(normals[B]);
            buffer.vertex(points[B]);
            buffer.end();
        }
    }

    private static class BuildResult {
        public final Point[] points;
        public final Vector[] normals;
        public final int segments;
        public final int layers;

        private BuildResult(Point[] points, Vector[] normals, int segments, int layers) {
            this.points = points;
            this.normals = normals;
            this.segments = segments;
            this.layers = layers;
        }
    }

    private static class Angle {

        public final double angle;
        public final double sin;
        public final double cos;

        private Angle(double angle) {
            this.angle = angle;
            this.sin = Math.sin(angle);
            this.cos = Math.cos(angle);
        }

        public static Angle[] generate(int segments) {
            Angle[] angles = new Angle[segments];
            for (int i = 0; i < segments; i++) {
                angles[i] = new Angle(i * Math.PI * 2.0 / segments);
            }
            return angles;
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Dome dome = new Dome(new Point(0,0,5), new Point(20,0,0), new Point(-20,0,0), new Point(0,20,0), 32);
        new SingleShapeDemo(dome, offWhite, SingleShapeDemo.Mode.WIREFRAME) {
            @Override
            protected void init() {
                // no-op
            }
        }.start();
    }
}
