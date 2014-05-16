package net.venaglia.gloo.util.impl;

import net.venaglia.gloo.util.SpatialMap;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 1:49 PM
 */
public abstract class AbstractMutableEntry<S> extends AbstractBasicEntry<S> implements SpatialMap.Entry<S> {

    protected AbstractMutableEntry(double x, double y, double z) {
        super(x, y, z);
    }
}
