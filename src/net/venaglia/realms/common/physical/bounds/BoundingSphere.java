package net.venaglia.realms.common.physical.bounds;

import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.complex.GeodesicSphere;
import net.venaglia.realms.common.util.matrix.Matrix_4x4;

import java.util.Random;

/**
 * User: ed
 * Date: 9/9/12
 * Time: 11:04 AM
 */
public class BoundingSphere extends AbstractBoundingVolume<BoundingSphere> {

    public static final BoundingSphere NULL = new SpecialBoundingSphere(true);
    public static final BoundingSphere INFINITE = new SpecialBoundingSphere(false);

    public final Point center;
    public final double radius;

    private BoundingBox box;

    public BoundingSphere(Point a, Point b, Point c, Point d) {
        this(fourPoints(a, b, c, d));
    }

    private BoundingSphere(Object[] centerAndRadius) {
        this((Point)centerAndRadius[0], (Double)centerAndRadius[1]);
    }

    public BoundingSphere(Point center, Point radius) {
        this(center, Vector.computeDistance(radius.x - center.x, radius.y - center.y, radius.z - center.z));
    }

    public BoundingSphere(Point center, double radius) {
        super(Type.SPHERE);
        if (center == null) {
            throw new NullPointerException("center");
        }
        this.center = center;
        this.radius = Math.abs(radius);
    }

    public BoundingSphere(BoundingBox box) {
        super(Type.BOX);
        if (box == null) {
            throw new NullPointerException("box");
        }
        this.center = new Point((box.corner1.x + box.corner2.x) * 0.5,
                                (box.corner1.y + box.corner2.y) * 0.5,
                                (box.corner1.z + box.corner2.z) * 0.5);
        this.radius = Vector.computeDistance((box.corner1.x - box.corner2.x),
                                             (box.corner1.y - box.corner2.y),
                                             (box.corner1.z - box.corner2.z)) * 0.5;
        this.box = box;
    }

    private BoundingSphere(double radius) {
        super(Type.SPHERE);
        this.center = Point.ORIGIN;
        this.radius = radius;
    }

    public BoundingSphere asSphere() {
        return this;
    }

    public BoundingBox asBox() {
        if (box == null) {
            box = new BoundingBox(this);
        }
        return box;
    }

    public Shape<?> asShape(float minAccuracy) {
        if (minAccuracy < 0 || minAccuracy >= 1.0f) {
            throw new IllegalArgumentException("minAccuracy must be at least zero, and less than one: " + minAccuracy);
        }
        int divisions = 0;
        float accuracy = 0.63662f;
        while (accuracy < minAccuracy) {
            divisions++;
            int n = (divisions + 1) * 4;
            double area = n * Math.sin(Math.PI * 2 / n) * 0.5;
            accuracy = 1.0f - (float)((Math.PI - area) / Math.PI);
        }
        return new GeodesicSphere(divisions);
    }

    public double min(Axis axis) {
        return center().ofAxis(axis) - radius;
    }

    public double max(Axis axis) {
        return center().ofAxis(axis) + radius;
    }

    public boolean includes(Point point) {
        return distanceToBoundary(point) <= 0.0;
    }

    public boolean includes(double x, double y, double z) {
        return distanceToBoundary(x,y,z) <= 0.0;
    }

    public boolean intersects(double v, Axis axis) {
        double c = center.ofAxis(axis);
        return between(c - radius, v, c + radius);
    }

    public boolean intersects(BoundingVolume<?> bounds) {
        return getBestFit() == Type.SPHERE
               ? (bounds.getBestFit() == Type.SPHERE
                  ? intersects(bounds.asSphere())
                  : intersects(bounds.asBox()))
               : asBox().intersects(bounds);
    }

    private boolean intersects(BoundingSphere boundingSphere) {
        return distanceToBoundary(boundingSphere.center) <= boundingSphere.radius;
    }

    private boolean intersects(BoundingBox boundingBox) {
        double i = center.x - closest(boundingBox.corner1.x, boundingBox.corner2.x, center.x);
        double j = center.y - closest(boundingBox.corner1.y, boundingBox.corner2.y, center.y);
        double k = center.z - closest(boundingBox.corner1.z, boundingBox.corner2.z, center.z);
        return Vector.computeDistance(i, j, k) <= radius;
    }

