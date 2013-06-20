package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.Faceted;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.physical.texture.mapping.MatrixMapping;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

import java.util.Arrays;

/**
 * User: ed
 * Date: 3/8/13
 * Time: 5:21 PM
 */
public class Teapot extends AbstractShape<Teapot> implements Faceted {

    private final DetailLevel detailLevel;
    private BezierPatch[] patches;
    private int[] normalBezierPointReferences;
    private int facetsPerPatch = -1;

    public Teapot() {
        this(genVertices(), DetailLevel.MEDIUM);
    }

    public Teapot(DetailLevel detailLevel) {
        this(genVertices(), detailLevel);
    }

    protected Teapot(Point[] vertices, DetailLevel detailLevel) {
        super(vertices);
        this.detailLevel = detailLevel;
        this.normalBezierPointReferences = new int[vertices.length];
        Arrays.fill(normalBezierPointReferences, -1);
        int k = 0;
        int[][] patchVertices = genPatchVertices();
        this.patches = new BezierPatch[patchVertices.length];
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
    protected Teapot build(Point[] points, XForm xForm) {
        return new Teapot(points, detailLevel);
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
            {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16},
            {4,17,18,19,8,20,21,22,12,23,24,25,16,26,27,28},
            {19,29,30,31,22,32,33,34,25,35,36,37,28,38,39,40},
            {31,41,42,1,34,43,44,5,37,45,46,9,40,47,48,13},
            {13,14,15,16,49,50,51,52,53,54,55,56,57,58,59,60},
            {16,26,27,28,52,61,62,63,56,64,65,66,60,67,68,69},
            {28,38,39,40,63,70,71,72,66,73,74,75,69,76,77,78},
            {40,47,48,13,72,79,80,49,75,81,82,53,78,83,84,57},
            {57,58,59,60,85,86,87,88,89,90,91,92,93,94,95,96},
            {60,67,68,69,88,97,98,99,92,100,101,102,96,103,104,105},
            {69,76,77,78,99,106,107,108,102,109,110,111,105,112,113,114},
            {78,83,84,57,108,115,116,85,111,117,118,89,114,119,120,93},
            {121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136},
            {124,137,138,121,128,139,140,125,132,141,142,129,136,143,144,133},
            {133,134,135,136,145,146,147,148,149,150,151,152,69,153,154,155},
            {136,143,144,133,148,156,157,145,152,158,159,149,155,160,161,69},
            {162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177},
            {165,178,179,162,169,180,181,166,173,182,183,170,177,184,185,174},
            {174,175,176,177,186,187,188,189,190,191,192,193,194,195,196,197},
            {177,184,185,174,189,198,199,186,193,200,201,190,197,202,203,194},
            {204,204,204,204,207,208,209,210,211,211,211,211,212,213,214,215},
            {204,204,204,204,210,217,218,219,211,211,211,211,215,220,221,222},
            {204,204,204,204,219,224,225,226,211,211,211,211,222,227,228,229},
            {204,204,204,204,226,230,231,207,211,211,211,211,229,232,233,212},
            {212,213,214,215,234,235,236,237,238,239,240,241,242,243,244,245},
            {215,220,221,222,237,246,247,248,241,249,250,251,245,252,253,254},
            {222,227,228,229,248,255,256,257,251,258,259,260,254,261,262,263},
            {229,232,233,212,257,264,265,234,260,266,267,238,263,268,269,242},
            {270,270,270,270,279,280,281,282,275,276,277,278,271,272,273,274},
            {270,270,270,270,282,289,290,291,278,286,287,288,274,283,284,285},
            {270,270,270,270,291,298,299,300,288,295,296,297,285,292,293,294},
            {270,270,270,270,300,305,306,279,297,303,304,275,294,301,302,271}
        };
    }

