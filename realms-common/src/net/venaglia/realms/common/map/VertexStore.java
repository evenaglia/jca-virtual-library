package net.venaglia.realms.common.map;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.common.util.Visitor;
import net.venaglia.realms.common.map.world.topo.VertexChangeEventBus;

/**
 * User: ed
 * Date: 6/10/14
 * Time: 1:19 PM
 */
public interface VertexStore {

    void read(RangeBasedLongSet vertexIds, VertexConsumer consumer);

    VertexWriter write(RangeBasedLongSet vertexIds);

    void flushChanges();

    VertexChangeEventBus getChangeEventBus();

    void visitVertexIds(RangeBasedLongSet set, Visitor<Long> visitor);

    interface VertexConsumer {

        /**
         * @param rgbColor Color of this vertex
         * @param i Component of the surface normal
         * @param j Component of the surface normal
         * @param k Component of the surface normal
         * @param elevation Elevation of this vector
         */
        void next(int rgbColor, double i, double j, double k, float elevation);

        /**
         * Called when all vertices have been loaded
         */
        void done();
    }

    interface VertexWriter extends VertexConsumer {

        boolean hasNext();

        /**
         * @return the next vertex id that should be written, or
         *     {@link Long#MIN_VALUE} if there are no more to be written.
         */
        long getNextId();
    }
}
