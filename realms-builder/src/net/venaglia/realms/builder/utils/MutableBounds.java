package net.venaglia.realms.builder.utils;

import net.venaglia.gloo.physical.bounds.MutableSimpleBounds;
import net.venaglia.gloo.physical.bounds.SimpleBoundingVolume;
import net.venaglia.realms.spec.map.GeoPoint;

/**
* User: ed
* Date: 9/6/14
* Time: 2:14 AM
*/
public class MutableBounds extends MutableSimpleBounds implements SimpleBoundingVolume {

    public MutableBounds() {
    }

    public MutableBounds(boolean spherical, double radius) {
        super(spherical, radius);
    }

    public MutableBounds load(GeoPoint geoPoint) {
        double c = Math.cos(geoPoint.latitude);
        double x = Math.sin(geoPoint.longitude) * c;
        double y = Math.cos(geoPoint.longitude) * c;
        double z = Math.sin(geoPoint.latitude);
        load(x * 1000.0, y * 1000.0, z * 1000.0);
        return this;
    }

}
