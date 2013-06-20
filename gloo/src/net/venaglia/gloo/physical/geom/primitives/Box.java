package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractQuadFacetedType;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User: ed
 * Date: 9/1/12
 * Time: 9:51 AM
 */
public final class Box extends AbstractQuadFacetedType<Box> {

    public static final Point[] VERTICES = getVertices();
    public static final Point[] VERTICES_FLIPPED;

    static {
        Point[] verticesFlipped = VERTICES.clone();
        Collections.reverse(Arrays.asList(verticesFlipped));
        VERTICES_FLIPPED = verticesFlipped;
    }

    private final boolean flipped;

    public Box() {
        super(VERTICES);
        flipped = false;
    }

    public Box(BoundingBox boundingBox) {
        super(buildVertices(boundingBox));
        flipped = false;
    }

    private static Point[] buildVertices(BoundingBox box) {
        return new Point[] {
                new Point(box.min(Axis.X), box.min(Axis.Y), box.max(Axis.Z)),
                new Point(box.max(Axis.X), box.min(Axis.Y), box.max(Axis.Z)),
                new Point(box.min(Axis.X), box.min(Axis.Y), box.min(Axis.Z)),
                new Point(box.max(Axis.X), box.min(Axis.Y), box.min(Axis.Z)),
                new Point(box.min(Axis.X), box.max(Axis.Y), box.max(Axis.Z)),
                new Point(box.max(Axis.X), box.max(Axis.Y), box.max(Axis.Z)),
                new Point(box.min(Axis.X), box.max(Axis.Y), box.min(Axis.Z)),
                new Point(box.max(Axis.X), box.max(Axis.Y), box.min(Axis.Z))
        };
    }

    private Box(Point[] points, boolean flipped) {
        super(points);
        this.flipped = flipped;
    }

    public Box flip() {
        return new Box(flipped ? VERTICES : VERTICES_FLIPPED, !flipped);
    }

    @Override
    protected Box build(Point[] points, XForm xForm) {
        return new Box(points, flipped);
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int i = index * 4;
        facetBuilder.usePoints(order[i], order[i + 1], order[i + 2], order[i + 3]);
    }

    public int facetCount() {
        return 6;
    }

    public Vector getNormal(int index) {
        return Vector.betweenPoints(Point.ORIGIN, VERTICES[index]);
    }

    private static final int[] order = {
            0,2,3,1, // front
            0,1,5,4, // top
            3,2,6,7, // bottom
            4,5,7,6, // back
            0,4,6,2, // left
            1,3,7,5  // right
    };

    public QuadSequence getQuads() {
        return new QuadSequence(pointSequence(order));
    }

    private List<Point> pointSequence(int[] indices) {
        List<Point> seq = new ArrayList<Point>(indices.length);
        for (int indice : indices) {
            seq.add(points[indice]);
        }
        return seq;
    }

    public void project(GeometryBuffer buffer) {
        project(buffer, GeometryBuffer.GeometrySequence.QUADS, order);
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
            e----------f
            : \        : \
            :   a----------b
            :   :          :
            :   :          :
            g---:      h   :
              \ :          :
                c----------d
        */
        return new Point[]{
                new Point(-0.5, -0.5, -0.5),                    // a
                new Point( 0.5, -0.5, -0.5),                    // b
                new Point(-0.5,  0.5, -0.5),                    // c
                new Point( 0.5,  0.5, -0.5),                    // d
                new Point(-0.5, -0.5,  0.5),                    // e
                new Point( 0.5, -0.5,  0.5),                    // f
                new Point(-0.5,  0.5,  0.5),                    // g
                new Point( 0.5,  0.5,  0.5)                     // h
        };
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Box box = new Box().flip();
        new SingleShapeDemo(box, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
