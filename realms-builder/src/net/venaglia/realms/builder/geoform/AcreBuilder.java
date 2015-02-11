package net.venaglia.realms.builder.geoform;

import static net.venaglia.realms.spec.map.AbstractCartographicElement.ELEMENT_ORDER;
import static net.venaglia.realms.builder.geoform.CartographicElementView.getFor;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.util.BasicSpatialMap;
import net.venaglia.realms.builder.utils.MutableBounds;
import net.venaglia.realms.spec.map.AbstractCartographicElement;
import net.venaglia.realms.spec.map.Acre;
import net.venaglia.realms.spec.map.Edge;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.spec.map.GeoPointBasedElement;
import net.venaglia.realms.spec.map.GlobalPointMap;
import net.venaglia.realms.spec.map.GlobalSector;
import net.venaglia.realms.spec.map.Globe;
import net.venaglia.realms.spec.map.Sector;
import net.venaglia.common.util.Consumer;
import net.venaglia.gloo.util.debug.OutputGraph;
import net.venaglia.realms.common.util.work.WorkQueue;
import net.venaglia.realms.spec.GeoSpec;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 12/6/12
* Time: 8:07 AM
* To change this template use File | Settings | File Templates.
*/
class AcreBuilder implements Runnable {

    private final int subdivisions;
    private final boolean performAssertions;

    private static TopographyIncrements increments;

    private Sector sector;
    private Map<GeoPoint,Sector[]> sectorMap;
    private final WorkQueue workQueue;
    private final Consumer<Acre> acreConsumer;

    public AcreBuilder(int subdivisions,
                       boolean performAssertions,
                       Sector sector,
                       Map<GeoPoint,Sector[]> sectorMap,
                       WorkQueue workQueue,
                       Consumer<Acre> acreConsumer) {
        this.subdivisions = subdivisions;
        this.performAssertions = performAssertions;
        this.sector = sector;
        this.sectorMap = sectorMap;
        this.workQueue = workQueue;
        this.acreConsumer = acreConsumer;
    }

    // run-time fields
    private int nextStep = 1;
    private Set<Sector> neighborsA;
    private Set<Sector> neighborsB;
    private Set<Sector> neighborsC;
    private AcreSeamSeq.Accessor acreSeamSeqAccessor;
    private GlobalPointMap globalPointMap;
    private PointLocator pointLocator;

    public void run() {
        run(nextStep++);
    }

    public Runnable secondRun() {
        return new Runnable() {
            public void run() {
                AcreBuilder.this.run(100);
                AcreBuilder.this.run(999);
            }
        };
    }

    public static TopographyIncrements getTopographyIncrements(int lastMultiZoneVertexId) {
        if (increments == null) {
            increments = new TopographyIncrements(lastMultiZoneVertexId);
        }
        return increments;
    }

    public Runnable thirdRun(AcreSeamSeq.Accessor acreSeamSeqAccessor, GlobalPointMap globalPointMap) {
        this.acreSeamSeqAccessor = acreSeamSeqAccessor;
        this.globalPointMap = globalPointMap;
        return new Runnable() {
            public void run() {
                AcreBuilder.this.run(200);
                AcreBuilder.this.run(1000);
            }
        };
    }

    private void run(int thisStep) {
        GeoPoint a = sector.points[0];
        GeoPoint b = sector.points[1];
        GeoPoint c = sector.points[2];
        run(thisStep, a, b, c);
    }

    private void run(int thisStep, GeoPoint a, GeoPoint b, GeoPoint c) {
        Acre[] innerAcres;
        switch (thisStep) {
            case 0:
                nextStep = 5;
                for (int i = 1; i < 5; i++) {
                    run(i, a, b, c);
                }
                break;
            case 1:
                innerAcres = buildInnerAcres();
                assert sector.getInnerAcres().length == innerAcres.length;
                System.arraycopy(innerAcres, 0, sector.getInnerAcres(), 0, innerAcres.length);
                break;
            case 2:
                neighborsA = getNeighbors(sectorMap.get(a));
                neighborsB = getNeighbors(sectorMap.get(b));
                neighborsC = getNeighbors(sectorMap.get(c));
                break;
            case 3:
                stitchNeighbors(Edge.AB, neighborsA, a, neighborsB, b);
                stitchNeighbors(Edge.BC, neighborsB, b, neighborsC, c);
                stitchNeighbors(Edge.CA, neighborsC, c, neighborsA, a);
                break;
            case 4:
                stitchNeighbors(neighborsA, a);
                stitchNeighbors(neighborsB, b);
                stitchNeighbors(neighborsC, c);
                break;
            case 100:
                addNeighbors(Edge.AB, neighborsA, a, neighborsB, b);
                addNeighbors(Edge.BC, neighborsB, b, neighborsC, c);
                addNeighbors(Edge.CA, neighborsC, c, neighborsA, a);
                break;
            case 200:
                assignVertexIds();
                break;
            case 999:
                neighborsA = null;
                neighborsB = null;
                neighborsC = null;
                break;
            case 1000:
                acreSeamSeqAccessor = null;
                globalPointMap = null;
                pointLocator = null;
                break;
        }
        if (nextStep < 5) {
            workQueue.addWorkUnit(this);
        }
    }

