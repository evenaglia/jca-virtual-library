package net.venaglia.gloo.util.surfaceFn;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.ZMap;
import net.venaglia.gloo.physical.geom.complex.Mesh;

import java.awt.*;
import java.nio.DoubleBuffer;

/**
 * User: ed
 * Date: 2/19/13
 * Time: 4:54 PM
 */
public class NurbsFn extends AbstractFn {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final int cols;
    private final int rows;
    private final double[] z;

    public NurbsFn(double x1, double y1, double x2, double y2, int cols, int rows, double[] z) {
        this.x1 = Math.min(x1, x2);
        this.x2 = Math.max(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.y2 = Math.max(y1, y2);
        this.cols = cols;
        this.rows = rows;
        this.z = z;
        if (x2 - x1 < 3 || y2 - y1 < 3) {
            throw new IllegalArgumentException("Size is too small, width and height must be at least 3: width = " + (x2 - x1) + ", height = " + (y2 - y1));
        }
        if (rows < 3 || cols < 3) {
            throw new IllegalArgumentException("Size is too small, rows and cols must be at least 3: rows = " + rows + ", cols = " + cols);
        }
        if (z.length != rows * cols) {
            throw new IllegalArgumentException("z does not contain the expected number of elements: expected = " + (rows * cols) + ", actual = " + z.length);
        }
    }

    public double getZ(double a, double b) {
        double x = 1.0 - (x2 - a) / (x2 - x1);
        double y = 1.0 - (y2 - b) / (y2 - y1);
        if (x < 0.0 || x > 1.0 || y < 0.0 || y > 1.0) {
            throw new IllegalArgumentException(String.format("Requested point (%5.3f,%5.3f) does not lie within (%5.3f,%5.3f)-(%5.3f,%5.3f)", a, b, x1, y1, x2, y2));
        }
        double[] buffer = new double[rows];
        double[] midpoints = new double[cols];
        for (int i = 0; i < cols; i++) {
            midpoints[i] = reduce(y, stripe(i, cols, buffer));
        }
        return reduce(x, midpoints);
//        int i = Math.min(cols - 1, (int)Math.floor(x * cols));
//        int j = Math.min(rows - 1, (int)Math.floor(y * rows));
//        return z[i + j * cols];
    }

    private double[] stripe(int base, int step, double[] stripe) {
        int count = stripe.length;
        for (int i = 0, j = base; i < count; i++, j += step) {
            stripe[i] = z[j];
        }
        return stripe;
    }

    private double reduce(double p, double[] v) {
        assert p >= 0.0 && p <= 1.0;
        double q = 1.0 - p;
        int l = v.length - 1;
        for (int i = 0; i < l; i++) {
            for (int j = l; j > i; j--) {
                v[j] = v[j - 1] * q + v[j] * p;
            }
        }
        return v[l];
    }

    public Mesh getControlPoints() {
        Point[] points = new Point[cols * rows];
        Vector[] normals = new Vector[cols * rows];
        double dx = (x2 - x1) / (cols - 1);
        double dy = (y2 - y1) / (rows - 1);
        int idx = 0;
        DoubleBuffer z = DoubleBuffer.wrap(this.z);
        ZMap zMap = new ZMap(z, new Rectangle(cols, rows));
        for (int j = 0; j < rows; j++) {
            double y = y1 + dy * j;
            for (int i = 0; i < cols; i++) {
                double x = x1 + dx * i;
                points[idx] = new Point(x, y, this.z[i + j * rows]);
                normals[idx++] = zMap.getNormal(i, j);
            }
        }
        return new Mesh(cols, rows, points, normals);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(rows * cols * 7 + rows * 2 + 30);
        buffer.append(String.format("(%5.3f,%5.3f)-(%5.3f,%5.3f)\n", x1, y1, x2, y2));
        for (int i = 0, j = 0; i < rows; i++, j += rows) {
            if (i > 0) buffer.append("\n");
            buffer.append("[");
            for (int k = 0; k < cols; k++) {
                if (k > 0) buffer.append(",");
                buffer.append(String.format("%6.3f", z[j + k]));
            }
            buffer.append("]");
        }
        return buffer.toString();
    }
}