    private double closest(double min, double max, double value) {
        return Math.max(Math.min(value, max), min);
    }

    public boolean envelops(BoundingVolume<?> bounds) {
        return getBestFit() == Type.SPHERE
               ? (bounds.getBestFit() == Type.SPHERE
                  ? envelops(bounds.asSphere())
                  : envelops(bounds.asBox()))
               : asBox().envelops(bounds);
    }

    public boolean isInfinite() {
        return radius == Double.POSITIVE_INFINITY;
    }

    public boolean isNull() {
        return radius == 0.0;
    }

    private boolean envelops(BoundingSphere boundingSphere) {
        double d = 0.0 - distanceToBoundary(boundingSphere.center);
        return d >= 0.0 && d <= boundingSphere.radius;
    }

    private boolean envelops(BoundingBox boundingBox) {
        double x = Math.max(Math.abs(boundingBox.corner1.x - center.x), Math.abs(boundingBox.corner2.x - center.x));
        double y = Math.max(Math.abs(boundingBox.corner1.y - center.y), Math.abs(boundingBox.corner2.y - center.y));
        double z = Math.max(Math.abs(boundingBox.corner1.z - center.z), Math.abs(boundingBox.corner2.z - center.z));
        return x * x + y * y + z * z <= radius;
    }

    private double distanceToBoundary (Point point) {
        return distanceToBoundary(point.x, point.y, point.z);
    }

    private double distanceToBoundary (double x, double y, double z) {
        return Vector.computeDistance(center.x - x, center.y - y, center.z - z) - radius;
    }

    public Point center() {
        return center;
    }

    public double getLongestDimension() {
        return radius * 2.0;
    }

    public double volume() {
        double c = 4.0 * Math.PI / 3.0;
        return c * radius * radius * radius; // 4/3 * pi * r^3
    }

    public BoundingSphere scale(double magnitude) {
        if (radius == 0.0) {
            return NULL;
        }
        if (radius == Double.POSITIVE_INFINITY) {
            return INFINITE;
        }
        if (getBestFit() == Type.BOX) {
            return new BoundingSphere(box.scale(magnitude));
        }
        return new BoundingSphere(center.scale(magnitude), radius * magnitude);
    }

    public BoundingSphere scale(Vector magnitude) {
        if (radius == 0.0) {
            return NULL;
        }
        if (radius == Double.POSITIVE_INFINITY) {
            return INFINITE;
        }
        if (getBestFit() == Type.BOX) {
            return new BoundingSphere(box.scale(magnitude));
        }
        return new BoundingSphere(center.scale(magnitude), radius * magnitude.l);
    }

    public BoundingSphere translate(Vector magnitude) {
        if (radius == 0.0) {
            return NULL;
        }
        if (radius == Double.POSITIVE_INFINITY) {
            return INFINITE;
        }
        if (getBestFit() == Type.BOX) {
            return new BoundingSphere(box.translate(magnitude));
        }
        return new BoundingSphere(center.translate(magnitude), radius);
    }

    public BoundingSphere rotate(Vector x, Vector y, Vector z) {
        final boolean symmetric = x.l == y.l && y.l == z.l;
        if (symmetric) {
            if (x.l == 0) {
                return NULL;
            }
            if (x.l == 1) {
                return this;
            }
            if (x.l == Double.POSITIVE_INFINITY) {
                return INFINITE;
            }
            return new BoundingSphere(center.rotate(x, y, z), x.l);
        }
        throw new UnsupportedOperationException("Asymmetric scaling of a BoundingSphere is not supported.");
    }

    public BoundingSphere rotate(Axis axis, double angle) {
        if (angle == 0) {
            return this;
        }
        return new BoundingSphere(center.rotate(axis, angle), radius);
    }

    public BoundingSphere transform(XForm xForm) {
        if (!xForm.isSymmetric()) {
                throw new UnsupportedOperationException("The bounding sphere does not support arbitrary transformation, it must be symmetric");
        }
        return new BoundingSphere(xForm.apply(center), radius);
    }