    protected Acre[] buildInnerAcres() {
        int seq = 0, rowIndex = 1;
        Acre[] myAcres = new Acre[subdivisions * (subdivisions - 3) / 6 + 1];
        int[] strips = new int[subdivisions];
        for (int i = subdivisions * 2 - 1, j = 0, k = 0; i > 0; j += i, i -= 2, k++) {
            strips[k] = j;
        }
        GeoPoint[][] acres;
        Acre.Flavor[] flavors = Acre.Flavor.values();
        int[] rows = new int[subdivisions / 3 * 3 + 1];
        for (int i = 0, count = subdivisions / 3; i < subdivisions; i += 3, count--) {
            for (int j = 0, c = count; j < 3; j++, c--) {
                if (c > 0) {
                    acres = buildAcres(strips[i + j] + j * 2, strips[i + j + 1] + j * 2, c);
                    for (GeoPoint[] a : acres) {
                        myAcres[seq] = consume(new Acre(seq, sector, flavors[j], a[0], a[1], a[2], a[3], a[4], a[5], a[6]));
                        seq++;
                    }
                }
                rows[rowIndex++] = seq;
            }
        }
        assert rowIndex == rows.length;
        assert seq == myAcres.length;

        int neighborCount = 0;
        for (int i = 0, l = rows.length - 2; i < l; i++) {
            int rowA = rows[i];
            int rowB = rows[i + 1];
            int rowC = rows[i + 2];
            int lenA = rowB - rowA;
            int lenB = rowC - rowB;
            neighborCount += addNeighbors(myAcres, rowA, lenA, myAcres, rowB, lenB);
            if (i < l - 1) {
                int rowD = rows[i + 3];
                int lenC = rowD - rowC;
                neighborCount += addNeighbors(myAcres, rowA, lenA, myAcres, rowC, lenC);
            }
        }
        assert neighborCount > seq * 6 - (subdivisions * 4 - 6);
        return myAcres;
    }

    protected int[] findNearCornerAcres() {
        return new int[]{
                0,
                subdivisions / 3 - 1,
                subdivisions * (subdivisions - 3) / 6
        };
    }

    protected Map<Edge,int[]> findNearEdgeAcres() {
        int e = subdivisions / 3;
        int l = e * 2 - 1;
        Map<Edge,int[]> nearEdgeAcres = new EnumMap<Edge,int[]>(Edge.class);

        {
            int[] ids = new int[l];
            ids[0] = 0;
            for (int i = 1, j = 1, k = e; i < l; j++, k++) {
                ids[i++] = k;
                ids[i++] = j;
            }
            nearEdgeAcres.put(Edge.AB, ids);
        }

        {
            int[] ids = new int[l];
            ids[0] = e - 1;
            for (int i = 1, s = e - 1, j = e + s - 1, k = j + s * 2 - 1; i < l; j = k + --s, k = j + s * 2 - 1) {
                ids[i++] = j;
                ids[i++] = k;
            }
            nearEdgeAcres.put(Edge.BC, ids);
        }

        {
            int[] ids = new int[l];
            ids[e * 2 - 2] = 0;
            for (int i = e * 2 - 3, s = e - 1, j = e, k = j + s * 2 - 1; i >= 0; j = k + 1 + --s, k = j + s * 2 - 1) {
                ids[i--] = j;
                ids[i--] = k;
            }
            nearEdgeAcres.put(Edge.CA, ids);
        }

        return nearEdgeAcres;
    }

    private int addNeighbors(Acre[] acresA, int rowA, int lengthA, Acre[] acresB, int rowB, int lengthB) {
        if (lengthA <= 0 || lengthB <= 0) {
            return 0; // no-op
        }
        if (lengthB > lengthA) {
            int t = rowA;
            rowA = rowB;
            rowB = t;
            t = lengthA;
            lengthA = lengthB;
            lengthB = t;
        }
        if (lengthA == lengthB) {
            for (int i = 0; i < lengthA; i++) {
                Acre acreA = acresA[rowA + i];
                Acre acreB = acresB[rowB + i];
                acreA.addNeighbor(acreB, indexOfMyNeighbor(acreA, acreB));
                acreB.addNeighbor(acreA, indexOfMyNeighbor(acreB, acreA));
            }
            return lengthA * 2;
        } else {
            assert lengthA <= lengthB + 2;
            if (lengthA == lengthB + 1) {
                for (int i = 0; i < lengthB; i++) {
                    Acre acreA = acresA[rowA + i];
                    Acre acreB = acresB[rowB + i];
                    Acre acreC = acresA[rowA + i + 1];
                    acreA.addNeighbor(acreB, indexOfMyNeighbor(acreA, acreB));
                    acreB.addNeighbor(acreA, indexOfMyNeighbor(acreB, acreA));
                    acreB.addNeighbor(acreC, indexOfMyNeighbor(acreB, acreC));
                    acreC.addNeighbor(acreB, indexOfMyNeighbor(acreC, acreB));
                }
                return lengthB * 4;
            } else {
                for (int i = 0; i < lengthB; i++) {
                    Acre acreA = acresA[rowA + i + 1];
                    Acre acreB = acresB[rowB + i];
                    acreA.addNeighbor(acreB, indexOfMyNeighbor(acreA, acreB));
                    acreB.addNeighbor(acreA, indexOfMyNeighbor(acreB, acreA));
                }
                return lengthB * 4;
            }
        }
    }


