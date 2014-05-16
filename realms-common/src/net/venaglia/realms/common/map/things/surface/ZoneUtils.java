package net.venaglia.realms.common.map.things.surface;

import net.venaglia.gloo.physical.geom.Point;

import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 4/11/14
 * Time: 7:16 AM
 */
public class ZoneUtils {

    public static final int[][] TRIANGLE_STRIP_DRAW_ORDER_COARSE = buildDrawOrder(8);
    public static final int[][] TRIANGLE_STRIP_DRAW_ORDER_FINE = buildDrawOrder(64);
    public static final int[] MAP_COARSE_INDICES_FROM_FINE_INDICES = buildFineToCoarseMap(64, 8);
    public static final int[] MAP_PERIMETER_INDICES_FROM_COARSE_INDICES = buildPerimeterMap(8);
    public static final int[] MAP_PERIMETER_INDICES_FROM_FINE_INDICES = buildPerimeterMap(64);

    private ZoneUtils() throws InstantiationException {
        throw new InstantiationException("pure static class");
    }

    private static int[][] buildDrawOrder(int divisions) {
        int[][] result = new int[divisions][];
        int lineNum = 0;
        int bottomLineStart = 0;
        int topLineStart = divisions + 1;
        int topLineLength = divisions;
        while (topLineLength > 0) {
            int l = topLineLength + topLineLength + 1;
            int[] line = new int[l];
            result[lineNum++] = line;
            line[0] = bottomLineStart;
            for (int i = 1, j = bottomLineStart + 1, k = topLineStart; i < l; i += 2, j++, k++) {
                line[i] = k;
                line[i + 1] = j;
            }
            assert line[line.length - 1] != 0;
            // reverse the strip
            for (int i = 0, j = l - 1; i < l; i++, j--) {
                int t = line[i];
                line[i] = line[j];
                line[j] = t;
            }
            bottomLineStart = topLineStart;
            topLineStart += topLineLength;
            topLineLength--;
        }
        return result;
    }

    private static int[] buildFineToCoarseMap(int fineDivisions, int coarseDivisions) {
        Map<Point,Integer> pointLookup = new HashMap<Point,Integer>();
        Point[] generatePoints = generatePoints(fineDivisions, fineDivisions);
        for (int i = 0; i < generatePoints.length; i++) {
            pointLookup.put(generatePoints[i], i);
        }
        generatePoints = generatePoints(fineDivisions, coarseDivisions);
        int[] result = new int[generatePoints.length];
        for (int i = 0; i < generatePoints.length; i++) {
            result[i] = pointLookup.get(generatePoints[i]);
        }
        return result;
    }

    private static Point[] generatePoints(double scale, int divisions) {
        Point a = new Point(scale * 64, 0, 0);
        Point b = new Point(0, 0, 0);
        Point c = new Point(scale * 32, scale * 32 * Math.sqrt(3), 0);
        Point[] points = new Point[(divisions + 2) * (divisions + 1) / 2];
        int k = 0;
        for (int j = divisions; j >= 0; j--) {
            int j_ = divisions - j;
            Point s = new Point(a.x * j + c.x * j_, a.y * j + c.y * j_, 0);
            Point t = new Point(b.x * j + c.x * j_, b.y * j + c.y * j_, 0);
            for (int i = j; i >= 0; i--) {
                int i_ = j - i;
                double rescale = 1.0 / (divisions * j);
                Point p = new Point(s.x * i + t.x * i_, s.y * i + t.y * i_, 0);
                points[k++] = new Point(Math.round(p.x * rescale), Math.round(p.y * rescale), 0);
            }
        }
        assert k == points.length;
        return points;
    }

    private static int[] buildPerimeterMap(int divisions) {
        int[] result = new int[divisions * 3];
        int a = 0;
        for (int i = 0; i < divisions; i++) {
            result[a++] = i;
        }
        for (int i = divisions, j = divisions; j > 0; i += j--) {
            result[a++] = i;
        }
        int last = ((divisions + 1) * (divisions + 2)) / 2 - 1;
        for (int i = last, j = 2; i > 0; i -= j++) {
            result[a++] = i;
        }
        assert a == divisions * 3;
        return result;
    }

    public static void main(String[] args) {
        int c = 0;
        for (int[] ints : TRIANGLE_STRIP_DRAW_ORDER_COARSE) {
            System.out.println(toString(ints));
            c += ints.length;
        }
        System.out.println(c);

        System.out.println(toString(buildPerimeterMap(8)));
    }

    private static String toString(int[] ints) {
        if (ints ==  null) return "null";
        StringBuilder builder = new StringBuilder(ints.length * 5);
        for (int i : ints) {
            if (builder.length() > 0) builder.append(",");
            builder.append(i);
        }
        return builder.toString();
    }

    public static <A> void populateCoarseFromFine(A[] fine, A[] coarse) {
        for (int i = 0; i < coarse.length; i++) {
            coarse[i] = fine[MAP_COARSE_INDICES_FROM_FINE_INDICES[i]];
        }
    }

    public static <A> void populateFineFromCoarse(A[] coarse, A[] fine) {
        for (int i = 0; i < coarse.length; i++) {
            fine[MAP_COARSE_INDICES_FROM_FINE_INDICES[i]] = coarse[i];
        }
    }

    public static <A> void populatePerimeterFromFine(A[] fine, A[] perimeter) {
        for (int i = 0; i < perimeter.length; i++) {
            perimeter[i] = fine[MAP_PERIMETER_INDICES_FROM_FINE_INDICES[i]];
        }
    }

    public static <A> void populateFineFromPerimeter(A[] perimeter, A[] fine) {
        for (int i = 0; i < perimeter.length; i++) {
            fine[MAP_COARSE_INDICES_FROM_FINE_INDICES[i]] = perimeter[i];
        }
    }

}
