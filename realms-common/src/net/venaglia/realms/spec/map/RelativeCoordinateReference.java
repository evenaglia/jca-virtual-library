package net.venaglia.realms.spec.map;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.util.debug.OutputGraphProjection;
import net.venaglia.realms.common.map.world.AcreDetail;

import java.awt.geom.Point2D;

/**
 * User: ed
 * Date: 8/11/14
 * Time: 5:40 PM
 */
public class RelativeCoordinateReference implements OutputGraphProjection<GeoPoint> {

    private static final int DEPTH_LIMIT = 20;

    /**
     * Four corners:
     *
     *     a --------- b
     *     :           :
     *     :           :
     *     :           :
     *     :           :
     *     c --------- d
     */
    private final Point a, b, c, d; // four corners

    // for triangles
    public RelativeCoordinateReference(GeoPoint a, GeoPoint b, GeoPoint c) {
        this (c.toPoint(1000.0), null, b.toPoint(1000.0), a.toPoint(1000.0), null);
    }

    // for quads
    public RelativeCoordinateReference(GeoPoint a, GeoPoint b, GeoPoint c, GeoPoint d) {
        this(a.toPoint(1000.0), b.toPoint(1000.0), c.toPoint(1000.0), d.toPoint(1000.0), null);
    }

    // for quads
    public RelativeCoordinateReference(Point a, Point b, Point c, Point d) {
        this(normalize(a), normalize(b), normalize(c), normalize(d), null);
    }

