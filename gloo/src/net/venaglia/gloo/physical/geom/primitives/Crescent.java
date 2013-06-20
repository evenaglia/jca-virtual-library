package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

/**
 * User: ed
 * Date: 4/15/13
 * Time: 11:57 PM
 */
public class Crescent extends ExtrudedShape<Crescent> {

    public Crescent(double radius, double thickness, DetailLevel detailLevel) {
        super(buildControlPoints(radius), thickness, detailLevel);
    }

    public Crescent(Point[] points,
                    Vector[] normals,
                    DetailLevel detailLevel) {
        super(points, normals, detailLevel);
    }

    private static double[] buildControlPoints(double radius) {
        double ax = radius * 0.5;
        double ay = 0;
        double bx = radius * -0.5;
        double by = -radius;
        double cx = radius * 0.22;
        double cy = 0;
        double dx = radius * -0.5;
        double dy = radius;
        double u = radius * 0.55;
        double v = radius * 0.55 * 0.2;
        double w = radius * 0.0625;
        return new double[]{
                dx, dy,
                dx + u, dy,
                ax, ay + u,
                ax, ay,
                ax, ay - u,
                bx + u, by,
                bx, by,
                bx + v, by,
                cx, cy - u,
                cx, cy,
                cx, cy + u,
                dx + v, dy
        };
    }

    @Override
    protected void projectFaces(GeometryBuffer buffer) {
        int l = points.length - 2;
        int m = l >> 1;
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_STRIP);
        buffer.normal(normals[0]);
        buffer.vertex(ele(points, 1));
        for (int i = 0, j = 3; i < m; i++, j += 2) {
            buffer.vertex(ele(points, l - j));
            buffer.vertex(ele(points, j));
        }
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_STRIP);
        buffer.normal(normals[1]);
        for (int i = 0, j = m; i < m; i++, j += 2) {
            buffer.vertex(ele(points, l - j));
            buffer.vertex(ele(points, j + 2));
        }
        buffer.end();
    }

    @Override
    protected Crescent build(Point[] points, Vector[] normals, XForm xForm) {
        return new Crescent(points, normals, detailLevel);
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Crescent crescent = new Crescent(0.5, 0.125, DetailLevel.MEDIUM);
        new SingleShapeDemo(crescent, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }

}