    private int indexOfMyNeighbor(Acre me, Acre neighbor) {
        for (int i = 0, j = 1, l = me.points.length; i < l; i++, j = (i + 1) % l) {
            GeoPoint pointA = me.points[i];
            GeoPoint pointB = me.points[j];
            for (GeoPoint point : neighbor.points) {
                if (point.equals(pointA)) {
                    pointA = null;
                    if (pointB == null) return i;
                } else if (point.equals(pointB)) {
                    pointB = null;
                    if (pointA == null) return i;
                }
            }
        }
        throw new AssertionError("Failed to find the index of a neighbor: " + neighbor + " --> add to --> " + me);
    }

    private int addNeighbors(Edge edge,
                             Set<Sector> neighbors1,
                             GeoPoint shared1,
                             Set<Sector> neighbors2,
                             GeoPoint shared2) {
        int count = 0;
        Neighbor neighbor = findNeighbor(neighbors1, shared1, neighbors2, shared2);
        assert gatedAssertion(neighbor != null);
        if (neighbor != null) {
            assert neighbor.reverse;
            CartographicElementView elementView = getFor(subdivisions);
            (sector.id < neighbor.sector.id ? sector.sharedAcresLock : neighbor.sector.sharedAcresLock).writeLock().lock();
            (sector.id < neighbor.sector.id ? neighbor.sector.sharedAcresLock : sector.sharedAcresLock).writeLock().lock();
            count = updateSharedAcres(edge, count, neighbor, elementView);
        }
        return count;
    }

    private int updateSharedAcres(Edge edge,
                                  int count,
                                  Neighbor neighbor,
                                  CartographicElementView elementView) {
        try {
            List<Acre> myShared = elementView.get(sector.getSharedAcres(), CartographicElementView.Position.Shared, edge, false);
            List<Acre> urShared = elementView.get(neighbor.sector.getSharedAcres(), CartographicElementView.Position.Shared, neighbor.edge, true);
            assert myShared.size() == urShared.size();
            for (int i = 0, l = myShared.size(); i < l; i++) {
                updateSharedAcre(myShared, urShared, i);
            }

            Acre[] myInner = sector.getInnerAcres();
            Acre[] urInner = neighbor.sector.getInnerAcres();
            List<Acre> myNear = elementView.get(myInner, CartographicElementView.Position.Near, edge,  false);
            List<Acre> myBorder = elementView.get(myInner, CartographicElementView.Position.Border, edge, false);
            List<Acre> urBorder = elementView.get(urInner, CartographicElementView.Position.Border, neighbor.edge, true);

            count += addNeighbors(myShared.subList(1, myShared.size() - 1), myNear);
            count += addNeighbors(myBorder, urBorder);
            count += addNeighbors(myShared.subList(0, myShared.size() - 1), myBorder);
            count += addNeighbors(myShared.subList(1, myShared.size()), myBorder);
        } finally {
            (sector.id < neighbor.sector.id ? neighbor.sector.sharedAcresLock : sector.sharedAcresLock).writeLock().unlock();
            (sector.id < neighbor.sector.id ? sector.sharedAcresLock : neighbor.sector.sharedAcresLock).writeLock().unlock();
        }
        return count;
    }

    private void updateSharedAcre(List<Acre> myShared, List<Acre> urShared, int i) {
        Acre my = myShared.get(i);
        Acre ur = urShared.get(i);
        if (my != ur) {
            assert my == null || ur == null;
            if (my != null) {
                urShared.set(i, ur);
            } else {
                myShared.set(i, ur);
            }
        }
    }

    private int addNeighbors(List<Acre> left, List<Acre> right) {
        assert left.size() == right.size();
        int count = 0;
        for (int i = 0, j = left.size(); i < j; i++) {
            Acre l = left.get(i);
            Acre r = right.get(i);
            if (l != null && r != null) {
                l.addNeighbor(r, indexOfMyNeighbor(l, r));
                r.addNeighbor(l, indexOfMyNeighbor(r, l));
                count++;
            }
        }
        return count;
    }

    private GeoPoint[][] buildAcres(int stripA, int stripB, int count) {
        GeoPoint[][] acres = new GeoPoint[count][];
        for (int i = 0, j = 0; i < count; i++, j += 6) {
            Sector.Triangle a = sector.triangles[stripA + j + 1];
            Sector.Triangle b = sector.triangles[stripA + j + 3];
            Sector.Triangle c = sector.triangles[stripB + j + 1];
            acres[i] = new GeoPoint[]{a.c, a.a, a.b, b.b, b.c, c.c, c.a};
        }
        return acres;
    }

    private Set<Sector> getNeighbors(Sector[] sectors) {
        SortedSet<Sector> neighbors = new TreeSet<Sector>(ELEMENT_ORDER);
        for (Sector s : sectors) {
            if (s != null) {
                neighbors.add(s);
            }
        }
        return neighbors;
    }

    private void stitchNeighbors(Edge edge,
                                 Set<Sector> neighbors1,
                                 GeoPoint shared1,
                                 Set<Sector> neighbors2,
                                 GeoPoint shared2) {
        Neighbor neighbor = findNeighbor(neighbors1, shared1, neighbors2, shared2);
        assert gatedAssertion(neighbor != null);
        if (neighbor != null) {
            if (sector.id > neighbor.sector.id) {
                stitchEdge(edge, neighbor);
            }
            neighbor.sector.edgesBuilt.incrementAndGet();
        }
        sector.edgesBuilt.incrementAndGet();
    }

