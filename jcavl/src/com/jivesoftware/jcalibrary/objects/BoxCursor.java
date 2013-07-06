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
 * Time: 5:23 PM
 */
public class BoxCursor extends AbstractShape<BoxCursor> {

    private static final Vector[] NORMALS = {
            Vector.X,
            Vector.Y,
            Vector.Z,
            Vector.X.reverse(),
            Vector.Y.reverse(),
            Vector.Z.reverse(),
    };

    private final Vector[] normals;

    public BoxCursor(double x, double y, double z, DetailLevel detailLevel) {
        this(x, y, z, 0.125, 0.025, 0.25, detailLevel);
    }

    public BoxCursor(double x, double y, double z, double radius, double frame, double margin, DetailLevel detailLevel) {
        this(buildPoints(x, y, z, radius, frame, margin, detailLevel), NORMALS.clone());
    }

    private BoxCursor(Point[] points, Vector[] normals) {
        super(points);
        this.normals = normals;
    }

    @Override
    protected BoxCursor build(Point[] points, XForm xForm) {
        return new BoxCursor(points, xForm.apply(normals));
    }

    public Vector getNormal(int index) {
        if (index < 0 && index >= points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        int part = index * 6 / points.length;
        return normals[part];
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        int part = points.length / 6;
        for (int i = 0, j = 0; i < 6; i++, j += part) {
            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            buffer.normal(normals[i]);
            for (int k = 0; k < part; k += 4) {
                buffer.vertex(points[j + k]);
                buffer.vertex(points[j + k + 1]);
            }
            buffer.vertex(points[j]);
            buffer.vertex(points[j + 1]);
            buffer.end();

            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            buffer.normal(normals[(i + 3) % 6]);
            for (int k = 0; k < part; k += 4) {
                buffer.vertex(points[j + k + 3]);
                buffer.vertex(points[j + k + 2]);
            }
            buffer.vertex(points[j + 3]);
            buffer.vertex(points[j + 2]);
            buffer.end();

            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            buffer.normal(normals[i]);
            for (int k = 0; k < part; k += 4) {
                buffer.vertex(points[j + k + 2]);
                buffer.vertex(points[j + k]);
            }
            buffer.vertex(points[j + 2]);
            buffer.vertex(points[j]);
            buffer.end();

            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            buffer.normal(normals[(i + 3) % 6]);
            for (int k = 0; k < part; k += 4) {
                buffer.vertex(points[j + k + 1]);
                buffer.vertex(points[j + k + 3]);
            }
            buffer.vertex(points[j + 1]);
            buffer.vertex(points[j + 3]);
            buffer.end();
        }
    }

    private static Point[] buildPoints(double x, double y, double z, double radius, double frame, double margin, DetailLevel detailLevel) {
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be greater than zero: " + radius);
        }
        if (frame * 0.5 >= radius) {
            throw new IllegalArgumentException("frame must be less than double the radius: " + frame);
        }
        double f = frame * 0.5;
        double a = x - margin * 2 - radius;
        double b = y - margin * 2 - radius;
        double c = z - margin * 2 - radius;
        List<Point> points = new ArrayList<Point>((detailLevel.steps + 4) * 12);
        double rotate = Math.PI * 0.5;
        importPoints(new RoundedRectangle(a + f, b + f, radius + f, detailLevel),
                     new RoundedRectangle(a - f, b - f, radius - f, detailLevel),
                     Vector.Z.scale(z * 0.5),
                     Vector.Z.scale(z * 0.5 - frame * 2),
                     points);
        importPoints(new RoundedRectangle(a + f, c + f, radius + f, detailLevel).rotate(Axis.X, rotate),
                     new RoundedRectangle(a - f, c - f, radius - f, detailLevel).rotate(Axis.X, rotate),
                     Vector.Y.scale(y * -0.5),
                     Vector.Y.scale(y * -0.5 - frame * 2),
                     points);
        importPoints(new RoundedRectangle(b + f, c + f, radius + f, detailLevel).rotate(Axis.Y, rotate),
                     new RoundedRectangle(b - f, c - f, radius - f, detailLevel).rotate(Axis.Y, rotate),
                     Vector.X.scale(x * 0.5),
                     Vector.X.scale(x * 0.5 - frame * 2),
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
        r1 = r1.flip();
        r2 = r2.flip();
        xlate1 = xlate1.reverse();
        xlate2 = xlate2.reverse();
        for (int i = 0, l = r1.points.length; i < l; i++) {
            points.add(r1.points[i].translate(xlate1));
            points.add(r2.points[i].translate(xlate1));
            points.add(r1.points[i].translate(xlate2));
            points.add(r2.points[i].translate(xlate2));
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        BoxCursor boxCursor = new BoxCursor(3,3,3,0.125,0.025,0.25, DetailLevel.MEDIUM).rotate(Axis.X, 0.85);
        SingleShapeDemo demo = new SingleShapeDemo(boxCursor, offWhite, SingleShapeDemo.Mode.SHADED);
        Brush brush = new Brush(Brush.SELF_ILLUMINATED);
        brush.setCulling(Brush.PolygonSide.BACK);
        boxCursor.setMaterial(Material.paint(Color.CYAN, brush));
        demo.start();
    }
}
