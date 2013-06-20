package net.venaglia.realms.spec.map;

import static net.venaglia.realms.spec.GeoSpec.*;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.impl.SweepAndPrune;
import net.venaglia.realms.common.util.work.WorkQueue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 7/15/12
 * Time: 9:38 PM
 *
 * A sector represents one small piece of a global sector; representing
 * approximately 1/24500 of the globe's surface, or 1/1225th of a global
 * sector.
 *
 * To form a sector, the sides of a global sector are evenly divided into 36
 * segments. A "grid" is drawn across the triangle between points equidistant
 * from any single vertex of the global sector. This forms 1296 triangular
 * sectors within the global sector.
 *
 * The sides of each sector are further divided into 300 equal parts, creating
 * 90000 inner triangles. The triangles on each point are combined with point
 * triangles of 5 other sectors, 198 edge triangles pairs and the 99 inner
 * triangles between them are combined with neighboring sectors, and hexagonal
 * groupings of the remaining 89700 internal triangles are combined to
 * create a total of 14950 hexagonal Acres.
 *
 * The world consists of:
 *                  20  GlobalSectors
 *              24,500  Sectors
 *   2,205,000,800,000  Inner triangles
 *         367,500,003  Acres
 *                           12  joining 5 global sectors
 *                       12,238  joining 6 sectors
 *                      103,950  joining 2 global sectors
 *                    3,630,250  joining 2 sectors
 */
public class Sector extends AbstractCartographicElement {

    // the (x,y,z) center point of these acres are the keys in this map, r=1 is used to compute (x,y,z)
    public final SpatialMap<Acre> acres = new SweepAndPrune<Acre>();
    private final boolean inverted;
//    public final SpatialMap<Acre> acres = new DummySpatialMap<Acre>();
    public Triangle[] triangles;
    public AtomicInteger edgesBuilt = null;
    public ReadWriteLock sharedAcresLock = null;

    private Acre[] innerAcres;
    private Acre[] sharedAcres;

    public Sector(int seq, boolean inverted, GlobalSector globalSector, GeoPoint... points) {
        super(seq, 11, 32, globalSector, 3, points);
        this.inverted = inverted;
    }

    @Override
    public GlobalSector getParent() {
        return (GlobalSector)super.getParent();
    }

    @Override
    protected Acre findAcreByID(long globalSectorId1, long sectorId1, long globalSectorId2, long sectorId2, long id) {
        if (sectorId1 != this.id) {
            return null;
        }
        int seq = (int)(id & 0xFFFFL);
        if (globalSectorId2 == 0 && sectorId2 == 0) {
            if (seq == 0 || seq > innerAcres.length) {
                return null;
            }
            return innerAcres[seq - 1];
        }
        if (seq == 1) {
            for (int i = 0, l = sharedAcres.length, j = l / 3; i < l; i += j) {
                if (sharedAcres[i].id == id) {
                    return sharedAcres[i];
                }
            }
        }
        for (int i = seq, l = sharedAcres.length, j = l / 3, k = j - i; i < l; i += j, k += j) {
            if (sharedAcres[i].id == id) {
                return sharedAcres[i];
            }
            if (sharedAcres[k].id == id) {
                return sharedAcres[k];
            }
        }
        return null;
    }

    public Acre[] getInnerAcres() {
        return innerAcres;
    }

    public Acre[] getSharedAcres() {
        return sharedAcres;
    }

    public void clearTriangles() {
        this.triangles = null;
        this.edgesBuilt = null;
    }

    public Triangle getCornerTriangle(char vertex) {
        switch (vertex) {
            case 'A':
                return triangles[0];
            case 'B':
                int divisions = (int)SECTOR_DIVISIONS.get();
                return triangles[divisions * 2 - 2];
            case 'C':
                return triangles[triangles.length - 1];
            default:
                throw new IllegalArgumentException("Invalid vertex '" + vertex + "', must be one of 'A', 'B', or 'C'");
        }
    }

