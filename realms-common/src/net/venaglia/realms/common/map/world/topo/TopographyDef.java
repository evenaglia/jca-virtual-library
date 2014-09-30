package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.common.util.impl.AbstractCachingRef;

import java.util.ArrayList;
import java.util.List;

import static net.venaglia.realms.common.map.world.topo.Topography.*;

/**
 * User: ed
 * Date: 6/12/14
 * Time: 2:37 PM
 */
public class TopographyDef extends AbstractCachingRef<Topography> {

    private final long[] definition;

    public TopographyDef(long[] definition) {
        assert calculateSize(definition) < 65536; // anything bigger than this is probably a bug.
        this.definition = definition;
    }

    @SuppressWarnings("UnusedParameters")
    private TopographyDef(long[] definition, Void v) {
        this.definition = definition;
    }

    protected Topography getImpl() {
        return parse(definition);
    }

    public long[] getDefinition() {
        return definition;
    }

    public static class Builder {

        private int size = 0;

        private final List<Long> sequence = new ArrayList<Long>();

        public Builder add(long pointId, boolean shared) {
            assert pointId >= 0 && pointId <= VALUE_MASK;
            sequence.add(shared ? pointId | SHARED_MASK : pointId);
            size++;
            return this;
        }

        public Builder add(long fromPointId, long toPointId, boolean shared) {
            long span = Math.abs(toPointId - fromPointId);
            assert fromPointId >= 0L && toPointId >= 0L;
            assert span < 65536L && span > 1L;
            assert fromPointId <= VALUE_MASK && toPointId <= VALUE_MASK;
            sequence.add(fromPointId);
            boolean reverse = toPointId < fromPointId;
            long second = span | TO_MASK;
            if (reverse) {
                second |= REVERSE_MASK;
            }
            if (shared) {
                second |= SHARED_MASK;
            }
            sequence.add(second);
            size += span;
            return this;
        }

        public int size() {
            return size;
        }

        public TopographyDef done() {
            assert sequence.size() < 16 && size < 65536; // anything bigger than this is probably a bug.
            long[] vertexIds = new long[sequence.size()];
            for (int i = 0; i < sequence.size(); i++) {
                vertexIds[i] = sequence.get(i);
            }
            return new TopographyDef(vertexIds, null);
        }
    }

    private static Topography parse(long[] vertexIds) {
        assert vertexIds != null && vertexIds.length > 0 && vertexIds[0] >= 0;
        RangeBasedLongSet sharedVertexIds = new RangeBasedLongSet();
        int c = 0;

        int size = calculateSize(vertexIds);

        long[] mutableIndices = new long[size];
        for (int i = 0; i < vertexIds.length; i++) {
            long id = vertexIds[i];
            boolean span = (id & TO_MASK) == TO_MASK;
            if (span) {
                c = processSpan(vertexIds[i - 1], id, mutableIndices, c, sharedVertexIds);
            } else {
                boolean shared = (id & SHARED_MASK) == SHARED_MASK;
                if (shared) {
                    sharedVertexIds.add(id);
                }
            }
        }
        assert size == 2145 || size == 3;
        sharedVertexIds.removeAll(VertexBlock.NON_SHARED_VERTEX_STORES.begin, Long.MAX_VALUE);
        return new Topography(vertexIds, sharedVertexIds, size, mutableIndices);
    }

    private static int processSpan(long last,
                                   long rawValue,
                                   long[] mutableIndices,
                                   int index,
                                   RangeBasedLongSet sharedVertexIds) {
        boolean shared = (rawValue & SHARED_MASK) == SHARED_MASK;
        boolean reverse = (rawValue & REVERSE_MASK) == REVERSE_MASK;
        long value = rawValue & VALUE_MASK;
        int size = mutableIndices.length;
        long now = reverse ? last - value : last + value;
        if (last < VertexBlock.NON_SHARED_VERTEX_STORES.begin || now < VertexBlock.NON_SHARED_VERTEX_STORES.begin) {
            long j = last;
            long step = reverse ? -1L : 1L;
            for (int k = 0; k <= value && index < size; k++) {
                mutableIndices[index++] = j;
                j += step;
            }
        }
        if (shared) {
            sharedVertexIds.addAll(Math.min(last, now), Math.max(last, now));
        }
        return index;
    }

    public static int calculateSize(long[] vertexIds) {
        int size = 0;
        for (long vertexId : vertexIds) {
            long id = vertexId;
            boolean span = (id & TO_MASK) == TO_MASK;
            if (span) {
                id &= VALUE_MASK;
                assert id < 65536L; // too many points in a single span is an anomaly
                size += id;
            } else {
                assert id == (id & VALUE_MASK); // if span is not set, the others should not be set
                size++;
            }
        }
        return size;
    }
}
