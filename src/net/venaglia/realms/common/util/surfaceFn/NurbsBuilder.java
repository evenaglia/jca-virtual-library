package net.venaglia.realms.common.util.surfaceFn;

import static net.venaglia.realms.common.physical.geom.ZMap.Fn;

/**
 * User: ed
 * Date: 2/19/13
 * Time: 8:38 PM
 */
public class NurbsBuilder {

    private double x1 = Double.MAX_VALUE;
    private double y1 = Double.MAX_VALUE;
    private double x2 = -Double.MAX_VALUE;
    private double y2 = -Double.MAX_VALUE;
    private int cols = -1;
    private int rows = -1;
    private boolean composite = false;
    private Fn fn;

    public NurbsBuilder setBounds(double x1, double y1, double x2, double y2) {
        this.x1 = Math.min(x1, x2);
        this.x2 = Math.max(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.y2 = Math.max(y1, y2);
        return this;
    }

    public NurbsBuilder setDivisions(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        return this;
    }

    public NurbsBuilder setComposite(boolean composite) {
        this.composite = composite;
        return this;
    }

    public NurbsBuilder setFn(Fn fn) {
        this.fn = fn;
        return this;
    }

    public AbstractFn build() {
        validate();
        if (!composite) {
            return buildNurbs(x1, y1, x2, y2, cols, rows);
        }
        Fn[] delegates = new Fn[cols * rows];
        int k = 0;
        double dx = (x2 - x1) / cols;
        double dy = (y2 - y1) / rows;
        for (int j = 0; j < rows; j++) {
            double b1 = dy * j + y1;
            double b2 = b1 + dy;
            for (int i = 0; i < cols; i++) {
                double a1 = dx * i + x1;
                double a2 = a1 + dx;
                delegates[k++] = buildNurbs(a1, b1, a2, b2, 5, 5);
            }
        }
        return new CompositeFn(x1, y1, x2, y2, rows, cols, delegates);
    }

    private NurbsFn buildNurbs(double x1, double y1, double x2, double y2, int cols, int rows) {
        double[] z = new double[(cols + 2) * (rows + 2)];
        int k = 0;
        double dx = (x2 - x1) / (double)(cols - 1);
        double dy = (y2 - y1) / (double)(rows - 1);
        for (int j = -1; j <= rows; j++) {
            double y = j * dy + y1;
            for (int i = -1; i <= cols; i++) {
                double x = i * dx + x1;
                z[k++] = fn.getZ(x, y);
            }
        }
        for (int i = 1; i < rows; i++) {
            smooth(z, cols, i, 1, 0, 1);
            smooth(z, cols, i, rows, 0, -1);
        }
        for (int j = 1; j < cols; j++) {
            smooth(z, cols, 1, j, 0, 1);
            smooth(z, cols, cols, j, 0, -1);
        }
        smooth(z, cols, 1, 1, 1, 1);
        smooth(z, cols, cols, 1, -1, 1);
        smooth(z, cols, 1, rows, 1, -1);
        smooth(z, cols, cols, rows, -1, -1);
        double[] zCrop = new double[cols * rows];
        k = 0;
        for (int i = 0, j = cols + 2; i < rows; i++, j += cols + 2, k += cols) {
            System.arraycopy(z, j, zCrop, k, cols);
        }
        return new NurbsFn(x1, y1, x2, y2, cols, rows, zCrop);
    }

    private void smooth(double[] z, int cols, int xAnchor, int yAnchor, int dx, int dy) {
        int idxAnchor = xAnchor + yAnchor * cols;
        int idxDelta = dx + dy * cols;
        int idxInner = idxAnchor + idxDelta;
        int idxOuter = idxAnchor - idxDelta;
        double zIdeal = z[idxAnchor] * 2.0 - z[idxOuter];
//        z[idxInner] = (z[idxInner] + zIdeal) * 0.5;
    }

    private void validate() {
        if (x1 > x2 && y1 > y2) {
            throw new IllegalStateException("No coordinates have been specified");
        }
        if (x1 >= x2 || y1 >= y2) {
            throw new IllegalStateException(String.format("Invalid bounds: (%5.3f,%5.3f)-(%5.3f,%5.3f)", x1, y1, x2, y2));
        }
        if (rows == -1 && cols == -1) {
            throw new IllegalStateException("Row and column divisions have not been specified");
        }
        if (composite) {
            if (rows < 2 || cols < 2) {
                throw new IllegalStateException("Invalid number of row or column divisions for composite nurbs: rows = " + rows + ", cols = " + cols);
            }
        } else {
            if (rows < 4 || cols < 4) {
                throw new IllegalStateException("Invalid number of row or column divisions for simple nurbs: rows = " + rows + ", cols = " + cols);
            }
        }
        if (fn == null) {
            throw new IllegalStateException("No function has been specified");
        }
    }
}
