package net.venaglia.realms.common.physical.geom;

import net.venaglia.realms.common.util.Series;
import net.venaglia.realms.common.util.matrix.Matrix_4x4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * User: ed
 * Date: 9/3/12
 * Time: 8:34 AM
 */
public final class Facet implements Series<Point>, Element<Facet> {

    public enum Type {
        TRIANGLE, QUAD, POLY, MIXED
    }

    public final Type type;
    public final Point a;
    public final Point b;
    public final Point c;
    public final Point d;

    private final Point[] more;

    public Facet(Point a, Point b, Point c) {
        this(
                Type.TRIANGLE,
                assertNotNull(a, "a"),
                assertNotNull(b, "b"),
                assertNotNull(c, "c"),
                null,
                null
        );
    }

    public Facet(Point a, Point b, Point c, Point d) {
        this(
                Type.QUAD,
                assertNotNull(a, "a"),
                assertNotNull(b, "b"),
                assertNotNull(c, "c"),
                assertNotNull(d, "d"),
                null
        );
    }

    public Facet(Point a, Point b, Point c, Point d, Point... more) {
        this(
                Type.POLY,
                assertNotNull(a, "a"),
                assertNotNull(b, "b"),
                assertNotNull(c, "c"),
                assertNotNull(d, "d"),
                assertNotNull(more, new String[]{"e","f","g","h","i","j"})
        );
    }

    private Facet(Type type, Point a, Point b, Point c, Point d, Point[] more) {
        this.type = type;
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.more = more;
    }

    private static Point assertNotNull(Point p, String name) {
        if (p == null) {
            throw new NullPointerException(name);
        }
        return p;
    }

    private static Point[] assertNotNull(Point[] p, String[] names) {
        if (p.length > names.length) {
            throw new IllegalArgumentException("Too many points in polygon facet");
        }
        for (int i = 0; i < p.length; i++) {
            if (p[i] == null) {
                throw new NullPointerException(names[i]);
            }
        }
        return p;
    }

    public int size() {
        return d == null ? 3 : 4;
    }

    public Iterator<Point> iterator() {
        if (more != null) {
            List<Point> points = new ArrayList<Point>(more.length + 4);
            Collections.addAll(points, a, b, c, d);
            Collections.addAll(points, more);
            return points.iterator();
        }
        return d == null ? Arrays.asList(a,b,c).iterator() : Arrays.asList(a,b,c,d).iterator();
    }

    public Facet scale(double magnitude) {
        if (magnitude == 0.0) {
            throw new IllegalArgumentException();
        }
        if (magnitude == 1.0) {
            return this;
        }
        Point d = type == Type.TRIANGLE ? null : this.d.scale(magnitude);
        Point[] more = type == Type.POLY ? scale(this.more, magnitude) : null;
        return new Facet(type, a.scale(magnitude), b.scale(magnitude), c.scale(magnitude), d, more);
    }

    private Point[] scale(Point[] more, double magnitude) {
        int l = more.length;
        Point[] result = new Point[l];
        for (int i = 0; i < l; i++) {
            result[i] = more[i].scale(magnitude);
        }
        return result;
    }

    public Facet scale(Vector magnitude) {
        if (magnitude.l == 0.0) {
            throw new IllegalArgumentException();
        }
        Point d = type == Type.TRIANGLE ? null : this.d.scale(magnitude);
        Point[] more = type == Type.POLY ? scale(this.more, magnitude) : null;
        return new Facet(type, a.scale(magnitude), b.scale(magnitude), c.scale(magnitude), d, more);
    }

    private Point[] scale(Point[] more, Vector magnitude) {
        int l = more.length;
        Point[] result = new Point[l];
        for (int i = 0; i < l; i++) {
            result[i] = more[i].scale(magnitude);
        }
        return result;
    }

    public Facet translate(Vector magnitude) {
        if (magnitude.l == 0) {
            return this;
        }
        Point d = type == Type.TRIANGLE ? null : this.d.translate(magnitude);
        Point[] more = type == Type.POLY ? translate(this.more, magnitude) : null;
        return new Facet(type, a.translate(magnitude), b.translate(magnitude), c.translate(magnitude), d, more);
    }

    private Point[] translate(Point[] more, Vector magnitude) {
        int l = more.length;
        Point[] result = new Point[l];
        for (int i = 0; i < l; i++) {
            result[i] = more[i].translate(magnitude);
        }
        return result;
    }

    public Facet rotate(Vector x, Vector y, Vector z) {
        return applyXFrom(new MatrixXForm(Matrix_4x4.rotate(x, y, z)));
    }

    public Facet rotate(Axis axis, double angle) {
        return applyXFrom(new MatrixXForm(Matrix_4x4.rotate(axis, angle)));
    }

    public Facet transform(XForm xForm) {
        Point d = type == Type.TRIANGLE ? null : xForm.apply(this.d);
        Point[] more = type == Type.POLY ? xForm.apply(this.more) : null;
        return new Facet(type, xForm.apply(a), xForm.apply(b), xForm.apply(c), d, more);
    }

    public Facet copy() {
        return this;
    }

    private Facet applyXFrom(XForm xForm) {
        Point a = xForm.apply(this.a);
        Point b = xForm.apply(this.b);
        Point c = xForm.apply(this.c);
        Point d = this.d == null ? null : xForm.apply(this.d);
        Point[] more = this.more == null ? null : xForm.apply(this.more);
        return new Facet(type, a, b, c, d, more);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Facet points = (Facet)o;

        if (type != points.type) return false;
        if (!a.equals(points.a)) return false;
        if (!b.equals(points.b)) return false;
        if (!c.equals(points.c)) return false;
        if (d != null ? !d.equals(points.d) : points.d != null) return false;
        if (!Arrays.equals(more, points.more)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + a.hashCode();
        result = 31 * result + b.hashCode();
        result = 31 * result + c.hashCode();
        result = 31 * result + (d != null ? d.hashCode() : 0);
        result = 31 * result + (more != null ? Arrays.hashCode(more) : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(64);
        buffer.append(String.format("facet([%.2f,%.2f,%.2f],[%.2f,%.2f,%.2f],[%.2f,%.2f,%.2f]",
                                    a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z));
        if (d != null) {
            buffer.append(String.format(",[%.2f,%.2f,%.2f]", d.x, d.y, d.z));
        }
        if (more != null) {
            for (Point p : more) {
                buffer.append(String.format(",[%.2f,%.2f,%.2f]", p.x, p.y, p.z));
            }
        }
        buffer.append(")");
        return buffer.toString();
    }
}
