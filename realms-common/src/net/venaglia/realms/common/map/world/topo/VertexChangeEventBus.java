package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.RangeBasedLongSet;

/**
 * User: ed
 * Date: 6/3/14
 * Time: 8:45 PM
 */
public interface VertexChangeEventBus {

    void addListener(VertexChangeEventListener listener, RangeBasedLongSet sharedVertexIds);

    void notifyChanged(long vertexId);
}
