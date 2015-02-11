package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.gloo.demo.SingleShapeDemo;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.CompositeShape;
import net.venaglia.gloo.physical.geom.FlippableShape;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.geom.complex.GeodesicSphere;
import net.venaglia.gloo.projection.GeometryBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 1/20/15
 * Time: 8:42 AM
 */
public class TectonicVectorArrow extends AbstractShape<TectonicVectorArrow> implements FlippableShape<TectonicVectorArrow> {

    private static final Material RED = Material.makeWireFrame(Color.RED);
    private static final Material GREEN = Material.makeWireFrame(Color.GREEN);
    private static final Material BLUE = Material.makeWireFrame(Color.BLUE);

    private Vector[] normals;

    private TectonicVectorArrow(Point[] points,
                                Vector[] normals) {
        super(points);
        assert points.length == 43 || points.length == 47;
        assert normals.length == 22;
        this.normals = normals;
    }

    @Override
    protected TectonicVectorArrow build(Point[] points, XForm xForm) {
        return new TectonicVectorArrow(points, xForm.apply(normals));
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_STRIP);
        for (int i = 0; i < 43; i++) {
            Point point = points[i];
            buffer.normal(getNormal(i));
            buffer.vertex(point);
        }
        buffer.end();

        if (points.length > 43) {
            buffer.start(GeometryBuffer.GeometrySequence.LINES);
            RED.apply(0L, buffer);
            buffer.vertex(points[43]);
            buffer.vertex(points[44]);
            GREEN.apply(0L, buffer);
            buffer.vertex(points[43]);
            buffer.vertex(points[45]);
            BLUE.apply(0L, buffer);
            buffer.vertex(points[43]);
            buffer.vertex(points[46]);
            buffer.end();
        }
    }

    @Override
    public Vector getNormal(int index) {
        return normals[index >> 1];
    }

    @Override
    public TectonicVectorArrow flip() {
        Point[] points = this.points.clone();
        Vector[] normals = this.normals.clone();
        for (int i = 0; i < 43; i += 2) {
            Point point = points[i];
            points[i] = points[i + 1];
            points[i + 1] = point;
        }
        for (int i = 0; i < normals.length; i++) {
            normals[i] = normals[i].scale(-1.0);
        }
        return new TectonicVectorArrow(points, normals);
    }

    public static TectonicVectorArrow createArrow(TectonicPoint tectonicPoint) {
        double scale = 0.015625;
        Vector zAxis = Vector.betweenPoints(Point.ORIGIN, tectonicPoint.point).normalize(scale);
        Vector xAxis = tectonicPoint.vector.normalize(scale);
        Vector yAxis = zAxis.cross(xAxis).normalize(scale);
        xAxis = zAxis.cross(yAxis).normalize(scale);
        List<Point> points = new ArrayList<>(47);
        List<Vector> normals = new ArrayList<>(22);
        double z;
        for (int i = -10; i <= 3; i++) {
            z = 10.25 - computeCosineFromSine(i * scale, 10.25);
            points.add(fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, i, -3, z));
            points.add(fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, i, 3, z));
            Point midPoint = fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, i, 0, z);
            normals.add(Vector.betweenPoints(Point.ORIGIN, midPoint).normalize());
        }
        for (int i = 3, j = 7; i < 10; i++, j--) {
            z = 10.25 - computeCosineFromSine(i * scale, 10.25);
            points.add(fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, i, -j, z));
            points.add(fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, i, j, z));
            Point midPoint = fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, i, 0, z);
            normals.add(Vector.betweenPoints(Point.ORIGIN, midPoint).normalize());
        }
        z = 10.25 - computeCosineFromSine(10 * scale, 10.25);
        points.add(fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, 10, 0, z));
        Point midPoint = fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, 10, 0, z);
        normals.add(Vector.betweenPoints(Point.ORIGIN, midPoint).normalize());
        // save points to render local coordinate axes, for debugging
//        Point arrowCenter = fromLocalXY(tectonicPoint.point, xAxis, yAxis, zAxis, 0, 0, 0);
//        points.add(arrowCenter);
//        points.add(arrowCenter.translate(xAxis));
//        points.add(arrowCenter.translate(yAxis));
//        points.add(arrowCenter.translate(zAxis));
        return new TectonicVectorArrow(points.toArray(new Point[points.size()]),
                                       normals.toArray(new Vector[normals.size()]));
    }

    private static double computeCosineFromSine(double sine, double radius) {
        double sin = sine / radius;
        return Math.sqrt(1.0 - sin * sin) * radius;
    }

    private static Point fromLocalXY(Point zero, Vector xAxis, Vector yAxis, Vector zAxis,
                                     double x, double y, double z) {
        Vector vector = new Vector(x, y, z).rotate(xAxis, yAxis, zAxis);
        double i = zero.x + vector.i;
        double j = zero.y + vector.j;
        double k = zero.z + vector.k;
        double l = 1.03125 / Vector.computeDistance(i, j, k);
        return new Point(i * l, j * l, k * l);
    }

    public static void main(String[] args) {
        double sq3 = Math.sqrt(3);
        TectonicPoint tectonicPoint = new TectonicPoint(new Point(sq3, sq3, sq3), Vector.X, 0, TectonicPoint.PointClass.OTHER,
                                                        TectonicPoint.Source.OTHER);
        TectonicVectorArrow arrow1 = createArrow(tectonicPoint).setMaterial(Material.makeSelfIlluminating(Color.GREEN));
        Shape<?> shape = new CompositeShape(new GeodesicSphere(8).setMaterial(Material.makePoints(Color.GRAY_75)), arrow1);
        new SingleShapeDemo(shape, Color.GREEN, SingleShapeDemo.Mode.WIREFRAME).start();
    }
}
