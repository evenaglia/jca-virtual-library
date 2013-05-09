package net.venaglia.realms.common.util.matrix;


import java.io.Serializable;

/**
 * User: ed
 * Date: 9/12/12
 * Time: 5:21 PM
 */
public class Matrix_3x3 implements Serializable, Cloneable {

    public double m00, m01, m02;
    public double m10, m11, m12;
    public double m20, m21, m22;

    public double determinant() {
        return determinant(m00, m10, m20,
                           m01, m11, m21,
                           m02, m12, m22);
    }

    public Matrix_3x3 load(double m00, double m10, double m20,
                           double m01, double m11, double m21,
                           double m02, double m12, double m22) {
        this.m00 = m00; this.m10 = m10; this.m20 = m20;
        this.m01 = m01; this.m11 = m11; this.m21 = m21;
        this.m02 = m02; this.m12 = m12; this.m22 = m22;
        return this;
    }

    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    @Override
    public Matrix_3x3 clone() {
        return new Matrix_3x3().load(m00, m10, m20, m01, m11, m21, m02, m12, m22);
    }

    @Override
    public String toString() {
        return String.format("| %7.3f , %7.3f , %7.3f |\n" +
                             "| %7.3f , %7.3f , %7.3f |\n" +
                             "| %7.3f , %7.3f , %7.3f |",
                             m00, m10, m20,
                             m01, m11, m21,
                             m02, m12, m22);
    }

    public static double determinant(double m00, double m10, double m20,
                                     double m01, double m11, double m21,
                                     double m02, double m12, double m22) {
        return m00 * (m11 * m22 - m21 * m12)
             - m10 * (m01 * m22 - m21 * m02)
             + m20 * (m01 * m12 - m11 * m02);
    }
}
