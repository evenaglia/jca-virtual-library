package net.venaglia.realms.common.util.matrix;

import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;

import java.io.Serializable;

/**
 * User: ed
 * Date: 9/12/12
 * Time: 5:21 PM
 */
public class Matrix_4x4 implements Serializable, Cloneable {

    public double m00, m01, m02, m03;
    public double m10, m11, m12, m13;
    public double m20, m21, m22, m23;
    public double m30, m31, m32, m33;

    public static void accumulate(Matrix_4x4 accum, Matrix_4x4 apply) {
        product(accum.m00, accum.m10, accum.m20, accum.m30,
                           accum.m01, accum.m11, accum.m21, accum.m31,
                           accum.m02, accum.m12, accum.m22, accum.m32,
                           accum.m03, accum.m13, accum.m23, accum.m33,
                           apply.m00, apply.m10, apply.m20, apply.m30,
                           apply.m01, apply.m11, apply.m21, apply.m31,
                           apply.m02, apply.m12, apply.m22, apply.m32,
                           apply.m03, apply.m13, apply.m23, apply.m33,
                           accum);
    }

    public static Matrix_4x4 identity() {
        return scale(1);
    }

    public static Matrix_4x4 scale(double r) {
        Matrix_4x4 m = new Matrix_4x4();
        m.load(r, 0, 0, 0,
               0, r, 0, 0,
               0, 0, r, 0,
               0, 0, 0, 1);
        return m;
    }

    public static Matrix_4x4 scale(Vector v) {
        Matrix_4x4 m = new Matrix_4x4();
        m.load(v.i,   0,   0,   0,
                 0, v.j,   0,   0,
                 0,   0, v.k,   0,
                 0,   0,   0,   1);
        return m;
    }

    public static Matrix_4x4 translate(Vector v) {
        Matrix_4x4 m = new Matrix_4x4();
        m.load(1, 0, 0, v.i,
               0, 1, 0, v.j,
               0, 0, 1, v.k,
               0, 0, 0, 1);
        return m;
    }

    public static Matrix_4x4 center(Point p) {
        Matrix_4x4 m = new Matrix_4x4();
        m.load(1, 0, 0, -p.x,
               0, 1, 0, -p.y,
               0, 0, 1, -p.z,
               0, 0, 0, 1);
        return m;
    }

    public static Matrix_4x4 translate(Axis axis, double distance) {
        Matrix_4x4 m = new Matrix_4x4();
        m.load(1, 0, 0, axis == Axis.X ? distance : 0,
               0, 1, 0, axis == Axis.Y ? distance : 0,
               0, 0, 1, axis == Axis.Z ? distance : 0,
               0, 0, 0, 1);
        return m;
    }

    public static Matrix_4x4 rotate(Vector x, Vector y, Vector z) {
        Matrix_4x4 m = new Matrix_4x4();
//        m.load(x.i, y.i, z.i, 0,
//               x.j, y.j, z.j, 0,
//               x.k, y.k, z.k, 0,
//                 0,   0,   0, 1);
        m.load(x.i, x.j, x.k, 0,
               y.i, y.j, y.k, 0,
               z.i, z.j, z.k, 0,
                 0,   0,   0, 1);
        return m;
    }

    public static Matrix_4x4 rotate(Axis axis, double angle) {
        Matrix_4x4 m = new Matrix_4x4();
        double a = Math.cos(angle);
        double b = Math.sin(angle);
        switch (axis) {
            case X:
                m.load( 1, 0, 0, 0,
                        0, a,-b, 0,
                        0, b, a, 0,
                        0, 0, 0, 1);
                break;
            case Y:
                m.load( a, 0, b, 0,
                        0, 1, 0, 0,
                       -b, 0, a, 0,
                        0, 0, 0, 1);
                break;
            case Z:
                m.load( a,-b, 0, 0,
                        b, a, 0, 0,
                        0, 0, 1, 0,
                        0, 0, 0, 1);
                break;
        }
        return m;
    }

    public double determinant() {
        return determinant(m00, m10, m20, m30,
                           m01, m11, m21, m31,
                           m02, m12, m22, m32,
                           m03, m13, m23, m33);
    }

