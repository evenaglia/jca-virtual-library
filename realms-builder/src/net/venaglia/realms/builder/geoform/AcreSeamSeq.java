package net.venaglia.realms.builder.geoform;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.MutableSimpleBounds;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.impl.OctreeMap;
import net.venaglia.gloo.util.impl.SettableEntry;
import net.venaglia.realms.builder.utils.MutableBounds;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.Acre;
import net.venaglia.realms.spec.map.GeoPoint;

/**
* User: ed
* Date: 7/2/14
* Time: 5:39 PM
*/
public class AcreSeamSeq {

    @SuppressWarnings("UnnecessaryBoxing")
    private static Long DUMMY = new Long(Long.MIN_VALUE);

    private long seq;
    private boolean frozen = false;
    private final OctreeMap<Long> map;

    AcreSeamSeq(long startSeq) {
        seq = startSeq;
        map = new OctreeMap<Long>(new BoundingSphere(Point.ORIGIN, 1024.0)) {
            @Override
            protected AbstractEntry<Long> createEntry(Long obj, double x, double y, double z) {
                return new SettableEntry<Long>(x, y, z, obj);
            }
        };
    }

    public Accessor getAccessor() {
        return new AccessorImpl();
    }

    private void touchImpl(AccessorImpl accessor, MutableSimpleBounds bounds) {
        assert !frozen;
        map.intersect(bounds, accessor.reset());
        if (!accessor.wasFound()) {
            map.add(DUMMY, bounds.getCenterX(), bounds.getCenterY(), bounds.getCenterZ());
        }
    }

    private long getImpl(AccessorImpl accessor, MutableSimpleBounds bounds) {
        assert frozen;
        map.intersect(bounds, accessor.reset());
        return accessor.getFound();
    }

    synchronized void freeze() {
        if (!frozen) {
            frozen = true;
            long seq = this.seq;
            for (SpatialMap.Entry<Long> entry : map) {
                ((SettableEntry<Long>)entry).set(seq++);
            }
            this.seq = seq;
            assert map.size() == (GeoSpec.ACRES.get() * 6 - 12) / 2;
        }
    }

    public static Point[] buildMidpoints(Acre acre) {
        return buildMidpointsImpl(acre, false);
    }

    /**
     *                    (5)-----(6)-----(8)
     *                    / \     / \     / \
     *                   /   \   /   \   /   \
     *                  /     \ /     \ /     \
     *                (3)-----(4)-----(7)-----(9)
     *                / \     / \     / \     / \
     *               /   \   /   \   /   \   /   \
     *              /     \ /     \ /     \ /     \
     *            (2)-----(1)-----(0)----(10)----(11)
     *              \     / \     / \     / \     /
     *               \   /   \   /   \   /   \   /
     *                \ /     \ /     \ /     \ /
     *               (18)----(16)----(13)----(12)
     *                  \     / \     / \     /
     *                   \   /   \   /   \   /
     *                    \ /     \ /     \ /
     *                   (17)----(15)----(14)
     *
     * @param acre The acre to build points for
     * @return A Point[] containing key points for this acre.
     */
    public static Point[] buildAcrePoints(Acre acre) {
        GeoPoint[] acrePoints = acre.points;
        assert acrePoints.length == 6 || acrePoints.length == 5;
        int l = acrePoints.length;
        Point[] points = new Point[l * 3 + 1];
        points[0] = acre.center.toPoint(1000.0);
        for (int i = 0, j = 2; i < l; i++, j += 3) {
            // 2, 5, 8, 11, 14, 17
            points[j] = acrePoints[i].toPoint(1000.0);
        }
        for (int i = 0, j = 1; i < l; i++, j += 3) {
            // 1=0-2, 4=0-5, 7=0-8, 10=0-11, 13=0-14, 16=0-17
            calcMidpointsImpl(points[0], points[j + 1], 2, points, j , false);
        }
        for (int i = 0, j = 2, k = 5; i < l; i++, j = k, k += 3) {
            // 3=2-5, 6=5-8, 9=8-11, 12=11-14, 15=14-17, 18=17-2
            calcMidpointsImpl(points[j], points[k > points.length ? 2 : k], 2, points, j + 1 , false);
        }
        return points;
    }

    private static Point[] buildMidpointsImpl(Acre acre, boolean spokes) {
        GeoPoint[] acrePoints = acre.points;
        assert acrePoints.length == 6 || acrePoints.length == 5;
        int l = acrePoints.length;
        Point center = spokes ? acre.center.toPoint(1000.0) : null;
        Point[] midpoints = new Point[l];
        for (int i = l - 1, j = 0; j < l; i = j, j++) {
            Point a = center == null ? acrePoints[i].toPoint(1000.0) : center;
            Point b = acrePoints[j].toPoint(1000.0);
            double x = (a.x + b.x) * 0.5;
            double y = (a.y + b.y) * 0.5;
            double z = (a.z + b.z) * 0.5;
            double s = 1000.0 / Vector.computeDistance(x, y, z);
            midpoints[j] = new Point(x * s, y * s, z * s);
        }
        return midpoints;
    }

    public static Point[] buildMidpoints(Point from, Point to, int steps) {
        Point[] midpoints = new Point[steps - 1];
        calcMidpointsImpl(from, to, steps, midpoints, 0, false);
        return midpoints;
    }

    public static Point[] buildInnerPoints(Point a, Point b, Point c, int steps) {
        Point[] midpoints = new Point[(steps - 1) * (steps - 2) / 2];
        Point[] aLeg = new Point[steps - 1];
        Point[] bLeg = new Point[steps - 1];
        calcMidpointsImpl(a, c, steps, aLeg, 0, false);
        calcMidpointsImpl(b, c, steps, bLeg, 0, false);
        for (int i = 0, j = 0, k = steps - 1, l = steps - 2; i < l; i++, j += k) {
            calcMidpointsImpl(aLeg[i], bLeg[i], k--, midpoints, j, false);
        }
        return midpoints;
    }

    private static void calcMidpointsImpl(Point a, Point b, int steps, Point[] midpoints, int offset, boolean includeEnds) {
        int base;
        if (includeEnds) {
            base = offset;
            midpoints[base] = a;
            midpoints[base + steps] = b;
        } else {
            base = offset - 1;
        }
        for (int i = 1; i < steps; i++) {
            double q = ((double)i) / ((double)steps);
            double p = 1.0 - q;
            double x = a.x * p + b.x * q;
            double y = a.y * p + b.y * q;
            double z = a.z * p + b.z * q;
            double s = 1000.0 / Vector.computeDistance(x, y, z);
            midpoints[base + i] = new Point(x * s, y * s, z * s);
        }
    }

    private class AccessorImpl implements Accessor {

        private MutableBounds bounds = new MutableBounds();
        private Long foundSeq;
        private SpatialMap.Consumer<Long> localConsumer = new SpatialMap.Consumer<Long>() {
            public void found(SpatialMap.Entry<Long> entry, double x, double y, double z) {
                foundSeq = entry.get();
            }
        };

        private SpatialMap.Consumer<Long> reset() {
            foundSeq = null;
            return localConsumer;
        }

        long getFound() {
            assert foundSeq != null;
            return foundSeq;
        }

        boolean wasFound() {
            return foundSeq != null;
        }

        public void touch(Point p) {
            touchImpl(this, bounds.load(p));
        }

        public long get(Point p) {
            return getImpl(this, bounds.load(p));
        }
    }

    public interface Accessor {
        void touch(Point p);
        long get(Point p);
    }
}
