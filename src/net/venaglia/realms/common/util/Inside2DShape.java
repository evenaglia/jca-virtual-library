package net.venaglia.realms.common.util;

import net.venaglia.realms.common.util.debug.OutputGraph;

import java.awt.*;

/**
 * User: ed
 * Date: 1/2/13
 * Time: 10:09 PM
 */
public class Inside2DShape {

    public static final Inside2DShape INSTANCE = new Inside2DShape();

    private Inside2DShape() {
        // singleton
    }

    public boolean test(double... coordinates) {
        return countWinding(false, coordinates) % 2 == 1;
    }

    private int countWinding(boolean debug, double... coordinates) {
        assert coordinates.length >= 6;
        assert coordinates.length % 2 == 0;
        double a = coordinates[0];
        double b = coordinates[1];
        int crossings = 0;
        for (int i = 1, l = coordinates.length >> 1; i <= l; i++) {
            double c = i == l ? coordinates[0] : coordinates[i * 2];
            double d = i == l ? coordinates[1] : coordinates[i * 2 + 1];
            if (debug) {
                testing(a, b, c, d);
            }
            if ((a < 0 && c <= 0) || (b > 0 && d >= 0) || (b < 0 && d <= 0) || (b == d)) {
                // does not cross
                if (debug) {
                    doesNotCross("both ends above, below, or to the left");
                }
            } else if (a > 0 && c > 0) {
                crossings++;
                if (debug) {
                    crosses(a, b, c, d);
                }
            } else {
                double pct = b / (b - d);
                double atZero = c * pct + a * (1.0 - pct);
                if (atZero > 0) {
                    crossings++;
                    if (debug) {
                        crosses(atZero);
                    }
                } else {
                    if (debug) {
                        doesNotCross("does not cross the positive X axis");
                    }
                }
            }
            a = c;
            b = d;
        }
        if (debug) {
            totalCrossings(crossings);
        }
        return crossings;
    }

    protected void testing(double a, double b, double c, double d) {
    }

    protected void doesNotCross(String s) {
    }

    protected void crosses(double v) {
    }

    protected void crosses(double a, double b, double c, double d) {
    }

    protected void totalCrossings(int crossings) {
    }

    public static void main(String[] args) {
        final OutputGraph out = new OutputGraph("Inside2DShape", 1024, 0, 0, 250.0);
        Inside2DShape i2ds = new Inside2DShape() {

            double a, b, c, d;

            @Override
            protected void testing(double a, double b, double c, double d) {
                this.a = a;
                this.b = b;
                this.c = c;
                this.d = d;
            }

            @Override
            protected void doesNotCross(String s) {
                out.addLine(Color.gray, a, b, c, d);
            }

            @Override
            protected void crosses(double v) {
                out.addPoint(null, null, v, 0.0);
                out.addLine(Color.green, a, b, c, d);
            }

            @Override
            protected void crosses(double a, double b, double c, double d) {
                out.addLine(Color.green, a, b, c, d);
            }

            @Override
            protected void totalCrossings(int crossings) {
                out.addLabel(crossings % 2 == 1 ? Color.green : Color.red, String.valueOf(crossings), 0, -0.05);
            }
        };
        out.addPoint(Color.yellow, null, 0, 0);
        out.addArrow(Color.yellow, 0, 0, 3, 0);
        i2ds.countWinding(true, 0.1, -1, 1.5, -1, -0.7, 0.2, 0.1, 0.2);
    }
}