    private RelativeCoordinateReference(Point a, Point b, Point c, Point d, Void v) {
        assert v == null; // prevents unused arg warning
        if (b == null) {
            // for triangle constructor
            Vector cd = Vector.betweenPoints(c, d);
            b = a.translate(cd.scale(0.5));
            a = a.translate(cd.scale(-0.5));
        }
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    private static Point normalize(Point p) {
        if (Point.ORIGIN.equals(p)) {
            throw new IllegalArgumentException("Can't normalize the origin (0,0,0) to a fixed radius");
        }
        double s = 1000.0 / Vector.computeDistance(p.x, p.y, p.z);
        return s == 1.0 ? p : new Point(p.x * s, p.y * s, p.z * s);
    }

    public Point2D resolve(GeoPoint point) {
        Point p = point.toPoint(1000.0);
        return new Point2D.Double(seek(p, a, b, c, d), seek(p, a, c, b, d));
    }

    public void project(GeoPoint point, double[] output, int offset) {
        Point p = point.toPoint(1000.0);
        project(p, output, offset);
    }

    public void project(Point p, double[] output, int offset) {
        output[offset] = seek(p, a, b, c, d);
        output[offset + 1] = seek(p, a, c, b, d);
    }

    public Point resolve(GeoPoint point, double evevation) {
        Point p = point.toPoint(1000.0);
        return new Point(seek(p, a, b, c, d), seek(p, a, c, b, d), evevation);
    }

    // passing 2.0 makes rendered points larger, 0.5 makes rendered points smaller
    public RelativeCoordinateReference scale(double scale) {
        if (scale == 1.0) {
            return this;
        }
        if (scale == 0.0 || Double.isNaN(scale) || Double.isInfinite(scale)) {
            throw new IllegalArgumentException();
        }
        double s = 1.0 / scale;
        Point m = new Point((a.x + b.x + c.x + d.x) * 0.25,
                            (a.y + b.y + c.y + d.y) * 0.25,
                            (a.z + b.z + c.z + d.z) * 0.25);
        return new RelativeCoordinateReference(
                m.translate(Vector.betweenPoints(m, a).scale(s)),
                m.translate(Vector.betweenPoints(m, b).scale(s)),
                m.translate(Vector.betweenPoints(m, c).scale(s)),
                m.translate(Vector.betweenPoints(m, d).scale(s))
        );
    }

    private Square findSquare(Point p, int limit) {
        int x = calculateSteps(p, a, b, c, d, limit);
        int y = calculateSteps(p, b, d, a, c, limit);
        Point ax = Point.midPoint(a, b, x);
        Point bx = Point.midPoint(a, b, x + 1);
        Point cx = Point.midPoint(c, d, x);
        Point dx = Point.midPoint(c, d, x + 1);
        Point a_ = Point.midPoint(ax, cx, y);
        Point b_ = Point.midPoint(bx, dx, y);
        Point c_ = Point.midPoint(ax, cx, y + 1);
        Point d_ = Point.midPoint(bx, dx, y + 1);
        return new Square(x, y, a_, b_, c_, d_);
    }

    private int calculateSteps(Point p, Point s0, Point s1, Point t0, Point t1, int limit) {
        double d0 = distance(p, s0, t0);
        double d1 = distance(p, s1, t1);
        double d_;
        int direction, next;
        if (d0 < d1) {
            direction = -1;
            next = -1;
        } else {
            direction = 1;
            next = 2;
            d_ = d0; d0 = d1; d1 = d_; // swap
        }
        for (int i = 0; i < limit; i++) {
            d_ = distance(p, Point.midPoint(s0, s1, next), Point.midPoint(t0, t1, next));
            if (d_ < d1) {
                d1 = d_ > d0 ? d_ : d0;
                d0 = d_ > d0 ? d0 : d_;
                next += direction;
            } else {
                return i * direction;
            }
        }
        return limit * direction;
    }

    private double seek(Point p, Point s0, Point s1, Point t0, Point t1) {
        double outlier = 1.0;
        double part0 = 0.0 - outlier;
        double part1 = 1.0 + outlier;
        double d0 = distance(p, Point.midPoint(s0, s1, part0), Point.midPoint(t0, t1, part0));
        double d1 = distance(p, Point.midPoint(s0, s1, part1), Point.midPoint(t0, t1, part1));
        return seek(p, s0, s1, t0, t1, d0, d1, part0, part1, DEPTH_LIMIT);
    }

    private double seek(Point p,
                        Point s0, Point s1,
                        Point t0, Point t1,
                        double d0, double d1,
                        double part0, double part1,
                        int depthLimit) {
        for (int d = 0; d < depthLimit; d++) {
            double part = (part0 + part1) * 0.5;
            Point sm = Point.midPoint(s0, s1, part);
            Point tm = Point.midPoint(t0, t1, part);
            double dm = distance(p, sm, tm);
            if (d0 < d1) {
                d1 = dm;
                part1 = part;
            } else {
                d0 = dm;
                part0 = part;
            }
        }
        return d0 < d1 ? part0 : part1;
    }

    private double distance(Point x0, Point x1, Point x2) {
        Vector denominator = Vector.betweenPoints(x1, x2);
        if (denominator.l == 0) {
            // calculate distance from point
            return Vector.computeDistance(x0, x1);
        }
        Vector numerator = denominator.cross(Vector.betweenPoints(x0, x1));
        return numerator.l / denominator.l;
    }

    public static RelativeCoordinateReference forAcre(Acre acre) {
        return forAcreImpl(acre.points);
    }

    public static RelativeCoordinateReference forAcre(AcreDetail acreDetail) {
        return forAcreImpl(acreDetail.getVertices());
    }

    private static RelativeCoordinateReference forAcreImpl(GeoPoint[] points) {
        // todo: this distorts the acre to be square, squash it by 86.6%
        // find the four corners of this acre
        Point a, b, c, d;
        Point t1, t2, b1, b2, p1, p2;
        if (points == null) {
            throw new NullPointerException("points");
        }
        switch (points.length) {
            case 5:
                b1 = points[0].toPoint(1000.0);
                b2 = points[1].toPoint(1000.0);
                t1 = points[3].toPoint(1000.0);
                // special case, these pentagonal acres are equilateral
                t2 = t1.translate(Vector.betweenPoints(b1, b2));
                p1 = points[2].toPoint(1000.0);
                p2 = points[4].toPoint(1000.0);
                break;
            case 6:
                b1 = points[0].toPoint(1000.0);
                b2 = points[1].toPoint(1000.0);
                t1 = points[3].toPoint(1000.0);
                t2 = points[4].toPoint(1000.0);
                p1 = points[2].toPoint(1000.0);
                p2 = points[5].toPoint(1000.0);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        a = findClosestPointOnLine(t1, t2, p1);
        b = findClosestPointOnLine(t1, t2, p2);
        c = findClosestPointOnLine(b1, b2, p1);
        d = findClosestPointOnLine(b1, b2, p2);
        return new RelativeCoordinateReference(
                Point.midPoint(a, c, -0.067),
                Point.midPoint(b, d, -0.067),
                Point.midPoint(c, a, -0.067),
                Point.midPoint(d, b, -0.067)
        );
    }

    private static Point findClosestPointOnLine(Point onLine_pointA, Point onLine_pointB, Point nearbyPoint) {
        Vector a = Vector.betweenPoints(onLine_pointA, nearbyPoint);
        Vector u = Vector.betweenPoints(onLine_pointA, onLine_pointB).normalize();
        return onLine_pointA.translate(u.scale(a.dot(u)));
    }

    private static class Square {

        public int x, y;
        public Point s0, s1, t0, t1;

        private Square(int x, int y, Point s0, Point s1, Point t0, Point t1) {
            this.x = x;
            this.y = y;
            this.s0 = s0;
            this.s1 = s1;
            this.t0 = t0;
            this.t1 = t1;
        }
    }

    // self-test methods

    private static boolean isClose(double a, double b) {
        if (a == b) {
            return true;
        }
        if (a == 0 || b == 0) {
            return false;
        }
        if (Double.isNaN(a) || Double.isNaN(b)) {
            return Double.isNaN(a) && Double.isNaN(b);
        }
        if (Double.isInfinite(a) || Double.isInfinite(b)) {
            return a > 0 == b > 0 && Double.isInfinite(a) && Double.isInfinite(b);
        }
        double ratio = a / b;
        return Math.abs(1.0 - Math.abs(ratio)) < 0.00001; // < 10 ppm
    }

    private static Point obfuscate(Point point, double scale) {
        return point.rotate(Axis.Z, 0.5).translate(new Vector(3, 4, 5)).rotate(Axis.Y, 1.5).scale(scale).rotate(Axis.X, 2.5);
    }

    public static void main(String[] args) {
        Point[] points = {
                new Point(0,0,0),
                new Point(1,0,0),
                new Point(0,1,0),
                new Point(1,1,0)
        };
        for (int i = 0; i < points.length; i++) {
            points[i] = obfuscate(points[i], 7.5);
        }

        // assert that this is still a square
        assert isClose(7.5, Vector.computeDistance(points[0], points[1]));
        assert isClose(7.5, Vector.computeDistance(points[0], points[2]));
        assert isClose(7.5, Vector.computeDistance(points[2], points[3]));
        assert isClose(7.5, Vector.computeDistance(points[1], points[3]));
        assert isClose(10.606601717798211, Vector.computeDistance(points[0], points[3]));
        assert isClose(10.606601717798211, Vector.computeDistance(points[1], points[2]));

        Point[] test = {
                new Point(0.5,0.5,0.0625),
                new Point(0.5,0.5,-0.0625),
                new Point(0.838,0.145,0),
                new Point(0.653,0.913,23),
                new Point(0.893,0.342,-2),
                new Point(0.223,0.777,-11),
        };
        double[] buffer = {0,0};
        RelativeCoordinateReference reference = new RelativeCoordinateReference(points[0], points[1], points[2], points[3], null);
        for (Point point : test) {
            Point p = obfuscate(point, 7.5);
            reference.project(p, buffer, 0);
            assert isClose(point.x, buffer[0]);
            assert isClose(point.y, buffer[1]);
        }
    }
}