    public BoundingSphere copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoundingSphere that = (BoundingSphere)o;

        if (Double.compare(that.radius, radius) != 0) return false;
        if (!center.equals(that.center)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = center.hashCode();
        temp = radius != +0.0d ? Double.doubleToLongBits(radius) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("boundingSphere[%.2f,%.2f,%.2f,r=%.2f]", center.x, center.y, center.z, radius);
    }

    private static class SpecialBoundingSphere extends BoundingSphere {

        private final boolean isNull;

        private SpecialBoundingSphere(boolean isNull) {
            super(isNull ? 0.0 : Double.POSITIVE_INFINITY);
            this.isNull = isNull;
        }

        @Override
        public BoundingBox asBox() {
            return isNull ? BoundingBox.NULL : BoundingBox.INFINITE;
        }

        @Override
        public double min(Axis axis) {
            return Double.NEGATIVE_INFINITY;
        }

        @Override
        public double max(Axis axis) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public double getLongestDimension() {
            return isNull ? 0 : Double.POSITIVE_INFINITY;
        }

        @Override
        public double volume() {
            return isNull ? 0 : Double.POSITIVE_INFINITY;
        }

        @Override
        public Shape<?> asShape(float minAccuracy) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean includes(Point point) {
            return !isNull;
        }

        @Override
        public boolean intersects(double v, Axis axis) {
            return !isNull;
        }

        @Override
        public boolean intersects(BoundingVolume<?> bounds) {
            return !isNull;
        }

        @Override
        public boolean envelops(BoundingVolume<?> bounds) {
            return !isNull;
        }

        @Override
        public BoundingSphere scale(double magnitude) {
            return this;
        }

        @Override
        public BoundingSphere scale(Vector magnitude) {
            return this;
        }

        @Override
        public BoundingSphere translate(Vector magnitude) {
            return this;
        }
    }

    private static Object[] fourPoints(Point p0, Point p1, Point p2, Point p3) {
        Matrix_4x4 a = new Matrix_4x4();

        /* Find determinant M11 */
        double _m11 = Matrix_4x4.determinant(p0.x, p1.x, p2.x, p3.x,
                                             p0.y, p1.y, p2.y, p3.y,
                                             p0.z, p1.z, p2.z, p3.z,
                                             1.0, 1.0, 1.0, 1.0);

        double l0 = p0.x * p0.x + p0.y * p0.y + p0.z * p0.z;
        double l1 = p1.x * p1.x + p1.y * p1.y + p1.z * p1.z;
        double l2 = p2.x * p2.x + p2.y * p2.y + p2.z * p2.z;
        double l3 = p3.x * p3.x + p3.y * p3.y + p3.z * p3.z;

        if (Math.abs(_m11 / Math.sqrt(l0 + l1 + l2 + l3)) < 0.000001f) {
            String msg = String.format("Cannot derive a sphere, 4 points are co-planar or 3 points are co-linear:\n" +
                                       "p0[%.2f,%.2f,%.2f],p1[%.2f,%.2f,%.2f],p2[%.2f,%.2f,%.2f],p=[%.2f,%.2f,%.2f]",
                                       p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z);
            throw new IllegalArgumentException(msg);
        }

        /* Find determinant M12 */
        double _m12 = Matrix_4x4.determinant(l0, l1, l2, l3,
                                             p0.y, p1.y, p2.y, p3.y,
                                             p0.z, p1.z, p2.z, p3.z,
                                             1.0, 1.0, 1.0, 1.0);

        /* Find determinant M13 */
        double _m13 = Matrix_4x4.determinant(p0.x, p1.x, p2.x, p3.x,
                                             l0, l1, l2, l3,
                                             p0.z, p1.z, p2.z, p3.z,
                                             1.0, 1.0, 1.0, 1.0);

        /* Find determinant M14 */
        double _m14 = Matrix_4x4.determinant(p0.x, p1.x, p2.x, p3.x,
                                             p0.y, p1.y, p2.y, p3.y,
                                             l0, l1, l2, l3,
                                             1.0, 1.0, 1.0, 1.0);

        /* Find determinant M15 */
        double _m15 = Matrix_4x4.determinant(l0, l1, l2, l3,
                                             p0.x, p1.x, p2.x, p3.x,
                                             p0.y, p1.y, p2.y, p3.y,
                                             p0.z, p1.z, p2.z, p3.z);

        _m11 = 1.0 / _m11;
        double scale = 0.5 * _m11;
        Point c = new Point(_m12 * scale, _m13 * scale, _m14 * scale);
        double r = Math.sqrt(c.x * c.x + c.y * c.y + c.z * c.z - _m15 * _m11);
        return new Object[]{c,r};
    }

