package net.venaglia.realms.builder.map;

import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.primitives.Polygon;
import net.venaglia.realms.common.physical.geom.primitives.QuadSequence;
import net.venaglia.realms.common.physical.geom.primitives.TriangleSequence;
import net.venaglia.realms.common.util.Inside2DShape;

import java.util.Arrays;
import java.util.Comparator;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 9:38 PM
 *
 * A sector of the globe, of arbitrary size, defined by a convex polygon on it's surface.
 */
public abstract class AbstractCartographicElement implements GeoPointBasedElement {

    private final AbstractCartographicElement parent;

    public final long[] neighbors;

    private static final Runnable NO_OP = new Runnable() {
        public void run() { }
    };
    private static final long[] BITS;

    protected static final long ACRE_BIT = 0x1000000000000L;

    static {
        BITS = new long[48];
        for (int i = 0; i < BITS.length; i++) {
            BITS[i] = (1L << i) - 1;
        }
    }

    public final long id; // bits: 0000 0000 0000 0000 - gggg gsss ssss ssss - 0000 0000 0000 00tt - tttt tttt tttt tttt
    public static final Comparator<AbstractCartographicElement> ELEMENT_ORDER = new Comparator<AbstractCartographicElement>() {
        public int compare(AbstractCartographicElement a, AbstractCartographicElement b) {
            return a.id < b.id ? 1 : a.id > b.id ? -1 : 0;
        }
    };
    //    single-sector acre bits: 0000 0000 0000 0001 - gggg gsss ssss ssss - 0000 0000 0000 0000 - aaaa aaaa aaaa aaaa
    //    double-sector acre bits: 0000 0000 0000 0001 - gggg gsss ssss ssss - GGGG GSSS SSSS SSSS - 0000 0000 00aa aaaa
    //     multi-sector acre bits: 0000 0000 0000 0001 - gggg gsss ssss ssss - GGGG GSSS SSSS SSSS - 0000 0000 0000 0000
    //
    // Fields:
    //     gggg - the seq of the global sector [1..20]
    //     ssss - the seq of the sector within its global sector [1..1296]
    //     tttt - the seq of the triangle within its sector [1..90000]
    //     aaaa - the seq of the acre within its sector [1..14950]
    // Multi-sector acres:
    //     In doulbe-sector acres, ggss is derived from the largest id of the two sectors, GGSS is the lesser of those two.
    //     For acres that span more than two sectors, ggss is derived from the largest id. GGSS is taken from the highest
    //     id of a sector NOT adjacent to highest.

    private final long mask;

    private Runnable initializeSubElements = NO_OP;

    /**
     * The points defining the boundary of this sector, listed counterclockwise.
     */
    public final GeoPoint[] points;

    protected AbstractCartographicElement(int seq, int bits, int position, AbstractCartographicElement parent, int countNeighbors, GeoPoint... points) {
        this.id = computeId(seq, bits, position, parent == null ? 0 : parent.id);
        this.mask = computeId(-1, bits, position, parent == null ? 0 : parent.mask);
        this.parent = parent;
        this.neighbors = new long[countNeighbors];
        this.points = points;
    }

    protected AbstractCartographicElement(long id, AbstractCartographicElement parent, int countNeighbors, GeoPoint[] points) {
        this.id = id;
        this.mask = -1L;
        this.parent = parent;
        this.neighbors = new long[countNeighbors];
        this.points = points;
    }

    protected static long computeId(int seq, int bits, int position, long parentId) {
        return parentId | (((seq + 1L) & BITS[bits]) << position);
    }

    public void setInit(Runnable runnable) {
        if (initializeSubElements == null) {
            throw new IllegalStateException();
        }
        initializeSubElements = runnable;
    }

    public Runnable getInit() {
        return new Runnable() {
            public void run() {
                init();
            }
        };
    }

    public AbstractCartographicElement getParent() {
        return parent;
    }