    private Neighbor findNeighbor(Set<Sector> neighbors1, GeoPoint shared1, Set<Sector> neighbors2, GeoPoint shared2) {
        for (Sector neighbor : neighbors1) {
            if (neighbor == sector) continue;
            if (neighbors2.contains(neighbor)) {
                Edge edge;
                boolean reversed;
                int point = whichPoint(neighbor, shared1) << 4 | whichPoint(neighbor, shared2);
                switch (point) {
                    case 0x12: edge = Edge.AB; reversed = false; break;
                    case 0x21: edge = Edge.AB; reversed = true;  break;
                    case 0x13: edge = Edge.CA; reversed = true;  break;
                    case 0x31: edge = Edge.CA; reversed = false; break;
                    case 0x23: edge = Edge.BC; reversed = false; break;
                    case 0x32: edge = Edge.BC; reversed = true;  break;
                    default: continue;
                }
                return new Neighbor(neighbor, edge, reversed);
            }
        }
        return null;
    }

    private static class Neighbor {
        public final Sector sector;
        public final Edge edge;
        public final boolean reverse;

        private Neighbor(Sector sector, Edge edge, boolean reverse) {
            this.sector = sector;
            this.edge = edge;
            this.reverse = reverse;
        }
    }

    private void assignVertexIds() {
        pointLocator = new PointLocator();
        for (Acre acre : sector.getInnerAcres()) {
            Point[] midpoints = AcreSeamSeq.buildMidpoints(acre);
            acre.seamStartVertexIds = buildSeamStarts(acre.packId, midpoints);
            acre.zoneStartVertexIds = buildZoneStarts(acre.packId, acre.points.length);
            acre.topographyDef = buildTopographyDef(acre.center, acre.points);
        }
        for (Acre acre : sector.getSharedAcres()) {
            Point[] midpoints = AcreSeamSeq.buildMidpoints(acre);
            acre.seamStartVertexIds = buildSeamStarts(acre.packId, midpoints);
            acre.zoneStartVertexIds = buildZoneStarts(acre.packId, acre.points.length);
            acre.topographyDef = buildTopographyDef(acre.center, acre.points);
        }
    }

    /**
     * starts[0..4] : zone seam vertex start ids
     * starts[5..6] : acre seam vertex ids
     */
    private long[] buildSeamStarts(long id, Point[] midpoints) {
        int length = midpoints.length;
        assert id >= 0;
        long innerId = increments.zoneSeamSeqStart + increments.zoneSeamIncrement * id;
        long[] result = new long[length * 7];
        for (int i = 0, j = 0; i < length; i++, j += 7) {
            long acreSeamId = acreSeamSeqAccessor.get(midpoints[i]);
            long sharedId = increments.acreSeamSeqStart + increments.acreSeamIncrement * acreSeamId;
            for (int k = 0; k < 5; k++) {
                result[j + k] = innerId;
                innerId += increments.zoneSeamStep;
            }
            result[j + 5] = sharedId;
            result[j + 6] = sharedId + increments.acreSeamStep;
        }
        return result;
    }

    private long[] buildZoneStarts(long id, int length) {
        assert id >= 0;
        long vertexId = increments.zoneVertexSeqStart + increments.zoneVertexIncrement * id;
        int l = length * 4;
        long[] result = new long[l];
        for (int i = 0; i < l; i++) {
            result[i] = vertexId;
            vertexId += increments.zoneVertexStep;
        }
        return result;
    }

    /**
     * def[0] : center vertex id
     * def[1..6] : edge midpoint vertex ids
     * def[7..12] : corner vertex ids
     * def[13..18] : spoke midpoint vertex ids
     */
    private long[] buildTopographyDef(GeoPoint center, GeoPoint[] points) {
        assert points.length == 6 || points.length == 5;
        long[] def = new long[points.length * 3 + 1];
        int p = 0;
        def[p++] = pointLocator.find(center);
        for (int i = 0, l = points.length - 1; i <= l; i++) {
            GeoPoint p1 = points[i];
            GeoPoint p2 = points[i < l ? i + 1 :  0];
            def[p++] = pointLocator.findBetween(p1, p2);
        }
        for (GeoPoint point : points) {
            def[p++] = pointLocator.find(point);
        }
        for (GeoPoint point : points) {
            def[p++] = pointLocator.findBetween(center, point);
        }
        assert p == def.length;
        return def;
    }

    private class PointLocator implements BasicSpatialMap.BasicConsumer<GeoPoint> {

        private boolean found = false;
        private GlobalPointMap.GlobalPointEntry globalPointEntry;
        private MutableBounds bounds = new MutableBounds();

        public PointLocator reset() {
            globalPointEntry = null;
            found = false;
            return this;
        }

        public void found(BasicSpatialMap.BasicEntry<GeoPoint> entry, double x, double y, double z) {
            if (entry instanceof GlobalPointMap.GlobalPointEntry) {
                globalPointEntry = (GlobalPointMap.GlobalPointEntry)entry;
                found = true;
            }
        }

