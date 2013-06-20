package net.venaglia.gloo.util.surfaceFn;

/**
 * User: ed
 * Date: 2/12/13
 * Time: 9:57 PM
 */
public class SpiralSine extends AbstractFn {

    private final double scale;
    private final double twist;
    private final int spokes;

    public SpiralSine(double scale, double twist, int spokes) {
        if (spokes < 1) {
            throw new IllegalArgumentException("Spokes must be greater than 0: " + spokes);
        }
        this.scale = scale;
        this.twist = twist;
        this.spokes = spokes;
    }

    public double getZ(double x, double y) {
        double r = Math.sqrt(x * x + y * y);
        double a = Math.atan2(x, y) * spokes + r * twist;
        return Math.sin(a) * scale * r;
    }
}
