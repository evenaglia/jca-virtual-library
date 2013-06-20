package net.venaglia.gloo.util.surfaceFn;

/**
 * User: ed
 * Date: 2/13/13
 * Time: 8:10 AM
 */
public class SineWave extends AbstractFn {

    private final double i, j;
    private final double frequency;
    private final double amplitude;

    public SineWave(double angle, double wavelength, double amplitude) {
        this.frequency = Math.PI / wavelength;
        this.amplitude = amplitude;
        this.i = Math.sin(angle);
        this.j = -Math.cos(angle);
    }

    public double getZ(double x, double y) {
        double a = (x * i + y * j) * frequency;
        return Math.sin(a) * amplitude;
    }
}
