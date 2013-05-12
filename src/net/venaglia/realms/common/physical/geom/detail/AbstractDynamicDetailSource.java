package net.venaglia.realms.common.physical.geom.detail;

import net.venaglia.realms.common.physical.bounds.Bounded;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.projection.Projectable;

/**
 * User: ed
 * Date: 5/11/13
 * Time: 8:55 PM
 */
public abstract class AbstractDynamicDetailSource<T extends Projectable & Bounded> implements DynamicDetailSource<T> {

    private final float sizeFactor;

    protected AbstractDynamicDetailSource() {
        this(0.5f);
    }

    protected AbstractDynamicDetailSource(float sizeFactor) {
        this.sizeFactor = sizeFactor;
    }

    protected final DetailLevel limitTo(DetailLevel requested, DetailLevel max) {
        return requested.compareTo(max) < 0 ? requested : max;
    }

    @Override
    public float getSizeFactor() {
        return sizeFactor;
    }

    @Override
    public Shape<?> getTarget() {
        return null; // compute default target from shape
    }
}
