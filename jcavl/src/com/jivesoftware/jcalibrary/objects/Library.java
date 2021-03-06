package com.jivesoftware.jcalibrary.objects;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.CompositeShape;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.primitives.Disc;
import net.venaglia.gloo.physical.geom.primitives.Dome;
import net.venaglia.gloo.physical.geom.primitives.QuadSequence;
import net.venaglia.gloo.physical.geom.primitives.Ring;
import net.venaglia.gloo.physical.geom.primitives.Torus;
import net.venaglia.gloo.physical.geom.primitives.Tube;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.physical.texture.mapping.CylindricalMapping;
import net.venaglia.gloo.physical.texture.mapping.MatrixMapping;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 4:11 PM
 */
public class Library implements Projectable {

    /*                                          |
          +------------------20-----------------:
          |                                     |
          |                                     :
          5                                     |
          |                                     :
          |                            +---4----|
          +-----6-----+                2        : <-- 0
                      1                |        |
                      +-------10-------+        :
                                                |
     */

    private CompositeShape floor;
    private CompositeShape room;
    private CompositeShape stripLights;
    private CompositeShape overheadLights;

    private final double ceilingHeight;
    //    private int[] demoIndexes;
//    private Vector[] demoTranslations;
//    private DemoObjects[] demoObjects;

    public Library() {
        final int segmentsBase = 36;
        final int segments0 = segmentsBase * 2;
        final int segments1 = segmentsBase * 3;
        final int segments2 = segmentsBase * 4;
        final int segments3 = segmentsBase * 6;
        this.ceilingHeight = 3.8;
//        Texture hexGrid = new TextureFactory().loadClasspathResource("images/hex-grid-2048.png").setForceAlpha(true).build();
        Texture hexGrid = new TextureFactory().loadClasspathResource("images/hex-grid-256.png").setMipMapped(true).build();
        Texture grid = new TextureFactory().loadClasspathResource("images/grid-256.png").setMipMapped(true).build();
        Brush environmentBrush = new Brush();
        environmentBrush.setTexturing(false);
        environmentBrush.setLighting(false);
        environmentBrush.setDepth(null);

        this.floor = new CompositeShape();
        this.floor.addShape(new Dome(new Point(0,0, ceilingHeight + 4), new Point(0,20, ceilingHeight), new Point(20,0,
                                                                                                                  ceilingHeight), new Point(-20,0,
                                                                                                                                            ceilingHeight), segments2));
        this.floor.addShape(new Ring(segments2, 20, 14));
        this.floor.addShape(new Ring(segments1, 14, 4).translate(Vector.Z.scale(-0.5)));
        this.floor.addShape(new Disc(segments0, 4).translate(Vector.Z.scale(0.5)));
        this.floor.setMaterial(Material.makeTexture(hexGrid, new MatrixMapping(Matrix_4x4.scale(new Vector(1,12.0/7.0,1)))));
//        this.floor.setMaterial(Material.makeTexture(grid, new MatrixMapping(Matrix_4x4.scale(new Vector(2,2,1)))));
        this.floor.inheritMaterialToContainedShapes();

        Material grid1 = Material.makeTexture(grid, new CylindricalMapping(2,251));
        Material grid2 = Material.makeTexture(grid, new CylindricalMapping(2,176));
        Material grid3 = Material.makeTexture(grid, new CylindricalMapping(2,50));
//        Material grid1 = Material.makeTexture(hexGrid, new CylindricalMapping(2,147));
//        Material grid2 = Material.makeTexture(hexGrid, new CylindricalMapping(2,103));
//        Material grid3 = Material.makeTexture(hexGrid, new CylindricalMapping(2,29));
        this.room = new CompositeShape();
//        this.room.addShape(new Tube(segments3, 20, ceilingHeight).flip().translate(Vector.Z.scale(ceilingHeight * 0.5)).setMaterial(grid1));
        this.room.addShape(new Tube(segments2, 14, 0.5).flip().translate(Vector.Z.scale(-0.25)).setMaterial(grid2));
        this.room.addShape(new Tube(segments1, 4, 1).translate(Vector.Z.scale(0)).setMaterial(grid3));
        this.room.setMaterial(Material.INHERIT);

        this.stripLights = new CompositeShape();
        this.stripLights.addShape(new Torus(segments3, 4, 20, 0.025, false).translate(new Vector(0,0, ceilingHeight)));
        this.stripLights.addShape(new Torus(segments3, 4, 20, 0.025, false));
        this.stripLights.addShape(new Torus(segments2, 4, 14, 0.025, false));
        this.stripLights.addShape(new Torus(segments2, 4, 14, 0.025, false).translate(new Vector(0,0,-0.5)));
        this.stripLights.addShape(new Torus(segments1, 4, 4, 0.025, false).translate(new Vector(0,0,-0.5)));
        this.stripLights.addShape(new Torus(segments1, 4, 4, 0.025, false).translate(new Vector(0,0,0.5)));
        this.stripLights.setMaterial(Material.makeSelfIlluminating(Color.CYAN));
        this.stripLights.inheritMaterialToContainedShapes();

//        final boolean wireframe = false;
//        final Vector baseVector = new Vector(0,9,1);
//        final double angle = Math.PI * 0.2;
//        this.demoIndexes = new int[]{ 0, 1, 2, 3, 4 };
//        this.demoTranslations = new Vector[]{
//                baseVector.rotate(Axis.Z, angle * 1),
//                baseVector.rotate(Axis.Z, angle * 3),
//                baseVector.rotate(Axis.Z, angle * 5),
//                baseVector.rotate(Axis.Z, angle * 7),
//                baseVector.rotate(Axis.Z, angle * 9)
//        };
//        this.demoObjects = new DemoObjects[]{
////                new DemoObjects(0.5, segmentsBase * 0.111f, DemoObjects.ObjectCategory.INTERESTING_SHAPES, wireframe),
////                new DemoObjects(0.5, segmentsBase * 0.111f, DemoObjects.ObjectCategory.CURVED_SURFACES, wireframe),
////                new DemoObjects(0.5, segmentsBase * 0.111f, DemoObjects.ObjectCategory.ROUND_SOLIDS, wireframe),
////                new DemoObjects(0.5, 1                     , DemoObjects.ObjectCategory.PLATONIC_SOLIDS, wireframe),
////                new DemoObjects(0.5, segmentsBase * 0.111f, DemoObjects.ObjectCategory.EXTRUDED_SHAPES, wireframe),
//                new DemoObjects(0.5, segmentsBase * 0.333f, DemoObjects.ObjectCategory.INTERESTING_SHAPES, wireframe),
//                new DemoObjects(0.5, segmentsBase * 0.333f, DemoObjects.ObjectCategory.CURVED_SURFACES, wireframe),
//                new DemoObjects(0.5, segmentsBase * 0.333f, DemoObjects.ObjectCategory.ROUND_SOLIDS, wireframe),
//                new DemoObjects(0.5, 1                    , DemoObjects.ObjectCategory.PLATONIC_SOLIDS, wireframe),
//                new DemoObjects(0.5, segmentsBase * 0.333f, DemoObjects.ObjectCategory.EXTRUDED_SHAPES, wireframe),
//                new DemoObjects(0.5, segmentsBase, DemoObjects.ObjectCategory.INTERESTING_SHAPES, wireframe),
//                new DemoObjects(0.5, segmentsBase, DemoObjects.ObjectCategory.CURVED_SURFACES, wireframe),
//                new DemoObjects(0.5, segmentsBase, DemoObjects.ObjectCategory.ROUND_SOLIDS, wireframe),
//                new DemoObjects(0.5, 1                    , DemoObjects.ObjectCategory.PLATONIC_SOLIDS, wireframe),
//                new DemoObjects(0.5, segmentsBase, DemoObjects.ObjectCategory.EXTRUDED_SHAPES, wireframe),
//                new DemoObjects(0.5, segments0, DemoObjects.ObjectCategory.INTERESTING_SHAPES, wireframe),
//                new DemoObjects(0.5, segments0, DemoObjects.ObjectCategory.CURVED_SURFACES, wireframe),
//                new DemoObjects(0.5, segments0, DemoObjects.ObjectCategory.ROUND_SOLIDS, wireframe),
//                new DemoObjects(0.5, 1        , DemoObjects.ObjectCategory.PLATONIC_SOLIDS, wireframe),
//                new DemoObjects(0.5, segments0, DemoObjects.ObjectCategory.EXTRUDED_SHAPES, wireframe),
//        };

        this.overheadLights = new CompositeShape();
        QuadSequence baseLight = new QuadSequence(new Point(-2,0,-0.25), new Point(-2,0,0.25), new Point(2,0,0.25), new Point(2,0,-0.25))
                .rotate(Axis.X, Math.PI * 0.45)
                .translate(rotatedVector(0,0));
        this.overheadLights.addShape(baseLight.rotate(Axis.Z, rotatedAngle(1,3)));
        this.overheadLights.addShape(baseLight.rotate(Axis.Z, rotatedAngle(2,3)));
        this.overheadLights.addShape(baseLight);
        this.overheadLights.inheritMaterialToContainedShapes();
        this.overheadLights.setMaterial(Material.makeSelfIlluminating(Color.WHITE));
    }

