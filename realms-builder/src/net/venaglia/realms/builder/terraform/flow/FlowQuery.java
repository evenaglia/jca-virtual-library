package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.realms.spec.map.GeoPoint;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:18 AM
 */
public interface FlowQuery {

    GeoPoint getPoint();

    double getRadius();

    double getScale();

    /**
     * @param data the point data object
     */
    void processDataForPoint(FlowPointData data);
}
