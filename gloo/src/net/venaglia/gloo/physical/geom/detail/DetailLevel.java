package net.venaglia.gloo.physical.geom.detail;

/**
 * User: ed
 * Date: 4/24/13
 * Time: 9:33 PM
 */
public enum DetailLevel {
    LOW(4), MEDIUM_LOW(8), MEDIUM(16), MEDIUM_HIGH(32), HIGH(64);

    public final int steps;
    public final double fraction;

    DetailLevel(int steps) {
        this.steps = steps;
        this.fraction = 1.0 / (steps - 1);
    }

    public DetailLevel less(int n) {
        int o = ordinal() - n;
        return o >= 0 ? values()[o] : null;
    }

    public DetailLevel more(int n) {
        int o = ordinal() + n;
        return o < 5 ? values()[o] : HIGH;
    }
}