    public char getVertex(GeoPoint vertex) {
        if (vertex.equals(points[0])) {
            return 'A';
        }
        else if (vertex.equals(points[1])) {
            return 'B';
        }
        else if (vertex.equals(points[2])) {
            return 'C';
        }
        return '?';
    }

    public GeoPoint[] getPoints(GeoPoint base) {
        if (base.equals(points[0])) {
            return new GeoPoint[]{ points[0], points[1], points[2] };
        }
        else if (base.equals(points[1])) {
            return new GeoPoint[]{ points[1], points[2], points[0] };
        }
        else if (base.equals(points[2])) {
            return new GeoPoint[]{ points[2], points[0], points[1] };
        }
        return null;
    }

    public boolean isInverted() {
        return inverted;
    }

    public class Initializer extends AbstractSectorInitializer<Triangle> {

        public Initializer(Point a,
                           Point b,
                           Point c,
                           WorkQueue q) {
            this((int)SECTOR_DIVISIONS.get(), a, b, c, q);
        }

        public Initializer(int divisions, Point a, Point b, Point c, WorkQueue q) {
            super(divisions, a, b, c, q);
        }

        @Override
        protected boolean isInverted() {
            return inverted;
        }

        @Override
        protected Triangle[] getChildren(int length) {
            triangles = new Triangle[length];
            return triangles;
        }

        @Override
        protected Triangle buildChild(int index,
                                      GeoPoint a,
                                      GeoPoint b,
                                      GeoPoint c,
                                      Point i,
                                      Point j,
                                      Point k,
                                      boolean inverted) {
            return new Triangle(index, a, b, c, i, j, k, inverted);
        }

        @Override
        protected void postProcess(Triangle[] children) {
            innerAcres = new Acre[subdivisions * (subdivisions - 3) / 6 + 1];
            sharedAcres = new Acre[subdivisions];
            sharedAcresLock = new ReentrantReadWriteLock();
        }
    }

    public static class Triangle implements GeoPointBasedElement {

//        private static final AtomicInteger count = new AtomicInteger();

        public final int index;
        public GeoPoint a;
        public GeoPoint b;
        public GeoPoint c;
        public final Point i;
        public final Point j;
        public final Point k;
        public final boolean inverted;

        private Triangle(int index, GeoPoint a, GeoPoint b, GeoPoint c, Point i, Point j, Point k, boolean inverted) {
            this.index = index;
            this.a = a;
            this.b = b;
            this.c = c;
            this.i = i;
            this.j = j;
            this.k = k;
            this.inverted = inverted;
//            int cnt = count.incrementAndGet();
//            if (cnt % 1000000 == 0) {
//                System.out.println(cnt + " triangles...");
//            }
        }

        public int countGeoPoints() {
            return 3;
        }

        public GeoPoint getGeoPoint(int index) {
            switch (index) {
                case 0:
                    return a;
                case 1:
                    return b;
                case 2:
                    return c;
            }
            throw new ArrayIndexOutOfBoundsException(index);
        }

        public void setGeoPoint(int index, GeoPoint geoPoint) {
            assert geoPoint.toPoint(1000.0).computeDistance(getGeoPoint(index).toPoint(1000.0)) < 0.0005;
            switch (index) {
                case 0:
                    a = geoPoint;
                    break;
                case 1:
                    b = geoPoint;
                    break;
                case 2:
                    c = geoPoint;
                    break;
                default:
                    throw new ArrayIndexOutOfBoundsException(index);
            }
        }

        public GeoPoint[] getPoints(char startingVertex) {
            switch (startingVertex) {
                case 'A':
                    return new GeoPoint[]{ a, b, c };
                case 'B':
                    return new GeoPoint[]{ b, c, a };
                case 'C':
                    return new GeoPoint[]{ c, a, b };
                default:
                    throw new IllegalArgumentException("Invalid starting vertex '" + startingVertex + "', must be one of 'A', 'B', or 'C'");
            }
        }

        @Override
        public String toString() {
            return String.format("(%s,%s,%s)", a, b, c);
        }
    }
}
