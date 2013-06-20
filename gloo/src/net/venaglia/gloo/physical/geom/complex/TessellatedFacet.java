package net.venaglia.gloo.physical.geom.complex;

import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractFacetedShape;
import net.venaglia.gloo.projection.GeometryBuffer;

import java.util.Arrays;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 6:22 PM
 */
public class TessellatedFacet extends AbstractFacetedShape<TessellatedFacet> {

    protected final Facet.Type type;
    protected final int divisions;
    protected final int[][] strips;

    private Vector normal;

    public TessellatedFacet(Facet facet, int divisionCount) {
        super(tessellate(facet, divisionCount));
        this.type = facet.type;
        this.divisions = divisionCount;
        this.strips = getStrips(divisionCount, facet.type);
        this.normal = Vector.cross(facet.a, facet.b, facet.c).normalize();
    }

    protected TessellatedFacet(Point[] points, int[][] strips, int divisions, Facet.Type type) {
        super(points);
        this.strips = strips;
        this.divisions = divisions;
        this.type = type;
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        // todo
    }

    public Facet.Type getFacetType() {
        return type;
    }

    @Override
    protected TessellatedFacet build(Point[] points, XForm xForm) {
        return new TessellatedFacet(assertLength(points, this.points.length), strips, divisions, type);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        GeometryBuffer.GeometrySequence geometrySequence = type == Facet.Type.TRIANGLE
                                                                 ? GeometryBuffer.GeometrySequence.TRIANGLE_STRIP
                                                                 : GeometryBuffer.GeometrySequence.QUAD_STRIP;
        for (int[] strip : strips) {
            buffer.start(geometrySequence);
            buffer.normal(normal);
            for (int i : strip) {
                buffer.vertex(points[i]);
            }
            buffer.end();
        }
    }

    public int facetCount() {
        return divisions * divisions;
    }

    public Vector getNormal(int index) {
        return normal;
    }

    private static Point[] tessellate(Facet facet, int divisions) {
        Point[] points;
        int k;
        switch (facet.type) {
            case TRIANGLE:
                if (divisions == 0) {
                    return new Point[]{ facet.a, facet.b, facet.c };
                }
                points = new Point[getPointCount(divisions, facet.type)];
                k = 0;
                for (int i = 0; i <= divisions; i++) {
                    int count = divisions - i;
                    double p = ((double)i) / ((double)divisions);
                    Point a = midpoint(facet.c, facet.a, p);
                    Point b = midpoint(facet.b, facet.a, p);
                    for (int j = 0; count > 0 && j <= count; j++) {
                        double q = ((double)j) / ((double)count);
                        points[k++] = midpoint(a, b, q);
                    }
                }
                points[k] = facet.a;
                return points;
            case QUAD:
                if (divisions == 0) {
                    return new Point[]{ facet.a, facet.d, facet.b, facet.c };
                }
                points = new Point[getPointCount(divisions, facet.type)];
                k = 0;
                for (int i = 0; i <= divisions; i++) {
                    double p = ((double)i) / ((double)divisions);
                    Point a = midpoint(facet.a, facet.b, p);
                    Point b = midpoint(facet.d, facet.c, p);
                    for (int j = 0; j <= divisions; j++) {
                        double q = ((double)j) / ((double)divisions);
                        points[k++] = midpoint(a, b, q);
                    }
                }
                return points;
            case POLY:
                throw new IllegalArgumentException("TesellatedFacet does not support POLY type facets");
        }
        throw new IllegalStateException();
    }