        public long findBetween(GeoPoint pointA, GeoPoint pointB) {
            Point a = pointA.toPoint(1000.0);
            Point b = pointB.toPoint(1000.0);
            double x = (a.x + b.x) * 0.5;
            double y = (a.y + b.y) * 0.5;
            double z = (a.z + b.z) * 0.5;
            double l = 1000.0 / Vector.computeDistance(x, y, z);
            x *= l;
            y *= l;
            z *= l;
            assert Math.abs(Vector.computeDistance(a.x - x, a.y - y, a.z - z) - Vector.computeDistance(b.x - x, b.y - y, b.z - z)) < 0.0005;
            globalPointMap.intersect(bounds.load(x, y, z), this.reset());
            assert found;
            assert globalPointEntry.getSeq() >= 0;
            return globalPointEntry.getSeq();
        }

        public long find(GeoPoint point) {
            globalPointMap.intersect(bounds.load(point), this.reset());
            assert found;
            assert globalPointEntry.getSeq() >= 0;
            return globalPointEntry.getSeq();
        }

        public long find(Point point) {
            globalPointMap.intersect(bounds.load(point), this.reset());
            assert found;
            assert globalPointEntry.getSeq() >= 0;
            return globalPointEntry.getSeq();
        }
    }

    private int whichPoint(Sector neighbor, GeoPoint point) {
        for (int i = 0; i < 3; i++) {
            if (neighbor.points[i].equals(point)) {
                return i + 1;
            }
        }
        return 0;
    }

    private void stitchEdge(Edge edge, Neighbor neighbor) {
        CartographicElementView elementView = getFor(subdivisions);
        List<Sector.Triangle> myEdge = elementView.get(sector.triangles, CartographicElementView.Position.Border, edge, false);
        List<Sector.Triangle> urEdge = elementView.get(neighbor.sector.triangles, CartographicElementView.Position.Border, neighbor.edge, neighbor.reverse);
        List<Acre> sharedAcres = elementView.get(sector.getSharedAcres(), CartographicElementView.Position.Shared, edge, false);
        makeAcres(myEdge, urEdge, neighbor, sharedAcres);
        assert !sharedAcres.subList(1, sharedAcres.size() - 1).contains(null);
    }

    private void makeAcres(List<Sector.Triangle> myEdge, List<Sector.Triangle> urEdge, Neighbor neighbor, List<Acre> sharedAcres) {
        assert myEdge.size() == urEdge.size();
        assert myEdge.size() % 3 == 0;
        int seq = 1;
        Boolean reverse = null;
        for (int i = 2, l = myEdge.size() - 2; i < l; i += 3) {
            Sector.Triangle t1 = myEdge.get(i);
            Sector.Triangle t2 = myEdge.get(i + 1);
            Sector.Triangle t3 = urEdge.get(i);
            Sector.Triangle t4 = urEdge.get(i + 1);
            GeoPoint[] points = sortPoints(t1.a, t1.b, t1.c, t2.a, t2.b, t2.c, t3.a, t3.b, t3.c, t4.a, t4.b, t4.c);
            assert points != null;
            if (reverse == null) {
                reverse = isCCW(points[0], points[1], points[2]);
            }
            Acre acre = reverse
                    ? new Acre(seq, sector, neighbor.sector, null, points[0],
                               points[6], points[5], points[4], points[3], points[2], points[1])
                    : new Acre(seq, sector, neighbor.sector, null, points[0],
                               points[1], points[2], points[3], points[4], points[5], points[6]);
            assert sharedAcres.get(seq) == null;
            sharedAcres.set(seq, acre);
            consume(acre);
            seq++;
        }
    }

    private Acre consume(Acre a) {
        if (acreConsumer != null) {
            acreConsumer.consume(a);
        }
        return a;
    }

    private GeoPoint[] sortPoints(GeoPoint... geoPoints) {
        Map<GeoPoint,Integer> pointCount = new LinkedHashMap<GeoPoint,Integer>();
        for (GeoPoint gp : geoPoints) {
            if (pointCount.containsKey(gp)) {
                pointCount.put(gp, pointCount.get(gp) + 1);
            } else {
                pointCount.put(gp, 1);
            }
        }
        GeoPoint center = null;
        GeoPoint end[] = new GeoPoint[2];
        GeoPoint side[] = new GeoPoint[4];
        int endIdx = 0;
        int sieIdx = 0;
        for (Map.Entry<GeoPoint,Integer> entry : pointCount.entrySet()) {
            switch (entry.getValue()) {
                case 4:
                    assert center == null;
                    center = entry.getKey();
                    break;
                case 2:
                    assert endIdx < end.length;
                    end[endIdx++] = entry.getKey();
                    break;
                case 1:
                    assert sieIdx < side.length;
                    side[sieIdx++] = entry.getKey();
                    break;
            }
        }
        return new GeoPoint[]{
                center, end[0], side[0], side[1], end[1], side[3], side[2]
        };
    }

