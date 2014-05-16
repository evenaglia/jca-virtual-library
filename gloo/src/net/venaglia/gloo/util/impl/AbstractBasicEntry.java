package net.venaglia.gloo.util.impl;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.util.BasicSpatialMap;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 1:49 PM
 */
public abstract class AbstractBasicEntry<S> implements BasicSpatialMap.BasicEntry<S> {

    protected double x;
    protected double y;
    protected double z;

    protected AbstractBasicEntry(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getAxis(Axis axis) {
        return axis.of(x,y,z);
    }

    @Override
    public String toString() {
        return String.format("Entry[%.4f,%.4f,%.4f] -> (%s)", x, y, z, get());
    }
}
