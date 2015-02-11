package net.venaglia.realms.common.weather;

import net.venaglia.realms.spec.map.GeoPoint;

/**
 * User: ed
 * Date: 2/5/15
 * Time: 5:35 PM
 */
public interface TerrainSample {

    GeoPoint getCenter();

    void update(float temp, float wind, float solar, float rain, TerrainReturn ret);
}