    private static void assertCloseEnough(double expected, double actual) {
        if (Math.abs((expected - actual) / actual) > 0.005) {
            throw new AssertionError();
        }
    }

    private static void assertCloseEnough(Point expected, Point actual) {
        assertCloseEnough(expected.x, actual.x);
        assertCloseEnough(expected.y, actual.y);
        assertCloseEnough(expected.z, actual.z);
    }

    private static Vector toVector(Point a, Point b) {
        return new Vector(b.x - a.x, b.y - a.y, b.z - a.z);
    }

    private static void assertRadius(double expected, Point actualCenter, Point[] test) {
        assertCloseEnough(expected, toVector(actualCenter, test[0]).l);
        assertCloseEnough(expected, toVector(actualCenter, test[1]).l);
        assertCloseEnough(expected, toVector(actualCenter, test[2]).l);
        assertCloseEnough(expected, toVector(actualCenter, test[3]).l);
    }

    private static boolean test(boolean showSphere, double[] rand) {
        int randIndex = 0;
        Point c = new Point(rand[randIndex++], rand[randIndex++], rand[randIndex++]);
        double r = Math.abs(rand[randIndex++] / 10.0);
        Point[] test = new Point[4];
        for (int i = 0; i < 4; i++) {
            test[i] = c.translate(new Vector(rand[randIndex++], rand[randIndex++], rand[randIndex++]).normalize(r));
        }

        if (showSphere) System.out.printf(
                "   Points: a[%.2f,%.2f,%.2f],b[%.2f,%.2f,%.2f],c[%.2f,%.2f,%.2f],d[%.2f,%.2f,%.2f]\n",
                doubleArray(rand, 4, 16));
        BoundingSphere expected = new BoundingSphere(c, r);
        if (showSphere) System.out.println("Expecting: " + expected);
        BoundingSphere actual = null;
        try {
            actual = new BoundingSphere(test[0], test[1], test[2], test[3]);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (showSphere) System.out.println("   Actual: " + actual);

        assertCloseEnough(r, actual.radius);
        assertCloseEnough(c, actual.center);
        assertRadius(r, actual.center, test);

        // co-planar
        test[3] = test[1].translate(toVector(test[0], test[2]));
        try {
            new BoundingSphere(test[0], test[1], test[2], test[3]);
            throw new AssertionError();
        } catch (RuntimeException e) {
            // expected
        }

        // co-linear
        test[3] = test[1].translate(toVector(test[0], test[1]));
        try {
            new BoundingSphere(test[0], test[1], test[2], test[3]);
            throw new AssertionError();
        } catch (RuntimeException e) {
            // expected
        }
        return true;
    }

    private static Object[] doubleArray(double[] rand, int start, int end) {
        Object[] objects = new Object[end - start];
        for (int i = 0, j = start; j < end; i++, j++) {
            objects[i] = rand[j];
        }
        return objects;
    }

    public static void main(String[] args) {
//        new BoundingSphere(Point.ORIGIN, 1).asProjectable(0.95f);
        Random r = new Random();
        double[] rand = new double[16];
        for (int i = 0, l = 1000000; i < l; i++) {
            for (int j = 0; j < rand.length; j++) {
                rand[j] = r.nextGaussian() * 100.0 + 10000.0;
            }
            try {
                boolean valid = test(false, rand);
                if (!valid) i--;
            } catch (Throwable e) {
                System.err.println("Fail on loop " + i + " of " + l);
                test(true, rand);
            }
        }
    }
}
