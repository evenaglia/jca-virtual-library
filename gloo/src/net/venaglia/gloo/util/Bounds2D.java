package net.venaglia.gloo.util;

import java.awt.geom.RectangularShape;

/**
 * User: ed
 * Date: 7/15/14
 * Time: 8:06 AM
 */
public abstract class Bounds2D {

    public final double x1;
    public final double y1;
    public final double x2;
    public final double y2;

    private Bounds2D(double x1, double y1, double x2, double y2) {
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
    }

    public abstract boolean includes(double x, double y);

    public static Bounds2D fromRectangle(RectangularShape rectangle) {
        return createRectangle(rectangle.getMinX(),
                               rectangle.getMinY(),
                               rectangle.getMaxX(),
                               rectangle.getMaxY());
    }

    public static Bounds2D createRectangle(double x1, double y1, double x2, double y2) {
        return new Bounds2D(x1, y1, x2, y2) {
            @Override
            public boolean includes(double x, double y) {
                return x1 <= x && x2 >= x &&
                       y1 <= y && y2 >= y;
            }
        };
    }

    public static Bounds2D createCircle(final double x, final double y, double r) {
        final double r2 = r * r;
        return new Bounds2D(x - r, y - r, x + r, x + r) {
            @Override
            public boolean includes(double i, double j) {
                double a = i - x, b = j - y;
                return a * a + b * b <= r2;
            }
        };
    }
}
