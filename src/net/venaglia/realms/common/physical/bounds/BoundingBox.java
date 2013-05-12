package net.venaglia.realms.common.physical.bounds;

import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.Projectable;

/**
 * User: ed
 * Date: 9/10/12
 * Time: 8:18 PM
 */
public class BoundingBox extends AbstractBoundingVolume<BoundingBox> {

    public static final BoundingBox NULL = new SpecialBoundingBox(true);
    public static final BoundingBox INFINITE = new SpecialBoundingBox(false);

    public final Point corner1;
    public final Point corner2;

    private BoundingSphere sphere;
    private Point center;

    public BoundingBox(BoundingSphere sphere) {
        super(Type.SPHERE);
        if (sphere == null) {
            throw new NullPointerException("sphere");
        }
        Point c = sphere.center;
        double r = sphere.radius;
        this.corner1 = new Point(c.x - r, c.y - r, c.z - r);
        this.corner2 = new Point(c.x + r, c.y + r, c.z + r);
        this.sphere = sphere;
    }

    public BoundingBox(Point corner1, Point corner2) {
        super(Type.BOX);
        this.corner1 = new Point(Math.min(corner1.x, corner2.x),
                                 Math.min(corner1.y, corner2.y),
                                 Math.min(corner1.z, corner2.z));
        this.corner2 = new Point(Math.max(corner1.x, corner2.x),
                                 Math.max(corner1.y, corner2.y),
                                 Math.max(corner1.z, corner2.z));
        if (isUndefined() || isInfinite() || isNull()) {
            throw new IllegalArgumentException();
        }
    }

    public BoundingBox(Point... points) {
        super(Type.BOX);
        if (points.length < 2) {
            throw new IllegalArgumentException("Must specify more than two points");
        }
        double minX = points[0].x;
        double maxX = points[0].x;
        double minY = points[0].y;
        double maxY = points[0].y;
        double minZ = points[0].z;
        double maxZ = points[0].z;
        for (Point point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
            minZ = Math.min(minZ, point.z);
            maxZ = Math.max(maxZ, point.z);
        }
        this.corner1 = new Point(minX, minY, minZ);
        this.corner2 = new Point(maxX, maxY, maxZ);
        if (isUndefined() || isInfinite() || isNull()) {
            throw new IllegalArgumentException();
        }
    }

    public BoundingBox(BoundingBox... boxes) {
        this(toPoints(boxes));
    }

    private static Point[] toPoints(BoundingBox[] boxes) {
        Point[] points = new Point[boxes.length * 2];
        int k = 0;
        for (BoundingBox box : boxes) {
            points[k++] = box.corner1;
            points[k++] = box.corner2;
        }
        return points;
    }

    private BoundingBox(double size) {
        super(Type.BOX);
        this.corner1 = size == 0.0
                       ? Point.ORIGIN
                       : new Point(Double.NEGATIVE_INFINITY,
                                   Double.NEGATIVE_INFINITY,
                                   Double.NEGATIVE_INFINITY);
        this.corner2 = size == 0.0
                       ? Point.ORIGIN
                       : new Point(Double.POSITIVE_INFINITY,
                                   Double.POSITIVE_INFINITY,
                                   Double.POSITIVE_INFINITY);
        this.center = Point.ORIGIN;
    }

    public Point center() {
        if (center == null) {
            center = new Point((corner1.x + corner2.x) * 0.5,
                               (corner1.y + corner2.y) * 0.5,
                               (corner1.z + corner2.z) * 0.5);
        }
        return center;
    }

    public BoundingSphere asSphere() {
        if (sphere == null) {
            sphere = new BoundingSphere(this);
        }
        return sphere;
    }

    public BoundingBox asBox() {
        return this;
    }

    public Shape<?> asShape(float minAccuracy) {
        return new Box(this);
    }

    public double min(Axis axis) {
        return corner1.ofAxis(axis);
    }

    public double max(Axis axis) {
        return corner2.ofAxis(axis);
    }

    public boolean includes(Point point) {
        return getBestFit() == Type.BOX
               ? includes(point.x, point.y, point.z)
               : asSphere().includes(point);
    }

    public boolean includes(double x, double y, double z) {
        return corner1.x <= x && corner2.x >= x &&
               corner1.y <= y && corner2.y >= y &&
               corner1.z <= z && corner2.z >= z;
    }

    public boolean intersects(double v, Axis axis) {
        return between(corner1.ofAxis(axis), v, corner2.ofAxis(axis));
    }

    public boolean intersects(BoundingVolume<?> bounds) {
        return getBestFit() == Type.BOX
               ? (bounds.getBestFit() == Type.BOX
                  ? intersects(bounds.asBox())
                  : intersects(bounds.asSphere()))
               : asSphere().intersects(bounds);
    }

    private boolean intersects(BoundingBox boundingBox) {
        return boundingBox.corner1.x <= corner2.x && boundingBox.corner2.x >= corner1.x &&
               boundingBox.corner1.y <= corner2.x && boundingBox.corner2.y >= corner1.y &&
               boundingBox.corner1.z <= corner2.x && boundingBox.corner2.z >= corner1.z;
    }