    protected void stitchNeighbors(Set<Sector> neighbors, GeoPoint center) {
        assert gatedAssertion(neighbors.size() == 6 ||
                              neighbors.size() == 5 && countDistinctGlobalSectors(neighbors) == 5);
        if (!sector.equals(neighbors.iterator().next())) {
            return;
        }
        Map<GeoPoint,GeoPoint> nextPoints = new HashMap<GeoPoint,GeoPoint>();
        Sector second = null;
        for (Sector s : neighbors) {
            char vertex = s.getVertex(center);
            Sector.Triangle cornerTriangle = s.getCornerTriangle(vertex);
            GeoPoint[] geoPoints = cornerTriangle.getPoints(vertex);
            nextPoints.put(geoPoints[1], geoPoints[2]);
            if (second == null && countShared(s.points, sector.points) == 1) {
                second = s;
            }
        }
        assert gatedAssertion(second != null);
        if (second != null) {
            GeoPoint[] acre = new GeoPoint[nextPoints.size()];
            GeoPoint point = nextPoints.values().iterator().next();
            for (int idx = 0; idx < nextPoints.size(); idx++) {
                assert !Arrays.asList(acre).contains(point);
                acre[idx] = point;
                point = nextPoints.get(point);
                assert gatedAssertion(point != null);
                if (point == null) {
                    return;
                }
            }
            Sector[] others = new Sector[neighbors.size() - 2];
            int idx = 0;
            for (Sector s : neighbors) {
                if (s != sector && s != second) {
                    others[idx++] = s;
                }
            }
            Acre newAcre = new Acre(1, sector, second, Arrays.asList(others), center, acre);
            CartographicElementView cartographicElementView = getFor(subdivisions);
            for (Sector s : neighbors) {
                char vertex= s.getVertex(center);
                assert vertex == 'A' || vertex == 'B' || vertex == 'C';
                Edge e = Edge.values()[vertex - 'A'];
                List<Acre> vertexAcre = cartographicElementView.get(s.getSharedAcres(),
                                                                    CartographicElementView.Position.Vertex,
                                                                    e,
                                                                    false);
                assert vertexAcre.size() == 1;
                assert vertexAcre.get(0) == null;
                vertexAcre.set(0, newAcre);
            }
            consume(newAcre);
        }
    }

    private int countDistinctGlobalSectors(Set<Sector> neighbors) {
        Set<GlobalSector> globalSectors = new HashSet<GlobalSector>();
        for (Sector s : neighbors) {
            globalSectors.add(s.getParent());
        }
        return globalSectors.size();
    }

    private int countShared(GeoPoint[] a, GeoPoint[] b) {
        Set<GeoPoint> points = new HashSet<GeoPoint>(Arrays.asList(a));
        points.retainAll(Arrays.asList(b));
        return points.size();
    }


    private boolean isCCW(GeoPoint a, GeoPoint b, GeoPoint c) {
        Point i = a.toPoint(1.0);
        Point j = b.toPoint(1.0);
        Point k = c.toPoint(1.0);
        Vector cross = Vector.cross(i, j, k).normalize();
        Point l = i.translate(cross);
        double d = Vector.computeDistance(l.x, l.y, l.z);
        return d < 1.0;
    }

    private boolean gatedAssertion(boolean value) {
        return !performAssertions || value;
    }

