package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.projection.GeometryBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 8:26 PM
 */
public final class Sphere extends AbstractShape<Sphere> {

//    public enum DetailLevel {
//        VERY_COARSE, // use only for the smallest spheres, or those that are very far away
//        COARSE,
//        MEDIUM,
//        FINE,
//        VERY_FINE;
//
//        public int divisions() {
//            switch (this) {
//                case COARSE:
//                    return 12;
//                case MEDIUM:
//                    return 24;
//                case FINE:
//                    return 48;
//                case VERY_FINE:
//                    return 120;
//            }
//            return 8;
//        }
//    }

    private final DetailLevel detailLevel;
    private final Point center;

    public Sphere(DetailLevel detailLevel) {
        super(synthesizePoints(detailLevel.steps * 2));
        this.detailLevel = detailLevel;
        this.center = Point.ORIGIN;
    }

    private Sphere(Point[] points, DetailLevel detailLevel) {
        super(points);
        this.detailLevel = detailLevel;
        Point top = points[points.length - 1];
        Point bottom = points[points.length - 2];
        double x = top.x + bottom.x * 0.5;
        double y = top.y + bottom.y * 0.5;
        double z = top.z + bottom.z * 0.5;
        this.center = new Point(x,y,z);
    }

    @Override
    protected Sphere build(Point[] points, XForm xForm) {
        return new Sphere(assertLength(points, this.points.length), detailLevel);
    }

    @Override
    protected BoundingVolume<?> createBoundingVolume() {
        double maxRadius = 0.0;
        for (Point p : points) {
            maxRadius = Math.max(maxRadius,
                                 Vector.computeDistance(p.x - center.x,
                                                        p.y - center.y,
                                                        p.z - center.z));
        }
        return new BoundingSphere(center, maxRadius);
    }

    public Vector getNormal(int index) {
        return Vector.betweenPoints(center, points[index]);
    }

    private List<Point> pointSequence(int[] indices) {
        List<Point> seq = new ArrayList<Point>(indices.length);
        for (int indice : indices) {
            seq.add(points[indice]);
        }
        return seq;
    }

    public void project(GeometryBuffer buffer) {
        project(buffer, GeometryBuffer.GeometrySequence.TRIANGLE_FAN, synthesizePolarCapTriangleFan(true));
        project(buffer, GeometryBuffer.GeometrySequence.TRIANGLE_FAN, synthesizePolarCapTriangleFan(false));
        int divisions = detailLevel.steps * 2;
        for (int i = 0; i < divisions; i++) {
            project(buffer, GeometryBuffer.GeometrySequence.QUAD_STRIP, synthesizeLongitudeQuadStrip(i));
        }
    }

    private void project(GeometryBuffer buffer, GeometryBuffer.GeometrySequence seq, int[] indices) {
        buffer.start(seq);
        for (int i : indices) {
            buffer.normal(getNormal(i));
            buffer.vertex(points[i]);
        }
        buffer.end();
    }

    private int[] synthesizeLongitudeQuadStrip(int division) {
        int divisions = detailLevel.steps * 2;
        int l = divisions / 2 - 1;
        int i = division * l;
        int j = (i + l) % (divisions * l);
        int[] indices = new int[l * 2];
        for (int b = 0, c = 0; b < l; b++, c += 2) {
            indices[c] = i + b;
            indices[c + 1] = j + b;
        }
        return indices;
    }

    private int[] synthesizePolarCapTriangleFan(boolean top) {
        int divisions = detailLevel.steps * 2;
        int l = divisions / 2 - 1;
        int i = top ? 0 : l - 1;
        int[] indices = new int[divisions + 2];
        indices[0] = points.length - (top ? 2 : 1);
        for (int a = 0; a < divisions; a++) {
            indices[a + 1] = top ? l * (divisions - a) - l + i : l * a + i;
        }
        indices[indices.length - 1] = indices[1];
        return indices;
    }

    private static Point[] synthesizePoints(int divisions) {
        double a = Math.PI * 2 / divisions;
        int k = 0, l = divisions / 2 - 1;
        int total = divisions * l + 2;
        Point[] points = new Point[total];
        for (int i = 0; i < divisions; i++) {
            double b = a * i, x = Math.sin(b), y = Math.cos(b);
            for (int j = 1; j <= l; j++) {
                double r = Math.sin(a * j), z = Math.cos(a * j);
                points[k++] = new Point(x * r, y * r, z);
            }
        }
        points[k++] = new Point(0.0,0.0,1.0);
        points[k] = new Point(0.0,0.0,-1.0);
        return points;
    }

    @Override
    public String toString() {
        double r = Vector.betweenPoints(center, points[points.length - 1]).l;
        return String.format("sphere[%.2f,%.2f,%.2f],r[%.2f]", center.x, center.y, center.z, r);
    }
}
