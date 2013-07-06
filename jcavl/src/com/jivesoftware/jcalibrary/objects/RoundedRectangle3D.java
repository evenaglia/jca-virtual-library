package com.jivesoftware.jcalibrary.objects;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.geom.primitives.RoundedRectangle;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 10:36 PM
 */
public class RoundedRectangle3D extends AbstractShape<RoundedRectangle3D> {

    private static final Vector[] BASE_NORMALS = { Vector.Z, Vector.Z.reverse() };

    private final Vector[] normals;

    public RoundedRectangle3D(double x, double y, DetailLevel detailLevel) {
        this(x, y, 0.125, 0.025, 0.25, detailLevel);
    }
    public RoundedRectangle3D(double x, double y, double radius, double frame, double margin, DetailLevel detailLevel) {
        this(buildPoints(x, y, radius, frame, margin, detailLevel), BASE_NORMALS.clone());
    }

    public RoundedRectangle3D(Point[] points, Vector[] normals) {
        super(points);
        this.normals = normals;
    }

    @Override
    protected RoundedRectangle3D build(Point[] points, XForm xForm) {
        return new RoundedRectangle3D(points, xForm.apply(normals));
    }

    public Vector getNormal(int index) {
        if (index < 0 || index >= points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normals[index * 2 / points.length];
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        int l = points.length;
        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        buffer.normal(normals[0]);
        for (int k = 0; k < l; k += 4) {
            buffer.vertex(points[k]);
            buffer.vertex(points[k + 1]);
        }
        buffer.vertex(points[0]);
        buffer.vertex(points[1]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        buffer.normal(normals[1]);
        for (int k = 0; k < l; k += 4) {
            buffer.vertex(points[k + 3]);
            buffer.vertex(points[k + 2]);
        }
        buffer.vertex(points[3]);
        buffer.vertex(points[2]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        buffer.normal(normals[0]);
        for (int k = 0; k < l; k += 4) {
            buffer.vertex(points[k + 2]);
            buffer.vertex(points[k]);
        }
        buffer.vertex(points[2]);
        buffer.vertex(points[0]);
        buffer.end();

        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        buffer.normal(normals[1]);
        for (int k = 0; k < l; k += 4) {
            buffer.vertex(points[k + 1]);
            buffer.vertex(points[k + 3]);
        }
        buffer.vertex(points[1]);
        buffer.vertex(points[3]);
        buffer.end();
    }

    private static Point[] buildPoints(double x, double y, double radius, double frame, double margin, DetailLevel detailLevel) {
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be greater than zero: " + radius);
        }
        if (frame * 0.5 >= radius) {
            throw new IllegalArgumentException("frame must be less than double the radius: " + frame);
        }
        double f = frame * 0.5;
        double a = x - margin * 2 - radius;
        double b = y - margin * 2 - radius;
        List<Point> points = new ArrayList<Point>((detailLevel.steps + 4) * 12);
        importPoints(new RoundedRectangle(a + f, b + f, radius + f, detailLevel),
                     new RoundedRectangle(a - f, b - f, radius - f, detailLevel),
                     Vector.Z.scale(frame),
                     Vector.Z.scale(-frame),
                     points);
        return points.toArray(new Point[points.size()]);
    }

    private static void importPoints(RoundedRectangle r1, RoundedRectangle r2, Vector xlate1, Vector xlate2, List<Point> points) {
        for (int i = 0, l = r1.points.length; i < l; i++) {
            points.add(r1.points[i].translate(xlate1));
            points.add(r2.points[i].translate(xlate1));
            points.add(r1.points[i].translate(xlate2));
            points.add(r2.points[i].translate(xlate2));
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        RoundedRectangle3D rect = new RoundedRectangle3D(3,3,0.125,0.025,0.25, DetailLevel.MEDIUM).rotate(Axis.X, 0.85);
        SingleShapeDemo demo = new SingleShapeDemo(rect, offWhite, SingleShapeDemo.Mode.WIREFRAME);
        Brush brush = new Brush(Brush.SELF_ILLUMINATED);
        brush.setCulling(Brush.PolygonSide.BACK);
        rect.setMaterial(Material.paint(Color.CYAN, brush));
        demo.start();
    }

}
