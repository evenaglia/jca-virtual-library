package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.PlatonicShape;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractPolygonFacetedType;
import net.venaglia.gloo.physical.lights.FixedPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.camera.PerspectiveCamera;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;

/**
 * User: ed
 * Date: 10/24/12
 * Time: 6:56 PM
 */
public class Dodecahedron extends AbstractPolygonFacetedType<Dodecahedron> implements PlatonicShape<Dodecahedron> {

    private static final int[][] facets = {
        {10,18,12,13,0},

        {13,11,1,3,0},
        {3,2,8,10,0},
        {8,6,16,18,10},
        {16,19,17,12,18},
        {17,14,11,13,12},

        {2,3,1,4,7},
        {6,8,2,7,9},
        {19,16,6,9,15},
        {14,17,19,15,5},
        {1,11,14,5,4},

        {5,15,9,7,4}
    };

    private static final int[][] edges = {
            {10, 18}, {18, 12}, {12, 13}, {13, 0}, {0, 10},
            {10, 8}, {18, 16}, {12, 17}, {13, 11}, {0, 3},
            {8, 6}, {6, 16}, {16, 19}, {19, 17}, {17, 14}, {14, 11}, {11, 1}, {1, 3}, {3, 2}, {2, 8},
            {5, 14}, {15, 19}, {9, 6}, {7, 2}, {4, 1},
            {5, 15}, {15, 9}, {9, 7}, {7, 4}, {4, 5}
    };

    private final Point center;

    public Dodecahedron() {
        this(synthesizePoints(), Point.ORIGIN);
    }

    private Dodecahedron(Point[] points, Point center) {
        super(points);
        this.center = center;
    }

    public int facetCount() {
        return facets.length;
    }

    public Polygon[] getPolygons() {
        Polygon[] polygons = new Polygon[12];
        for (int i = 0; i < 12; i++) {
            Point[] points = new Point[5];
            for (int j = 0; j < 5; j++) {
                points[j] = this.points[facets[i][j]];
            }
            Vector normal = Vector.cross(points[0], points[1], points[3]).normalize();
            polygons[i] = new Polygon(normal, points);
        }
        return polygons;
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int[] facet = facets[index];
        facetBuilder.usePoints(facet[0], facet[1], facet[2], facet[3], facet[4]);
    }

    @Override
    public int edgeCount() {
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
        return PlatonicBaseType.DODECAHEDRON;
    }

    @Override
    protected Dodecahedron build(Point[] points, XForm xForm) {
        return new Dodecahedron(points, xForm.apply(center));
    }

    public Vector getNormal(int index) {
        return Vector.betweenPoints(center, points[index]);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        for (int[] facet : facets) {
            project(buffer, facet);
        }
    }

    private void project(GeometryBuffer buffer, int[] indices) {
        buffer.start(GeometryBuffer.GeometrySequence.POLYGON);

        for (int i : indices) {
            buffer.vertex(points[i]);
        }
        buffer.end();
    }

    private static Point[] synthesizePoints() {
        // Calculate constants that will be used to generate vertices
        double phi = (Math.sqrt(5) - 1) / 2; // The golden ratio
        double R = 0.5 / Math.sqrt(3);

        double a = R * 1.0;
        double b = R / phi;
        double c = R * phi;

        // Generate each vertex
        Point[] vertices = new Point[20];
        int v = 0;
        for (int i = -1; i < 2; i += 2) {
            for (int j = -1; j < 2; j += 2) {
                vertices[v++] = new Point(0,          // green
                                          i * c * R,
                                          j * b * R);
                vertices[v++] = new Point(i * c * R,  // blue
                                          j * b * R,
                                          0);
                vertices[v++] = new Point(i * b * R,  // red
                                          0,
                                          j * c * R);
                for (int k = -1; k < 2; k += 2) {     // orange
                    vertices[v++] = new Point(i * a * R,
                                              j * a * R,
                                              k * a * R);
                }
            }
        }
        return vertices;
    }

    public static void main(String[] args) {
        final Dodecahedron dodecahedron = new Dodecahedron();
        final Camera camera = new PerspectiveCamera();
        final Light[] lights = { new FixedPointSourceLight(new Point(0,-4,12)) };
        View3D v = new View3D(1024, 768);
        v.setMainLoop(new View3DMainLoop() {

            double a = 0.0;

            public boolean beforeFrame(long nowMS) {
                double x = Math.sin(a) * 2.0;
                double y = Math.cos(a) * -2.0;
                camera.setPosition(new Point(x, y, -0.55));
                camera.setDirection(new Vector(-x, -y, 0.55));
                camera.setRight(new Vector(y * -0.25, x * 0.25, 0));
                camera.computeClippingDistances(dodecahedron.getBounds());
                a += 0.005;
                return true;
            }

            public MouseTargets getMouseTargets(long nowMS) {
                return null;
            }

            public void renderFrame(long nowMS, ProjectionBuffer buffer) {
                buffer.useCamera(camera);
                buffer.useLights(lights);
                buffer.color(Color.BLUE);
                buffer.applyBrush(Brush.FRONT_SHADED);
                for (Polygon p : dodecahedron.getPolygons()) {
                    p.setMaterial(Material.INHERIT);
                    p.project(nowMS, buffer);
                }
                buffer.applyBrush(Brush.WIRE_FRAME);
                dodecahedron.project(nowMS, buffer);
            }

            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            public void afterFrame(long nowMS) {
            }
        });
        v.start();
    }
}
