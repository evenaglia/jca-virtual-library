package net.venaglia.gloo.physical.geom.complex;

import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.ZMap;
import net.venaglia.gloo.physical.geom.base.AbstractFacetedShape;
import net.venaglia.gloo.projection.GeometryBuffer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: ed
 * Date: 2/20/13
 * Time: 12:36 AM
 */
public class Mesh extends AbstractFacetedShape<Mesh> {

    public final int width;
    public final int height;
    public final Vector[] normals;

    public Mesh (ZMap zMap) {
        this(zMap.getWidth(), zMap.getHeight(), zMap.toPoints(), buildNormals(zMap));
    }

    public Mesh(int width, int height, Point[] points, Vector[] normals) {
        super(points);
        this.width = width;
        this.height = height;
        if (width < 2 || height < 2) {
            throw new IllegalArgumentException("The width & height must be at least 2: width = " + width + ", height = " + height);
        }
        if (width * height != points.length) {
            throw new IllegalArgumentException("The number of points is now what was expected: expected = " + (width * height) + ", actual = " + points.length);
        }
        if (width * height != normals.length) {
            throw new IllegalArgumentException("The number of normals is now what was expected: expected = " + (width * height) + ", actual = " + normals.length);
        }
        this.normals = normals;
    }

    private static Vector[] buildNormals(ZMap zMap) {
        int w = zMap.getWidth();
        int h = zMap.getHeight();
        Vector[] normals = new Vector[w * h];
        int k = 0;
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                normals[k++] = zMap.getNormal(j, j);
            }
        }
        return normals;
    }

    public Mesh flip() {
        Point[] points = this.points.clone();
        List<Point> pointList = Arrays.asList(points);
        for (int j = 0; j < height; j++) {
            int i = j * width;
            Collections.reverse(pointList.subList(i, i + width));
        }
        Vector[] normals = this.normals.clone();
        for (int i = 0, l = normals.length; i < l; i++) {
            normals[i] = normals[i].reverse();

        }
        return new Mesh(width, height, points, normals);
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
        return new Mesh(width, height, points, xForm.apply(normals));
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
                buffer.normal(normals[base1 + i]);
                buffer.vertex(points[base2 + i]);
                buffer.normal(normals[base2 + i]);
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
