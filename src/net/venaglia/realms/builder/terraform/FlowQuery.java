package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.builder.map.GeoPoint;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:18 AM
 */
public interface FlowQuery {

    GeoPoint getPoint();

    /**
     * @param data the point data object
     */
    void processDataForPoint(FlowPointData data);
}
