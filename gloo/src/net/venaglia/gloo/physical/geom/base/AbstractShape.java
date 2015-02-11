package net.venaglia.gloo.physical.geom.base;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.MatrixXForm;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.decorators.Transformation;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.common.util.Lock;
import net.venaglia.common.util.impl.ThreadSafeLock;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:35 PM
 */
public abstract class AbstractShape<T extends AbstractShape<T>> implements Shape<T> {

    public final Point[] points;

    protected final Lock lock = new ThreadSafeLock();

    protected Transformation transformation;
    protected Material material = Material.DEFAULT;
    protected BoundingVolume<?> boundingVolume;

    private boolean isStatic = true;

    protected static Point[] toArray(Point a, Point b) {
        return assertNotNull(new Point[]{a,b});
    }

    protected static Point[] toArray(Point a, Point b, Point[] more) {
        List<Point> p = new ArrayList<Point>(more.length + 2);
        p.add(a);
        p.add(b);
        Collections.addAll(p, more);
        return assertNotNull(p.toArray(new Point[p.size()]));
    }

    protected static Point[] toArray(Point a, Point b, Point c) {
        return assertNotNull(new Point[]{a,b,c});
    }

    protected static Point[] toArray(Point a, Point b, Point c, Point[] more) {
        List<Point> p = new ArrayList<Point>(more.length + 4);
        p.add(a);
        p.add(b);
        p.add(c);
        Collections.addAll(p, more);
        return assertNotNull(p.toArray(new Point[p.size()]));
    }

    protected static Point[] toArray(Point a, Point b, Point c, Point d) {
        return assertNotNull(new Point[]{ a, b, c, d });
    }

    protected static Point[] toArray(Point a, Point b, Point c, Point d, Point[] more) {
        List<Point> p = new ArrayList<Point>(more.length + 4);
        p.add(a);
        p.add(b);
        p.add(c);
        p.add(d);
        Collections.addAll(p, more);
        return assertNotNull(p.toArray(new Point[p.size()]));
    }

    protected static Point[] toArray(Iterable<Point> points) {
        List<Point> buffer = new ArrayList<Point>();
        for (Point p : points) {
            buffer.add(p);
        }
        return buffer.toArray(new Point[buffer.size()]);
    }

    protected static <E> E[] assertLength(E[] array, int length) {
        if (array.length == length) {
            return array;
        } else if (array.length < length) {
            throw new IllegalArgumentException("Too few points: expected=" + length + ", actual=" + array.length);
        } else {
            throw new IllegalArgumentException("Too many points: expected=" + length + ", actual=" + array.length);
        }
    }

    protected static Point[] assertMinLength(Point[] points, int minLength) {
        if (points.length < minLength) {
            throw new IllegalArgumentException("Too few points: expectedMin=" + minLength + ", actual=" + points.length);
        }
        return points;
    }

    protected static Point[] assertMultiple(Point[] points, int multiple) {
        if (points.length % multiple != 0) {
            throw new IllegalArgumentException("Number of points must be a multiple of " + multiple + ": actual=" + points.length);
        }
        return points;
    }

    protected static Point[] assertNotNull(Point[] points) {
        if (points == null) throw new NullPointerException("points");
        for (int i = 0, l = points.length; i < l; i++) {
            if (points[i] == null) throw new NullPointerException("points[" + i + "]");
        }
        return points;
    }

    protected AbstractShape(Point[] points) {
        this.points = points;
        if (points.length == 0) {
            throw new IllegalArgumentException("A shape must have at least one point");
        }
    }

    @SuppressWarnings("unchecked")
    protected T self() {
        return (T)this;
    }

    @SuppressWarnings("unchecked")
    protected abstract T build(Point[] points, XForm xForm);

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    public final int size() {
        return points.length;
    }

    protected BoundingVolume<?> createBoundingVolume() {
        return new BoundingBox(points);
    }

    public BoundingVolume<?> getBounds() {
        if (boundingVolume == null) {
            boundingVolume = createBoundingVolume();
        }
        return boundingVolume;
    }

    public T copy() {
        T copy = build(points.clone(), XForm.IDENTITY);
        if (transformation != null) {
            copy.getTransformation().transform(transformation);
        }
        return copy;
    }

    public T scale(final double magnitude) {
        if (magnitude == 0.0) {
            throw new IllegalArgumentException();
        }
        if (magnitude == 1.0) {
            return self();
        }
        XForm x = new MatrixXForm(Matrix_4x4.scale(magnitude));
        return build(x.apply(points), x);
    }

    public T scale(final Vector magnitude) {
        if (magnitude.l == 0.0) {
            throw new IllegalArgumentException();
        }
        XForm x = new MatrixXForm(Matrix_4x4.scale(magnitude));
        return build(x.apply(points), x);
    }

    public T translate(final Vector magnitude) {
        if (magnitude.equals(Vector.ZERO)) {
            return self();
        }
        XForm x = new MatrixXForm(Matrix_4x4.translate(magnitude));
        return build(x.apply(points), x);
    }

    public T rotate(final Vector x, final Vector y, final Vector z) {
        final boolean symmetric = x.l == y.l && y.l == z.l;
        if (symmetric) {
            if (x.l == 0) {
                throw new IllegalArgumentException();
            }
            if (x.l == 1) {
                return self();
            }
        }
        XForm xForm = new MatrixXForm(Matrix_4x4.rotate(x, y, z));
        return build(xForm.apply(points), xForm);
    }

    public T rotate(Axis axis, double angle) {
        if (angle == 0) {
            return self();
        }
        XForm xForm = new MatrixXForm(Matrix_4x4.rotate(axis, angle));
        return build(xForm.apply(points), xForm);
    }

    public T transform(XForm xForm) {
        return build(xForm.apply(points), xForm);
    }

    public Iterator<Point> iterator() {
        return Arrays.asList(points).iterator();
    }

    public Transformation getTransformation() {
        if (transformation == null) {
            transformation = new Transformation(lock, new Runnable() {
                public void run() {
                    isStatic = false;
                }
            });
        }
        return transformation;
    }

    public T setMaterial(Material material) {
        lock.assertUnlocked();
        this.material = material;
        return self();
    }

    public Material getMaterial() {
        return this.material;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (transformation == null) {
            material.apply(nowMS, buffer);
            project(buffer);
        } else {
            buffer.pushTransform();
            transformation.apply(nowMS, buffer);
            material.apply(nowMS, buffer);
            project(buffer);
            buffer.popTransform();
        }
    }

    protected abstract void project(GeometryBuffer buffer);

    public final Lock getLock() {
        return lock;
    }

    public final boolean isStatic() {
        return isStatic;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractShape line = (AbstractShape)o;

        return Arrays.equals(points, line.points);

    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append('(');
        for (int i = 0, l = points.length, j = l - 1; i < l; i++, j--) {
            if (i != 0) buf.append('-');
            Point p = points[i];
            buf.append(String.format("[%.2f,%.2f,%.2f]", p.x, p.y, p.z));
        }
        buf.append(')');
        return buf.toString();
    }
}
