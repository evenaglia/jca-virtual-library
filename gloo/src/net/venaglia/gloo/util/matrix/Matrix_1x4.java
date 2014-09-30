package net.venaglia.gloo.util.matrix;

import net.venaglia.gloo.physical.geom.XForm;

import java.io.Serializable;

/**
 * User: ed
 * Date: 3/2/13
 * Time: 12:53 AM
 */
public class Matrix_1x4 implements Serializable, Cloneable {

    public static final XForm.View<Matrix_1x4> MATRIX_XFORM_VIEW = new XForm.View<Matrix_1x4>() {
        public Matrix_1x4 convert(double x, double y, double z, double w) {
            return new Matrix_1x4().load(x, y, z, w);
        }
    };

    public double m00, m01, m02, m03;

    public Matrix_1x4 load(double m00, double m01, double m02, double m03) {
        this.m00 = m00; this.m01 = m01; this.m02 = m02; this.m03 = m03;
        return this;
    }

    @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
    @Override
    protected Matrix_1x4 clone() {
        return new Matrix_1x4().load(m00, m01, m02, m03);
    }

    @Override
    public String toString() {
        return String.format("| %7.3f |\n" +
                             "| %7.3f |\n" +
                             "| %7.3f |\n" +
                             "| %7.3f |",
                             m00, m01, m02, m03);
    }

}