    private static Point[] genVertices() {
        Point[] vertices = {
                new Point(1.4,0.0,2.4),
                new Point(1.4,-0.784,2.4),
                new Point(0.784,-1.4,2.4),
                new Point(0.0,-1.4,2.4),
                new Point(1.3375,0.0,2.53125),
                new Point(1.3375,-0.749,2.53125),
                new Point(0.749,-1.3375,2.53125),
                new Point(0.0,-1.3375,2.53125),
                new Point(1.4375,0.0,2.53125),
                new Point(1.4375,-0.805,2.53125),
                new Point(0.805,-1.4375,2.53125),
                new Point(0.0,-1.4375,2.53125),
                new Point(1.5,0.0,2.4),
                new Point(1.5,-0.84,2.4),
                new Point(0.84,-1.5,2.4),
                new Point(0.0,-1.5,2.4),
                new Point(-0.784,-1.4,2.4),
                new Point(-1.4,-0.784,2.4),
                new Point(-1.4,0.0,2.4),
                new Point(-0.749,-1.3375,2.53125),
                new Point(-1.3375,-0.749,2.53125),
                new Point(-1.3375,0.0,2.53125),
                new Point(-0.805,-1.4375,2.53125),
                new Point(-1.4375,-0.805,2.53125),
                new Point(-1.4375,0.0,2.53125),
                new Point(-0.84,-1.5,2.4),
                new Point(-1.5,-0.84,2.4),
                new Point(-1.5,0.0,2.4),
                new Point(-1.4,0.784,2.4),
                new Point(-0.784,1.4,2.4),
                new Point(0.0,1.4,2.4),
                new Point(-1.3375,0.749,2.53125),
                new Point(-0.749,1.3375,2.53125),
                new Point(0.0,1.3375,2.53125),
                new Point(-1.4375,0.805,2.53125),
                new Point(-0.805,1.4375,2.53125),
                new Point(0.0,1.4375,2.53125),
                new Point(-1.5,0.84,2.4),
                new Point(-0.84,1.5,2.4),
                new Point(0.0,1.5,2.4),
                new Point(0.784,1.4,2.4),
                new Point(1.4,0.784,2.4),
                new Point(0.749,1.3375,2.53125),
                new Point(1.3375,0.749,2.53125),
                new Point(0.805,1.4375,2.53125),
                new Point(1.4375,0.805,2.53125),
                new Point(0.84,1.5,2.4),
                new Point(1.5,0.84,2.4),
                new Point(1.75,0.0,1.875),
                new Point(1.75,-0.98,1.875),
                new Point(0.98,-1.75,1.875),
                new Point(0.0,-1.75,1.875),
                new Point(2.0,0.0,1.35),
                new Point(2.0,-1.12,1.35),
                new Point(1.12,-2.0,1.35),
                new Point(0.0,-2.0,1.35),
                new Point(2.0,0.0,0.9),
                new Point(2.0,-1.12,0.9),
                new Point(1.12,-2.0,0.9),
                new Point(0.0,-2.0,0.9),
                new Point(-0.98,-1.75,1.875),
                new Point(-1.75,-0.98,1.875),
                new Point(-1.75,0.0,1.875),
                new Point(-1.12,-2.0,1.35),
                new Point(-2.0,-1.12,1.35),
                new Point(-2.0,0.0,1.35),
                new Point(-1.12,-2.0,0.9),
                new Point(-2.0,-1.12,0.9),
                new Point(-2.0,0.0,0.9),
                new Point(-1.75,0.98,1.875),
                new Point(-0.98,1.75,1.875),
                new Point(0.0,1.75,1.875),
                new Point(-2.0,1.12,1.35),
                new Point(-1.12,2.0,1.35),
                new Point(0.0,2.0,1.35),
                new Point(-2.0,1.12,0.9),
                new Point(-1.12,2.0,0.9),
                new Point(0.0,2.0,0.9),
                new Point(0.98,1.75,1.875),
                new Point(1.75,0.98,1.875),
                new Point(1.12,2.0,1.35),
                new Point(2.0,1.12,1.35),
                new Point(1.12,2.0,0.9),
                new Point(2.0,1.12,0.9),
                new Point(2.0,0.0,0.45),
                new Point(2.0,-1.12,0.45),
                new Point(1.12,-2.0,0.45),
                new Point(0.0,-2.0,0.45),
                new Point(1.5,0.0,0.225),
                new Point(1.5,-0.84,0.225),
                new Point(0.84,-1.5,0.225),
                new Point(0.0,-1.5,0.225),
                new Point(1.5,0.0,0.15),
                new Point(1.5,-0.84,0.15),
                new Point(0.84,-1.5,0.15),
                new Point(0.0,-1.5,0.15),
                new Point(-1.12,-2.0,0.45),
                new Point(-2.0,-1.12,0.45),
                new Point(-2.0,0.0,0.45),
                new Point(-0.84,-1.5,0.225),
                new Point(-1.5,-0.84,0.225),
                new Point(-1.5,0.0,0.225),
                new Point(-0.84,-1.5,0.15),
                new Point(-1.5,-0.84,0.15),
                new Point(-1.5,0.0,0.15),
                new Point(-2.0,1.12,0.45),
                new Point(-1.12,2.0,0.45),
                new Point(0.0,2.0,0.45),
                new Point(-1.5,0.84,0.225),
                new Point(-0.84,1.5,0.225),
                new Point(0.0,1.5,0.225),
                new Point(-1.5,0.84,0.15),
                new Point(-0.84,1.5,0.15),
                new Point(0.0,1.5,0.15),
                new Point(1.12,2.0,0.45),
                new Point(2.0,1.12,0.45),
                new Point(0.84,1.5,0.225),
                new Point(1.5,0.84,0.225),
                new Point(0.84,1.5,0.15),
                new Point(1.5,0.84,0.15),
                new Point(-1.6,0.0,2.025),
                new Point(-1.6,-0.3,2.025),
                new Point(-1.5,-0.3,2.25),
                new Point(-1.5,0.0,2.25),
                new Point(-2.3,0.0,2.025),
                new Point(-2.3,-0.3,2.025),
                new Point(-2.5,-0.3,2.25),
                new Point(-2.5,0.0,2.25),
                new Point(-2.7,0.0,2.025),
                new Point(-2.7,-0.3,2.025),
                new Point(-3.0,-0.3,2.25),
                new Point(-3.0,0.0,2.25),
                new Point(-2.7,0.0,1.8),
                new Point(-2.7,-0.3,1.8),
                new Point(-3.0,-0.3,1.8),
                new Point(-3.0,0.0,1.8),
                new Point(-1.5,0.3,2.25),
                new Point(-1.6,0.3,2.025),
                new Point(-2.5,0.3,2.25),
                new Point(-2.3,0.3,2.025),
                new Point(-3.0,0.3,2.25),
                new Point(-2.7,0.3,2.025),
                new Point(-3.0,0.3,1.8),
                new Point(-2.7,0.3,1.8),
                new Point(-2.7,0.0,1.575),
                new Point(-2.7,-0.3,1.575),
                new Point(-3.0,-0.3,1.35),
                new Point(-3.0,0.0,1.35),
                new Point(-2.5,0.0,1.125),
                new Point(-2.5,-0.3,1.125),
                new Point(-2.65,-0.3,0.9375),
                new Point(-2.65,0.0,0.9375),
                new Point(-2.0,-0.3,0.9),
                new Point(-1.9,-0.3,0.6),
                new Point(-1.9,0.0,0.6),
                new Point(-3.0,0.3,1.35),
                new Point(-2.7,0.3,1.575),
                new Point(-2.65,0.3,0.9375),
                new Point(-2.5,0.3,1.125),
                new Point(-1.9,0.3,0.6),
                new Point(-2.0,0.3,0.9),
                new Point(1.7,0.0,1.425),
                new Point(1.7,-0.66,1.425),
                new Point(1.7,-0.66,0.6),
                new Point(1.7,0.0,0.6),
                new Point(2.6,0.0,1.425),
                new Point(2.6,-0.66,1.425),
                new Point(3.1,-0.66,0.825),
                new Point(3.1,0.0,0.825),
                new Point(2.3,0.0,2.1),
                new Point(2.3,-0.25,2.1),
                new Point(2.4,-0.25,2.025),
                new Point(2.4,0.0,2.025),
                new Point(2.7,0.0,2.4),
                new Point(2.7,-0.25,2.4),
                new Point(3.3,-0.25,2.4),
                new Point(3.3,0.0,2.4),
                new Point(1.7,0.66,0.6),
                new Point(1.7,0.66,1.425),
                new Point(3.1,0.66,0.825),
                new Point(2.6,0.66,1.425),
                new Point(2.4,0.25,2.025),
                new Point(2.3,0.25,2.1),
                new Point(3.3,0.25,2.4),
                new Point(2.7,0.25,2.4),
                new Point(2.8,0.0,2.475),
                new Point(2.8,-0.25,2.475),
                new Point(3.525,-0.25,2.49375),
                new Point(3.525,0.0,2.49375),
                new Point(2.9,0.0,2.475),
                new Point(2.9,-0.15,2.475),
                new Point(3.45,-0.15,2.5125),
                new Point(3.45,0.0,2.5125),
                new Point(2.8,0.0,2.4),
                new Point(2.8,-0.15,2.4),
                new Point(3.2,-0.15,2.4),
                new Point(3.2,0.0,2.4),
                new Point(3.525,0.25,2.49375),
                new Point(2.8,0.25,2.475),
                new Point(3.45,0.15,2.5125),
                new Point(2.9,0.15,2.475),
                new Point(3.2,0.15,2.4),
                new Point(2.8,0.15,2.4),
                new Point(0.0,0.0,3.15),
                new Point(0.0,-0.002,3.15),
                new Point(0.002,0.0,3.15),
                new Point(0.8,0.0,3.15),
                new Point(0.8,-0.45,3.15),
                new Point(0.45,-0.8,3.15),
                new Point(0.0,-0.8,3.15),
                new Point(0.0,0.0,2.85),
                new Point(0.2,0.0,2.7),
                new Point(0.2,-0.112,2.7),
                new Point(0.112,-0.2,2.7),
                new Point(0.0,-0.2,2.7),
                new Point(-0.002,0.0,3.15),
                new Point(-0.45,-0.8,3.15),
                new Point(-0.8,-0.45,3.15),
                new Point(-0.8,0.0,3.15),
                new Point(-0.112,-0.2,2.7),
                new Point(-0.2,-0.112,2.7),
                new Point(-0.2,0.0,2.7),
                new Point(0.0,0.002,3.15),
                new Point(-0.8,0.45,3.15),
                new Point(-0.45,0.8,3.15),
                new Point(0.0,0.8,3.15),
                new Point(-0.2,0.112,2.7),
                new Point(-0.112,0.2,2.7),
                new Point(0.0,0.2,2.7),
                new Point(0.45,0.8,3.15),
                new Point(0.8,0.45,3.15),
                new Point(0.112,0.2,2.7),
                new Point(0.2,0.112,2.7),
                new Point(0.4,0.0,2.55),
                new Point(0.4,-0.224,2.55),
                new Point(0.224,-0.4,2.55),
                new Point(0.0,-0.4,2.55),
                new Point(1.3,0.0,2.55),
                new Point(1.3,-0.728,2.55),
                new Point(0.728,-1.3,2.55),
                new Point(0.0,-1.3,2.55),
                new Point(1.3,0.0,2.4),
                new Point(1.3,-0.728,2.4),
                new Point(0.728,-1.3,2.4),
                new Point(0.0,-1.3,2.4),
                new Point(-0.224,-0.4,2.55),
                new Point(-0.4,-0.224,2.55),
                new Point(-0.4,0.0,2.55),
                new Point(-0.728,-1.3,2.55),
                new Point(-1.3,-0.728,2.55),
                new Point(-1.3,0.0,2.55),
                new Point(-0.728,-1.3,2.4),
                new Point(-1.3,-0.728,2.4),
                new Point(-1.3,0.0,2.4),
                new Point(-0.4,0.224,2.55),
                new Point(-0.224,0.4,2.55),
                new Point(0.0,0.4,2.55),
                new Point(-1.3,0.728,2.55),
                new Point(-0.728,1.3,2.55),
                new Point(0.0,1.3,2.55),
                new Point(-1.3,0.728,2.4),
                new Point(-0.728,1.3,2.4),
                new Point(0.0,1.3,2.4),
                new Point(0.224,0.4,2.55),
                new Point(0.4,0.224,2.55),
                new Point(0.728,1.3,2.55),
                new Point(1.3,0.728,2.55),
                new Point(0.728,1.3,2.4),
                new Point(1.3,0.728,2.4),
                new Point(0.0,0.0,0.0),
                new Point(1.5,0.0,0.15),
                new Point(1.5,0.84,0.15),
                new Point(0.84,1.5,0.15),
                new Point(0.0,1.5,0.15),
                new Point(1.5,0.0,0.075),
                new Point(1.5,0.84,0.075),
                new Point(0.84,1.5,0.075),
                new Point(0.0,1.5,0.075),
                new Point(1.425,0.0,0.0),
                new Point(1.425,0.798,0.0),
                new Point(0.798,1.425,0.0),
                new Point(0.0,1.425,0.0),
                new Point(-0.84,1.5,0.15),
                new Point(-1.5,0.84,0.15),
                new Point(-1.5,0.0,0.15),
                new Point(-0.84,1.5,0.075),
                new Point(-1.5,0.84,0.075),
                new Point(-1.5,0.0,0.075),
                new Point(-0.798,1.425,0.0),
                new Point(-1.425,0.798,0.0),
                new Point(-1.425,0.0,0.0),
                new Point(-1.5,-0.84,0.15),
                new Point(-0.84,-1.5,0.15),
                new Point(0.0,-1.5,0.15),
                new Point(-1.5,-0.84,0.075),
                new Point(-0.84,-1.5,0.075),
                new Point(0.0,-1.5,0.075),
                new Point(-1.425,-0.798,0.0),
                new Point(-0.798,-1.425,0.0),
                new Point(0.0,-1.425,0.0),
                new Point(0.84,-1.5,0.15),
                new Point(1.5,-0.84,0.15),
                new Point(0.84,-1.5,0.075),
                new Point(1.5,-0.84,0.075),
                new Point(0.798,-1.425,0.0),
                new Point(1.425,-0.798,0.0)
        };
        return vertices;
    }

