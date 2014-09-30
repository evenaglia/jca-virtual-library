package net.venaglia.gloo.physical.geom;

import static net.venaglia.gloo.physical.geom.XForm.View;

import net.venaglia.gloo.util.matrix.Matrix_4x4;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 8:50 PM
 */
public final class Point implements Element<Point> {

    public static final Point ORIGIN = new Point(0,0,0);

    public static final View<Point> POINT_XFORM_VIEW = new View<Point>() {
        public Point convert(double x, double y, double z, double w) {
            return new Point(x, y, z);
        }
    };


    public final double x;
    public final double y;
    public final double z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Point scale(double magnitude) {
        if (magnitude == 1.0) {
            return this;
        }
        return new Point(x * magnitude, y * magnitude, z * magnitude);
    }

    public double ofAxis(Axis axis) {
        return axis.of(x,y,z);
    }

    public Point scale(Vector magnitude) {
        if (magnitude.l == 0) {
            return ORIGIN;
        }
        return new Point(x * magnitude.i, y * magnitude.j, z * magnitude.k);
    }

    public Point translate(Vector magnitude) {
        if (Vector.ZERO.equals(magnitude)) {
            return this;
        }
        return new Point(x + magnitude.i, y + magnitude.j, z + magnitude.k);
    }

    public Point rotate(Vector x, Vector y, Vector z) {
        return new MatrixXForm(Matrix_4x4.rotate(x, y, z)).apply(this);
    }

    public Point rotate(Axis axis, double angle) {
        return new MatrixXForm(Matrix_4x4.rotate(axis, angle)).apply(this);
    }

    public Point transform(XForm xForm) {
        return xForm.apply(this);
    }

    public Point copy() {
        return this; // object is immutable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point p = (Point)o;

        return Double.compare(p.x, x) == 0 &&
               Double.compare(p.y, y) == 0 &&
               Double.compare(p.z, z) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = x != +0.0d ? Double.doubleToLongBits(x) : 0L;
        result = (int)(temp ^ (temp >>> 32));
        temp = y != +0.0d ? Double.doubleToLongBits(y) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        temp = z != +0.0d ? Double.doubleToLongBits(z) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return String.format("p[%.4f,%.4f,%.4f]", x, y, z);
    }

    public double computeDistance(Point p) {
        return Vector.computeDistance(p.x - x, p.y - y, p.z - z);
    }

    public static Point midPoint(Point a, Point b, double n) {
        if (n == 0.0) {
            return a;
        }
        if (n == 1.0) {
            return b;
        }
        double m = 1.0 - n;
        double x = (a.x * m + b.x * n);
        double y = (a.y * m + b.y * n);
        double z = (a.z * m + b.z * n);
        return new Point(x, y, z);
    }
}
