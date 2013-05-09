package net.venaglia.realms.common.physical.geom.primitives;


import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Faceted;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractTriangleFacetedType;
import net.venaglia.realms.common.projection.GeometryBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 8/25/12
 * Time: 8:22 PM
 */
public final class TriangleSequence extends AbstractTriangleFacetedType<TriangleSequence> implements Faceted {

    public TriangleSequence(Point... points) {
        super(assertMultiple(points, 3));
    }

    public TriangleSequence(TriangleStrip strip) {
        super(fromStrip(strip));
    }

    public TriangleSequence(TriangleFan strip) {
        super(fromFan(strip));
    }

    public TriangleSequence(Faceted shape) {
        super(fromFaceted(shape));
    }

    @Override
    protected TriangleSequence build(Point[] points, XForm xForm) {
        return new TriangleSequence(assertMultiple(points, 3));
    }

    public Vector getNormal(int index) {
        int i = index - (index % 3);
        return Vector.cross(points[i + 1], points[i + 2], points[i]).normalize();
    }

    public int facetCount() {
        return points.length / 3;
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int i = index * 3;
        facetBuilder.usePoints(i, i + 1, i + 2);
    }

    public void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.TRIANGLES);
        for (int i = 0, l = points.length; i < l; i += 3) {
            buffer.normal(getNormal(i));
            buffer.vertex(points[i]);
            buffer.vertex(points[i + 1]);
            buffer.vertex(points[i + 2]);
        }
        buffer.end();
    }

    private static Point[] fromStrip(TriangleStrip strip) {
        int count = strip.facetCount();
        Point[] points = new Point[count * 3];
        for (int i = 0, j = 0; i < count; i++) {
            points[j++] = strip.points[i];
            if ((i & 1) == 0) {
                points[j++] = strip.points[i + 1];
                points[j++] = strip.points[i + 2];
            } else {
                points[j++] = strip.points[i + 2];
                points[j++] = strip.points[i + 1];
            }
        }
        return points;
    }

    private static Point[] fromFan(TriangleFan fan) {
        int count = fan.facetCount();
        Point[] points = new Point[count * 3];
        for (int i = 0, j = 0; i < count; i++) {
            points[j++] = fan.points[0];
            points[j++] = fan.points[i + 1];
            points[j++] = fan.points[i + 2];
        }
        return points;
    }

    private static Point[] fromFaceted(Faceted shape) {
        if (shape.getFacetType() != Facet.Type.TRIANGLE) {
            throw new IllegalArgumentException("Passed shape does not break down into quads");
        }
        int l = l = shape.facetCount();
        List<Point> points = new ArrayList<Point>(l * 4);
        for (int i = 0; i < l; i++) {
            Facet facet = shape.getFacet(i);
            points.add(facet.a);
            points.add(facet.b);
            points.add(facet.c);
            points.add(facet.d);
        }
        return points.toArray(new Point[points.size()]);
    }
}
