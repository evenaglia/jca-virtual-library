package net.venaglia.realms.spec.map;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.realms.spec.GeoSpec;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * User: ed
 * Date: 1/2/13
 * Time: 7:57 AM
 *
 * Compound object used to identify the precise location of a particular object in respect to the world.
 */
public class GlobalLocation implements Externalizable {

    private Point pointInSpace;
    private GeoPoint geoPoint;
    private double altitude;
    private Acre acre;
    private Sector sector;

    public GlobalLocation(Point pointInSpace, Globe globe) {
        if (pointInSpace == null) {
            throw new NullPointerException("pointInSpace");
        }
        if (pointInSpace.equals(Point.ORIGIN)) {
            throw new IllegalArgumentException("pointInSpace cannot be the origin: " + pointInSpace);
        }
        this.pointInSpace = pointInSpace;
        this.geoPoint = GeoPoint.fromPoint(pointInSpace);
        this.altitude = Vector.computeDistance(pointInSpace.x, pointInSpace.y, pointInSpace.z) - GeoSpec.APPROX_RADIUS_METERS.get();
//        this.acre = globe.findAcreByPoint(geoPoint);
//        this.sector
    }

    public void writeExternal(ObjectOutput objectOutput) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void readExternal(ObjectInput objectInput) throws IOException, ClassNotFoundException {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