    private static int[][] getStrips(int divisions, Facet.Type type) {
        int[][] strips;
        int a,b;
        switch (type) {
            case TRIANGLE:
                if (divisions == 0) {
                    return new int[][]{{0,1,2}};
                }
                strips = new int[divisions][];
                a = 0;
                b = divisions + 1;
                for (int i = 0; i < divisions; i++) {
                    int count = divisions - i;
                    int[] strip = new int[count * 2 + 1];
                    int k = 0;
                    for (int j = 0; j <= count; j++) {
                        if (j > 0) strip[k++] = b + j - 1;
                        strip[k++] = a + j;
                    }
                    strips[i] = strip;
                    a = b;
                    b += count;
                }
                return strips;
            case QUAD:
                if (divisions == 0) {
                    return new int[][]{{0,2,1,3}};
                }
                a = 0;
                b = divisions + 1;
                strips = new int[divisions][];
                for (int i = 0; i < divisions; i++) {
                    int[] strip = new int[divisions * 2 + 2];
                    int k = 0;
                    for (int j = 0; j <= divisions; j++) {
                        strip[k++] = a + j;
                        strip[k++] = b + j;
                    }
                    strips[i] = strip;
                    a = b;
                    b += divisions + 1;
                }
                return strips;
        }
        throw new IllegalStateException();
    }

    /**
     * @param a The point to start at, returned when i = 0
     * @param b The point to end at, returned when i = 1
     * @param i A value from 0 to 1 describing a point between a and b
     * @return a point between a and b
     */
    private static Point midpoint(Point a, Point b, double i) {
        if (i == 0.0) return a;
        if (i == 1.0) return b;
        double j = 1.0 - i;
        return new Point(a.x * j + b.x * i, a.y * j + b.y * i, a.z * j + b.z * i);
    }

    private static int getPointCount(int divisions, Facet.Type type) {
        if (divisions < 0) {
            throw new IllegalArgumentException("division count cannot be negative");
        }
        switch (type) {
            case TRIANGLE:
                return (divisions + 1) * (divisions + 2) >> 1;
            case QUAD:
                return (divisions + 1) * (divisions + 1);
        }
        throw new IllegalStateException();
    }

    public static void main(String[] args) throws InterruptedException {
        final int divisionCount = 5;
        final Facet.Type facetType = Facet.Type.QUAD;
        int[][] strips = getStrips(divisionCount, facetType);
        for (int[] strip : strips) {
            System.out.println(Arrays.toString(strip));
        }
        Point[] points;
        //noinspection ConstantConditions
        if (facetType == Facet.Type.TRIANGLE) {
            points = tessellate(new Facet(new Point(0,Math.sqrt(3) - 1,0), new Point(-1,-1,0), new Point(1,-1,0)), divisionCount);
        } else {
            points = tessellate(new Facet(new Point(-1,1,0), new Point(-1,-1,0), new Point(1,-1,0), new Point(1,1,0)), divisionCount);
        }
        char[] screen = new char[3800];
        Arrays.fill(screen, ' ');
        for (int i = 0; i <= 125; i++) {
            int j = getIndex(new Point(i/100.0,0,0));
            if (j >= 0) screen[j] = '-';
            j = getIndex(new Point(i/-100.0,0,0));
            if (j >= 0) screen[j] = '-';
            j = getIndex(new Point(0,i/100.0,0));
            if (j >= 0) screen[j] = ':';
            j = getIndex(new Point(0,i/-100.0,0));
            if (j >= 0) screen[j] = '|';
        }
        screen[getIndex(Point.ORIGIN)] = '+';
        for (int i = 0, pointsLength = points.length; i < pointsLength; i++) {
            Point point = points[i];
            int j = getIndex(point);
//            assert j >= 0 && j < 3200;
            screen[j] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(i);
            String label = String.format("(%.2f,%.2f)", point.x, point.y);
            j += 100 - label.length() / 2;
            if (j < screen.length) {
                System.arraycopy(label.toCharArray(), 0, screen, j, label.length());
            }
        }
        for (int i = 0; i < screen.length; i += 100) {
            System.out.println(new String(screen, i, 100));
        }
        Thread.sleep(200);
    }

    private static int getIndex(Point point) {
        assert point.z == 0.0;
        int x = (int)Math.round(point.x * 40.0) + 50;
        int y = (int)Math.round(point.y * -18.0) + 18;
        if (x >= 0 && x < 100 && y >= 0 && y < 38) {
            return x + y * 100;
        }
        return -1;
    }
}
