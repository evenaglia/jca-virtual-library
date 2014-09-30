package net.venaglia.realms.builder.geoform;

import net.venaglia.realms.common.map.world.topo.VertexBlock;
import net.venaglia.realms.spec.GeoSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
* User: ed
* Date: 9/5/14
* Time: 5:08 PM
*/
public class TopographyIncrements {

    private long seq = 0;
    private long acreCount = GeoSpec.ACRES.get();

    public final long acreSeamSeqStart;
    public final long acreSeamIncrement; // per acre seam (2x step)
    public final long acreSeamStep;
    public final long zoneSeamSeqStart;
    public final long zoneSeamIncrement; // per acre (30x step)
    public final long zoneSeamStep;
    public final long zoneVertexSeqStart;
    public final long zoneVertexIncrement;
    public final long zoneVertexStep;
    public final long end;

    public TopographyIncrements(int start) {
        long acreSeamCount = acreCount * 12 / 2 - (12 * 2);
        long zoneCount = acreCount * 4 * 6 - (12 * 4);
        long zoneSeamCount = zoneCount * 3 / 2;

        // an acre seam is composed of two zone boundaries, shared by two acres
        // count = acreCount * 12 / 2 - (12 * 2) (because 12 acres are pentagonal)
        acreSeamSeqStart = roundToNextBlock(start);
        acreSeamStep = VertexBlock.VERTEX_COUNT / 64;
        acreSeamIncrement = (6 * 2) * acreSeamStep;

        // a zone seam is the boundary between two zones within the same acre
        // count = acreCount * 6 * 5 - (12 * 5) (because 12 acres are pentagonal)
        zoneSeamSeqStart = roundToNextBlock(acreSeamIncrement * acreSeamCount);
        zoneSeamStep = VertexBlock.VERTEX_COUNT / 64;
        zoneSeamIncrement = (6 * 5) * zoneSeamStep;

        // a zone vertex is a vertex that lies completely within a single zone
        zoneVertexSeqStart = roundToNextBlock(zoneSeamIncrement * zoneSeamCount);
        zoneVertexStep = VertexBlock.VERTEX_COUNT / 2;
        zoneVertexIncrement = (6 * 4) * zoneVertexStep;

        end = roundToNextBlock(zoneVertexIncrement * zoneCount);
    }

    private long roundToNextBlock(long increment) {
        return seq += (increment | VertexBlock.INDEX_MASK) + 1;
    }

    @Override
    public String toString() {
        String format = "" +
                "TopographyIncrements:\n" +
                "\tAcre Count     : %,15d\n" +
                "\tAcre Seam Count: %,15d\n" +
                "\tZone Seam Count: %,15d\n" +
                "\tZone Count     : %,15d\n" +
                "\tAcre Seams     : from %,15d to %,18d by %,7d/acre (%,12d steps, %,18d total vertices)\n" +
                "\tZone Seams     : from %,15d to %,18d by %,7d/acre (%,12d steps, %,18d total vertices)\n" +
                "\tZone Vertices  : from %,15d to %,18d by %,7d/acre (%,12d steps, %,18d total vertices)";
        List<Object> buffer = new ArrayList<Object>(20);
        long acreSeamCount = acreCount * 12 / 2 - (12 * 2);
        long zoneCount = acreCount * 4 * 6 - (12 * 4);
        long zoneSeamCount = zoneCount * 3 / 2;
        buffer.add(acreCount);
        buffer.add(acreSeamCount);
        buffer.add(zoneSeamCount);
        buffer.add(zoneCount);
        Collections.addAll(buffer, calculateDetail(acreSeamSeqStart, zoneSeamSeqStart, acreSeamIncrement));
        Collections.addAll(buffer, calculateDetail(zoneSeamSeqStart, zoneVertexSeqStart, zoneSeamIncrement));
        Collections.addAll(buffer, calculateDetail(zoneVertexSeqStart, end, zoneVertexIncrement));
        return String.format(format, buffer.toArray());
    }

    private Long[] calculateDetail(long start, long end, long increment) {
        long count = end - start;
        return new Long[] {
                start, end - 1, increment, count / increment, count
        };
    }
}