    protected final void init() {
        if (initializeSubElements != null) {
            Runnable runnable = initializeSubElements;
            initializeSubElements = null;
            runnable.run();
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T unsafe(Object o) {
        return (T)o;
    }

    public void addNeighbor(AbstractCartographicElement neighbor) {
        assert neighbor != this;
        assert getClass().equals(neighbor.getClass());
        if (!addNeighborImpl(neighbor.id)) {
            throw new AssertionError("Failed to add a neighbor, not enough space in array: " + neighbor + " --> add to --> " + this);
        }
    }

    public void addNeighbor(long neighborId) {
        if (!addNeighborImpl(neighborId)) {
            throw new AssertionError("Failed to add a neighbor, not enough space in array: [" + neighborId + "] --> add to --> " + this);
        }
    }

    public boolean addNeighborImpl(long neighborId) {
        assert id != neighborId;
//        assert countSharedPoints(points, neighbor.points) == 2;
        synchronized (neighbors) {
            for (int i = 0, l = neighbors.length; i < l; i++) {
                if (neighbors[i] == 0) {
                    neighbors[i] = neighborId;
                    return true;
                } else if (neighbors[i] == neighborId) {
                    // already present, exit silently
                    return true;
                }
            }
        }
        return false;
    }

    public boolean inside(GeoPoint geoPoint) {
        Point relativeTo = geoPoint.toPoint(1000.0);
        int l = this.points.length;
        double[] points = new double[l * 2];
        double x, y;
        for (int i = 0; i < l; i++) {
            Point p = this.points[i].toPoint(1000.0);
            switch (getLeastSignificantAxis()) {
                case 'X':
                    points[i * 2] =  p.y - relativeTo.y;
                    points[i * 2 + 1] =  p.z - relativeTo.z;
                    break;
                case 'Y':
                    points[i * 2] =  p.x - relativeTo.x;
                    points[i * 2 + 1] =  p.z - relativeTo.z;
                    break;
                case 'Z':
                    points[i * 2] =  p.x - relativeTo.x;
                    points[i * 2 + 1] =  p.y - relativeTo.y;
                    break;
            }
        }
        return Inside2DShape.INSTANCE.test(points);
    }

    private char leastSignificantAxis = '?';

    private char getLeastSignificantAxis() {
        if (leastSignificantAxis == '?') {
            double minX = Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxX = Double.MIN_VALUE;
            double maxY = Double.MIN_VALUE;
            double maxZ = Double.MIN_VALUE;
            for (GeoPoint gp : points) {
                Point p = gp.toPoint(1000.0);
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                minZ = Math.min(minZ, p.z);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
                maxZ = Math.max(maxZ, p.z);
            }
            double deltaX = maxX - minX;
            double deltaY = maxY - minY;
            double deltaZ = maxZ - minZ;
            double[] deltas = { deltaX, deltaY, deltaZ };
            Arrays.sort(deltas);
            if (deltaX == deltas[0]) {
                leastSignificantAxis = 'X';
            } else if (deltaY == deltas[0]) {
                leastSignificantAxis = 'Y';
            } else {
                leastSignificantAxis = 'Z';
            }
        }
        return leastSignificantAxis;
    }

    public BoundingVolume<?> getBounds(double radius) {
        int l = this.points.length;
        Point[] points = new Point[l * 2 + 2];
        double x = 0.0, y = 0.0, z = 0.0;
        for (int i = 0; i < l; i++) {
            GeoPoint gp = this.points[i];
            points[i] = gp.toPoint(radius * 0.95);
            points[i + l] = gp.toPoint(radius * 1.05);
            Point p = gp.toPoint(radius);
            x += p.x;
            y += p.y;
            z += p.z;
        }
        x /= points.length;
        y /= points.length;
        z /= points.length;
        Vector center = new Vector(x, y, z);
        points[l * 2] = Point.ORIGIN.translate(center.normalize(radius * 0.95));
        points[l * 2 + 1] = Point.ORIGIN.translate(center.normalize(radius * 1.05));
        return new BoundingBox(points);
    }

    public <G extends AbstractCartographicElement> G findByID(long id) {
        if (this.id == id) {
            return unsafe(this);
        }
        if (this.id != 0 && ((this.id & mask) != (id & mask) || (this.id & 0xFFFE000000000000L) != 0L)) {
            return null; // not in this element
        }
        if ((id & ACRE_BIT) == ACRE_BIT) {
            long sectorId1 = id & 0xFFFF00000000L;
            long sectorId2 = (id & 0xFFFF0000L) << 16;
            return unsafe(findAcreByID((sectorId1 & 0xF80000000000L),
                          sectorId1,
                          (sectorId2 & 0xF80000000000L),
                          sectorId2,
                          id));
        }
        if ((id & 0xFFL) == 0) {
            if ((id & 0x7FF00000000L) == 0) {
                return unsafe(findGlobalSectorById(id));
            } else {
                return unsafe(findSectorById((id & 0xFFFF00000000L), id));
            }
        } else {
            return unsafe(findTriangleById((id & 0xF80000000000L), (id & 0xFFFF00000000L), id));
        }
    }

    private Object findTriangleById(long globalSectorId, long sectorId, long id) {
        return null;
    }

    protected Acre findAcreByID(long globalSectorId1, long sectorId1, long globalSectorId2, long sectorId2, long id) {
        return null;
    }

    protected Sector findSectorById(long globalSectorId, long id) {
        return null;
    }

    protected GlobalSector findGlobalSectorById(long id) {
        return null;
    }

    public String getIDString() {
        return toIdString(id);
    }

    public static String toIdString(long id) {
        if (id == 0) {
            return "Globe[1]";
        }
        int gs = (int)(((id & 0xf80000000000L) >> 43) & 0x1f);
        int s = (int)(((id & 0x7ff00000000L) >> 32) & 0x7ff);
        if ((id & 0x1000000000000L) == 0) {
            int t = (int)(id & 0x3ffffL);
            if (s == 0) {
                return String.format("GS[%d]", gs);
            }
            if (t == 0) {
                return String.format("S[%d]:GS[%d]", s, gs);
            }
            return String.format("T[%d]:S[%d]:GS[%d]", t, s, gs);
        } else {
            int gs2 = (int)(((id & 0xf8000000L) >> 27) & 0x1f);
            int s2 = (int)(((id & 0x7ff0000L) >> 16) & 0x7ff);
            int a = (int)(id & 0xffffL);
            if (gs2 == 0) {
                return String.format("A[%d]:S[%d]:GS[%d]", a, s, gs);
            }
            return String.format("A[%d]:[S[%d]:GS[%d]~S[%d]:GS[%d]]", a, s, gs, s2, gs2);
        }
    }

    public Shape get3DShape(double radius) {
        int length = this.points.length;
        Point[] points = new Point[length];
        for (int i = 0; i < length; i++) {
            points[i] = this.points[i].toPoint(radius);
        }
        switch (length) {
            case 0:
            case 1:
            case 2:
                throw new IllegalStateException("Too few points");
            case 3:
                return new TriangleSequence(points);
            case 4:
                return new QuadSequence(points);
            default:
                double x = 0.0, y = 0.0, z = 0.0;
                for (int i = 0; i < length; i++) {
                    x += points[i].x;
                    y += points[i].y;
                    z += points[i].z;
                }
                x /= length;
                y /= length;
                z /= length;
                return new Polygon(Vector.betweenPoints(Point.ORIGIN, new Point(x, y, z)).normalize(), points);
        }
    }

    public int countGeoPoints() {
        return points.length;
    }

    public GeoPoint getGeoPoint(int index) {
        return points[index];
    }

    public void setGeoPoint(int index, GeoPoint geoPoint) {
        assert geoPoint.toPoint(1000.0).computeDistance(points[index].toPoint(1000.0)) < 0.0005;
        points[index] = geoPoint;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(getClass().getSimpleName());
        buffer.append("(");
        buffer.append(getIDString());
        buffer.append(" @ ");
        boolean first = true;
        for (GeoPoint gp : points) {
            if (first) first = false;
            else buffer.append(",");
            buffer.append(gp);
        }
        buffer.append(")");
        return buffer.toString();
    }
}
