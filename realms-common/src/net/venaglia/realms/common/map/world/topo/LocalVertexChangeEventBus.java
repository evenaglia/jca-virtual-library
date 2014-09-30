package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.RangeBasedLongSet;

/**
 * User: ed
 * Date: 6/5/14
 * Time: 5:17 PM
 *
 * Simple implementation that retains hard references to objects.
 *
 * <b>This implementation should only be used for test or terraform runs</b>
 */
public class LocalVertexChangeEventBus implements VertexChangeEventBus {

//    private VertexChangeEventListener[] multiSharedListeners = new VertexChangeEventListener[VertexStore.COUNT_DUAL_SHARED_VERTEX];

    public void addListener(VertexChangeEventListener listener, RangeBasedLongSet sharedVertexIds) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void notifyChanged(long vertexId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
