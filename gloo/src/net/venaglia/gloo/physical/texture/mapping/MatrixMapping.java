package net.venaglia.gloo.physical.texture.mapping;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

/**
 * User: ed
 * Date: 3/18/13
 * Time: 9:56 PM
 */
public class MatrixMapping implements TextureMapping {

    private Matrix_4x4 matrix;

    public MatrixMapping(Matrix_4x4 matrix) {
        if (matrix == null) {
            throw new NullPointerException("matrix");
        }
        this.matrix = matrix;
    }

    public MatrixMapping(double m00, double m10, double m20, double m30,
                         double m01, double m11, double m21, double m31) {
        this(new Matrix_4x4().load(m00, m10, m20, m30, m01, m11, m21, m31, 0, 0, 0, 0, 0, 0, 0, 0));
    }

    public void newSequence() {
        // no-op
    }

    public TextureCoordinate unwrap(Point p) {
        double a00 = matrix.m00, a10 = matrix.m10, a20 = matrix.m20, a30 = matrix.m30;
        double a01 = matrix.m01, a11 = matrix.m11, a21 = matrix.m21, a31 = matrix.m31;
        double b00 = p.x, b01 = p.y, b02 = p.z;
        double n00 = a00 * b00 + a10 * b01 + a20 * b02 + a30;
        double n01 = a01 * b00 + a11 * b01 + a21 * b02 + a31;
        return new TextureCoordinate((float)n00, (float)n01);
    }

    public void unwrap(double x, double y, double z, float[] out) {
        double a00 = matrix.m00, a10 = matrix.m10, a20 = matrix.m20, a30 = matrix.m30;
        double a01 = matrix.m01, a11 = matrix.m11, a21 = matrix.m21, a31 = matrix.m31;
        double n00 = a00 * x + a10 * y + a20 * z + a30;
        double n01 = a01 * x + a11 * y + a21 * z + a31;
        out[0] = (float)n00;
        out[1] = (float)n01;
    }

    public MatrixMapping copy() {
        return new MatrixMapping(matrix.clone());
    }

    @Override
    public String toString() {
        return "MatrixMapping:\n\t" + (matrix.toString().replace("\n", "\n\t"));
    }
}

