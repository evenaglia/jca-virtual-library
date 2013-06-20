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
    private Matrix_1x4.View<Vector> curriedVector;

    public MatrixXForm(Matrix_4x4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("matrix");
        }
        this.matrix = matrix;
    }

    public Vector apply(Vector v) {
        return apply(v.i, v.j, v.k, getCurriedVector());
    }

    private Matrix_1x4.View<Vector> getCurriedVector() {
        if (curriedVector == null) {
            curriedVector = apply(0, 0, 0, Matrix_1x4.View.CURRIED_VECTOR);
        }
        return curriedVector;
    }

    public Point apply(Point p) {
        return apply(p.x, p.y, p.z, Matrix_1x4.View.POINT);
    }

    private <V> V apply(double b00, double b01, double b02, Matrix_1x4.View<V> view) {
        double a00 = matrix.m00, a10 = matrix.m10, a20 = matrix.m20, a30 = matrix.m30;
        double a01 = matrix.m01, a11 = matrix.m11, a21 = matrix.m21, a31 = matrix.m31;
        double a02 = matrix.m02, a12 = matrix.m12, a22 = matrix.m22, a32 = matrix.m32;
        double n00 = a00 * b00 + a10 * b01 + a20 * b02 + a30;
        double n01 = a01 * b00 + a11 * b01 + a21 * b02 + a31;
        double n02 = a02 * b00 + a12 * b01 + a22 * b02 + a32;
        return view.convert(n00, n01, n02, 0);
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
