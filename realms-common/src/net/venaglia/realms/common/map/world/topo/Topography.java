package net.venaglia.realms.common.map.world.topo;

import static net.venaglia.realms.common.Configuration.VERTEX_CHANGE_EVENT_BUS;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.gloo.physical.decorators.Color;
import sun.security.provider.certpath.Vertex;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 5/29/14
 * Time: 8:03 AM
 */
public class Topography implements VertexChangeEventListener {

    static final long SHARED_MASK  = 0x1000000000000000L;
    static final long TO_MASK      = 0x2000000000000000L;
    static final long REVERSE_MASK = 0x4000000000000000L;
    static final long VALUE_MASK   = 0x07FFFFFFFFFFFFFFL;

    private final long[] vertexIds;
    private final RangeBasedLongSet sharedVertexIds;
    private final int size;

    private ByteBuffer vbo;

    /**
     * One of:
     * <li>2145 - full detail of a zone</li>
     * <li></li>
     * to be continued...
     */
    private long[] mutableIndices; // always 2145, unless CORNERS_ONLY is true then 3
    private Color[] mutableColors;
    private Vertex[] mutableNormals;
    private float[] mutableElevations;

    Topography(long[] vertexIds, RangeBasedLongSet sharedVertexIds, int size, long[] mutableIndices) {
        this.vertexIds = vertexIds;
        this.sharedVertexIds = sharedVertexIds;
        this.size = size;
        this.mutableIndices = mutableIndices;
        if (sharedVertexIds != null) {
            VERTEX_CHANGE_EVENT_BUS.<VertexChangeEventBus>getBean().addListener(this, sharedVertexIds);
        }
    }

    public void handleVertexChange(RangeBasedLongSet changedVertexIds) {
        for (int i = 0, l = mutableIndices.length; i < l; i++) {
            long index = mutableIndices[i];
            if (changedVertexIds.contains(index)) {
                load(i);
            }
        }
    }

    private void load(int i) {
        // todo
    }

    public int size() {
        return size;
    }
}
