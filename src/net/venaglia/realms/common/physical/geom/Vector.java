package net.venaglia.realms.common.physical.geom;

import net.venaglia.realms.common.physical.geom.primitives.Line;
import net.venaglia.realms.common.util.matrix.Matrix_4x4;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 9:15 PM
 */
public final class Vector implements Element<Vector> {

    public static final Vector ZERO = new Vector(0,0,0,0);
    public static final Vector X = new Vector(1.0,0,0,1.0);
    public static final Vector Y = new Vector(0,1.0,0,1.0);
    public static final Vector Z = new Vector(0,0,1.0,1.0);

    public final double i;
    public final double j;
    public final double k;
    public final double l;

    public Vector(double i, double j, double k) {
        this.i = i;
        this.j = j;
        this.k = k;
        this.l = computeDistance(i, j, k);
    }

    private Vector(double i, double j, double k, double l) {
        this.i = i;
        this.j = j;
        this.k = k;
        this.l = l;
    }

    /**
     * Creates a vector from one point to another
     * @param a The point to start from
     * @param b The point to end at
     * @return A vector: a &rarr; b
     */
    public static Vector betweenPoints(Point a, Point b) {
        return new Vector(b.x - a.x, b.y - a.y, b.z - a.z);
    }

    /**
     * @return The cross product of two vectors: (a &rarr; b) X (a &rarr; c)
     */
    public static Vector cross(Point a, Point b, Point c) {
        Vector v1 = betweenPoints(a, b);
        Vector v2 = betweenPoints(a, c);
        return v1.cross(v2);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Vector v = (Vector)o;

        return Double.compare(v.i, i) == 0 &&
               Double.compare(v.j, j) == 0 &&
               Double.compare(v.k, k) == 0 &&
               Double.compare(v.l, l) == 0;
    }

    public double ofAxis(Axis axis) {
        return axis.of(i,j,k);
    }

    public Vector scale(double m) {
        if (m == 1.0) {
            return this;
        }
        return new Vector(i * m, j * m, k * m, Math.abs(l * m));
    }

    public Vector scale(Vector m) {
        if (m.l == 0.0) {
            return ZERO;
        }
        return new Vector(i * m.i, j * m.j, k * m.k, Math.abs(l * m.l));
    }

    public Vector translate(Vector magnitude) {
        return sum(magnitude);
    }

    public Vector rotate(Vector x, Vector y, Vector z) {
        return new MatrixXForm(Matrix_4x4.rotate(x, y, z)).apply(this);
    }

    public Vector rotate(Axis axis, double angle) {
        return new MatrixXForm(Matrix_4x4.rotate(axis, angle)).apply(this);
    }

    public Vector transform(XForm xForm) {
        return xForm.apply(this);
    }

    public Vector copy() {
        return this; // object is immutable
    }

    public Vector normalize() {
        if (l == 1.0 || l == 0.0) {
            return this;
        }
        return new Vector(i / l, j / l, k / l, 1.0);
    }

    public Vector normalize(double length) {
        if (l == length || l == 0.0) {
            return this;
        }
        return new Vector(i * length / l, j * length / l, k * length / l, length);
    }

    public Vector reverse() {
        return new Vector(0.0 - i, 0.0 - j, 0.0 - k, l);
    }

    public Line toLine(Point start) {
        return new Line(start, new Point(start.x + i, start.y + j, start.z + k));
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = i != +0.0d ? Double.doubleToLongBits(i) : 0L;
        result = (int)(temp ^ (temp >>> 32));
        temp = j != +0.0d ? Double.doubleToLongBits(j) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = k != +0.0d ? Double.doubleToLongBits(k) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = l != +0.0d ? Double.doubleToLongBits(l) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("v[%.2f,%.2f,%.2f]:|%.2f|", i, j, k, l);
    }

    public Vector cross(Vector b) {
        Vector a = this;
        double i = a.j * b.k - a.k * b.j;
        double j = a.k * b.i - a.i * b.k;
        double k = a.i * b.j - a.j * b.i;
        return new Vector(i, j, k);
    }

    public double dot(Vector b) {
        Vector a = this;
        return a.i * b.i + a.j * b.j + a.k * b.k;
    }

    public Vector sum(Vector b) {
        Vector a = this;
        return new Vector(a.i + b.i, a.j + b.j, a.k + b.k);
    }

    public double angle(Vector b) {
        Vector a = this;
        if (a.l == 0 || b.l == 0) {
            return Double.NaN;
        }
        double cos = a.dot(b) / (a.l * b.l);
        return Math.abs(Math.acos(cos) * 180.0 / Math.PI);
    }

    public static double computeDistance(double i, double j, double k) {
        return Math.sqrt(i * i + j * j + k * k);
    }

    public static double computeDistance(Point a, Point b) {
        return computeDistance(a.x - b.x, a.y - b.y, a.z - b.z);
    }
}
