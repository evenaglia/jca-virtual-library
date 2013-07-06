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
public class BoxCursorWireframe extends AbstractShape<BoxCursorWireframe> {

    private static final Vector[] NORMALS = {
            Vector.X,
            Vector.Y,
            Vector.Z,
            Vector.X.reverse(),
            Vector.Y.reverse(),
            Vector.Z.reverse(),
    };

    private final Vector[] normals;

    public BoxCursorWireframe(double x, double y, double z, DetailLevel detailLevel) {
        this(x, y, z, 0.125, 0.25, detailLevel);
    }

    public BoxCursorWireframe(double x,
                              double y,
                              double z,
                              double radius,
                              double margin,
                              DetailLevel detailLevel) {
        this(buildPoints(x, y, z, radius, margin, detailLevel), NORMALS.clone());
    }

    private BoxCursorWireframe(Point[] points, Vector[] normals) {
        super(points);
        this.normals = normals;
    }

    @Override
    protected BoxCursorWireframe build(Point[] points, XForm xForm) {
        return new BoxCursorWireframe(points, xForm.apply(normals));
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
            buffer.start(GeometryBuffer.GeometrySequence.LINE_LOOP);
            buffer.normal(normals[i]);
            for (int k = 0; k < part; k++) {
                buffer.vertex(points[j + k]);
            }
            buffer.end();
        }
    }

    private static Point[] buildPoints(double x, double y, double z, double radius, double margin, DetailLevel detailLevel) {
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be greater than zero: " + radius);
        }
        double a = x - margin * 2 - radius;
        double b = y - margin * 2 - radius;
        double c = z - margin * 2 - radius;
        List<Point> points = new ArrayList<Point>((detailLevel.steps + 4) * 12);
        double rotate = Math.PI * 0.5;
        importPoints(new RoundedRectangle(a, b, radius, detailLevel),
                     Vector.Z.scale(z * 0.5),
                     points);
        importPoints(new RoundedRectangle(a, c, radius, detailLevel).rotate(Axis.X, rotate),
                     Vector.Y.scale(y * -0.5),
                     points);
        importPoints(new RoundedRectangle(b, c, radius, detailLevel).rotate(Axis.Y, rotate),
                     Vector.X.scale(x * 0.5),
                     points);
        return points.toArray(new Point[points.size()]);
    }

    private static void importPoints(RoundedRectangle rr, Vector xlate, List<Point> points) {
        for (int i = 0, l = rr.points.length; i < l; i++) {
            points.add(rr.points[i].translate(xlate));
        }
        rr = rr.flip();
        xlate = xlate.reverse();
        for (int i = 0, l = rr.points.length; i < l; i++) {
            points.add(rr.points[i].translate(xlate));
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        BoxCursorWireframe boxCursor = new BoxCursorWireframe(3,3,3,0.125,0.25, DetailLevel.MEDIUM).rotate(Axis.X, 0.85);
        SingleShapeDemo demo = new SingleShapeDemo(boxCursor, offWhite, SingleShapeDemo.Mode.SHADED);
        Brush brush = new Brush(Brush.SELF_ILLUMINATED);
        brush.setCulling(Brush.PolygonSide.BACK);
        boxCursor.setMaterial(Material.paint(Color.CYAN, brush));
        demo.start();
    }
}
