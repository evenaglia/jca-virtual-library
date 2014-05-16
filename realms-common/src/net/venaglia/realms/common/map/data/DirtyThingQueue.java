package net.venaglia.realms.common.map.data;

/**
 * User: ed
 * Date: 4/4/14
 * Time: 6:55 PM
 */
public interface DirtyThingQueue {

    void add(ThingRefImpl<?> thingRef);
}
