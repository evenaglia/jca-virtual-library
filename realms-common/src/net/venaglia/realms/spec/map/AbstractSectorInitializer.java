package net.venaglia.realms.spec.map;

import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.realms.common.util.work.WorkQueue;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: ed
 * Date: 11/14/12
 * Time: 6:05 PM
 */
public abstract class AbstractSectorInitializer<T> implements Runnable {

    private static final Map<String,int[]> EDGE_INDICES = new ConcurrentHashMap<String,int[]>();

    protected final int subdivisions;
    protected final Point a;
    protected final Point b;
    protected final Point c;
    protected final Map<Edge,int[]> edgeElements = new EnumMap<Edge,int[]>(Edge.class);

    protected WorkQueue workQueue;

    private int nextStep = 0;
    private T[] children;

    public AbstractSectorInitializer(int subdivisions, Point a, Point b, Point c) {
        this.subdivisions = subdivisions;
        this.a = a;
        this.b = b;
        this.c = c;
    }

    protected AbstractSectorInitializer(int subdivisions,
                                        Point a,
                                        Point b,
                                        Point c,
                                        WorkQueue workQueue) {
        this(subdivisions, a, b, c);
        this.workQueue = workQueue;
        if (workQueue != null) {
            nextStep = 1;
        }
    }

    public void run() {
        int thisStep = nextStep++;
        switch (thisStep) {
            case 0:
                nextStep = 5;
                step1();
                step2();
                step3();
                step4();
                break;
            case 1:
                step1();
                break;
            case 2:
                step2();
                break;
            case 3:
                step3();
                break;
            case 4:
                step4();
                break;
        }
        if (nextStep < 5) {
            workQueue.addWorkUnit(this);
        }
    }

    private void step1() {
        children = getChildren(subdivisions * subdivisions);
        if (subdivisions == 1) {
            assert children.length == 1;
            children[0] = buildChild(0,
                                     GeoPoint.fromPoint(a),
                                     GeoPoint.fromPoint(b),
                                     GeoPoint.fromPoint(c),
                                     a, b, c,
                                     isInverted());
        }
        int i = 0;
        Point[] points = subdividePoints();
        GeoPoint[] geoPoints = toGeoPoints(points);
        usingPoints(points);
        int startR = 0;
        int startL = subdivisions + 1;
        for (int k = subdivisions; k > 0; k--) {
            int[][] strip = generateTriangles(startR, startL, k * 2 - 1);
            boolean inverted = isInverted();
            for (int[] triangle : strip) {
                children[i] = buildChild(i,
                                         geoPoints[triangle[0]],
                                         geoPoints[triangle[1]],
                                         geoPoints[triangle[2]],
                                         points[triangle[0]],
                                         points[triangle[1]],
                                         points[triangle[2]],
                                         inverted);
                i++;
                inverted = !inverted;
            }
            startR = startL;
            startL += k;
        }
        assert i == children.length;
        assert !containsNull(children);
    }

    protected void usingPoints(Point[] points) {
        // no-op
    }

