package net.venaglia.realms.common.map.things.surface;

import net.venaglia.common.util.Pair;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.util.debug.OutputGraph;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 4/20/14
 * Time: 7:52 PM
 */
public class ZoneDivision {

    public static final ZoneDivision ROOT;
    public static final ZoneDivision[] DIVISIONS;
    public static final ZonePoint[] POINTS;

    static {
        Pair<ZoneDivision,ZoneDivision[]> build = build();
        ROOT = build.getA();
        DIVISIONS = build.getB();
        POINTS = new ZonePoint[2145];
        for (ZonePoint point : ROOT.points) {
            POINTS[point.index] = point;
        }
        for (ZoneDivision zd : DIVISIONS) {
            for (ZonePoint point : zd.points) {
                if (POINTS[point.index] == null) {
                    POINTS[point.index] = point;
                }
            }
        }
    }

    public final ZonePoint[] points;

    private final int seq;

    private final int indexA; // value are 0 to 2144
    private final int indexB; // value are 0 to 2144
    private final int indexC; // value are 0 to 2144
    private final int[] indices; // 45 elements, value are 0 to 2144

    private final double[] fractionX; // a <--> b
    private final double[] fractionY; // a-b <--> c

    private ZoneDivision(int seq,
                         int indexA,
                         int indexB,
                         int indexC,
                         int[] indices,
                         double[] fractionX,
                         double[] fractionY) {
        if (indices.length != fractionX.length || fractionX.length != fractionY.length) {
            throw new IllegalArgumentException();
        }
        this.seq = seq;
        this.indexA = indexA;
        this.indexB = indexB;
        this.indexC = indexC;
        this.indices = indices;
        this.fractionX = fractionX;
        this.fractionY = fractionY;
        this.points = new ZonePoint[indices.length];
        for (int i = 0; i < indices.length; i++) {
            this.points[i] = new ZonePoint(i);
        }
    }

    private void drawDebug(OutputGraph debug, Point[] allPoints, Set<String> elements) {
        Point a = allPoints[indexA];
        Point b = allPoints[indexB];
        Point c = allPoints[indexC];
        Point m = midPoint(a, b, c);
        if (elements.contains("inner")) {
            double[] points = new double[indices.length * 2];
            for (int i = 0, j = 0; i < indices.length; i++) {
                Point p = allPoints[indices[i]];
                points[j++] = p.x;
                points[j++] = p.y;
            }
            debug.addPixels(Color.LIGHT_GRAY, points);
        }
        if (elements.contains("fractions")) {
            for (int i = 0; i < indices.length; i++) {
                Point p = allPoints[indices[i]];
                debug.addArrow(Color.RED, p.x, p.y, p.x + fractionX[i] * 3.0 + 1, p.y);
                debug.addArrow(Color.RED, p.x, p.y, p.x, p.y + fractionY[i] * 3.0 + 1);
            }
        }
        if (elements.contains("edges")) {
            Point i = closerTo(a, m, 1.0);
            Point j = closerTo(b, m, 1.0);
            Point k = closerTo(c, m, 1.0);
            debug.addLine(Color.CYAN, i.x, i.y, j.x, j.y, k.x, k.y, i.x, i.y);
        }
        if (elements.contains("seq")) {
            debug.addLabel(Color.WHITE, String.valueOf(seq), m.x, m.y);
        }
        if (elements.contains("corners")) {
            debug.addPoint(Color.WHITE, String.valueOf(indexA), a.x, a.y);
            debug.addPoint(Color.WHITE, String.valueOf(indexB), b.x, b.y);
            debug.addPoint(Color.WHITE, String.valueOf(indexC), c.x, c.y);
        }
    }

    private Point midPoint(Point a, Point b, Point c) {
        return new Point((a.x + b.x + c.x) / 3.0, (a.y + b.y + c.y) / 3.0, (a.z + b.z + c.z) / 3.0);
    }

    private Point closerTo(Point p, Point toward, double d) {
        Vector v = Vector.betweenPoints(p, toward);
        if (v.l <= d) return toward;
        return p.translate(v.normalize(d));
    }

    @Override
    public String toString() {
        return "ZoneDivision{" +
                "\n\tseq=" + seq +
                ",\n\tindexA=" + indexA +
                ",\n\tindexB=" + indexB +
                ",\n\tindexC=" + indexC +
                ",\n\tindices=" + Arrays.toString(indices) +
                ",\n\tfractionX=" + Arrays.toString(fractionX) +
                ",\n\tfractionY=" + Arrays.toString(fractionY) +
                "\n}";
    }

