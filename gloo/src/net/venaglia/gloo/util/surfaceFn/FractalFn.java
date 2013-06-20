package net.venaglia.gloo.util.surfaceFn;

import java.util.Arrays;
import java.util.Random;

/**
 * User: ed
 * Date: 2/17/13
 * Time: 9:15 AM
 */
public class FractalFn extends AbstractFn {

    private final Random rnd;
    private final int size;
    private final double stdDeviation;
    private final int baseX;
    private final int baseY;
    private final double[] z;
    private boolean initialized;

    public FractalFn(int size, long seed, double stdDeviation, int baseX, int baseY) {
        this.size = size;
        this.stdDeviation = stdDeviation;
        this.baseX = baseX;
        this.baseY = baseY;
        this.rnd = new Random(seed);
        this.z = new double[size * size];
        Arrays.fill(this.z, Double.NaN);
    }

    private void init() {
        if (!initialized) {
            initialized = true;
            setZ(0, 0, rnd.nextGaussian() * stdDeviation);
            setZ(0, size - 1, rnd.nextGaussian() * stdDeviation);
            setZ(size - 1, 0, rnd.nextGaussian() * stdDeviation);
            setZ(size - 1, size - 1, rnd.nextGaussian() * stdDeviation);
            render(0, 0, size - 1, size - 1, stdDeviation * 0.5, true);
        }
    }

    private void render(int x1, int y1, int x3, int y3, double stdDeviation, boolean allowRecursion) {
        int x2 = (x1 + x3) >> 1;
        int y2 = (y1 + y3) >> 1;
        if (x1 == x2 && y1 == y2) {
            return;
        }
        double z11 = getZImpl(x1, y1);
        double z12;
        double z13 = getZImpl(x1, y3);
        double z21;
        double z22; // not actually used
        double z23;
        double z31 = getZImpl(x3, y1);
        double z32;
        double z33 = getZImpl(x3, y3);
        z21 = genZ(x2, y1, (z11 + z31) * 0.5, stdDeviation);
        z23 = genZ(x2, y3, (z13 + z33) * 0.5, stdDeviation);
        z12 = genZ(x1, y2, (z11 + z13) * 0.5, stdDeviation);
        z32 = genZ(x3, y2, (z31 + z33) * 0.5, stdDeviation);

        double newDeviation = stdDeviation * 0.55;
        if (!hasZ(x2, y2)) {
            double base = (z11 + z13 + z31 + z33) * 0.10 +
                          (z21 + z12 + z23 + z32) * 0.15;
            setZ(x2, y2, rnd.nextGaussian() * stdDeviation + base);
            if (allowRecursion) {
                render(x1, y1, x2, y2, newDeviation, false);
                render(x2, y1, x3, y2, newDeviation, false);
                render(x1, y2, x2, y3, newDeviation, false);
                render(x2, y2, x3, y3, newDeviation, false);
            }
        }
        if (allowRecursion) {
            render(x1, y1, x2, y2, newDeviation, true);
            render(x2, y1, x3, y2, newDeviation, true);
            render(x1, y2, x2, y3, newDeviation, true);
            render(x2, y2, x3, y3, newDeviation, true);
        }
    }

    private double genZ(int x, int y, double baseZ, double stdDeviation) {
        double z;
        if (!hasZ(x, y)) {
            z = rnd.nextGaussian() * stdDeviation + baseZ;
            setZ(x, y, z);
        } else {
            z = getZImpl(x, y);
        }
        return z;
    }

    public double getZ(double a, double b) {
        init();
        int a1 = (int)Math.floor(a);
        int a2 = (int)Math.ceil(a);
        int b1 = (int)Math.floor(b);
        int b2 = (int)Math.ceil(b);
        if (a1 == a2 && b1 == b2) {
            return getZ(a1, b1);
        }
        int x1 = a1 - baseX;
        int x2 = a2 - baseX;
        int y1 = b1 - baseY;
        int y2 = b2 - baseY;
        if (x1 < 0 || x2 >= size || y1 < 0 || y2 >= size)  {
            String msg = String.format("Point (%5.3f,%5.3f) does not lie within (%d,%d)-(%d,%d)",
                                       a, b, baseX, baseY, baseX + size - 1, baseY + size - 1);
            throw new ArrayIndexOutOfBoundsException(msg);
        }

        double i1 = a2 - a;
        double i2 = 1.0 - i1;
        double j1 = b2 - b;
        double j2 = 1.0 - j1;
        return getZImpl(x1, y1) * i1 * j1 + getZImpl(x2, y1) * i2 * j1 + getZImpl(x1, y2) * i1 * j2 + getZImpl(x2, y2) * i1 * j2;
    }

    public double getZ(int a, int b) {
        int x = a - baseX;
        int y = b - baseY;
        if (x < 0 || x >= size || y < 0 || y >= size)  {
            String msg = String.format("Point (%d,%d) does not lie within (%d,%d)-(%d,%d)",
                                       a, b, baseX, baseY, baseX + size - 1, baseY + size - 1);
            throw new ArrayIndexOutOfBoundsException(msg);
        }
        return getZImpl(x, y);
    }

    private double getZImpl(int x, int y) {
        return z[x + y * size];
    }

    private void setZ(int x, int y, double z) {
        this.z[x + y * size] = z;
    }

    private boolean hasZ(int x, int y) {
        return !Double.isNaN(this.z[x + y * size]);
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(size * size * 7 + size * 2);
        for (int i = 0, j = 0; i < size; i++, j += size) {
            if (i > 0) buffer.append("\n");
            buffer.append("[");
            for (int k = 0; k < size; k++) {
                if (k > 0) buffer.append(",");
                buffer.append(String.format("%6.3f", z[j + k]));
            }
            buffer.append("]");
        }
        return buffer.toString();
    }
}
