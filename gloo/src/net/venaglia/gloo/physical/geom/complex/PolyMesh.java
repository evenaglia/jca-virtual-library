package net.venaglia.gloo.physical.geom.complex;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.ZMap;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.base.AbstractFacetedShape;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.common.util.Pair;
import net.venaglia.common.util.RangeBasedIntegerSet;
import net.venaglia.gloo.util.debug.OutputGraph;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

/**
 * User: ed
 * Date: 1/29/13
 * Time: 8:24 AM
 */
public class PolyMesh extends AbstractFacetedShape<PolyMesh> {

    private final int[][] strips;
    private final int[] facets;
    private final StripType[] stripTypes;

    public final Vector[] normals;

    private PolyMesh(Point[] points, Vector[] normals, int[][] strips, StripType[] stripTypes) {
        super(points);
        this.normals = normals;
        this.strips = strips;
        this.stripTypes = stripTypes;
        int l = stripTypes.length;
        this.facets = new int[l + 1];
        int count = 0;
        for (int i = 0; i < l; i++) {
            count += stripTypes[i] == StripType.TRIANGLE ? strips[i].length - 2 : 1;
            this.facets[i + 1] = count;
        }
    }

    public Vector getNormal(int index) {
        return normals[index];
    }

    public int facetCount() {
        return facets[facets.length - 1];
    }

    public Facet.Type getFacetType() {
        return Facet.Type.MIXED;
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int i = Arrays.binarySearch(facets, index);
        if (i <= 0) {
            i = -2 - i;
        }
        int[] strip = strips[i];
        if (stripTypes[i] == StripType.POLYGON) {
            int j = index - facets[i];
            if (j % 2 == 0) {
                facetBuilder.usePoints(strip[j], strip[j + 1], strip[j + 2]);
            } else {
                facetBuilder.usePoints(strip[j], strip[j + 2], strip[j + 1]);
            }
        } else if (strip.length == 3) {
            facetBuilder.usePoints(strip[0], strip[1], strip[2]);
        } else if (strip.length == 4) {
            facetBuilder.usePoints(strip[0], strip[1], strip[2], strip[4]);
        } else {
            int[] extra = new int[strip.length - 4];
            System.arraycopy(strip, 4, extra, 0, extra.length);
            facetBuilder.usePoints(strip[0], strip[1], strip[2], strip[4], extra);
        }
    }

