package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.VertexStore;
import net.venaglia.common.util.Visitor;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import static net.venaglia.realms.common.map.world.topo.VertexBlock.VERTEX_ID_MASK;
import static net.venaglia.realms.common.map.world.topo.VertexBlock.VERTEX_COUNT;
import static net.venaglia.realms.common.map.world.topo.VertexBlock.INDEX_MASK;

/**
 * User: ed
 * Date: 6/10/14
 * Time: 5:33 PM
 */
abstract class AbstractVertexStore implements VertexStore {

    protected final VertexChangeEventBus eventBus;

    protected AbstractVertexStore() {
        this(Configuration.VERTEX_CHANGE_EVENT_BUS.getBean());
    }

    protected AbstractVertexStore(VertexChangeEventBus eventBus) {
        this.eventBus = eventBus;
    }

    public void read(final RangeBasedLongSet vertexIds, VertexConsumer consumer) {
        final Set<Long> ids = new HashSet<Long>();
        for (Iterator<Long> i = new SnapIterator(vertexIds, VERTEX_COUNT); i.hasNext();) {
            ids.add(i.next());
        }
        preFetch(ids);
        VertexBlock block = null;
        for (Iterator<Long> i = new SnapIterator(vertexIds); i.hasNext();) {
            Long id = i.next();
            long blockId = id & VERTEX_ID_MASK;
            if (block == null || block.getId() != blockId) {
                block = getBlock(blockId);
            }
            int index = (int)(id & INDEX_MASK);
            block.visit(index, consumer);
        }
    }

    protected abstract void preFetch(Set<Long> blockIds);

    protected abstract VertexBlock getBlock(Long id);

    public VertexWriter write(final RangeBasedLongSet vertexIds) {
        return new VertexWriter() {

            private long next = -1L;
            private VertexBlock block;

            public boolean hasNext() {
                try {
                    vertexIds.getNext(next + 1);
                    return true;
                } catch (NoSuchElementException e) {
                    return false;
                }
            }

            public long getNextId() {
                return vertexIds.getNext(next);
            }

            public void next(int rgbColor, double i, double j, double k, float elevation) {
                next = vertexIds.getNext(next + 1);
                long blockId = next & VERTEX_ID_MASK;
                if (block == null || block.getId() != blockId) {
                    block = getBlock(blockId);
                }
                block.set((int)(next & INDEX_MASK), rgbColor, i, j, k, elevation);
            }

            public void done() {
                if (hasNext()) {
                    throw new IllegalStateException("Not all ids have been set yet");
                }
            }
        };
    }

    public VertexChangeEventBus getChangeEventBus() {
        return eventBus;
    }

    public void visitVertexIds(RangeBasedLongSet set, Visitor<Long> visitor) {
        if (set.isEmpty()) {
            return;
        }
        long last = Long.MIN_VALUE;
        long marker = 0L;
        do {
            long start = set.getNext(marker);
            long end = set.getNextNotIncluded(start);
            try {
                marker = set.getNext(end);
            } catch (NoSuchElementException e) {
                marker = Long.MAX_VALUE;
            }
            for (long i = Math.max(start & VERTEX_ID_MASK, last + VERTEX_COUNT), j = (end - 1) & VERTEX_ID_MASK; i <= j; i += VERTEX_COUNT) {
                visitor.visit(i);
                last = i;
            }
        } while (marker < Long.MAX_VALUE);
    }
}
