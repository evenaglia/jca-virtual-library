package net.venaglia.realms.common.map;

import net.venaglia.common.util.Ref;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.map.things.Thing;
import net.venaglia.realms.common.map.things.ThingMetadata;
import net.venaglia.realms.common.map.things.ThingRef;
import net.venaglia.realms.common.map.world.WorldMapImpl;
import net.venaglia.realms.common.util.Visitor;

import java.util.UUID;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 7:27 PM
 *
 * The map of the world
 */
public interface WorldMap {

    Ref<? extends WorldMap> INSTANCE = new AbstractCachingRef<WorldMap>() {
        @Override
        protected WorldMap getImpl() {
            return new WorldMapImpl();
        }
    };

    UUID getInstanceUuid();

    <T extends Thing> ThingRef<T> getThing(Long thingId);

    <T extends Thing> ThingRef<T> createThing(ThingMetadata<T> metadata,
                                              Point position,
                                              Visitor<ThingRef<T>> initializer);

    VertexStore getVertexStore();

    BinaryStore getBinaryStore();

    PropertyStore getPropertyStore();
}