    private boolean intersects(BoundingSphere boundingSphere) {
        double i = boundingSphere.center.x - closest(corner1.x, corner2.x, boundingSphere.center.x);
        double j = boundingSphere.center.y - closest(corner1.y, corner2.y, boundingSphere.center.y);
        double k = boundingSphere.center.z - closest(corner1.z, corner2.z, boundingSphere.center.z);
        return Vector.computeDistance(i, j, k) <= boundingSphere.radius;
    }

    private double closest(double min, double max, double value) {
        return Math.max(Math.min(value, max), min);
    }

    public boolean envelops(BoundingVolume<?> bounds) {
        return getBestFit() == Type.BOX
               ? (bounds.getBestFit() == Type.BOX
                  ? envelops(bounds.asBox())
                  : envelops(bounds.asSphere()))
               : asSphere().envelops(bounds);
    }

    private boolean envelops(BoundingBox boundingBox) {
        return boundingBox.corner1.x >= corner1.x && boundingBox.corner2.x <= corner2.x &&
               boundingBox.corner1.y >= corner1.x && boundingBox.corner2.y <= corner2.y &&
               boundingBox.corner1.z >= corner1.x && boundingBox.corner2.z <= corner2.z;
    }

    private boolean envelops(BoundingSphere boundingSphere) {
        return corner1.x <= boundingSphere.center.x - boundingSphere.radius &&
               corner2.x >= boundingSphere.center.x + boundingSphere.radius &&
               corner1.y <= boundingSphere.center.y - boundingSphere.radius &&
               corner2.y >= boundingSphere.center.y + boundingSphere.radius &&
               corner1.z <= boundingSphere.center.z - boundingSphere.radius &&
               corner2.z >= boundingSphere.center.z + boundingSphere.radius;
    }

    private boolean isUndefined() {
        return Double.isNaN(corner1.x) ||
               Double.isNaN(corner1.y) ||
               Double.isNaN(corner1.z) ||
               Double.isNaN(corner2.x) ||
               Double.isNaN(corner2.y) ||
               Double.isNaN(corner2.z);
    }

    public boolean isInfinite() {
        return corner1.x == Double.NEGATIVE_INFINITY &&
               corner1.y == Double.NEGATIVE_INFINITY &&
               corner1.z == Double.NEGATIVE_INFINITY &&
               corner2.x == Double.POSITIVE_INFINITY &&
               corner2.y == Double.POSITIVE_INFINITY &&
               corner2.z == Double.POSITIVE_INFINITY;
    }

    public boolean isNull() {
        return corner1.x >= corner2.x &&
               corner1.y >= corner2.y &&
               corner1.z >= corner2.z;
    }

    public double getLongestDimension() {
        return Vector.computeDistance(corner2.x - corner1.x, corner2.y - corner1.y, corner2.z - corner1.z);
    }

    public double volume() {
        return (corner2.x - corner1.x) * (corner2.y - corner1.y) * (corner2.z - corner1.z);
    }

    public BoundingBox scale(double magnitude) {
        if (getBestFit() == Type.SPHERE) {
            return new BoundingBox(sphere.scale(magnitude));
        }
        return new BoundingBox(corner1.scale(magnitude), corner2.scale(magnitude));
    }

    public BoundingBox scale(Vector magnitude) {
        if (getBestFit() == Type.SPHERE) {
            return new BoundingBox(sphere.scale(magnitude));
        }
        return new BoundingBox(corner1.scale(magnitude), corner2.scale(magnitude));
    }

    public BoundingBox translate(Vector magnitude) {
        if (getBestFit() == Type.SPHERE) {
            return new BoundingBox(sphere.translate(magnitude));
        }
        return new BoundingBox(corner1.translate(magnitude), corner2.translate(magnitude));
    }

    public BoundingBox rotate(Vector x, Vector y, Vector z) {
        throw new UnsupportedOperationException("The bounding box cannot be rotated, it must be orthogonal");
    }

    public BoundingBox rotate(Axis axis, double angle) {
        throw new UnsupportedOperationException("The bounding box cannot be rotated, it must be orthogonal");
    }

    public BoundingBox transform(XForm xForm) {
        if (!xForm.isOrthogonal()) {
            throw new UnsupportedOperationException("The bounding box does not support arbitrary transformation, it must be orthogonal");
        }
        return new BoundingBox(xForm.apply(corner1), xForm.apply(corner2));
    }

    public BoundingBox copy() {
        return this;
    }

    private static class SpecialBoundingBox extends BoundingBox {

        private final boolean isNull;

        protected SpecialBoundingBox(boolean isNull) {
            super(isNull ? 0.0 : Double.POSITIVE_INFINITY);
            this.isNull = isNull;
        }

        @Override
        public BoundingSphere asSphere() {
            return isNull ? BoundingSphere.NULL : BoundingSphere.INFINITE;
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
        public BoundingBox scale(double magnitude) {
            return this;
        }

        @Override
        public BoundingBox scale(Vector magnitude) {
            return this;
        }

        @Override
        public BoundingBox translate(Vector magnitude) {
            return this;
        }

        @Override
        public BoundingBox rotate(Vector x, Vector y, Vector z) {
            return this;
        }

        @Override
        public BoundingBox rotate(Axis axis, double angle) {
            return this;
        }
    }
}
