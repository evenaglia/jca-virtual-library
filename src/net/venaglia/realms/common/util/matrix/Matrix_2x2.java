package net.venaglia.realms.common.util.matrix;

import java.io.Serializable;

/**
 * User: ed
 * Date: 9/12/12
 * Time: 5:21 PM
 */
public class Matrix_2x2 implements Serializable, Cloneable {

    public double m00, m01;
    public double m10, m11;

    public double determinant() {
        return determinant(m00, m10,
                           m01, m11);
    }

    public Matrix_2x2 load(double m00, double m10,
                           double m01, double m11) {
        this.m00 = m00; this.m10 = m10;
        this.m01 = m01; this.m11 = m11;
        return this;
    }

    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    @Override
    public Matrix_2x2 clone() {
        return new Matrix_2x2().load(m00, m10, m01, m11);
    }

    @Override
    public String toString() {
        return String.format("| %7.3f , %7.3f |\n" +
                             "| %7.3f , %7.3f |\n" +
                             m00, m10,
                             m01, m11);
    }

    public static double determinant(double m00, double m10,
                                     double m01, double m11) {
        return m00 * m11 - m10 * m01;
    }
}