    private boolean containsNull(T[] children) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, l = children.length; i < l; i++) {
            T child = children[i];
            if (child == null) {
                return true;
            }
        }
        return false;
    }

    private void step2() {
        int firstRowCount = subdivisions * 2 - 1;
        edgeElements.put(Edge.AB, pluckEdgeElements(0, 2, 0, subdivisions));
        edgeElements.put(Edge.BC, pluckEdgeElements(firstRowCount - 1, firstRowCount - 2, -2, subdivisions));
        edgeElements.put(Edge.CA, pluckEdgeElements(children.length - 1, -3, -2, subdivisions));
    }

    protected int[] getNearEdgeTriangles() {
        int[] result = new int[(subdivisions * 2 - 3) * 3];
        int n = 0;
        int[] edge;
        edge = edgeElements.get(Edge.AB);
        for (int i = 1, l = edge.length - 1; i <= l; i++) {
            result[n++] = edge[i];
            if (i < l) {
                result[n++] = edge[i] - 1;
            }
        }
        edge = edgeElements.get(Edge.BC);
        for (int i = 1, l = edge.length - 1; i <= l; i++) {
            result[n++] = edge[i];
            if (i < l) {
                result[n++] = edge[i - 1] - 1;
            }
        }
        edge = edgeElements.get(Edge.CA);
        for (int i = 1, l = edge.length - 1; i <= l; i++) {
            result[n++] = edge[i];
            if (i < l) {
                result[n++] = edge[i] + 1;
            }
        }
        assert n == result.length;
        assert noDuplicates(result);
        return result;
    }

    private boolean noDuplicates(int[] result) {
        Set<Integer> found = new HashSet<Integer>(result.length);
        for (int i : result) {
            if (found.contains(i)) {
                return false;
            }
            found.add(i);
        }
        return true;
    }

    private void step3() {
        setNeighbors(children);
    }

    private void step4() {
        postProcess(children);
    }

    protected void setNeighbors(AbstractCartographicElement[] children) {
        int below = -1;
        int base = 0;
        for (int row = subdivisions * 2 - 1; row > 0; row -= 2) {
            for (int col = row - 1; col > 0; col--) {
                setNeighbors(children[base + col], children[base + col - 1]);
            }
            if (below >= 0) {
                for (int col = row - 1; col >= 0; col -= 2 ) {
                    setNeighbors(children[base + col], children[below + col]);
                }
            }
            below = base + 1;
            base += row;
        }
        assert validNeighborCounts(children);
    }

    private boolean validNeighborCounts(AbstractCartographicElement[] children) {
        int b = subdivisions * 2 - 2;
        int c = subdivisions * subdivisions - 1;
        Set<Integer> edges = new HashSet<Integer>();
        for (int i = 2; i < b; i += 2) {
            edges.add(i);
        }
        for (int i = b + 1, s = b - 1; i < c; i += s, s -= 2) {
            edges.add(i);
            edges.add(i + s - 1);
        }
        for (int i = 0, l = subdivisions * subdivisions; i < l; i++) {
            int count = countNeighbors(children[i]);
            int expected = 3;
            if (subdivisions == 1) {
                expected = 0;
            } else if (i == 0 || i == b || i == c) {
                expected = 1;
            } else if (edges.contains(i)) {
                expected = 2;
            }
            if (count != expected) {
                System.err.printf("Child %d has %d %s, but %d %s expected: [ %s, %s, %s ]\n",
                                  i,
                                  count,
                                  count == 1 ? "neighbor" : "neighbors",
                                  expected,
                                  expected == 1 ? "was" : "were",
                                  children[i].neighbors[0] == 0 ? "\u00B7" : children[i].neighbors[0],
                                  children[i].neighbors[1] == 0 ? "\u00B7" : children[i].neighbors[1],
                                  children[i].neighbors[2] == 0 ? "\u00B7" : children[i].neighbors[2]);
                return false;
            }
        }
        return true;
    }

    protected void setNeighbors(T[] children) {
        if (children instanceof AbstractCartographicElement[]) {
            setNeighbors((AbstractCartographicElement[])children);
        }
    }

    protected void setNeighbors(AbstractCartographicElement a, AbstractCartographicElement b) {
        assert countSharedPoints(a, b) == 2;
        a.addNeighbor(b);
        b.addNeighbor(a);
    }

    protected abstract boolean isInverted();

    protected abstract T[] getChildren(int length);

    protected abstract T buildChild(int index,
                                    GeoPoint a,
                                    GeoPoint b,
                                    GeoPoint c,
                                    Point i,
                                    Point j,
                                    Point k,
                                    boolean inverted);

    protected void postProcess(T[] children) {
        // no-op
    }

    private int countSharedPoints(AbstractCartographicElement a, AbstractCartographicElement b) {
        Set<GeoPoint> points = new HashSet<GeoPoint>(Arrays.asList(a.points));
        points.retainAll(Arrays.asList(b.points));
        return points.size();
    }

    private int countNeighbors(AbstractCartographicElement element) {
        int c = 0;
        for (long id : element.neighbors) {
            if (id != 0) c++;
        }
        return c;
    }

    private int[][] generateTriangles(int startR, int startL, int l) {
        int[][] triangles = new int[l][];
        for (int i = 0; i < l; i++) {
            int[] triangle;
            if ((i & 1) == 0) {
                triangle = new int[]{ startR, ++startR, startL };
            } else {
                triangle = new int[]{ startL, startR, ++startL };
            }
            triangles[i] = triangle;
        }
        return triangles;
    }

    private GeoPoint[] toGeoPoints(Point[] points) {
        GeoPoint[] geoPoints = new GeoPoint[points.length];
        for (int i = 0; i < points.length; i++) {
            geoPoints[i] = GeoPoint.fromPoint(points[i]);
        }
        return geoPoints;
    }

    private Point[] subdividePoints() {
        Point[] points = new Point[(subdivisions + 1) * (subdivisions + 2) / 2];
        int idxA = 0;
        int idxB = subdivisions;
        int idxC = points.length - 1;
        points[idxA] = a;
        points[idxB] = b;
        points[idxC] = c;
        int idxP = 0;
        for (int i = 0, j = subdivisions; i <= subdivisions; i++, j--) {
            Point s = midpoint(a, j, c, i);
            Point t = midpoint(b, j, c, i);
            for (int k = 0, l = j; k <= j; k++, l--) {
                if (points[idxP] == null) {
                    points[idxP] = midpoint(s, l, t, k);
                }
                idxP++;
            }
        }
        assert idxP == points.length;
        return points;
    }

    private Point midpoint(Point a, int i, Point b, int j) {
        if (i == 0) {
            assert j != 0;
            return b;
        } else if (j == 0) {
            return a;
        }
        double x = a.x * i + b.x * j;
        double y = a.y * i + b.y * j;
        double z = a.z * i + b.z * j;
        double d = i + j;
        return new Point(x / d, y / d, z / d);
    }

    private int[] pluckEdgeElements(int start, int step, int accel, int count) {
        String key = String.format("%d:%d:%d:%d", start, step, accel, count);
        if (EDGE_INDICES.containsKey(key)) {
            return EDGE_INDICES.get(key);
        }

        int[] result = new int[count];
        for (int i = 0, j = start, k = step; i < count; i++, j += k, k += accel ) {
            result[i] = j;
        }
        EDGE_INDICES.put(key, result);
        return result;
    }

    public static void main(String[] args) {
        final double z = 100;
        final boolean[] success = {false};
        new AbstractSectorInitializer<Facet>(5, new Point(5,0,z), new Point(0,0,z), new Point(0,5,z)) {

            /*  points:             C

                                    20
                                  19  18
                                17  16  15
                              14  13  12  11
                            10  09  08  07  06
                          05  04  03  02  01  00
                     B                              A
             */

            private Point[] points = {
                    new Point(5,0,z),
                    new Point(4,0,z),
                    new Point(3,0,z),
                    new Point(2,0,z),
                    new Point(1,0,z),
                    new Point(0,0,z),
                    new Point(4,1,z),
                    new Point(3,1,z),
                    new Point(2,1,z),
                    new Point(1,1,z),
                    new Point(0,1,z),
                    new Point(3,2,z),
                    new Point(2,2,z),
                    new Point(1,2,z),
                    new Point(0,2,z),
                    new Point(2,3,z),
                    new Point(1,3,z),
                    new Point(0,3,z),
                    new Point(1,4,z),
                    new Point(0,4,z),
                    new Point(0,5,z)
            };

            /* facets:                 24
                                       22
                                    23    21
                                    19    17
                                 20    18    16
                                 14    12    10
                              15    13    11    09
                              07    05    03    01
                           08    06    04    02    00
             */

            Facet[] facets = {
                    f( 0, 1, 6, 0),  // 00
                    f( 6, 1, 7, 1),  // 01
                    f( 1, 2, 7, 2),  // 02
                    f( 7, 2, 8, 3),  // 03
                    f( 2, 3, 8, 4),  // 04
                    f( 8, 3, 9, 5),  // 05
                    f( 3, 4, 9, 6),  // 06
                    f( 9, 4,10, 7),  // 07
                    f( 4, 5,10, 8),  // 08 end of row
                    f( 6, 7,11, 9),  // 09
                    f(11, 7,12,10),  // 10
                    f( 7, 8,12,11),  // 11
                    f(12, 8,13,12),  // 12
                    f( 8, 9,13,13),  // 13
                    f(13, 9,14,14),  // 14
                    f( 9,10,14,15),  // 15 end of row
                    f(11,12,15,16),  // 16
                    f(15,12,16,17),  // 17
                    f(12,13,16,18),  // 18
                    f(16,13,17,19),  // 19
                    f(13,14,17,20),  // 20 end of row
                    f(15,16,18,21),  // 21
                    f(18,16,19,22),  // 22
                    f(16,17,19,23),  // 23 end of row
                    f(18,19,20,24)   // 24 end of row
            };

            int[] facetsAB = {
                    0, 2, 4, 6, 8
            };

            int[] facetsBC = {
                    8, 15, 20, 23, 24
            };

            int[] facetsCA = {
                    24, 21, 16, 9, 0
            };

            Boolean[] invertedFacets = new Boolean[25];

            Boolean[] invertedFacetsExpected = {
                    false, true, false, true, false, true, false, true, false,
                    false, true, false, true, false, true, false,
                    false, true, false, true, false,
                    false, true, false,
                    false
            };

            private Map<Facet,AbstractCartographicElement> elementsByFacet =
                    new HashMap<Facet,AbstractCartographicElement>();

            private Facet f(int a, int b, int c, int d) {
                return new Facet(points[a], points[b], points[c], new Point(d,d,d));
            }

            @Override
            protected boolean isInverted() {
                return false;
            }

            @Override
            protected Facet[] getChildren(int length) {
                return new Facet[length];
            }

            @Override
            protected Facet buildChild(int index,
                                       GeoPoint a,
                                       GeoPoint b,
                                       GeoPoint c,
                                       Point i,
                                       Point j,
                                       Point k,
                                       boolean inverted) {
                Facet facet = new Facet(i, j, k, new Point(index, index, index));
                AbstractCartographicElement element =
                        new AbstractCartographicElement(index + 1, 5, 0, null, 3, a, b, c) {};
                assert !elementsByFacet.containsKey(facet);
                elementsByFacet.put(facet, element);
                invertedFacets[index] = inverted;
                return facet;
            }

            @Override
            protected void setNeighbors(Facet[] children) {
                AbstractCartographicElement[] elements = new AbstractCartographicElement[children.length];
                int i = 0;
                for (Facet f : children) {
                    assert elementsByFacet.containsKey(f);
                    elements[i++] = elementsByFacet.get(f);
                }
                setNeighbors(elements);
            }

            @Override
            protected void postProcess(Facet[] children) {
                assert Arrays.equals(children, facets);
                assert Arrays.equals(edgeElements.get(Edge.AB), facetsAB);
                assert Arrays.equals(edgeElements.get(Edge.BC), facetsBC);
                assert Arrays.equals(edgeElements.get(Edge.CA), facetsCA);
                assert Arrays.equals(invertedFacetsExpected, invertedFacets);
                success[0] = true;
            }
        }.run();
        assert success[0];
    }
}
