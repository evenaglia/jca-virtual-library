package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.util.SpatialMap;

import java.util.Comparator;

/**
 * User: ed
 * Date: 9/20/12
 * Time: 11:07 PM
 */
public class SpatialMapEntryComparator implements Comparator<SpatialMap.Entry<Object>> {

    public static final Comparator<SpatialMap.Entry<Object>> ORDER_BY_X = new SpatialMapEntryComparator(Axis.X);
    public static final Comparator<SpatialMap.Entry<Object>> ORDER_BY_Y = new SpatialMapEntryComparator(Axis.Y);
    public static final Comparator<SpatialMap.Entry<Object>> ORDER_BY_Z = new SpatialMapEntryComparator(Axis.Z);

    private final Axis axis;

    private SpatialMapEntryComparator(Axis axis) {
        this.axis = axis;
    }

    public int compare(SpatialMap.Entry<Object> a, SpatialMap.Entry<Object> b) {
        return Double.compare(a.getAxis(axis), b.getAxis(axis));
    }

    public static Comparator<SpatialMap.Entry<Object>> forAxis(Axis axis) {
        switch (axis) {
            case X:
                return ORDER_BY_X;
            case Y:
                return ORDER_BY_Y;
            case Z:
                return ORDER_BY_Z;
        }
        return null;
    }
}
