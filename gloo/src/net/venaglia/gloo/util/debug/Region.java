package net.venaglia.gloo.util.debug;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 8/21/14
 * Time: 5:37 PM
 */
public class Region extends AbstractRegion {

    boolean open = true;
    List<Double> points = new ArrayList<Double>();

    Region() {}

    public Region addPoint(double x, double y) {
        if (!open) {
            throw new IllegalStateException("Region is already closed");
        }
        points.add(x);
        points.add(y);
        return this;
    }

    public Region close() {
        if (open) {
            int size = size();
            if (size < 3 && size != 1) {
                throw new IllegalStateException("Too few points: " + size);
            }
            open = false;
        }
        return this;
    }

    public int size() {
        return points.size() >> 1;
    }

    @Override
    boolean isPoint() {
        return !isOpen() && size() == 1;
    }

    boolean isOpen() {
        return open;
    }

    double[] getBounds() {
        double[] result = new double[points.size()];
        for (int i = 0, l = points.size(); i < l; i++) {
            result[i] = points.get(i);
        }
        return result;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0, l = points.size(); i < l; i += 2) {
            if (i > 0) {
                buffer.append("-");
            }
            buffer.append(String.format("(%.4f,%.4f)", points.get(i), points.get(i + 1)));
        }
        return buffer.toString();
    }

    public static Region fromLastElement() {
        return new Region() {
            {
                open = false;
            }

            @Override
            boolean isFromLastElement() {
                return true;
            }

            @Override
            double[] getBounds() {
                return new double[0];
            }
        };
    }
}
