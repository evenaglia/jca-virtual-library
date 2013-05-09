package net.venaglia.realms.common.physical.geom;

import net.venaglia.realms.common.util.surfaceFn.NurbsBuilder;

import java.awt.*;
import java.nio.DoubleBuffer;

/**
 * User: ed
 * Date: 2/1/13
 * Time: 5:33 PM
 */
public class ZMap {

    private final DoubleBuffer z;
    private final int baseX;
    private final int baseY;
    private final int width;
    private final int height;

    public ZMap(Rectangle rectangle) {
        if (rectangle.width < 2 || rectangle.height < 2) {
            throw new IllegalArgumentException("Invalid dimensions: " + rectangle.width + " x " + rectangle.height);
        }
        this.baseX = rectangle.x;
        this.baseY = rectangle.y;
        this.width = rectangle.width;
        this.height = rectangle.height;
        this.z = DoubleBuffer.allocate(this.width * this.height);
    }

    public ZMap(DoubleBuffer z, Rectangle rectangle) {
        if (rectangle.width < 2 || rectangle.height < 2) {
            throw new IllegalArgumentException("Invalid dimensions: " + rectangle.width + " x " + rectangle.height);
        }
        this.baseX = rectangle.x;
        this.baseY = rectangle.y;
        this.width = rectangle.width;
        this.height = rectangle.height;
        this.z = z;
        int minLen = width * height;
        if (z.limit() < minLen) {
            throw new IllegalArgumentException("Buffer is too small, should contain " + minLen + " elements");
        }
        if (z.limit() > minLen) {
            throw new IllegalArgumentException("Buffer is too large, should contain " + minLen + " elements");
        }
    }

    public void load(Fn fn) {
        int i = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                z.put(i++, fn.getZ(x + baseX, y + baseY));
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public ZMap duplicate() {
        return new ZMap(z.duplicate(), new Rectangle(baseX, baseX, width, height));
    }

    public Vector getNormal(int x, int y) {
        double[] v = { 0.0, 0.0, 0.0 };
        getNormalImpl(x, y, x - baseX, y - baseY, 1.0, v, 0);
        return new Vector(v[0], v[1], v[2]);
    }

    private void getNormalImpl(double a, double b, int x, int y, double length, double[] v, int vi) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            String msg;
            if (a == (int)a && b == (int)b) {
                msg = String.format("(%d,%d) does not lie within (%d,%d)-(%d,%d)",
                                    (int)a, (int)b, baseX, baseY, baseX + width - 1, baseY + height - 1);
            } else {
                msg = String.format("(%5.3f,%5.3f) does not lie within (%d,%d)-(%d,%d)",
                                    a, b, baseX, baseY, baseX + width - 1, baseY + height - 1);
            }
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        if (length == 0.0) {
            v[vi    ] = 0.0;
            v[vi + 1] = 0.0;
            v[vi + 2] = 0.0;
            return;
        }
        int idx = y * width + x;
        double w0 = z.get(idx);
        double w1 = x > 0 ? z.get(idx - 1) - w0: Double.MIN_VALUE;
        double w2 = y > 0 ? z.get(idx - width) - w0 : Double.MIN_VALUE;
        double w3 = x < width - 1 ? z.get(idx + 1) - w0 : Double.MIN_VALUE;
        double w4 = y < height - 1 ? z.get(idx + width) - w0 : Double.MIN_VALUE;
        accumulateNormal(v, vi, -1,  0, w1,  0, -1, w2);
        accumulateNormal(v, vi,  0, -1, w2,  1,  0, w3);
        accumulateNormal(v, vi,  1,  0, w3,  0,  1, w4);
        accumulateNormal(v, vi,  0,  1, w4, -1,  0, w1);
        // normalize
        double l = length / Vector.computeDistance(v[vi], v[vi + 1], v[vi + 2]);
        v[vi    ] *= l;
        v[vi + 1] *= l;
        v[vi + 2] *= l;
    }

