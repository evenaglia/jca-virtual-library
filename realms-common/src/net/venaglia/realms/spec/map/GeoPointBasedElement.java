package net.venaglia.realms.spec.map;

/**
 * User: ed
 * Date: 12/19/12
 * Time: 5:35 PM
 */
public interface GeoPointBasedElement {

    int countGeoPoints();

    GeoPoint getGeoPoint(int index);

    void setGeoPoint(int index, GeoPoint geoPoint);
}
