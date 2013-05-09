package net.venaglia.realms.demo;

import net.venaglia.realms.builder.geoform.GeoSpec;
import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.elements.GraphAcre;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.projection.DisplayList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.impl.DisplayListBuffer;

/**
 * User: ed
 * Date: 3/25/13
 * Time: 8:26 AM
 */
public abstract class WorldLoader extends AbstractDemo {

    private DisplayList list = new DisplayListBuffer("world");

    @Override
    protected String getTitle() {
        return "World Loader - " + GeoSpec.getGeoIdentity();
    }

    @Override
    protected double getCameraDistance() {
        return 8000;
    }

    @Override
    protected BoundingVolume<?> getRenderingBounds() {
        return new BoundingSphere(Point.ORIGIN, 1024.0);
    }

    @Override
    protected void init() {
        final WorldMap worldMap = new WorldMap();
        list.record(new DisplayList.DisplayListRecorder() {
            public void record(GeometryBuffer buffer) {
                for (GraphAcre acre : worldMap.graph.asSeries()) {

                }
            }
        });
    }

    // todo: finish me!!!
}
