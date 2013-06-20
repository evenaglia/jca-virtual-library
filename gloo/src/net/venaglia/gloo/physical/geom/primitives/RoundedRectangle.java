package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.FlippableShape;
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
 * Date: 4/22/13
 * Time: 8:25 PM
 */
public class RoundedRectangle extends AbstractShape<RoundedRectangle> implements FlippableShape<RoundedRectangle> {

    private final Vector normal;

    public RoundedRectangle (double width, double height, double radius, DetailLevel detailLevel) {
        this(buildPoints(width, height, radius, detailLevel), Vector.Z);
    }

    public RoundedRectangle (Point[] points, Vector normal) {
        super(points);
        this.normal = normal;
    }

    public RoundedRectangle flip() {
        Point[] points = this.points.clone();
        Collections.reverse(Arrays.asList(points));
        return new RoundedRectangle(points, normal.reverse());
    }

    @Override
    protected RoundedRectangle build(Point[] points, XForm xForm) {
        return new RoundedRectangle(points, xForm.apply(normal));
    }

    public Vector getNormal(int index) {
        if (index < 0 || index >= points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normal;
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_FAN);
        for (Point point : points) {
            buffer.vertex(point);
        }
        buffer.end();
    }

    private static Point[] buildPoints(double width, double height, double radius, DetailLevel detailLevel) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Radius must be greater than zero: " + radius);
        }
        radius = Math.min(radius, Math.min(width, height) * 0.5);
        Point[] points = new Point[detailLevel.steps + 4];
        Angle[] angles = Angle.buildAngles(detailLevel.steps);
        int q = detailLevel.steps >> 2;
        int k = 0;
        double[] corners = {
            width *  0.5, height *  0.5,
            width *  0.5, height * -0.5,
            width * -0.5, height * -0.5,
            width * -0.5, height *  0.5
        };
        double baseX = corners[0];
        double baseY = corners[1];
        for (int i = 0, j = 2, l = detailLevel.steps; i <= l; i++) {
            Angle angle = angles[i % l];
            if (i > 0 && i % q == 0) {
                points[k++] = new Point(baseX + angle.sin * radius, baseY + angle.cos * radius, 0);
                baseX = corners[j++ % 8];
                baseY = corners[j++ % 8];
                if (k >= points.length) {
                    break;
                }
            }
            points[k++] = new Point(baseX + angle.sin * radius, baseY + angle.cos * radius, 0);
        }
        return points;
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

        private static Angle[] buildAngles(int segments) {
            Angle[] angles = new Angle[segments];
            for (int i = 0; i < segments; i++) {
                angles[i] = new Angle(Math.PI * 2.0 * i / segments);
            }
            return angles;
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        RoundedRectangle disc = new RoundedRectangle(3,3,0.25, DetailLevel.HIGH).rotate(Axis.X, 0.85);
        new SingleShapeDemo(disc, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
