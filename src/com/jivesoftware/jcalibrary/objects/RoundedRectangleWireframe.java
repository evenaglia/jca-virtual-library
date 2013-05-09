package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.physical.geom.primitives.RoundedRectangle;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.demo.SingleShapeDemo;

/**
 * User: ed
 * Date: 4/22/13
 * Time: 10:36 PM
 */
public class RoundedRectangleWireframe extends AbstractShape<RoundedRectangleWireframe> {

    private final Vector normal;

    public RoundedRectangleWireframe(double x, double y, DetailLevel detailLevel) {
        this(x, y, 0.125, 0.25, detailLevel);
    }
    public RoundedRectangleWireframe(double x,
                                     double y,
                                     double radius,
                                     double margin,
                                     DetailLevel detailLevel) {
        this(buildPoints(x, y, radius, margin, detailLevel), Vector.Z);
    }

    public RoundedRectangleWireframe(Point[] points, Vector normal) {
        super(points);
        this.normal = normal;
    }

    @Override
    protected RoundedRectangleWireframe build(Point[] points, XForm xForm) {
        return new RoundedRectangleWireframe(points, xForm.apply(normal));
    }

    public Vector getNormal(int index) {
        if (index < 0 || index >= points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normal;
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.LINE_LOOP);
        buffer.normal(normal);
        for (Point point : points) {
            buffer.vertex(point);
        }
        buffer.end();
    }

    private static Point[] buildPoints(double x, double y, double radius, double margin, DetailLevel detailLevel) {
        if (radius <= 0) {
            throw new IllegalArgumentException("radius must be greater than zero: " + radius);
        }
        double a = x - margin * 2 - radius;
        double b = y - margin * 2 - radius;
        return new RoundedRectangle(a, b, radius, detailLevel).points;
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        RoundedRectangleWireframe rect = new RoundedRectangleWireframe(3,3,0.125,0.25, DetailLevel.MEDIUM).rotate(Axis.X, 0.85);
        SingleShapeDemo demo = new SingleShapeDemo(rect, offWhite, SingleShapeDemo.Mode.WIREFRAME);
        Brush brush = new Brush(Brush.NO_LIGHTING);
        brush.setCulling(Brush.PolygonSide.BACK);
        rect.setMaterial(Material.paint(Color.CYAN, brush));
        demo.start();
    }

}
