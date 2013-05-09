package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Faceted;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractQuadFacetedType;
import net.venaglia.realms.common.projection.GeometryBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 8/28/12
 * Time: 8:49 AM
 */
public final class QuadSequence extends AbstractQuadFacetedType<QuadSequence> {

    private Vector[] normals;

    public QuadSequence(Point... points) {
        super(assertMultiple(points, 4));
    }

    public QuadSequence(Iterable<Point> points) {
        this(toArray(points));
    }

    public QuadSequence(Faceted shape) {
        super(fromFaceted(shape));
    }

    @Override
    protected QuadSequence build(Point[] points, XForm xForm) {
        return new QuadSequence(points);
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int i = index * 4;
        facetBuilder.usePoints(i, i + 1, i + 2, i + 3);
    }

    public int facetCount() {
        return points.length >> 2;
    }

    public Vector getNormal(int index) {
        if (normals == null) {
            normals = new Vector[points.length >> 2];
        }
        int j = index >> 2;
        if (normals[j] == null) {
            int i = index - (index % 4);
            if (points[i] == points[i + 1] || points[i] == points[i + 2]) {
                normals[j] = Vector.cross(points[i + 2], points[i + 3], points[i + 1]).normalize();
            } else {
                normals[j] = Vector.cross(points[i + 2], points[i], points[i + 1]).normalize();
            }
        }
        return normals[j];
    }

    public void project(GeometryBuffer buffer) {
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        for (int i = 0, l = points.length; i < l; i += 4) {
            buffer.normal(getNormal(i));
            buffer.vertex(points[i]);
            buffer.vertex(points[i + 1]);
            buffer.vertex(points[i + 2]);
            buffer.vertex(points[i + 3]);
        }
        buffer.end();
    }

    private static Point[] fromFaceted(Faceted shape) {
        if (shape.getFacetType() != Facet.Type.QUAD) {
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