    public Vector rotatedVector(int n, int m) {
        double radius = 16;
        double angle = rotatedAngle(n, m);
        return new Vector(Math.sin(angle) * radius, Math.cos(angle) * radius, ceilingHeight + 1);
    }

    private double rotatedAngle(int n, int m) {
        return n == 0 ? 0 : Math.PI * 2 * n / m;
    }

    public double getCeilingHeight() {
        return ceilingHeight;
    }

    public boolean isStatic() {
        return true;
    }

//    public void updateDetail(Point viewFrom) {
//        for (int i = 0; i < demoIndexes.length; i++) {
//            Vector v = demoTranslations[i];
//            double d = Vector.computeDistance(v.i - viewFrom.x, v.j - viewFrom.y, v.k - viewFrom.z);
//            int detail = d > 8 ? (d > 24 ? 0 : 0) : d > 3 ? 1 : 2;
//            demoIndexes[i] = detail * demoIndexes.length + i;
//        }
//    }

    public void project(long nowMS, GeometryBuffer buffer) {
        buffer.pushBrush();
        buffer.popBrush();
        floor.project(nowMS, buffer);
        room.project(nowMS, buffer);
        stripLights.project(nowMS, buffer);
        overheadLights.project(nowMS, buffer);

//        for (int i = 0; i < demoIndexes.length; i++) {
//            buffer.pushTransform();
//            buffer.translate(demoTranslations[i]);
//            for (Shape<?> shape : demoObjects[demoIndexes[i]]) {
//                shape.project(nowMS, buffer);
//            }
//            buffer.popTransform();
//        }
    }
}
