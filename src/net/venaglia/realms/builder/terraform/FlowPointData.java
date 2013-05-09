package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.builder.map.GeoPoint;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:09 AM
 */
public class FlowPointData {

    protected GeoPoint geoPoint;
    protected Point point;
    protected Vector magnitude;
    protected Vector direction;
    protected double velocity;
    protected double pressure; // as an exponent, 0 represents average pressure across the globe

    public GeoPoint getGeoPoint() {
        return geoPoint;
    }

    public Point getPoint() {
        return point;
    }

    public Vector getMagnitude() {
        return magnitude;
    }

    public Vector getDirection() {
        return direction;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getPressure() {
        return pressure;
    }
}
