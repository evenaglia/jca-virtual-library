package net.venaglia.gloo.physical.geom;

import net.venaglia.gloo.util.matrix.Matrix_1x4;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 10:40 PM
 */
public interface XForm {

    XForm IDENTITY = new XForm() {
        public Vector apply(Vector vector) {
            return vector;
        }

        public <V> V apply(Vector vector, View<V> view) {
            return view.convert(vector.i, vector.j, vector.k, 1);
        }

        public Point apply(Point point) {
            return point;
        }

        public <V> V apply(Point point, View<V> view) {
            return view.convert(point.x, point.y, point.z, 1);
        }

        public <V> V apply(Matrix_1x4 matrix, View<V> view) {
            return view.convert(matrix.m00, matrix.m01, matrix.m02, matrix.m03);
        }

        public Vector[] apply(Vector[] vectors) {
            return vectors.clone();
        }

        public Point[] apply(Point[] points) {
            return points.clone();
        }

        public boolean isSymmetric() {
            return true;
        }

        public boolean isOrthogonal() {
            return true;
        }

        public <V> V apply(double x, double y, double z, double w, View<V> view) {
            return view.convert(x, y, z, w);
        }
    };

    Vector apply(Vector vector);

    Point apply(Point point);

    <V> V apply(Vector vector, XForm.View<V> view);

    <V> V apply(Point point, XForm.View<V> view);

    <V> V apply(Matrix_1x4 point, XForm.View<V> view);

    Vector[] apply(Vector[] vectors);

    Point[] apply(Point[] points);

    boolean isSymmetric();

    boolean isOrthogonal();

    <V> V apply(double x, double y, double z, double w, XForm.View<V> view);

    interface View<T> {
        T convert(double x, double y, double z, double w);
    }

    View<double[]> ARRAY_XFORM_VIEW = new View<double[]>() {
        public double[] convert(double x, double y, double z, double w) {
            return new double[]{ x, y, z, w };
        }
    };
}
