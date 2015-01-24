package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.geom.PlatonicShape;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractTriangleFacetedType;
import net.venaglia.gloo.projection.GeometryBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 9/1/12
 * Time: 9:51 AM
 */
public final class Tetrahedron extends AbstractTriangleFacetedType<Tetrahedron> implements PlatonicShape<Tetrahedron> {

    public static final Point[] VERTICES = getVertices();

    private static final int[][] edges = {
            {0, 1}, {0, 2}, {0, 3}, {1, 2}, {2, 3}, {3, 1}
    };

    public Tetrahedron() {
        super(VERTICES);
    }

    private Tetrahedron(Point[] points) {
        super(points);
    }

    @Override
    protected Tetrahedron build(Point[] points, XForm xForm) {
        return new Tetrahedron(points);
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        switch (index) {
            case 0:
                facetBuilder.usePoints(2,1,3);
                break;
            case 1:
                facetBuilder.usePoints(1,0,3);
                break;
            case 2:
                facetBuilder.usePoints(3,0,2);
                break;
            case 3:
                facetBuilder.usePoints(0,1,2);
                break;
        }
    }

    public int facetCount() {
        return 4;
    }

    @Override
    public int getEdgeCount() {
        return edges.length;
    }

    @Override
    public Edge getEdge(int i) {
        if (i < 0 || i >= edges.length) {
            throw new IllegalArgumentException();
        }
        int[] endpoints = edges[i];
        return new Edge(points[endpoints[0]], points[endpoints[1]]);
    }

    @Override
    public PlatonicBaseType getPlatanicBaseType() {
        return PlatonicBaseType.TETRAHEDRON;
    }

    public Vector getNormal(int index) {
        return Vector.betweenPoints(Point.ORIGIN, VERTICES[index]);
    }

    private static final int[] order = {2,1,3,0,2,1};

    public TriangleStrip getStrip() {
        return new TriangleStrip(pointSequence(order));
    }

    public TriangleSequence getTriangles() {
        return new TriangleSequence(getStrip());
    }

    private List<Point> pointSequence(int[] indices) {
        List<Point> seq = new ArrayList<Point>(indices.length);
        for (int indice : indices) {
            seq.add(points[indice]);
        }
        return seq;
    }

    public void project(GeometryBuffer buffer) {
        project(buffer, GeometryBuffer.GeometrySequence.TRIANGLE_STRIP, order);
    }

    private void project(GeometryBuffer buffer, GeometryBuffer.GeometrySequence seq, int[] indices) {
        buffer.start(seq);
        for (int i : indices) {
            buffer.normal(getNormal(i));
            buffer.vertex(points[i]);
        }
        buffer.end();
    }

    private static Point[] getVertices() {
        /*
                       d
                      / \
                     /   \
                    /     \
                   /       \
                  /    a    \
                 /           \
                c-------------b
        */
        double sq3 = Math.sqrt(3);
        return new Point[] {
                new Point(0.0, sq3 - 1/sq3, 0.0),               // a
                new Point(1.0, -1/sq3, 0.0),                    // b
                new Point(-1.0, -1/sq3, 0.0),                   // c
                new Point(0.0, 0.0, 2.0 * Math.sqrt(2.0/3.0))   // d
        };
    }
}