    public Matrix_4x4 loadIdentity() {
        this.load(1,0,0,0,
                  0,1,0,0,
                  0,0,1,0,
                  0,0,0,1);
        return this;
    }

    public Matrix_4x4 loadRotation(Axis axis, double angle) {
        double a = Math.cos(angle);
        double b = Math.sin(angle);
        switch (axis) {
            case X:
                load( 1, 0, 0, 0,
                      0, a,-b, 0,
                      0, b, a, 0,
                      0, 0, 0, 1);
                break;
            case Y:
                load( a, 0, b, 0,
                      0, 1, 0, 0,
                     -b, 0, a, 0,
                      0, 0, 0, 1);
                break;
            case Z:
                load( a,-b, 0, 0,
                      b, a, 0, 0,
                      0, 0, 1, 0,
                      0, 0, 0, 1);
                break;
        }
        return this;
    }

    public Matrix_4x4 load(double m00, double m10, double m20, double m30,
                           double m01, double m11, double m21, double m31,
                           double m02, double m12, double m22, double m32,
                           double m03, double m13, double m23, double m33) {
        this.m00 = m00; this.m10 = m10; this.m20 = m20; this.m30 = m30;
        this.m01 = m01; this.m11 = m11; this.m21 = m21; this.m31 = m31;
        this.m02 = m02; this.m12 = m12; this.m22 = m22; this.m32 = m32;
        this.m03 = m03; this.m13 = m13; this.m23 = m23; this.m33 = m33;
        return this;
    }

    public Matrix_4x4 product(Matrix_4x4 _) {
        Matrix_4x4 result = new Matrix_4x4();
        product(m00, m10, m20, m30,
                m01, m11, m21, m31,
                m02, m12, m22, m32,
                m03, m13, m23, m33,
                _.m00, _.m10, _.m20, _.m30,
                _.m01, _.m11, _.m21, _.m31,
                _.m02, _.m12, _.m22, _.m32,
                _.m03, _.m13, _.m23, _.m33,
                result);
        return result;
    }

    public Matrix_1x4 product(Matrix_1x4 _) {
        return product(m00, m10, m20, m30,
                       m01, m11, m21, m31,
                       m02, m12, m22, m32,
                       m03, m13, m23, m33,
                       _.m00,
                       _.m01,
                       _.m02,
                       _.m03,
                      Matrix_1x4.View.MATRIX);
    }

    public <T> T product(double x, double y, double z, Matrix_1x4.View<T> view) {
        return product(m00, m10, m20, m30,
                       m01, m11, m21, m31,
                       m02, m12, m22, m32,
                       m03, m13, m23, m33,
                       x,
                       y,
                       z,
                       0,
                       view);
    }

    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    @Override
    public Matrix_4x4 clone() {
        return new Matrix_4x4().load(m00, m10, m20, m30, m01, m11, m21, m31, m02, m12, m22, m32, m03, m13, m23, m33);
    }

    @Override
    public String toString() {
        return String.format("| %7.3f , %7.3f , %7.3f , %7.3f |\n" +
                             "| %7.3f , %7.3f , %7.3f , %7.3f |\n" +
                             "| %7.3f , %7.3f , %7.3f , %7.3f |\n" +
                             "| %7.3f , %7.3f , %7.3f , %7.3f |",
                             m00, m10, m20, m30,
                             m01, m11, m21, m31,
                             m02, m12, m22, m32,
                             m03, m13, m23, m33);
    }

    public static double determinant(double m00, double m10, double m20, double m30,
                                     double m01, double m11, double m21, double m31,
                                     double m02, double m12, double m22, double m32,
                                     double m03, double m13, double m23, double m33) {
        return m00 * (  m11 * (m22 * m33 - m32 * m23)
                      - m21 * (m12 * m33 - m32 * m13)
                      + m31 * (m12 * m23 - m22 * m13))
             - m10 * (  m01 * (m22 * m33 - m32 * m23)
                      - m21 * (m02 * m33 - m32 * m03)
                      + m31 * (m02 * m23 - m22 * m03))
             + m20 * (  m01 * (m12 * m33 - m32 * m13)
                      - m11 * (m02 * m33 - m32 * m03)
                      + m31 * (m02 * m13 - m12 * m03))
             - m30 * (  m01 * (m12 * m23 - m22 * m13)
                      - m11 * (m02 * m23 - m22 * m03)
                      + m21 * (m02 * m13 - m12 * m03));
    }

