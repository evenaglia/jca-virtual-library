package net.venaglia.realms.common.map;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.realms.common.map.things.Thing;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingRef;
import net.venaglia.common.util.Identifiable;

import java.util.Iterator;
import java.util.Set;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 7:31 PM
 *
 * A region of 3D space that contains things
 */
public interface Cube extends Identifiable, Volume, Iterable<ThingRef<?>> {

    BoundingBox getBounds();

    int size();
    <T extends Thing> Iterator<ThingRef<T>> iterator(Set<String> types);
    <T extends Thing> Iterator<ThingRef<T>> iterator(ThingMetadata<T> metadata);
}
