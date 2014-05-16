package net.venaglia.realms.common.map.things.surface;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.realms.common.map.things.AbstractThing;
import net.venaglia.realms.common.map.things.ThingProperties;
import net.venaglia.realms.common.map.things.ThingWriter;
import net.venaglia.realms.common.map.things.annotations.Property;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/10/14
 * Time: 6:07 PM
 *
 * A zone represents 1/24th of an Acre (1/20th of a pentagonal acre)
 *
 * The height map of a Zone is built from the tessellation of a triangle formed by 3 corner points.
 * The length of each side is approximately 10 meters, and is evenly divided into 64 points.
 * The perimeter heights and normals are stored as a sequence of 192 values, winding clockwise around the Zone.
 * The subdivisions formed by coarse and fine divisions, where coarse divisions are 1/8th, and fine divisions are 1/64th of the Zone's edge.
 *
 * The default surface map is built using a single random seed, but the height
 * map may be later converted to a persisted field. This allows for landscape
 * excavation necessary for creating caverns, towns or roads.
 *
 * The surface map also contains color information for each of the 45 coarse points.
 *
 * This yields for the coarse detail:
 *     Points: 45
 *     Triangles: 64
 *     Strips: 8
 * This yields for the fine detail:
 *     Points: 2145
 *     Triangles: 4096
 *     Strips: 64
 *
 * A single fine detail triangle covers slightly less than 64 sq inches of the
 * surface, points on the corners should be snapped to the nearest neighboring
 * point (within 1/64th of a meter) to ensure no gaps in rendering.
 *
 * When persisted, the height map is recorded as a series of vectors. The
 * direction of the vector accurately represents the surface normal, while its
 * magnitude is a value between 1.0 and 2.0 and represents its elevation. Two
 * additional values the minimum and maximum elevation for the Zone. The total
 * persisted size of a the height map should be:
 *     2145 points + 2 doubles = 2145(24b) + 2(8b) = 51496b
 *
 * A custom zone is essentially an arbitrary shape, consisting of points,
 * normals, colors and triangle strips.
 *
 * Total size of the persisted Zone is expected to be:
 *     unresolved:
 *         1 enum + 3 points + 3 vectors + 3 colors =
 *         (1b) + 3(24b) + 3(24b) + 3(12b) =
 *         181b
 *     generated:
 *         1 enum + 192 points + 192 vectors + 45 colors + 1 long + 1 float =
 *         (1b) + 192(24b) + 192(24b) + 45(12b) + (8b) + (4b) =
 *         9937b
 *     modified:
 *         1 enum + 192 points + 2145 vectors + 45 colors + 2 doubles =
 *         (1b) + 192(24b) + 2145(24b) + 45(12b) + 2(8) =
 *         56813b
 *     custom: (inline low-res detail)
 *         1 enum + 45 points + 45 vectors + 45 colors + 80 shorts =
 *         (1b) + 45(24b) + 45(24b) + 45(12b) + 80(b) =
 *         2781b
 *     custom: (hi-res detail)
 *         2145 points + 2145 vectors + 2145 colors + 4224 shorts =
 *         2145(24b) + 2145(24b) + 2145(12b) + 4224(2b) =
 *         137148b
 */
public class Zone extends AbstractThing {

    private enum SurfaceState {
        unresolved, // 3 vectors + 3 points + 3 colors
        generated, // 192 vectors + 192 points + 45 colors + seed + smoothness
        modified, // 2145 vectors + 192 points + 45 colors + min/max elevation
        custom // 2145 vectors + 2145 points + 2145 colors + 4224 shorts
    }

    private enum Extent {
        coarse,
        perimeter,
        fine
    }

    private SurfaceState surfaceState;

    private ByteBuffer renderData;
    private ByteBuffer renderIndices;

    private Point[] cornerPoints; // 3 values, subset of perimeterPoints
    private Vector[] cornerNormals; // 3 values, subset of perimeterNormals
    private Color[] cornerColors; // 3 values, subset of colors_coarse

    private Point[] perimeterPoints; // 192 values, subset of surfaceMap_fine
    private Vector[] perimeterNormals; // 192 values, subset of normalMap_fine

    private Point[] surfaceMap_coarse; // 45 values, subset of surfaceMap_fine
    private Vector[] normalMap_coarse; // 45 values, subset of normalMap_fine
    private Color[] colors_coarse; // 45 values, subset of colors_fine
    private Point[] surfaceMap_fine; // 2145 values
    private Vector[] normalMap_fine; // 2145 values
    private Color[] colors_fine; // 2145 values

    // serialization fields

    @Override
    public void writeChangesTo(ThingProperties properties, ThingWriter thingWriter) {
        // todo: prepare buffers
        super.writeChangesTo(properties, thingWriter);
    }

