package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

/**
* User: ed
* Date: 1/20/15
* Time: 8:37 AM
*/
public class TectonicPoint {

    public enum PointClass {
        PLATONIC,
        MIDPOINT,
        INTERPOLATED,
        OTHER
    }

    public final Point point;
    public final Vector vector;
    public final PointClass pointClass;

    public TectonicPoint(Point point, Vector vector, PointClass pointClass) {
        this.point = point;
        this.vector = vector;
        this.pointClass = pointClass;
    }
}
