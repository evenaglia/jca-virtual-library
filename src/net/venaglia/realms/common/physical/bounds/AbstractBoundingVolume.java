package net.venaglia.realms.common.physical.bounds;

/**
 * User: ed
 * Date: 9/10/12
 * Time: 8:16 PM
 */
public abstract class AbstractBoundingVolume<T extends AbstractBoundingVolume<T>> implements BoundingVolume<T> {

    private final Type bestFit;

    protected AbstractBoundingVolume(Type bestFit) {
        this.bestFit = bestFit;
    }

    protected static boolean between(double min, double v, double max) {
        return min <= v && v <= max;
    }

    public BoundingVolume<?> getBounds() {
        return this;
    }

    public Type getBestFit() {
        return bestFit;
    }

    public BoundingVolume<?> asBestFit() {
        return bestFit.cast(this);
    }
}