    @Override
    protected void afterLoad() {
        surfaceState = SurfaceState.values()[surfaceStateOrdinal];
        cornerPoints = new Point[3];
        cornerNormals = new Vector[3];
        cornerColors = new Color[3];
        if (surfaceState != SurfaceState.unresolved) {
            perimeterPoints = new Point[192];
            perimeterNormals = new Vector[192];
            surfaceMap_coarse = new Point[45];
            normalMap_coarse = new Vector[45];
            colors_coarse = new Color[45];
            surfaceMap_fine = new Point[2145];
            normalMap_fine = new Vector[2145];
            normalMap_fine = new Vector[2145];
        }
        ZoneSequencer sequencer = ZoneSequencer.SOURCE.get().load(pointData, vertexData, colorData);
        try {
            switch (surfaceState) {
                case unresolved:
                    sequencer.readPoints(cornerPoints);
                    sequencer.readVectors(cornerNormals);
                    sequencer.readColors(cornerColors);
                    break;
                case generated:
                    sequencer.readPoints(perimeterPoints);
                    sequencer.readVectors(perimeterNormals);
                    ZoneUtils.populateFineFromPerimeter(perimeterPoints, surfaceMap_fine);
                    ZoneUtils.populateFineFromPerimeter(perimeterNormals, normalMap_fine);
                    generateMissingPoints(surfaceMap_fine, normalMap_fine, randomSeed, Extent.fine);
                    ZoneUtils.populateCoarseFromFine(surfaceMap_fine, surfaceMap_coarse);
                    ZoneUtils.populateCoarseFromFine(normalMap_fine, normalMap_coarse);
                    break;
                case modified:
                    sequencer.readPoints(perimeterPoints);
                    sequencer.readVectors(normalMap_fine);
                    ZoneUtils.populateFineFromPerimeter(perimeterPoints, surfaceMap_fine);
                    generateMissingPoints(surfaceMap_fine, normalMap_fine, minElevation, maxElevation);
                    ZoneUtils.populateCoarseFromFine(surfaceMap_fine, surfaceMap_coarse);
                    ZoneUtils.populateCoarseFromFine(normalMap_fine, normalMap_coarse);
                    break;
                case custom:
                    throw new UnsupportedOperationException("custom not yet implemented");
            }
        } finally {
            sequencer.unload();
        }
    }

    private void generateMissingPoints(Point[] points, Vector[] normals, long randomSeed, Extent extent) {
        if (extent == null) throw new NullPointerException("extent");
        RandomSequence random = RandomSequence.SOURCE.get().load(randomSeed);
        float coarseSmoothness = smoothness * 0.125F;
        for (ZoneDivision.ZonePoint zp : ZoneDivision.ROOT.points) {
            zp.calculateFor(points, random, coarseSmoothness);
        }
        if (extent == Extent.coarse) return;
        float fineSmoothness = smoothness * 0.015625f;
        for (int index : ZoneUtils.MAP_PERIMETER_INDICES_FROM_FINE_INDICES) {
            ZoneDivision.POINTS[index].calculateFor(points, random, fineSmoothness);
        }
        if (extent == Extent.perimeter) return;
        for (ZoneDivision division : ZoneDivision.DIVISIONS) {
            for (ZoneDivision.ZonePoint zp : division.points) {
                zp.calculateFor(points, random, fineSmoothness);
            }
        }
    }

    /**
     * @param point The base point, near which we a calculating a nurb control point.
     * @param normal The desired surface normal (l=1) at the base point.
     * @param adjacent The other corner nurb control point, on the adjacent edge.
     * @param sphereCenter The center of the globe.
     * @return The nurb control point nearest to <code>point</code> on the side shared with <code>adjacent</code>
     */
    private Point getSurfaceNurbControlPoint(Point point, Vector normal, Point adjacent, Point sphereCenter) {
        Point normalPoint = point.translate(normal);
        Vector forwardVector = Vector.betweenPoints(point, adjacent);
        Point midpoint = point.translate(forwardVector.normalize(forwardVector.l * 0.33333));
        return findClosestPoint(sphereCenter, midpoint,
                                point, point.translate(Vector.cross(point, normalPoint, adjacent)));
    }

    private Point findClosestPoint(Point onLine_pointA, Point onLine_pointB, Point nearbyLine_pointA, Point nearbyLine_pointB) {
        Point A1 = onLine_pointA;
        Point A2 = onLine_pointB;
        Point B1 = nearbyLine_pointA;
        Point B2 = nearbyLine_pointB;

        Vector A2_A1 = Vector.betweenPoints(A1, A2);
        Vector B2_B1 = Vector.betweenPoints(B1, B2);
        Vector A1_B1 = Vector.betweenPoints(B1, A1);

//        nA = dot(cross(B2-B1,A1-B1),cross(A2-A1,B2-B1));
//        nB = dot(cross(A2-A1,A1-B1),cross(A2-A1,B2-B1));
//        d = dot(cross(A2-A1,B2-B1),cross(A2-A1,B2-B1));
//        A0 = A1 + (nA/d)*(A2-A1);
//        B0 = B1 + (nB/d)*(B2-B1);

        Vector A2_A1_x_B2_B1 = A2_A1.cross(B2_B1);

        double nA = B2_B1.cross(A1_B1).dot(A2_A1_x_B2_B1);
//        double nB = A2_A1.cross(A1_B1).dot(A2_A1_x_B2_B1);
        double d = A2_A1_x_B2_B1.dot(A2_A1_x_B2_B1);
        Point A0 = A1.translate(A2_A1.scale(nA/d));
//        Point B0 = B1.translate(B2_B1.scale(nB/d));
        return A0;
    }

    private void generateMissingPoints(Point[] points, Vector[] normals, double minElevation, double maxElevation) {
        double scale = maxElevation - minElevation;
        for (ZoneDivision.ZonePoint zp : ZoneDivision.ROOT.points) {
            zp.calculateFor(points, (zp.get(normals).l - 1.0) * scale + minElevation);
        }
        for (ZoneDivision division : ZoneDivision.DIVISIONS) {
            for (ZoneDivision.ZonePoint zp : division.points) {
                zp.calculateFor(points, (zp.get(normals).l - 1.0) * scale + minElevation);
            }
        }
    }


    @Property(name = "st")
    byte surfaceStateOrdinal;

    @Property(name = "p")
    private double[] pointData; // length is always a multiple of 3

    @Property(name = "v")
    private double[] vertexData; // length is always a multiple of 3

    @Property(name = "min")
    private double minElevation;

    @Property(name = "max")
    private double maxElevation;

    @Property(name = "c")
    private float[] colorData; // length is a multiple of 3

    @Property(name = "smooth")
    private float smoothness;

    @Property(name = "random")
    private long randomSeed;
}
