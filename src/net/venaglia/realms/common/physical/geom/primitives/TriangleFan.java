package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractTriangleFacetedType;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:35 PM
 */
public final class TriangleFan extends AbstractTriangleFacetedType<TriangleFan> {

    public TriangleFan(Point a, Point b, Point c) {
        super(toArray(a, b, c));
    }

    public TriangleFan(Point a, Point b, Point c, Point... points) {
        super(toArray(a, b, c, points));
    }

    public TriangleFan(Iterable<Point> points) {
        super(assertMinLength(toArray(points), 3));
    }

    private TriangleFan(Point[] points) {
        super(assertMinLength(points, 3));
    }

    @Override
    protected TriangleFan build(Point[] points, XForm xForm) {
        return new TriangleFan(points);
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        facetBuilder.usePoints(0, index + 1, index + 2);
    }

    public int facetCount() {
        return points.length - 2;
    }

    public Vector getNormal(int a) {
        if (a < 0 || a >= points.length) {
            throw new ArrayIndexOutOfBoundsException(a);
        }
        int b, c, d, e;
        if ((a & 1) == 0) {
            b = a + 2;
            c = a + 1;
            d = a - 1;
            e = a - 2;
        } else {
            b = a - 2;
            c = a - 1;
            d = a + 1;
            e = a + 2;
        }
        Vector[] v = {toVector(a,b), toVector(a, c), toVector(a, d), toVector(a, e)};
        Vector[] n = {cross(v[0], v[1]), cross(v[1], v[2]), cross(v[2], v[3])};
        double i = 0.0, j = 0.0, k = 0.0;
        for (int x = 1; x < 3; x++) {
            Vector m = n[x];
            if (m != null) {
                i += m.i;
                j += m.j;
                k += m.k;
            }
        }
        return new Vector(i, j, k).normalize();
    }

    private Vector toVector(int from, int to) {
        if (from < 0 || from >= points.length || to < 0 || to >= points.length) {
            return null;
        }
        return Vector.betweenPoints(points[from], points[to]);
    }

    private Vector cross(Vector a, Vector b) {
        return  (a != null && b != null) ? a.cross(b) : null;
    }

    public void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLE_STRIP);
        for (int i = 0, l = points.length; i < l; i++) {
            buffer.normal(getNormal(i));
            buffer.vertex(points[i]);
        }
        buffer.end();
    }
}
