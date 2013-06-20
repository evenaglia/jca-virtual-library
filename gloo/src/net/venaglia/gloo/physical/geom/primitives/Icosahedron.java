package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
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
public final class Icosahedron extends AbstractTriangleFacetedType<Icosahedron> {

    public static final Point[] VERTICES = getVertices();

    /**
     * The center of this Icosahedron
     */
    public final Point center;

    /**
     * The radius of this Icosahedron's vertices. May be NaN if this Icosahedron has been transformed in an asymmetrical manner.
     */
    public final double radius;

    public Icosahedron() {
        super(VERTICES);
        this.center = Point.ORIGIN;
        this.radius = points[0].z;
    }

    private Icosahedron(Point[] points, boolean hasRadius) {
        super(points);
        Point a = points[0];
        Point b = points[11];
        this.center = new Point((a.x + b.x) * 0.5, (a.y + b.y) * 0.5, (a.z + b.z) * 0.5);
        this.radius = hasRadius ? Vector.betweenPoints(a, b).l * 0.5 : Double.NaN;
    }

    @Override
    protected Icosahedron build(Point[] points, XForm xForm) {
        return new Icosahedron(points, xForm.isSymmetric() && !Double.isNaN(radius));
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        switch (index) {
            case 0:
                facetBuilder.usePoints(0,5,4);
                break;
            case 1:
                facetBuilder.usePoints(0,4,3);
                break;
            case 2:
                facetBuilder.usePoints(0,3,2);
                break;
            case 3:
                facetBuilder.usePoints(0,2,1);
                break;
            case 4:
                facetBuilder.usePoints(0,1,5);
                break;
            case 5:
                facetBuilder.usePoints(11,6,7);
                break;
            case 6:
                facetBuilder.usePoints(11,7,8);
                break;
            case 7:
                facetBuilder.usePoints(11,8,9);
                break;
            case 8:
                facetBuilder.usePoints(11,9,10);
                break;
            case 9:
                facetBuilder.usePoints(11,10,6);
                break;
            case 10:
                facetBuilder.usePoints(2,9,1);
                break;
            case 11:
                facetBuilder.usePoints(9,8,1);
                break;
            case 12:
                facetBuilder.usePoints(1,8,5);
                break;
            case 13:
                facetBuilder.usePoints(8,7,5);
                break;
            case 14:
                facetBuilder.usePoints(5,7,4);
                break;
            case 15:
                facetBuilder.usePoints(7,6,4);
                break;
            case 16:
                facetBuilder.usePoints(4,6,3);
                break;
            case 17:
                facetBuilder.usePoints(6,10,3);
                break;
            case 18:
                facetBuilder.usePoints(3,10,2);
                break;
            case 19:
                facetBuilder.usePoints(10,9,2);
                break;
        }
    }

    @Override
    protected BoundingVolume<?> createBoundingVolume() {
        double maxRadius = 0.0;
        for (Point p : points) {
            maxRadius = Math.max(maxRadius,
                                 Vector.computeDistance(p.x - center.x,
                                                        p.y - center.y,
                                                        p.z - center.z));
        }
        return new BoundingSphere(center, maxRadius);
    }

    public int facetCount() {
        return 20;
    }

    public Vector getNormal(int index) {
        return Vector.betweenPoints(Point.ORIGIN, VERTICES[index]);
    }

    private static final int[] top = {0,5,4,3,2,1,5};
    private static final int[] bottom = {11,6,7,8,9,10,6};
    private static final int[] equator = {2,9,1,8,5,7,4,6,3,10,2,9};

    public TriangleFan getTop() {
        return new TriangleFan(pointSequence(top));
    }

    public TriangleFan getBottom() {
        return new TriangleFan(pointSequence(bottom));
    }

    public TriangleStrip getEquator() {
        return new TriangleStrip(pointSequence(equator));
    }

    private List<Point> pointSequence(int[] indices) {
        List<Point> seq = new ArrayList<Point>(indices.length);
        for (int indice : indices) {
            seq.add(points[indice]);
        }
        return seq;
    }

    public void project(GeometryBuffer buffer) {
        project(buffer, GeometryBuffer.GeometrySequence.TRIANGLE_FAN, top);
        project(buffer, GeometryBuffer.GeometrySequence.TRIANGLE_FAN, bottom);
        project(buffer, GeometryBuffer.GeometrySequence.TRIANGLE_STRIP, equator);
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
        // compute a bunch of values to build the coordinates:
        double s = 1.0;
        double s2 = s * 0.5;
        double t1 = Math.PI * 0.4;
        double t2 = Math.PI * 0.1;
        double t3 = Math.PI * -0.3;
        double t4 = Math.PI * 0.2;
        double r = (s * 0.5) / Math.sin(t4);
        double cx = r * Math.cos(t2);
        double cy = r * Math.sin(t2);
        double h = Math.cos(t4) * r;
        double h1 = Math.sqrt(s * s - r * r);
        double h2 = Math.sqrt((h + r) * (h + r) - h * h);
        double z2 = (h2 - h1) * 0.5;
        double z1 = z2 + h1;

        return new Point[]{
                new Point(  0,   0,  z1),
                new Point(  0,   r,  z2),
                new Point( cx,  cy,  z2),
                new Point( s2,  -h,  z2),
                new Point(-s2,  -h,  z2),
                new Point(-cx,  cy,  z2),
                new Point(  0,  -r, -z2),
                new Point(-cx, -cy, -z2),
                new Point(-s2,   h, -z2),
                new Point( s2,   h, -z2),
                new Point( cx, -cy, -z2),
                new Point(  0,   0, -z1),
        };
    }
}
