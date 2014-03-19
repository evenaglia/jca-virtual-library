package net.venaglia.realms.demo;

import net.venaglia.gloo.demo.AbstractDemo;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.common.map_x.WorldMap;
import net.venaglia.realms.common.map_x.elements.GraphAcre;
import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;

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
        list.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                for (GraphAcre acre : worldMap.graph.asSeries()) {

                }
            }
        });
    }

    // todo: finish me!!!
}