    @Override
    protected PolyMesh build(Point[] points, XForm xForm) {
        return new PolyMesh(points, xForm.apply(normals), strips, stripTypes);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        for (int i = 0, l = strips.length; i < l; i++) {
            int[] strip = strips[i];
            if (stripTypes[i] == StripType.POLYGON) {
                buffer.start(GeometryBuffer.GeometrySequence.POLYGON);
                for (int j : strip) {
                    buffer.normal(normals[j]);
                    buffer.vertex(points[j]);
                }
                buffer.end();
            } else {
                buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_STRIP);
                for (int j : strip) {
                    buffer.normal(normals[j]);
                    buffer.vertex(points[j]);
                }
                buffer.end();
            }
        }
    }

    private enum StripType {
        POLYGON, TRIANGLE
    }

    public static class Builder {

        private final OutputGraph out;
        private final Set<String> debug;

        public Builder() {
            this(null, null);
        }

        public Builder(OutputGraph out, String debug) {
            this.out = out;
            this.debug = debug == null
                         ? Collections.<String>emptySet()
                         : new HashSet<String>(Arrays.asList(debug.split("\\s+")));
        }

        public PolyMesh build(ZMap.Source zMapSource, double... v) {
            int[] minMax = new int[0];
            Map<Integer,List<Segment>> rows = null;
            Map<Integer,RangeBasedIntegerSet> used = null;
            Point[] points = new Point[0];
            int[][] stripsIndexes = new int[0][];
            StripType[] stripTypes = new StripType[0];
            ZMap zMap = null;
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            try {
                assert v.length > 5 && (v.length & 2) == 0;
                rows = buildRows(v);
//rows.keySet().retainAll(Collections.singleton(-3));
                used = buildUsed(rows);
                List<Pair<StripType,int[]>> strips = new ArrayList<Pair<StripType,int[]>>();
                for (Map.Entry<Integer,List<Segment>> entry : rows.entrySet()) {
                    Integer y = entry.getKey();
                    buildStripForRow(strips, entry.getValue(), used.get(y), y);
                }
                for (Point p : pointIndex.keySet()) {
                    minX = Math.min(minX, (int)Math.floor(p.x));
                    maxX = Math.max(maxX, (int)Math.ceil(p.x));
                    minY = Math.min(minY, (int)Math.floor(p.y));
                    maxY = Math.max(maxY, (int)Math.ceil(p.y));
                }
                zMap = zMapSource.getZMap(minX, minY, maxX, maxY);
                points = buildPoints(zMap, minX, minY);
                stripsIndexes = buildStrips(strips);
                stripTypes = buildStripTypes(strips);
            } finally {
                debugOut(v, minMax, rows, used, stripsIndexes, stripTypes, points);
            }
            return new PolyMesh(points,
                                buildNormals(points, zMap, minX, minY),
                                stripsIndexes,
                                stripTypes);
        }

        private void debugOut(double[] v,
                              int[] minMax,
                              Map<Integer,List<Segment>> rows,
                              Map<Integer,RangeBasedIntegerSet> used,
                              int[][] stripsIndexes,
                              StripType[] stripTypes,
                              Point[] points) {
            if (out == null) {
                return;
            }
            int minX = minMax[0];
            int maxX = minMax[1] + 1;
            int minY = minMax[2];
            int maxY = minMax[3] + 1;
            if (debug.contains("used") && used != null) {
                for (Map.Entry<Integer,RangeBasedIntegerSet> entry : used.entrySet()) {
                    int y = entry.getKey();
                    for (Integer x : entry.getValue()) {
                        out.addLine(Color.red.darker().darker(), x, y, x + 1, y + 1);
                        out.addLine(Color.red.darker().darker(), x + 1, y, x, y + 1);
                    }
                }
            }
            if (debug.contains("grid")) {
                for (int x = minX + 1; x <= maxX; x++) {
                    out.addLine(Color.cyan.darker().darker(), x, minY, x, maxY);
                }
                for (int y = minY; y <= maxY; y++) {
                    out.addLine(Color.cyan.darker().darker(), minX + 1, y, maxX, y);
                }
                out.addLabel(Color.cyan.darker(), String.format("(%d,%d)", minX + 1,  minY), minX + 1, minY);
                out.addLabel(Color.cyan.darker(), String.format("(%d,%d)", minX + 1,  minY), maxX + 1, maxY);
                out.addLabel(Color.cyan.darker(), String.format("(%d,%d)", maxX + 1,  maxY), minX + 1, minY);
                out.addLabel(Color.cyan.darker(), String.format("(%d,%d)", maxX + 1,  maxY), maxX + 1, maxY);
            }
            if (debug.contains("poly") && v != null) {
                double[] p = new double[v.length + 2];
                System.arraycopy(v, 0, p, 0, v.length);
                System.arraycopy(v, 0, p, v.length, 2);
                out.addLine(Color.white, p);
                for (int i = 0; i < v.length; i += 2) {
                    out.addPoint(Color.white, String.format("(%5.3f,%5.3f)", v[i], v[i+1]), v[i], v[i+1]);
                }
            }
            if (debug.contains("segments") && rows != null) {
                for (Map.Entry<Integer,List<Segment>> entry : rows.entrySet()) {
                    for (Segment segment : entry.getValue()) {
                        out.addArrow(Color.green, segment.x1, segment.y1, segment.x2, segment.y2);
                    }
                }
            }
            if (debug.contains("strips") && stripTypes != null && stripsIndexes != null && points != null) {
                for (int i = 0, l = stripTypes.length; i < l; i++) {
                    int[] strip = stripsIndexes[i];
                    if (stripTypes[i] == StripType.TRIANGLE) {
                        strip = debugTriangleStripToPoly(stripsIndexes[i]);
                    }
                    double[] poly = debugPolyPoints(strip, points, minX, minY, true);
//                    out.addLine(Color.yellow, poly);
                    for (int j = 2; j < poly.length; j += 2) {
                        out.addArrow(Color.yellow, poly[j - 2], poly[j - 1], poly[j], poly[j + 1]);
//                        out.addPoint(Color.yellow.brighter(), String.format("p[%d]\n(%5.3f,%5.3f)", j / 2, poly[j - 2], poly[j - 1]), poly[j - 2], poly[j - 1]);
                    }
//                    for (int j = 0; j < strip.length; j++) {
//                        Point p = points[stripsIndexes[i][j]];
//                        out.addPoint(Color.cyan, String.format("p[%d]\n(%5.3f,%5.3f)", j, p.x, p.y), p.x, p.y);
//                    }
                }
            }
            if (debug.contains("points") && points != null) {
                for (int i = 0, l = points.length; i < l; i++) {
                    Point p = points[i];
                    out.addPoint(Color.white, String.valueOf(i), p.x + minX, p.y + minY);
                }
            }
        }

        private int[] debugTriangleStripToPoly(int[] indexes) {
            int l = indexes.length;
            int[] result = new int[l];
            int j = 0;
            result[j++] = indexes[0];
            for (int i = 1; i < l; i += 2) {
                result[j++] = indexes[i];
            }
            for (int i = l - 2 + (l % 2); i > 0; i -= 2) {
                result[j++] = indexes[i];
            }
            assert j == l;
            return result;
        }

        private double[] debugPolyPoints(int[] indexes, Point[] points, int minX, int minY, boolean close) {
            int l = indexes.length;
            double[] result = new double[l * 2 + (close ? 2 : 0)];
            for (int i = 0, j = 0; i < l; i++, j += 2) {
                Point p = points[indexes[i]];
                result[j    ] = p.x;
                result[j + 1] = p.y;
            }
            if (close) {
                result[l * 2] = result[0];
                result[l * 2 + 1] = result[1];
            }
            return result;
        }

        private void buildStripForRow(List<Pair<StripType,int[]>> strips,
                                      List<Segment> segments,
                                      RangeBasedIntegerSet use,
                                      int y) {
            Map<Integer,List<Segment>> columns = processRow(segments);
            List<Point[]> polygons = collectPolygons(columns, use, y);
            double y1 = Double.MAX_VALUE;
            double y2 = -Double.MAX_VALUE;
            int x2 = Integer.MIN_VALUE;
            boolean cont = false;
            StripBuffer buffer = new StripBuffer();
            double[] result = { 0.0, 0.0, 0.0 };
            for (Point[] poly : polygons) {
                boolean canContinue = true;
                assert poly.length > 2;
                if (poly.length > 4) {
                    if (cont && hasPoints(x2, y1, y2, poly) && allPointsOnAnEdge(poly)) {
                        result[0] = x2;
                        result[1] = y1;
                        result[2] = y2;
                        switch (verticalSplit(poly, result, buffer)) {
                            case 1:
                                x2 = (int)result[0];
                                y1 = result[1];
                                y2 = result[2];
                                cont = true;
                                continue;
                            case 0:
                                x2 = Integer.MIN_VALUE;
                                y1 = Double.MAX_VALUE;
                                y2 = -Double.MAX_VALUE;
                                cont = false;
                                continue;
                        }
                    }
                    appendPolyForRow(strips, buffer, poly);
                    canContinue = false;
                } else if (cont && hasPoints(x2, y1, y2, poly)) {
                    canContinue = continueStripForRow(strips, buffer, poly, x2, canContinue);
                } else {
                    accumulateStrip(strips, buffer, StripType.TRIANGLE);
                    if (beginStripForRow(strips, buffer, poly, canContinue, result)) {
                        x2 = (int)result[0];
                        y1 = result[1];
                        y2 = result[2];
                        cont = true;
                        continue;
                    }
                }
                if (canContinue) {
                    x2++;
                    y1 = Double.MAX_VALUE;
                    y2 = -Double.MAX_VALUE;
                    for (Point point : poly) {
                        if (x2 == point.x) {
                            y1 = Math.min(point.y, y1);
                            y2 = Math.max(point.y, y2);
                        }
                    }
                    cont = y1 < Double.MAX_VALUE && y1 != y2;
                } else {
                    x2 = Integer.MIN_VALUE;
                    y1 = Double.MAX_VALUE;
                    y2 = -Double.MAX_VALUE;
                    cont = false;
                }
            }
            accumulateStrip(strips, buffer, null);
        }

        private List<Point[]> collectPolygons(Map<Integer,List<Segment>> columns,
                                              RangeBasedIntegerSet use,
                                              int y) {
            List<Point[]> polygons = new ArrayList<Point[]>();
            RangeBasedIntegerSet notUsed = null;
            for (Integer x : use) {
                if (columns.containsKey(x)) {
                    Point[] poly = findPoly(columns.get(x));
                    if (poly != null) {
                        polygons.add(poly);
                    } else {
                        if (notUsed == null) {
                            notUsed = new RangeBasedIntegerSet();
                        }
                        notUsed.add(x);
                    }
                } else {
                    polygons.add(new Point[]{
                            getPoint(x, y + 1),
                            getPoint(x, y),
                            getPoint(x + 1, y),
                            getPoint(x + 1, y + 1)
                    });
                }
            }
            if (notUsed != null) {
                use.removeAll(notUsed);
            }
            return polygons;
        }

        private int verticalSplit(Point[] poly, double[] result, StripBuffer buffer) {
            NavigableMap<Double,Pair<Point,Point>> slicedPoly = slicePoly(poly);
            for (int i = 0, l = poly.length; i < l; i++) {
                Point p = poly[i];
                Point q = poly[(i + 1) % l];
                if (p.x == q.x) {
                    continue;
                }
                if (p.x > q.x) {
                    Point t = p;
                    p = q;
                    q = t;
                }
                NavigableMap<Double,Pair<Point,Point>> pointsBetween = slicedPoly.subMap(p.x, false, q.x, false);
                if (pointsBetween.isEmpty()) {
                    continue;
                }
                double a = 1.0 / (q.x - p.x);
                double b = q.y - p.y;
                for (Map.Entry<Double,Pair<Point,Point>> entry : pointsBetween.entrySet()) {
                    if (entry.getValue().getB() != null) {
                        return -1; // cannot split
                    }
                    double x = entry.getKey();
                    double y = snap(p.y + (x - p.x) * a * b);
                    Point s = entry.getValue().getA();
                    if (s.y == y) {
                        return -1; // cannot split
                    }
                    Point t = getPoint(x, y);
                    entry.setValue(new Pair<Point,Point>(s.y > t.y ? s : t, s.y < t.y ? s : t));
                }
            }
            assert !hasEmptyPairs(slicedPoly.values());

            for (Pair<Point,Point> points : slicedPoly.tailMap(result[0], false).values()) {
                buffer.add(getPointIndex(points.getA()));
                buffer.add(getPointIndex(points.getB()));
            }
            Double x2 = result[0] + 1.0;
            if (!slicedPoly.containsKey(x2)) {
                return 0;
            }
            Pair<Point,Point> continuePoints = slicedPoly.get(x2);
            result[0] = x2;
            result[1] = continuePoints.getB().y;
            result[2] = continuePoints.getA().y;
            return 1;
        }

        private NavigableMap<Double,Pair<Point,Point>> slicePoly(Point[] poly) {
            NavigableMap<Double,Pair<Point,Point>> slicedPoly = new TreeMap<Double,Pair<Point,Point>>();
            for (Point p : poly) {
                Pair<Point,Point> points = slicedPoly.get(p.x);
                if (points == null) {
                    points = new Pair<Point,Point>(p, null);
                } else if (points.getB() == null) {
                    Point q = points.getA();
                    if (p.y == q.y) {
                        return null; // cannot split
                    }
                    points = new Pair<Point,Point>(p.y > q.y ? p : q, p.y < q.y ? p : q);
                } else {
                    return null; // cannot split
                }
                slicedPoly.put(p.x, points);
            }
            return slicedPoly;
        }

        private boolean hasEmptyPairs(Collection<Pair<Point,Point>> pairs) {
            for (Pair<?,?> pair : pairs) {
                if (pair.getA() == null || pair.getB() == null) {
                    return true;
                }
            }
            return false;
        }

        private boolean allPointsOnAnEdge(Point[] poly) {
            for (Point point : poly) {
                if (point.x != Math.floor(point.x) && point.y != Math.floor(point.y)) {
                    return false;
                }
            }
            return true;
        }

        private boolean beginStripForRow(List<Pair<StripType,int[]>> strips,
                                         StripBuffer buffer,
                                         Point[] poly,
                                         boolean canContinue,
                                         double[] result) {
            int x3 = Integer.MIN_VALUE;
            for (Point point : poly) {
                x3 = Math.max(x3, (int)Math.ceil(point.x));
            }
            Point start1 = null;
            Point start2 = null;
            Point other1 = null;
            Point other2 = null;
            for (Point point : poly) {
                if (x3 == point.x) {
                    other1 = other1 == null || other1.y < point.y ? point : other1;
                    other2 = other2 == null || other2.y > point.y ? point : other2;
                    if (start1 != null && start2 == null) {
                        start2 = start1;
                        start1 = null;
                    }
                } else if (start1 == null) {
                    start1 = point;
                } else {
                    start2 = point;
                }
            }
            if (start1 != null) {
                buffer.add(getPointIndex(start1));
            }
            if (start2 != null) {
                buffer.add(getPointIndex(start2));
            }
            if (other1 == other2) {
                if (other1 != null) {
                    buffer.add(getPointIndex(other1));
                    accumulateStrip(strips, buffer, StripType.TRIANGLE);
                } else {
                    buffer.clear(); // discard
                }
                canContinue = false;
            } else if (poly.length == 3) {
                buffer.windInReverseDirection();
            }
            if (other1 != null) {
                buffer.add(getPointIndex(other1));
                buffer.add(getPointIndex(other2));
                canContinue = true;
                result[0] = x3;
                result[1] = other2.y;
                result[2] = other1.y;
            }
            return canContinue;
        }

        private void appendPolyForRow(List<Pair<StripType,int[]>> strips,
                                      StripBuffer buffer,
                                      Point[] poly) {
            accumulateStrip(strips, buffer, StripType.POLYGON);
            for (Point p : poly) {
                buffer.add(getPointIndex(p));
            }
            accumulateStrip(strips, buffer, StripType.TRIANGLE);
        }

        private boolean continueStripForRow(List<Pair<StripType,int[]>> strips,
                                            StripBuffer buffer,
                                            Point[] poly,
                                            int x2,
                                            boolean canContinue) {
            if (poly.length == 3) {
                Point other = null;
                for (Point point : poly) {
                    if (x2 != point.x) {
                        other = point;
                        break;
                    }
                }
                if (other != null) {
                    buffer.add(getPointIndex(other));
                }
                accumulateStrip(strips, buffer, StripType.TRIANGLE);
                canContinue = false;
            } else {
                Point other1 = null;
                Point other2 = null;
                for (Point point : poly) {
                    if (x2 != point.x) {
                        other1 = other1 == null || other1.y < point.y ? point : other1;
                        other2 = other2 == null || other2.y > point.y ? point : other2;
                    }
                }
                if (other1 == other2) {
                    if (other1 != null) {
                        buffer.add(getPointIndex(other1));
                    }
                    accumulateStrip(strips, buffer, StripType.TRIANGLE);
                    canContinue = false;
                }
                if (other1 != null) {
                    buffer.add(getPointIndex(other1));
                    buffer.add(getPointIndex(other2));
                }
            }
            return canContinue;
        }

        private boolean hasPoints(int x2, double y1, double y2, Point[] poly) {
            int c = 0;
            for (Point point : poly) {
                if (x2 == point.x && (y1 == point.y || y2 == point.y)) {
                    c++;
                }
            }
            return c == 2;
        }

        private void accumulateStrip(List<Pair<StripType, int[]>> strips,
                                     StripBuffer buffer,
                                     StripType next) {
            Pair<StripType,int[]> strip = buffer.buildStrip(next);
            if (strip != null) {
                strips.add(strip);
            }
        }

        private Vector[] buildNormals(Point[] points, ZMap zMap, int minX, int minY) {
            int l = points.length;
            Vector[] normals = new Vector[l];
            for (int i = 0; i < l; i++) {
                normals[i] = zMap.getNormal(points[i].x, points[i].y);
            }
            return normals;
        }

        private int[][] buildStrips(List<Pair<StripType,int[]>> strips) {
            int l = strips.size();
            int[][] result = new int[l][];
            for (int i = 0; i < l; i++) {
                result[i] = strips.get(i).getB();
            }
            return result;
        }

        private StripType[] buildStripTypes(List<Pair<StripType,int[]>> strips) {
            int l = strips.size();
            StripType[] result = new StripType[l];
            for (int i = 0; i < l; i++) {
                result[i] = strips.get(i).getA();
            }
            return result;
        }

        private Point[] findPoly(List<Segment> segments) {
            Segment first = segments.get(0);
            int l = segments.size();
            Segment last = segments.get(l - 1);
            Point[] midpoints = new Point[l - 1];
            if (first.x1 == last.x2 && first.y1 == last.y2) {
                Segment tmp = first;
                first = last;
                last = tmp;
                for (int i = 1; i < l; i++) {
                    Segment mid = segments.get(i);
                    midpoints[i - 1] = new Point(mid.x2, mid.y2, 0.0);
                }
            } else {
                for (int i = 1; i < l; i++) {
                    Segment mid = segments.get(i);
                    midpoints[i - 1] = new Point(mid.x1, mid.y1, 0.0);
                }
            }
            return findPoly(first.x, first.y, first.x1, first.y1, last.x2, last.y2, midpoints);
        }

        private Point[] findPoly(int x, int y, double x1, double y1, double x2, double y2, Point... midpoints) {
            NavigableMap<Character,Point> pointsByEdge = new TreeMap<Character,Point>();
            pointsByEdge.put('F', new Point(x, y, 0.0));
            pointsByEdge.put('C', new Point(x, y + 1, 0.0));
            pointsByEdge.put('L', new Point(x + 1, y + 1, 0.0));
            pointsByEdge.put('I', new Point(x + 1, y, 0.0));
            char edge1 = findEdge(x, y, x1, y1, x2, y2, false);
            char edge2 = findEdge(x, y, x2, y2, x1, y1, true);
            assert edge1 != edge2;
            pointsByEdge.put(edge1, new Point(x1, y1, 0.0));
            pointsByEdge.put(edge2, new Point(x2, y2, 0.0));

            List<Point> points = new ArrayList<Point>();
            if (edge1 < edge2) {
                points.addAll(pointsByEdge.tailMap(edge2).values());
                points.addAll(pointsByEdge.headMap(edge1, true).values());
            } else {
                points.addAll(pointsByEdge.subMap(edge2, true, edge1, true).values());
            }
            Collections.addAll(points, midpoints);
            if (points.size() < 3) {
                return null; // no poly
            }
            for (int i = 0; i < points.size() && points.get(0).x == x + 1; i++) {
                points.add(points.remove(0));
            }

            int l = points.size();
            Point[] result = new Point[l];
            for (int i = 0; i < l; i++) {
                result[i] = getPoint(points.get(i).x, points.get(i).y);
            }
            return result;
        }

        private char findEdge(int x, int y, double x1, double y1, double x2, double y2, boolean reverse) {
            /*
                  Find where on which the edge (x1,y1) lies, with respect to (x2,y2), in ccw order

                  C - B - A - L
                  :           :
                  D           K
                  :     M     :
                  E           J
                  :           :
                  F - G - H - I
             */
            if (x1 == x) {
                if (y1 == y) return 'F';
                if (y1 == y + 1) return 'C';
                return y1 < y2 ^ reverse ? 'E' : 'D';
            }
            if (y1 == y) {
                if (x1 == x + 1) return 'I';
                return x1 < x2 ^ reverse ? 'G' : 'H';
            }
            if (x1 == x + 1) {
                if (y1 == y + 1) return 'L';
                return y1 < y2 ^ reverse ? 'J' : 'K';
            }
            if (y1 == y + 1) {
                return x1 < x2 ^ reverse ? 'B' : 'A' ;
            }
//            return 'M';
            throw new IllegalArgumentException(String.format(
                    "One of the passed coordinates should lie on an integer boundary: (%s,%s) not in (%d,%d,%d,%d)",
                    x1, y1, x, y, x + 1, y + 1));
        }

        private Map<Integer,List<Segment>> processRow(List<Segment> row) {
            Map<Integer,List<Segment>> columns = new LinkedHashMap<Integer,List<Segment>>();
            int x = row.get(0).x;
            int a = 0;
            int l = row.size();
            for (int i = 1; i < l; i++) {
                Segment seg = row.get(i);
                if (x != seg.x) {
                    columns.put(x, row.subList(a, i));
                    x = seg.x;
                    a = i;
                }
            }
            columns.put(x, row.subList(a, l));
            return columns;
        }

        private Map<Integer,List<Segment>> buildRows(double[] v) {
            Map<Integer,List<Segment>> rows = new LinkedHashMap<Integer,List<Segment>>();
            List<Segment> segments = new ArrayList<Segment>();
            for (int x1 = 0, y1 = 1, x2 = 2, y2 = 3; x2 < v.length; x1 += 2, y1 += 2, x2 += 2, y2 += 2) {
                collectSegments(v[x1], v[y1], v[x2], v[y2], segments);
            }
            collectSegments(v[v.length - 2], v[v.length - 1], v[0], v[1], segments);
            Collections.sort(segments, Segment.SORT_Y_X);
            int rowStart = 0;
            int y = segments.get(0).y;
            for (int i = 1, l = segments.size(); i < l; i++) {
                Segment segment = segments.get(i);
                int b = segment.y;
                if (b != y) {
                    rows.put(y, segments.subList(rowStart, i));
                    y = b;
                    rowStart = i;
                }
            }
            rows.put(y, segments.subList(rowStart, segments.size()));
            return rows;
        }

        private void collectSegments(double x1, double y1, double x2, double y2, Collection<Segment> segments) {
            double[] dx = divide(x1, x2);
            double[] dy = divide(y1, y2);
            Arrays.sort(dx);
            Arrays.sort(dy);
            double[] d = new double[dx.length + dy.length];
            System.arraycopy(dx, 0, d, 0, dx.length);
            System.arraycopy(dy, 0, d, dx.length, dy.length);
            Arrays.sort(d);
            double a = snap(x1), b = snap(y1);
            for (int i = 1, l = d.length; i < l; i++) {
                if (d[i - 1] == d[i]) continue;
                double p = d[i];
                double q = 1.0 - p;
                double x = snap(x1 * q + x2 * p);
                double y = snap(y1 * q + y2 * p);
                if (a != x || b != y) {
                    // points are too close together, they effectively do not exist in this segment.
                    segments.add(new Segment(a, b, x, y));
                    a = x;
                    b = y;
                }
            }
        }

        private double snap(double v) {
            // snap to a very fine grid to avoid imprecision problems
            return Math.round(v * 16384.0) / 16384.0;
        }

        private double[] divide(double l, double r) {
            if (l == r) {
                return new double[0];
            }
            boolean reversed = r < l;
            if (reversed) {
                double t = r;
                r = l;
                l = t;
            }
            double s = r - l;
            int maxLen = (int)(Math.ceil(r) - Math.ceil(l)) + 2;
            double[] divisions = new double[maxLen];
            int i = 1;
            for (double mid = Math.ceil(l); mid < r; mid += 1.0) {
                assert i < maxLen;
                divisions[i++] = (mid - l) / s;
            }
            divisions[i++] = 1.0;
            assert i == maxLen;
            if (reversed) {
                for (int j = 0; j < maxLen; j++) {
                    divisions[j] = 1.0 - divisions[j];
                }
            }
            return divisions;
        }

        private Map<Integer,RangeBasedIntegerSet> buildUsed(Map<Integer,List<Segment>> rows) {
            Map<Integer,RangeBasedIntegerSet> used = new LinkedHashMap<Integer,RangeBasedIntegerSet>();
            for (Map.Entry<Integer,List<Segment>> entry : rows.entrySet()) {
                RangeBasedIntegerSet use = new RangeBasedIntegerSet();
                used.put(entry.getKey(), use);
                int inside = 0;
                int lastX = Integer.MAX_VALUE;
                for (Segment segment : entry.getValue()) {
                    if (segment.x != lastX + 1 && inside != 0) {
                        use.addAll(lastX, segment.x);
                    } else {
                        use.add(segment.x);
                    }
                    inside += segment.crosses;
                    lastX = segment.x;
                }
            }
            return used;
        }

        private int pointSeq = 0;
        private Map<Point,Integer> pointIndex = new HashMap<Point,Integer>();
        private final Map<Pair<Axis,Integer>,NavigableMap<Double,Point>> points =
                new HashMap<Pair<Axis, Integer>,NavigableMap<Double,Point>>();


        int getPointIndex(Point point) {
           return pointIndex.get(point);
        }

        int[] getPointIndexes(Point[] points) {
            int l = points.length;
            int[] result = new int[l];
            for (int i = 0; i < l; i++) {
                result[i] = pointIndex.get(points[i]);
            }
            return result;
        }

        Point getPoint(double x, double y) {
            int s = (int)Math.floor(x);
            int t = (int)Math.floor(y);
            Pair<Axis,Integer> key;
            double subKey;
            if (s == x) {
                key = new Pair<Axis,Integer>(Axis.X, s);
                subKey = y;
            } else if (t == y) {
                key = new Pair<Axis,Integer>(Axis.Y, t);
                subKey = x;
            } else {
                Point point = new Point(x, y, 0.0);
                pointIndex.put(point, pointSeq++);
                return point;
            }
            NavigableMap<Double,Point> points = this.points.get(key);
            if (points == null) {
                points = new TreeMap<Double,Point>();
                this.points.put(key, points);
            }
            Point point = points.get(subKey);
            if (point == null) {
                point = new Point(x, y, 0.0);
                pointIndex.put(point, pointSeq++);
                points.put(subKey, point);
            }
            return point;
        }
        
        Point[] buildPoints(ZMap zMap, int minX, int minY) {
            Point[] result = new Point[pointSeq];
            for (Map.Entry<Point,Integer> entry : pointIndex.entrySet()) {
                Point p = entry.getKey();
                Integer value = entry.getValue();
                result[value] = new Point(p.x, p.y, zMap.getZ(p.x, p.y));
            }
            return result;
        }

        private static class Segment {

            static final Comparator<Segment> SORT_X_Y = new Comparator<Segment>() {
                public int compare(Segment l, Segment r) {
                    int cmp = l.x - r.x;
                    if (cmp == 0) {
                        cmp = l.y - r.y;
                    }
                    return cmp;
                }
            };

            static final Comparator<Segment> SORT_Y_X = new Comparator<Segment>() {
                public int compare(Segment l, Segment r) {
                    int cmp = l.y - r.y;
                    if (cmp == 0) {
                        cmp = l.x - r.x;
                    }
                    return cmp;
                }
            };

            private final int x, y;
            private final double x1, y1, x2, y2;
            private final int crosses;

            private Segment(double x1, double y1, double x2, double y2) {
                this.x = (int)Math.floor(Math.min(x1, x2));
                this.y = (int)Math.floor(Math.min(y1, y2));
                assert x1 - x <= 1.0 && x2 - x <= 1.0 && y1 - y <= 1.0 && y2 - y <= 1.0;
                this.x1 = x1;
                this.y1 = y1;
                this.x2 = x2;
                this.y2 = y2;
                crosses = (y == y1 ? 1 : 0) + // enters from top
                          (y == y2 ? -1 : 0) + // exits to top
                          (y + 1 == y1 ? -1 : 0) + // enters from bottom
                          (y + 1 == y2 ? 1 : 0); // exits from bottom
            }

            @Override
            public String toString() {
                return String.format("segment{x=%d,y=%d,(%5.3f,%5.3f)-(%5.3f,%5.3f)}", x, y, x1, y1, x2, y2);
            }
        }

        private static class StripBuffer extends ArrayList<Integer> {

            private StripType type;
            private boolean reverse = false;

            Pair<StripType,int[]> buildStrip(StripType next) {
                if (isEmpty()) {
                    type = next;
                    return null;
                }
                if (type == null) {
                    throw new IllegalStateException("The current buffer is not empty, but has no StripType");
                }
//                assert size() > 2;
                int l = size();
                int[] result = new int[l];
                if (reverse) {
                    result[0] = get(0);
                    for (int i = 1; i < l; i++) {
                        int j = Math.min(((i + 1) ^ 1) - 1, l - 1);
                        result[i] = get(j);
                    }
                } else {
                    for (int i = 0; i < l; i++) {
                        result[i] = get(i);
                    }
                }
                clear();
                Pair<StripType, int[]> strip = new Pair<StripType, int[]>(type, result);
                type = next;
                reverse = false;
                return strip;
            }

            void windInReverseDirection() {
                reverse = true;
            }

            void clear(StripType next) {
                clear();
                type = next;
            }
        }
    }

    public static void main(String[] args) {
        Builder builder = new Builder(new OutputGraph("PolyMesh", 1024, 0, 0, 96), "grid poly used point-s segments strips");
        ZMap zMap = new ZMap(new Rectangle(-33, -33, 66, 66));
        final double sq3 = Math.sqrt(3);
        double[] poly = {
                1, -sq3,
                2, 0,
//                1, sq3,
//                -1, sq3,
                -2, 0,
                -1, -sq3
        };
        for (int i = 0; i < poly.length; i++) {
            poly[i] *= 2;
        }
        builder.build(zMap.asSource(), poly);
    }
}