    public static void main(String[] args) throws Exception {
        Vector centerOnOrigin = new Vector(0.0, 0.0, -1.25);
//        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
//        Teapot teapot = new Teapot(DetailLevel.MEDIUM_HIGH).translate(centerOnOrigin).rotate(Axis.X, 0.85);
//        new SingleShapeDemo(teapot, offWhite, SingleShapeDemo.Mode.SHADED).start();
        Teapot teapot = new Teapot(DetailLevel.MEDIUM_HIGH).translate(centerOnOrigin);
        Texture surface = new TextureFactory().loadClasspathResource("images/brick_texture.jpg").build();
        new SingleShapeDemo(teapot, surface, new MatrixMapping(0.25,0,0,0.5,0,0,0.25,0.5)).start();
//        Texture surface = new TextureFactory().loadClasspathResource("images/granite_texture.jpg").build();
//        Texture surface = new TextureFactory().loadClasspathResource("images/MoonMap_2500x1250.jpg").build();
//        new SingleShapeDemo(teapot, surface, new SphericalMapping()) {
//            protected Light[] getLights(double cameraDistance) {
//                return new Light[]{
//                        new FixedPointSourceLight(new net.venaglia.gloo.physical.geom.Point(-2.1f, 0.0f, 1.5f).scale(cameraDistance))
//                };
//            }
//        }.start();
    }
}