    public static void main(String[] args) {
        final Set<String> show = new HashSet<String>(Arrays.asList("point$ triangles acres neighbors sectors".split(" ")));
//        final Set<String> show = new HashSet<String>(Arrays.asList("global-sectors sectors".split(" ")));
        final Map<Long,Acre> acresByID = new HashMap<Long,Acre>();
        Consumer<Acre> acreConsumer = new Consumer<Acre>() {
            public void consume(Acre acre) {
                assert !acresByID.containsKey(acre.id);
                acresByID.put(acre.id, acre);
            }
        };

        Point[] points = {
                new Point(1.0, -1000.0, Math.sqrt(3) * -0.5),
                new Point(-1.0, -1000.0, Math.sqrt(3) * -0.5),
                new Point(0.0, -1000.0, Math.sqrt(3) * 0.5)
        };
        GeoPoint[] geoPoints = {
            GeoPoint.fromPoint(points[0]),
            GeoPoint.fromPoint(points[1]),
            GeoPoint.fromPoint(points[2])
        };
        for (int i = 0; i < 3; i++) {
            points[i] = geoPoints[i].toPoint(1000.0);
        }

        final OutputGraph out = new OutputGraph("Acres", new Dimension(2048, 2048), 0.0, 0.0, 1000.0);
        out.onClose(new Runnable() {
            public void run() {
                System.exit(0);
            }
        });

        Sector.MAP_ACRES_BY_CENTER_POINT.set(true);
        final GlobalSector gs = new GlobalSector(0, Globe.INSTANCE, geoPoints);
        final AtomicReference<Sector[]> sectors = new AtomicReference<Sector[]>();
        gs.setInit(gs.new Initializer(5, points[0], points[1], points[2], false, null) {

            @Override
            protected Sector[] getChildren(int length) {
                sectors.set(super.getChildren(length));
                return sectors.get();
            }

            @Override
            protected Sector buildChild(int index,
                                        GeoPoint a,
                                        GeoPoint b,
                                        GeoPoint c,
                                        Point i,
                                        Point j,
                                        Point k,
                                        boolean inverted) {
                TestSector sector = new TestSector(index, inverted, gs, new GeoPoint[]{a, b, c});
                sector.setInit(sector.new TestInitializer(new Point[]{i,j,k}, show, out));
                sectors.get()[index] = sector;
                return sector;
            }
        });
        gs.getInit().run();
        for (Sector s : sectors.get()) {
            s.getInit().run();
            s.edgesBuilt = new AtomicInteger();
        }
        if (show.contains("global-sectors")) {
            Point a = gs.points[0].toPoint(1000.0);
            Point b = gs.points[1].toPoint(1000.0);
            Point c = gs.points[2].toPoint(1000.0);
            double x = (a.x + b.x + c.x) / 3.0;
            double y = (a.z + b.z + c.z) / 3.0;
            if (show.contains("labels")) {
                out.addLabel(Color.orange, gs.getIDString(), x, y + 0.075);
            }
            x *= -0.02;
            y *= -0.02;
            out.addLine(Color.orange,
                        a.x * 1.02 + x, a.z * 1.02 + y,
                        b.x * 1.02 + x, b.z * 1.02 + y,
                        c.x * 1.02 + x, c.z * 1.02 + y,
                        a.x * 1.02 + x, a.z * 1.02 + y);
        }
        if (show.contains("sectors")) {
            for (Sector s : sectors.get()) {
                Point a = s.points[0].toPoint(1000.0);
                Point b = s.points[1].toPoint(1000.0);
                Point c = s.points[2].toPoint(1000.0);
                out.addLine(Color.magenta,
                            a.x, a.z,
                            b.x, b.z,
                            c.x, c.z,
                            a.x, a.z);
                if (show.contains("labels")) {
                    double x = (a.x + b.x + c.x) / 3.0;
                    double y = (a.z + b.z + c.z) / 3.0;
                    out.addLabel(Color.magenta, s.getIDString(), x, y);
                }
            }
        }
        GeoFactory.combinePoints(Arrays.<GeoPointBasedElement>asList(sectors.get()), Globe.INSTANCE);
        Map<GeoPoint,Sector[]> sectorMap = new HashMap<GeoPoint,Sector[]>();
        for (Sector s : sectors.get()) {
            GeoFactory.addSectorByPoint(sectorMap, s, s.points[0]);
            GeoFactory.addSectorByPoint(sectorMap, s, s.points[1]);
            GeoFactory.addSectorByPoint(sectorMap, s, s.points[2]);
        }
        Collection<AcreBuilder> builders = new ArrayList<AcreBuilder>();
        for (final Sector s : sectors.get()) {
            builders.add(new AcreBuilder(GeoSpec.SECTOR_DIVISIONS.iGet(), false, s, sectorMap, null, acreConsumer) {

                @Override
                protected int[] findNearCornerAcres() {
                    int[] nearCornerAcres = super.findNearCornerAcres();
                    if (show.contains("near-edge-acres")) {
                        System.out.println("Corner A: " + nearCornerAcres[0]);
                        System.out.println("Corner B: " + nearCornerAcres[1]);
                        System.out.println("Corner C: " + nearCornerAcres[2]);
                    }
                    return nearCornerAcres;
                }

                @Override
                protected Map<Edge,int[]> findNearEdgeAcres() {
                    Map<Edge,int[]> nearEdgeAcres = super.findNearEdgeAcres();
                    if (show.contains("near-edge-acres")) {
                        System.out.println("Edge AB: " + Arrays.toString(nearEdgeAcres.get(Edge.AB)));
                        System.out.println("Edge BC: " + Arrays.toString(nearEdgeAcres.get(Edge.BC)));
                        System.out.println("Edge CA: " + Arrays.toString(nearEdgeAcres.get(Edge.CA)));
                    }
                    return nearEdgeAcres;
                }
            });
        }
        for (AcreBuilder builder : builders) {
            builder.run(0);
        }
        for (AcreBuilder builder : builders) {
            builder.secondRun().run();
        }
        for (Acre a : acresByID.values()) {
            int c = 0;
            Point p = a.center.toPoint(1000.0);
            for (Sector s : sectors.get()) {
                Acre acre = s.acres.get(p);
                if (acre != null) {
                    assert acre == a;
                    c++;
                }
            }
            assert a.flavor == Acre.Flavor.MULTI_SECTOR ? c >= 5  : ( a.flavor == Acre.Flavor.DUAL_SECTOR ? c == 2 : c == 1 );
        }

        if (show.contains("acres")) {
            Set<Long> visitedAcreIds = new HashSet<Long>();
            for (Sector s : sectors.get()) {
                Acre[] acres = s.getInnerAcres();
                for (Acre acre : acres) {
                    debugAcre(acre, acre.getIDString(), visitedAcreIds, show, out);
                }
                acres = s.getSharedAcres();
                for (Acre acre : acres) {
                    if (acre != null) {
                        debugAcre(acre, acre.getIDString(), visitedAcreIds, show, out);
                    }
                }
            }
            assert visitedAcreIds.containsAll(acresByID.keySet());
        }
        if (show.contains("neighbors")) {
            double v = 0.7, w = 1.0 - v;
            for (Acre a : acresByID.values()) {
                for (long neighborID : a.neighbors) {
                    Acre n = acresByID.get(neighborID);
                    if (n != null) {
                        Point s = a.center.toPoint(1000.0);
                        Point t = n.center.toPoint(1000.0);
                        out.addArrow(Color.white,
                                     s.x * v + t.x * w,
                                     s.z * v + t.z * w,
                                     s.x * w + t.x * v,
                                     s.z * w + t.z * v);
                    }
                }
            }
        }
    }

