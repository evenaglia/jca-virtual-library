package net.venaglia.gloo.physical.geom;

import net.venaglia.gloo.util.matrix.Matrix_1x4;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 10:46 PM
 */
public class MatrixXForm implements XForm {

    private Matrix_4x4 matrix;
    private XForm.View<Vector> curriedVector;

    public MatrixXForm(Matrix_4x4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("matrix");
        }
        this.matrix = matrix;
    }

    public Vector apply(Vector v) {
        return apply(v.i, v.j, v.k, 1.0, getCurriedVector());
    }

    private XForm.View<Vector> getCurriedVector() {
        if (curriedVector == null) {
            curriedVector = apply(0, 0, 0, 1.0, Vector.CURRIED_VECTOR_XFORM_VIEW);
        }
        return curriedVector;
    }

    public Point apply(Point p) {
        return apply(p.x, p.y, p.z, 1.0, Point.POINT_XFORM_VIEW);
    }

    public <V> V apply(Vector vector, View<V> view) {
        return apply(vector.i, vector.j, vector.k, 1, view);
    }

    public <V> V apply(Point point, View<V> view) {
        return apply(point.x, point.y, point.z, 1, view);
    }

    public <V> V apply(Matrix_1x4 matrix, View<V> view) {
        return apply(matrix.m00, matrix.m01, matrix.m02, matrix.m03, view);
    }

    public <V> V apply(double b00, double b01, double b02, double b03, XForm.View<V> view) {
        double a00 = matrix.m00, a10 = matrix.m10, a20 = matrix.m20, a30 = matrix.m30;
        double a01 = matrix.m01, a11 = matrix.m11, a21 = matrix.m21, a31 = matrix.m31;
        double a02 = matrix.m02, a12 = matrix.m12, a22 = matrix.m22, a32 = matrix.m32;
        double a03 = matrix.m03, a13 = matrix.m13, a23 = matrix.m23, a33 = matrix.m33;
        double n00 = a00 * b00 + a10 * b01 + a20 * b02 + b03 * a30;
        double n01 = a01 * b00 + a11 * b01 + a21 * b02 + b03 * a31;
        double n02 = a02 * b00 + a12 * b01 + a22 * b02 + b03 * a32;
        double n03 = a03 * b00 + a13 * b01 + a23 * b02 + b03 * a33;
        return view.convert(n00, n01, n02, n03);
    }

    public Vector[] apply(Vector[] vectors) {
        Vector[] out = new Vector[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            out[i] = apply(vectors[i]);
        }
        return out;
    }

    public Point[] apply(Point[] points) {
        Point[] out = new Point[points.length];
        for (int i = 0; i < points.length; i++) {
            out[i] = apply(points[i]);
        }
        return out;
    }

    public boolean isSymmetric() {
        double n00 = matrix.m00 + matrix.m10 + matrix.m20;
        double n01 = matrix.m01 + matrix.m11 + matrix.m21;
        double n02 = matrix.m02 + matrix.m12 + matrix.m22;
        return Math.abs(n00 - n01) + Math.abs(n01 - n02) <= 0.000001;
    }

    public boolean isOrthogonal() {
        List<Double> sums = new ArrayList<Double>(3);
        sums.add(matrix.m00 + matrix.m10 + matrix.m20);
        sums.add(matrix.m01 + matrix.m11 + matrix.m21);
        sums.add(matrix.m02 + matrix.m12 + matrix.m22);
        return sums.remove(matrix.m00 + matrix.m01 + matrix.m02) &&
               sums.remove(matrix.m10 + matrix.m11 + matrix.m12) &&
               sums.remove(matrix.m20 + matrix.m21 + matrix.m22);
    }

    @Override
    public String toString() {
        return "MatrixTransform:\n\t" + (matrix.toString().replace("\n", "\n\t"));
    }
}
