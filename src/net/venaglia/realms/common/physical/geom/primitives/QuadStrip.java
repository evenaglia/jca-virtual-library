package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractQuadFacetedType;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:35 PM
 */
public final class QuadStrip extends AbstractQuadFacetedType<QuadStrip> {

    public QuadStrip(Point a, Point b, Point c, Point d) {
        super(AbstractShape.toArray(a, b, c, d));
    }

    public QuadStrip(Point a, Point b, Point c, Point d, Point... points) {
        super(toArray(a, b, c, d, points));
    }

    public QuadStrip(Iterable<Point> points) {
        super(assertMultiple(assertMinLength(toArray(points), 4), 2));
    }

    private QuadStrip(Point[] points) {
        super(points);
    }

    @Override
    protected QuadStrip build(Point[] points, XForm xForm) {
        return new QuadStrip(points);
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int i = index * 2;
        facetBuilder.usePoints(i, i + 1, i + 3, i + 2);
    }

    public int facetCount() {
        return (points.length  - 2) / 2;
    }

    public Vector getNormal(int a) {
        if (a < 0 || a >= points.length) {
            throw new ArrayIndexOutOfBoundsException(a);
        }
        int b, c, d;
        if ((a & 1) == 0) {
            b = a - 2;
            c = a + 1;
            d = a + 2;
        } else {
            b = a - 2;
            c = a - 1;
            d = a + 2;
        }
        Vector[] v = {toVector(a,b), toVector(a, c), toVector(a, d)};
        Vector[] n = {cross(v[0], v[1]), cross(v[1], v[2])};
        double i = 0.0, j = 0.0, k = 0.0;
        for (int x = 1; x < 2; x++) {
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
        buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
        for (int i = 0, l = points.length; i < l; i++) {
            if (i < 2) {
                buffer.normal(Vector.cross(points[i], points[i + 1], points[i + 2]).normalize());
            } else if (i < l - 2) {
                Vector a, b, c;
                if ((i & 1) == 0) {
                    a = Vector.betweenPoints(points[i], points[i - 2]);
                    b = Vector.betweenPoints(points[i], points[i + 1]);
                    c = Vector.betweenPoints(points[i], points[i + 2]);
                } else {
                    a = Vector.betweenPoints(points[i], points[i + 2]);
                    b = Vector.betweenPoints(points[i], points[i - 1]);
                    c = Vector.betweenPoints(points[i], points[i - 2]);
                }
                buffer.normal(cross(a, b).sum(cross(b, c)));
            } else if (i == l - 2) {
                buffer.normal(Vector.cross(points[i], points[i - 2], points[i - 1]).normalize());
            } else {
                buffer.normal(Vector.cross(points[l - 1], points[l - 2], points[l - 3]).normalize());
            }
        }
        buffer.end();
    }
}
