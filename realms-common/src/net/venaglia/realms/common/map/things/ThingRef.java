package net.venaglia.realms.common.map.things;

import net.venaglia.common.util.Identifiable;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.realms.common.map.Cube;

/**
 * User: ed
 * Date: 3/19/14
 * Time: 6:27 PM
 */
public interface ThingRef<T extends Thing> extends Identifiable, SpatialMap.Entry<T> {

    Cube getCube();
    Point getPoint();
    String getType();
    ThingMetadata<T> getMetadata();

    boolean isContainedBy(BoundingVolume<?> region);
    boolean isAt(double x, double y, double z);

    void lock(boolean write);
    void unlock(boolean write);
    void writeChangesTo(ThingWriter thingWriter, boolean setUnchangedValuesFirst);
}
