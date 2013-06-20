package net.venaglia.gloo.util.matrix;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

import java.io.Serializable;

/**
 * User: ed
 * Date: 3/2/13
 * Time: 12:53 AM
 */
public class Matrix_1x4 implements Serializable, Cloneable {

    public interface View<T> {

        T convert(double x, double y, double z, double w);

        View<Matrix_1x4> MATRIX = new View<Matrix_1x4>() {
            public Matrix_1x4 convert(double x, double y, double z, double w) {
                return new Matrix_1x4().load(x, y, z, w);
            }
        };

        View<double[]> ARRAY = new View<double[]>() {
            public double[] convert(double x, double y, double z, double w) {
                return new double[]{ x, y, z, w };
            }
        };

        View<Point> POINT = new View<Point>() {
            public Point convert(double x, double y, double z, double w) {
                return new Point(x, y, z);
            }
        };

        View<Vector> VECTOR = new View<Vector>() {
            public Vector convert(double x, double y, double z, double w) {
                return new Vector(x, y, z);
            }
        };

        View<View<Vector>> CURRIED_VECTOR = new View<View<Vector>>() {
            public View<Vector> convert(final double x1, final double y1, final double z1, final double w1) {
                return new View<Vector>() {
                    public Vector convert(double x2, double y2, double z2, double w2) {
                        return new Vector(x2 - x1, y2 - y1, z2 - z1);
                    }
                };
            }
        };
    }

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