    private static void debugAcre(Acre a, String label, Set<Long> visitedAcreIds, Set<String> show, OutputGraph out) {
        if (visitedAcreIds != null) {
            visitedAcreIds.add(a.id);
        }
        double x = 0, y = 0;
        int l = a.points.length;
        double[] coords = new double[l * 2 + 2];
        for (int j = 0; j <= l; j++) {
            Point p = a.points[j % l].toPoint(1000.0);
            coords[j * 2] = p.x;
            coords[j * 2 + 1] = p.z;
            if (j < l) {
                x += p.x;
                y += p.z;
            }
        }
        x /= l;
        y /= l;
        for (int j = 0; j <= l; j++) {
            coords[j * 2] = coords[j * 2] * 0.85 + x * 0.15;
            coords[j * 2 + 1] = coords[j * 2 + 1] * 0.85 + y * 0.15;
        }
        if (label == null) {
            out.addLine(Color.white, coords);
        } else {
            Color polyColor, textColor = Color.black;
            AbstractCartographicElement parent = a.getParent();
            boolean inverted = parent instanceof Sector && ((Sector)parent).isInverted();
            int neighbors = 0;
            for (long id : a.neighbors) {
                if (id != 0) {
                    neighbors++;
                }
            }
            switch (a.flavor) {
                case INNER1:
                    polyColor = inverted ? Color.green : Color.red;
                    break;
                case INNER2:
                    polyColor = inverted ? Color.red : Color.green;
                    break;
                case INNER3:
//                    polyColor = Color.blue;
//                    break;
                case DUAL_SECTOR:
                    polyColor = new Color(51, 51, 255);
                    break;
                case MULTI_SECTOR:
                    polyColor = Color.gray.brighter();
                    textColor = Color.black;
                    break;
                default:
                    polyColor = Color.black;
                    textColor = Color.red;
                    break;
            }
            if (neighbors < 6) {
                polyColor = polyColor.darker().darker();
                if (textColor == Color.black) {
                    textColor = Color.white;
                }
                out.addPoly(polyColor, coords);
                double[] closed = new double[coords.length + 2];
                System.arraycopy(coords, 0, closed, 0, coords.length);
                System.arraycopy(coords, 0, closed, coords.length, 2);
                out.addLine(polyColor, closed);
            } else {
                out.addPoly(polyColor, coords);
            }
            if (show.contains("labels")) {
                if (label.contains("~")) {
                    Matcher matcher = Pattern.compile("^(.*):\\[(.*)~(.*)]$").matcher(label);
                    matcher.find();
                    out.addLabel(textColor, matcher.group(1) + "\n" + matcher.group(2) + "\n" + matcher.group(3), x, y);
                } else {
                    Matcher matcher = Pattern.compile("^(.*?):(.*)$").matcher(label);
                    matcher.find();
                    out.addLabel(textColor, matcher.group(1) + "\n" + matcher.group(2), x, y);
                }
            }
        }
    }

    private static class TestSector extends Sector {

        public TestSector(int seq,
                          boolean inverted,
                          GlobalSector gs,
                          GeoPoint[] geoPoints) {
            super(seq, inverted, gs, geoPoints);
        }

        @Override
        public void clearTriangles() {
            // no-op, retain this data
        }

        private class TestInitializer extends Sector.Initializer {

            private final Set<String> show;
            private final OutputGraph out;

            public TestInitializer(Point[] points, Set<String> show, OutputGraph out) {
                super(12, points[0], points[1], points[2], null);
                GeoSpec.SECTOR_DIVISIONS.set(subdivisions);
                this.show = show;
                this.out = out;
                triangleNormal = new Color(0.0f, 1.0f, 0.75f);
                triangleInverted = new Color(0.0f, 0.75f, 1.0f);
            }

            @Override
            protected void usingPoints(Point[] points) {
                if (show.contains("points")) {
                    int i = 0;
                    for (Point p : points) {
                        out.addPoint(Color.white, String.valueOf(i++), p.x, p.z);
                    }
                }
                super.usingPoints(points);
            }

            private final Color triangleNormal;
            private final Color triangleInverted;

            @Override
            protected void postProcess(Sector.Triangle[] triangles) {
                if (show.contains("triangles")) {
                    int i = 0;
                    for (Sector.Triangle t : triangles) {
                        GeoPoint[] trianglePoints = t.getPoints('A');
                        double x = 0, y = 0;
                        int l = trianglePoints.length;
                        double[] points = new double[l * 2 + 2];
                        for (int j = 0; j <= l; j++) {
                            Point p = trianglePoints[j % l].toPoint(1000.0);
                            points[j * 2] = p.x;
                            points[j * 2 + 1] = p.z;
                            if (j < l) {
                                x += p.x;
                                y += p.z;
                            }
                        }
                        x /= l;
                        y /= l;
                        for (int j = 0; j <= l; j++) {
                            points[j * 2] = points[j * 2] * 0.95 + x * 0.05;
                            points[j * 2 + 1] = points[j * 2 + 1] * 0.95 + y * 0.05;
                        }
                        Color color = t.inverted ? triangleInverted : triangleNormal;
                        out.addLine(color, points);
                        if (show.contains("labels")) {
                            out.addLabel(color, "" + i, x, y);
                        }
                        i++;
                    }
                }
                super.postProcess(triangles);
            }

        }
    }

}