    public Vector getNormal(double x, double y) {
        int x1 = (int)Math.floor(x) - baseX;
        int x2 = (int)Math.ceil(x) - baseX;
        int y1 = (int)Math.floor(y) - baseY;
        int y2 = (int)Math.ceil(y) - baseY;
        if (x1 < 0 || x2 >= width || y1 < 0 || y2 >= height) {
            String msg = String.format("(%5.3f,%5.3f) does not lie within (%d,%d)-(%d,%d)",
                                          x, y, baseX, baseY, baseX + width - 1, baseY + height - 1);
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        if (x1 == x2 && y1 == y2) {
            double[] v = { 0.0, 0.0, 0.0 };
            getNormalImpl(x, y, x1, y1, 1.0, v, 0);
            return new Vector(v[0], v[1], v[2]);
        }
        double i = x2 - x;
        double j = y2 - y;
        double k = 1.0 - i;
        double l = 1.0 - j;
        double[] v = { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
        getNormalImpl(x, y, x1, y1, i * j, v, 0);
        getNormalImpl(x, y, x2, y1, k * j, v, 3);
        getNormalImpl(x, y, x1, y2, i * l, v, 6);
        getNormalImpl(x, y, x2, y2, k * l, v, 9);
        return new Vector(v[0] + v[3] + v[6] + v[9],
                          v[1] + v[4] + v[7] + v[10],
                          v[2] + v[5] + v[8] + v[11]).normalize();
    }

    private void accumulateNormal(double[] v, int vi, double x1, double y1, double z1, double x2, double y2, double z2) {
        if (z1 != Double.MIN_VALUE && z2 != Double.MIN_VALUE) {
            v[vi    ] += y1 * z2 - z1 * y2;
            v[vi + 1] += z1 * x2 - x1 * z2;
            v[vi + 2] += x1 * y2 - y1 * x2;
        }
    }

    public double getZ(int a, int b) {
        int x = a - baseX;
        int y = b - baseY;
        if (x < 0 || x >= width || y < 0 || y >= height) {
            String msg = String.format("(%d,%d) does not lie within (%d,%d)-(%d,%d)",
                                          a, b, baseX, baseY, baseX + width - 1, baseY + height - 1);
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        return z.get(y * width + x);
    }

    public void setZ(int a, int b, double z) {
        int x = a - baseX;
        int y = b - baseY;
        if (x < 0 || x >= width || y < 0 || y >= height) {
            String msg = String.format("(%d,%d) does not lie within (%d,%d)-(%d,%d)",
                                          a, b, baseX, baseY, baseX + width - 1, baseY + height - 1);
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        this.z.put(y * width + x, z);
    }

    public double getZ(double a, double b) {
        double x = a - baseX;
        double y = b - baseY;
        int x1 = (int)Math.floor(x);
        int x2 = (int)Math.ceil(x);
        int y1 = (int)Math.floor(y);
        int y2 = (int)Math.ceil(y);
        if (x1 == x2 && y1 == y2) {
            if (x1 < 0 || x2 >= width || y1 < 0 || y2 >= height) {
                String msg = String.format("(%d,%d) does not lie within (%d,%d)-(%d,%d)",
                                           (int)a, (int)b, baseX, baseY, baseX + width - 1, baseY + height - 1);
                throw new ArrayIndexOutOfBoundsException(msg);
            }
            return z.get(y1 * width + x1);
        }
        if (x1 < 0 || x2 >= width || y1 < 0 || y2 >= height) {
            String msg = String.format("(%5.3f,%5.3f) does not lie within (%d,%d)-(%d,%d)",
                                       a, b, baseX, baseY, baseX + width - 1, baseY + height - 1);
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        double i = ((double)x2) - x;
        double j = ((double)y2) - y;
        double k = 1.0 - i;
        double l = 1.0 - j;
        int idx = y1 * width + x1;
        int dx = x2 - x1;
        int dy = (y2 - y1) * width;
        a = z.get(idx);
        b = z.get(idx + dx);
        double c = z.get(idx + dy);
        double d = z.get(idx + dy + dx);
        return (a * i * j + b * k * j + c * i * l + d * k * l);
    }

    public ZMap clip(int a1, int b1, int a2, int b2) {
        int x1 = a1 - baseX;
        int y1 = b1 - baseY;
        int x2 = a2 - baseX;
        int y2 = b2 - baseY;
        if (x1 > x2) {
            int t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y1 > y2) {
            int t = y1;
            y1 = y2;
            y2 = t;
        }
        if (x1 < 0 || y1 < 0 || x2 >= width || y2 >= height) {
            String msg = String.format(
                    "Clipped region extends beyond this ZMap: (%d,%d)-(%d-%d) does not lie within (%d,%d)-(%d,%d)",
                    a1, b1, a2, b2, baseX, baseY, baseX + width - 1, baseY + height - 1);
            throw new IllegalArgumentException(msg);
        }
        int w = x2 - x1 + 1;
        int h = y2 - y1 + 1;
        if (w < 2 || h < 2) {
            String msg = String.format("Clipped region is too small: (%d,%d)-(%d-%d)", a1, b1, a2, b2);
            throw new IllegalArgumentException(msg);
        }
        DoubleBuffer b = DoubleBuffer.allocate(w * h);
        double[] line = new double[w];
        for (int i = y1 * width + x1, j = 0; j < h; i += width, j++) {
            z.position(i);
            z.get(line);
            b.put(line);
        }
        b.clear();
        return new ZMap(b, new Rectangle(a1, b1, w, h));
    }

    public Source asSource() {
        return new Source() {
            public ZMap getZMap(int x1, int y1, int x2, int y2) {
                return clip(x1, y1, x2, y2);
            }
        };
    }

    public Point[] toPoints() {
        Point[] points = new Point[width * height];
        z.clear();
        int k = 0;
        for (int j = 0, y = baseY; j < height; j++, y++) {
            for (int i = 0, x = baseX; i < width; i++, x++) {
                points[k++] = new Point(x, y, z.get());
            }
        }
        return points;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d)-(%d,%d)", baseX, baseY, baseX + width - 1, baseY + height - 1);
    }

    public interface Fn {
        double getZ(double x, double y);
        NurbsBuilder buildNurbs();
    }

    public interface Source {
        ZMap getZMap(int x1, int y1, int x2, int y2);
    }
}
