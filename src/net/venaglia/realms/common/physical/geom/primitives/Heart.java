package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Faceted;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: ed
 * Date: 3/8/13
 * Time: 5:21 PM
 */
public class Heart extends AbstractShape<Heart> implements Faceted {

    private final DetailLevel detailLevel;
    private BezierPatch[] patches;
    private int[] normalBezierPointReferences;
    private int facetsPerPatch = -1;

    public Heart() {
        this(genVertices(), DetailLevel.MEDIUM);
    }

    public Heart(DetailLevel detailLevel) {
        this(genVertices(), detailLevel);
    }

    protected Heart(Point[] vertices, DetailLevel detailLevel) {
        super(vertices);
        this.detailLevel = detailLevel;
        this.normalBezierPointReferences = new int[vertices.length];
        Arrays.fill(normalBezierPointReferences, -1);
        int k = 0;
        int[][] patchVertices = genPatchVertices();
        int length = patchVertices.length;
        this.patches = new BezierPatch[length * 4];
//        java.util.Random rand = new java.util.Random(1234567890L);
        for (int[] np : patchVertices) {
            int l = np.length;
            Point[] points = new Point[l];
            for (int i = 0; i < l; i++) {
                int pointIndex = np[i] - 1;
                points[i] = this.points[pointIndex];
                setNormalBezierPointIndex(pointIndex, k, i);
            }
            BezierPatch patch = new BezierPatch(points, detailLevel);
            patch.setMaterial(Material.INHERIT);
//            patch.setMaterial(Material.makeFrontShaded(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat())));
            patches[k++] = patch;
        }
        for (int scaleX = 1; scaleX > -2; scaleX -= 2) {
            for (int scaleY = 1; scaleY > -2; scaleY -= 2) {
                if (scaleX == scaleY && scaleX == 1) continue;
                for (int i = 0; i < length; i++) {
                    BezierPatch patch = patches[i].scale(new Vector(scaleX, scaleY, 1.0));
                    if (scaleX != scaleY) patch = patch.flip();
                    patch.setMaterial(Material.INHERIT);
                    patches[k++] = patch;
                }
            }
        }
        facetsPerPatch = patches[0].facetCount();
    }

    public int facetCount() {
        return facetsPerPatch * patches.length;
    }

    public Facet getFacet(int index) {
        BezierPatch patch = patches[index / facetsPerPatch];
        return patch.getFacet(index % facetsPerPatch);
    }

    public Facet.Type getFacetType() {
        return Facet.Type.QUAD;
    }

    private void setNormalBezierPointIndex(int pointIndex, int bezierIndex, int bezierPointIndex) {
        if (normalBezierPointReferences[pointIndex] != -1) {
            return;
        }
        normalBezierPointReferences[pointIndex] = bezierIndex << 6 | bezierPointIndex & 0x3F;
    }

    @Override
    protected Heart build(Point[] points, XForm xForm) {
        return new Heart(points, detailLevel);
    }

    public Vector getNormal(int index) {
        int ref = normalBezierPointReferences[index];
        return patches[ref >> 6].getNormal(ref & 0x3F);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        for (BezierPatch patch : this.patches) {
            patch.project(0, buffer);
        }
    }

    private static int[][] genPatchVertices() {
        return new int[][]{
            {1,1,1,1,2,3,4,5,6,7,8,9,10,11,12,13},
            {1,1,1,1,14,15,16,2,17,18,19,6,20,21,22,10}
        };
    }

    private static Point[] genVertices() {
        Point[] vertices = {
                new Point(0,-0.4,0),
                new Point(-0.52,-0.4,0.165),
                new Point(-0.4,-0.4,0.4),
                new Point(-0.2,-0.4,0.3),
                new Point(0,-0.4,0.22),
                new Point(-0.94,-0.225,0.3), // 6
                new Point(-0.86,-0.225,0.91),
                new Point(-0.2,-0.225,0.8),
                new Point(0,-0.225,0.4),
                new Point(-0.94,0,0.3), // 10
                new Point(-0.86,0,0.91),
                new Point(-0.2,0,0.8),
                new Point(0,0,0.4),
                new Point(0,-0.4,-0.22), // 14
                new Point(-0.2,-0.3,-0.6),
                new Point(-0.4,-0.4,-0.2),
                new Point(0,-0.2,-0.8), // 17
                new Point(-0.6,-0.2,-0.6),
                new Point(-0.99,-0.225,-0.06),
                new Point(0,0,-1), // 20
                new Point(-0.6,0,-0.6),
                new Point(-0.99,0,-0.06)
        };

        int length = vertices.length;
        List<Point> allpoints = new ArrayList<Point>(length);
        for (int scaleX = 1; scaleX > -2; scaleX -= 2) {
            for (int scaleY = 1; scaleY > -2; scaleY -= 2) {
                boolean includeZeroY  = scaleX == scaleY && scaleX == 1;
                for (Point vertex : vertices) {
                    Point p = vertex.scale(new Vector(scaleX, scaleY, 1.0));
                    if (includeZeroY || p.y != 0) {
                        allpoints.add(p);
                    }
                }
            }
        }
        return allpoints.toArray(new Point[allpoints.size()]);
    }

    public static void main(String[] args) throws Exception {
        Color offWhite = new Color(0.8f, 0.0f, 0.2f);
        Heart heart = new Heart(DetailLevel.MEDIUM_HIGH);
//        Heart heart = new Heart(BezierPatch.DetailLevel.MEDIUM_LOW);
        new SingleShapeDemo(heart, offWhite, SingleShapeDemo.Mode.SHADED).start();
//        new SingleShapeDemo(heart, offWhite, SingleShapeDemo.Mode.WIREFRAME).start();
    }
}
