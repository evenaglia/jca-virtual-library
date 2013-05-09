package net.venaglia.realms.common.physical.geom.complex;

import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.ZMap;
import net.venaglia.realms.common.physical.geom.base.AbstractFacetedShape;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 2/20/13
 * Time: 12:36 AM
 */
public class Mesh extends AbstractFacetedShape<Mesh> {

    private final int width;
    private final int height;

    public Mesh(int width, int height, Point[] points) {
        super(points);
        this.width = width;
        this.height = height;
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("The width & height must be at least 2: width = " + width + ", height = " + height);
        }
        if (width * height != points.length) {
            throw new IllegalArgumentException("The number of points is now what was expected: expected = " + (width * height) + ", actual = " + points.length);
        }
    }

    public Mesh (ZMap zMap) {
        super(zMap.toPoints());
        this.width = zMap.getWidth();
        this.height = zMap.getHeight();
    }

    public int facetCount() {
        return (width - 1) * (height - 1);
    }

    public Facet.Type getFacetType() {
        return Facet.Type.QUAD;
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int x1 = index % (width - 1);
        int y1 = index / (width - 1);
        int i = x1 + y1 * width;
        facetBuilder.usePoints(i, i + 1, i + width, i + 1 + width);
    }

    @Override
    protected Mesh build(Point[] points, XForm xForm) {
        return new Mesh(width, height, points);
    }

    public Vector getNormal(int index) {
        return new Vector(0, 0, 1);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        for (int j = height - 1; j > 0; j--) {
            int base1 = j * width;
            int base2 = base1 - width;
            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            for (int i = width - 1; i >= 0; i--) {
                buffer.vertex(points[base1 + i]);
                buffer.vertex(points[base2 + i]);
            }
            buffer.end();
        }
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder(height * width * 7 + height * 2 + 30);
        for (int i = 0, j = 0; i < height; i++, j += height) {
            if (i > 0) buffer.append("\n");
            buffer.append("[");
            for (int k = 0; k < width; k++) {
                if (k > 0) buffer.append(",");
                buffer.append(String.format("%6.3f", points[j + k].z));
            }
            buffer.append("]");
        }
        return buffer.toString();
    }
}
