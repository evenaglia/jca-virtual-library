package net.venaglia.realms.common.util.surfaceFn;

import static net.venaglia.realms.common.physical.geom.ZMap.Fn;

import net.venaglia.realms.common.physical.geom.ZMap;
import net.venaglia.realms.common.util.Series;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: ed
 * Date: 2/19/13
 * Time: 8:49 PM
 */
public class CompositeFn extends AbstractFn implements Series<Fn> {

    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;
    private final int rows;
    private final int cols;
    private final Fn[] delegates;
    private final double dx;
    private final double dy;

    public CompositeFn(double x1, double y1, double x2, double y2, int rows, int cols, Fn[] delegates) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.rows = rows;
        this.cols = cols;
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException("Rows and cols must both be at least 1: rows = " + rows + ", cols = " + cols);
        }
        if (delegates.length != rows * cols) {
            throw new IllegalArgumentException("Number of delegates does not match what is expected: expected = " + (rows * cols) + ", actual = " + delegates.length);
        }
        this.delegates = delegates;
        this.dx = (x2 - x1) / cols;
        this.dy = (y2 - y1) / rows;
    }

    public double getZ(double a, double b) {
        int x = a == x2 ? cols - 1 : (int)Math.floor((a - x1) / dx);
        int y = b == y2 ? rows - 1 : (int)Math.floor((b - y1) / dy);
        if (x < 0 || x >= cols || y < 0 || y >= rows) {
            String msg = String.format("Passed point (%5.3f,%5.3f) does not lie within (%5.3f,%5.3f)-(%5.3f,%5.3f)",
                                       a, b, x1, y1, x2, y2);
            throw new IllegalArgumentException(msg);
        }
        return delegates[x + y * cols].getZ(a, b);
    }

    public int size() {
        return delegates.length;
    }

    public Iterator<Fn> iterator() {
        return Collections.unmodifiableList(Arrays.asList(delegates)).iterator();
    }

    @Override
    public String toString() {
        Pattern pattern = Pattern.compile("^", Pattern.MULTILINE);
        StringBuilder buffer = new StringBuilder();
        buffer.append("CompositeFunction{");
        int k = 0;
        for (int j = 0; j < rows; j++) {
            double b1 = y1 + dy * j;
            double b2 = b1 + dy;
            for (int i = 0; i < cols; i++) {
                double a1 = x1 + dx * i;
                double a2 = a1 + dx;
                buffer.append(String.format("\n\t(%5.3f,%5.3f)-(%5.3f,%5.3f):\n", a1, b1, a2, b2));
                Matcher matcher = pattern.matcher(delegates[k++].toString());
                buffer.append(matcher.replaceAll("\t\t"));
            }
        }
        buffer.append("\n}");
        return buffer.toString();
    }
}