    private static Pair<ZoneDivision,ZoneDivision[]> build() {
        Point[] corners = new Point[] {
                new Point(512, 0, 0),
                new Point(0, 0, 0),
                new Point(256, 256 * Math.sqrt(3), 0)
        };
        Point a = corners[0];
        Point b = corners[1];
        Point c = corners[2];
        Map<Point,Integer> pointIndices = new HashMap<Point,Integer>(128);
        for (Pair<Point,double[]> pair : generatePoints(64, a, b, c)) {
            Point point = pair.getA();
            pointIndices.put(snapToGrid(point), pointIndices.size());
        }
        List<Pair<Point,double[]>> points = generatePoints(8, a, b, c);
        int[] drawOrder = getDrawOrder(ZoneUtils.TRIANGLE_STRIP_DRAW_ORDER_COARSE);
        List<ZoneDivision> zones = new ArrayList<ZoneDivision>(64);
        double[] fractionX = new double[45];
        double[] fractionY = new double[45];
        for (int i = 0, l = drawOrder.length; i < l; i += 3) {
            int indexA = drawOrder[i];
            int indexB = drawOrder[i + 1];
            int indexC = drawOrder[i + 2];
            a = points.get(indexA).getA();
            b = points.get(indexB).getA();
            c = points.get(indexC).getA();
            int[] indices = new int[45];
            int j = 0;
            for (Pair<Point,double[]> pair : generatePoints(8, a, b, c)) {
                indices[j] = pointIndices.get(snapToGrid(pair.getA()));
                if (i == 0) {
                    fractionX[j] = pair.getB()[0];
                    fractionY[j] = pair.getB()[1];
                }
                j++;
            }
            indexA = pointIndices.get(snapToGrid(a));
            indexB = pointIndices.get(snapToGrid(b));
            indexC = pointIndices.get(snapToGrid(c));
            zones.add(new ZoneDivision(zones.size(), indexA, indexB, indexC, indices, fractionX, fractionY));
        }

        int[] indices = new int[points.size()];
        fractionX = new double[points.size()];
        fractionY = new double[points.size()];
        int j = 0;
        for (Pair<Point, double[]> pair : points) {
            indices[j] = pointIndices.get(snapToGrid(pair.getA()));
            fractionX[j] = pair.getB()[0];
            fractionY[j] = pair.getB()[1];
            j++;
        }
        ZoneDivision root = new ZoneDivision(-1,
                                             pointIndices.get(snapToGrid(corners[0])),
                                             pointIndices.get(snapToGrid(corners[1])),
                                             pointIndices.get(snapToGrid(corners[2])),
                                             indices,
                                             fractionX,
                                             fractionY);
        return new Pair<ZoneDivision,ZoneDivision[]>(root,zones.toArray(new ZoneDivision[zones.size()]));
    }

    private static Point snapToGrid(Point p) {
        return new Point(Math.round(p.x), Math.round(p.y), 0);
    }

    private static List<Pair<Point,double[]>> generatePoints(int divisions, Point a, Point b, Point c) {
        List<Pair<Point,double[]>> result = new ArrayList<Pair<Point,double[]>>((divisions + 2) * (divisions + 1) / 2);
        for (int j = divisions; j > 0; j--) {
            int j_ = divisions - j;
            double fractionY = ((double)j) / divisions;
            Point s = new Point(a.x * j + c.x * j_, a.y * j + c.y * j_, 0);
            Point t = new Point(b.x * j + c.x * j_, b.y * j + c.y * j_, 0);
            for (int i = j; i >= 0; i--) {
                int i_ = j - i;
                double fractionX = ((double)i) / j;
                double rescale = 1.0 / (divisions * j);
                Point p = new Point((s.x * i + t.x * i_) * rescale, (s.y * i + t.y * i_) * rescale, 0);
                result.add(new Pair<Point,double[]>(p, new double[]{ fractionX, fractionY }));
            }
        }
        result.add(new Pair<Point,double[]>(c, new double[]{ 0, 0 }));
        return result;
    }

    private static int[] getDrawOrder(int[][] strips) {
        List<Integer> drawOrder = new ArrayList<Integer>(2500);
        for (int[] strip : strips) {
            for (int i = 2; i < strip.length; i++) {
                drawOrder.add(strip[i - 2]);
                drawOrder.add(strip[i]);
                drawOrder.add(strip[i - 1]);
            }
        }
        int[] result = new int[drawOrder.size()];
        for (int i = 0; i < drawOrder.size(); i++) {
            result[i] = drawOrder.get(i);
        }
        return result;
    }

    public class ZonePoint {
        private final int index;
        private final double partX;
        private final double partY;
        private final double partA;
        private final double partB;
        private final double partC;

        public ZonePoint(int ix) {
            index = indices[ix];
            partX = ZoneDivision.this.fractionX[ix];
            partY = ZoneDivision.this.fractionY[ix];
            partA = partX * partY;
            partC = 1.0 - partY;
            partB = (1.0 - partX) * partY;
        }

        public <O> O get(O[] array) {
            return array[index];
        }

        public void calculateFor(Point[] surface, RandomSequence random, float smoothness) {
            calculateFor(surface, random.getNext() * smoothness);
        }

        public void calculateFor(Point[] surface, double height) {
            if (surface[index] == null) {
                Point a = surface[indexA];
                Point b = surface[indexB];
                Point c = surface[indexC];
                Vector n = Vector.cross(a, c, b);
                Point p = new Point(a.x * partA + b.x * partB + c.x * partC,
                                    a.y * partA + b.y * partB + c.y * partC,
                                    a.z * partA + b.z * partB + c.z * partC).translate(n.normalize(height));
                surface[index] = p;
            }
        }
    }

    public static void main(String[] args) {
        OutputGraph debug = new OutputGraph("zones", 1024, 256, 220, 10.6667);
        debug.onClose(new Runnable() {
            public void run() {
                System.exit(0);
            }
        });
        Point[] allPoints;
        Set<String> elements = new HashSet<String>(Arrays.asList("corners edges fractions".split(" ")));

        {
            List<Point> buffer = new ArrayList<Point>(2145);
            Point a = new Point(512, 0, 0);
            Point b = new Point(0, 0, 0);
            Point c = new Point(256, 256 * Math.sqrt(3), 0);
            for (Pair<Point,double[]> pointPair : generatePoints(64, a, b, c)) {
                buffer.add(pointPair.getA());
            }
            allPoints = buffer.toArray(new Point[buffer.size()]);
        }

        for (ZoneDivision division : DIVISIONS) {
            division.drawDebug(debug, allPoints, elements);
            System.out.println(division);
        }
        System.out.println("DIVISIONS.length = " + DIVISIONS.length);
    }
}