    public static void product(Matrix_4x4 accumulate, Matrix_4x4 multiply) {
        product(accumulate.m00, accumulate.m10, accumulate.m20, accumulate.m30,
                accumulate.m01, accumulate.m11, accumulate.m21, accumulate.m31,
                accumulate.m02, accumulate.m12, accumulate.m22, accumulate.m32,
                accumulate.m03, accumulate.m13, accumulate.m23, accumulate.m33,
                multiply.m00, multiply.m10, multiply.m20, multiply.m30,
                multiply.m01, multiply.m11, multiply.m21, multiply.m31,
                multiply.m02, multiply.m12, multiply.m22, multiply.m32,
                multiply.m03, multiply.m13, multiply.m23, multiply.m33,
                accumulate);
    }

    public static void product(double a00, double a10, double a20, double a30,
                               double a01, double a11, double a21, double a31,
                               double a02, double a12, double a22, double a32,
                               double a03, double a13, double a23, double a33,
                               double b00, double b10, double b20, double b30,
                               double b01, double b11, double b21, double b31,
                               double b02, double b12, double b22, double b32,
                               double b03, double b13, double b23, double b33,
                               Matrix_4x4 out) {
        double n00, n10, n20, n30;
        double n01, n11, n21, n31;
        double n02, n12, n22, n32;
        double n03, n13, n23, n33;

        n00 = a00 * b00 + a10 * b01 + a20 * b02 + a30 * b03;
        n10 = a00 * b10 + a10 * b11 + a20 * b12 + a30 * b13;
        n20 = a00 * b20 + a10 * b21 + a20 * b22 + a30 * b23;
        n30 = a00 * b30 + a10 * b31 + a20 * b32 + a30 * b33;
        n01 = a01 * b00 + a11 * b01 + a21 * b02 + a31 * b03;
        n11 = a01 * b10 + a11 * b11 + a21 * b12 + a31 * b13;
        n21 = a01 * b20 + a11 * b21 + a21 * b22 + a31 * b23;
        n31 = a01 * b30 + a11 * b31 + a21 * b32 + a31 * b33;
        n02 = a02 * b00 + a12 * b01 + a22 * b02 + a32 * b03;
        n12 = a02 * b10 + a12 * b11 + a22 * b12 + a32 * b13;
        n22 = a02 * b20 + a12 * b21 + a22 * b22 + a32 * b23;
        n32 = a02 * b30 + a12 * b31 + a22 * b32 + a32 * b33;
        n03 = a03 * b00 + a13 * b01 + a23 * b02 + a33 * b03;
        n13 = a03 * b10 + a13 * b11 + a23 * b12 + a33 * b13;
        n23 = a03 * b20 + a13 * b21 + a23 * b22 + a33 * b23;
        n33 = a03 * b30 + a13 * b31 + a23 * b32 + a33 * b33;

        out.load(n00, n10, n20, n30,
                 n01, n11, n21, n31,
                 n02, n12, n22, n32,
                 n03, n13, n23, n33);
    }

    public static <T> T product(double a00, double a10, double a20, double a30,
                                double a01, double a11, double a21, double a31,
                                double a02, double a12, double a22, double a32,
                                double a03, double a13, double a23, double a33,
                                double b00,
                                double b01,
                                double b02,
                                double b03,
                                Matrix_1x4.View<T> view) {
        double n00, n01, n02, n03;

        n00 = a00 * b00 + a10 * b01 + a20 * b02 + a30 * b03;
        n01 = a01 * b00 + a11 * b01 + a21 * b02 + a31 * b03;
        n02 = a02 * b00 + a12 * b01 + a22 * b02 + a32 * b03;
        n03 = a03 * b00 + a13 * b01 + a23 * b02 + a33 * b03;

        return view.convert(n00, n01, n02, n03);
    }
}
