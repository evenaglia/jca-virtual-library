package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.RangeBasedLongSet;

/**
 * User: ed
 * Date: 6/3/14
 * Time: 8:46 PM
 */
public interface VertexChangeEventListener {

    void handleVertexChange(RangeBasedLongSet changedVertexIds);
}
