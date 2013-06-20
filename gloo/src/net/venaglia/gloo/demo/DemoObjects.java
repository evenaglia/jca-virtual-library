package net.venaglia.gloo.demo;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.decorators.Transformation;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.CompositeShape;
import net.venaglia.gloo.physical.geom.detail.AbstractDynamicDetailSource;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Faceted;
import net.venaglia.gloo.physical.geom.FlippableShape;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.complex.GeodesicSphere;
import net.venaglia.gloo.physical.geom.complex.Origin;
import net.venaglia.gloo.physical.geom.complex.TessellatedFacet;
import net.venaglia.gloo.physical.geom.detail.DynamicDetailSource;
import net.venaglia.gloo.physical.geom.primitives.*;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.impl.DelegatingGeometryBuffer;
import net.venaglia.gloo.projection.impl.DynamicDecorator;
import net.venaglia.common.util.Series;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 9/25/12
* Time: 9:48 AM
* To change this template use File | Settings | File Templates.
*/
public class DemoObjects implements Series<Shape<?>> {

    public enum ObjectCategory {
        PLATONIC_SOLIDS,
        FLAT_SURFACES,
        CURVED_SURFACES,
        ROUND_SOLIDS,
        EXTRUDED_SHAPES,
        INTERESTING_SHAPES,
        TEXTURES;

        public DynamicDetailSource<Shape<?>> getDynamicDetailSource(int shapeNum) {
            return this.getDynamicDetailSource(shapeNum, 0.5, false);
        }

        public DynamicDetailSource<Shape<?>> getDynamicDetailSource(int shapeNum,
                                                                    double timescale,
                                                                    boolean wireframe) {
            if (shapeNum < 0 || shapeNum >= 3) {
                throw new IllegalArgumentException("shapeNum must be one of [0,1,2]: " + shapeNum);
            }
            return buildDynamicDetailSource(this, shapeNum, timescale, wireframe);
        }
    }

    private final ObjectCategory category;
    private final Shape<?> origin;
    private final Brush brush;
    private final String[] names;
    private final Shape[] shapes;
    private final Color[] colors;

    private Texture texture;

    private static final Brush TWO_SIDED;

    static {
        Brush twoSided = new Brush(Brush.FRONT_SHADED);
        twoSided.setCulling(null);
        TWO_SIDED = twoSided;
    }

    public DemoObjects(final double timeScale,
                       final float divisionCount,
                       final ObjectCategory objectCategory,
                       final boolean wireframe) {

        this.category = objectCategory;
        this.origin = new Origin(0.25);

        this.names = new String[3];
        this.shapes = new Shape[]{
                buildShape0(divisionCount),
                buildShape1(divisionCount),
                buildShape2(divisionCount)
        };
        this.colors = new Color[]{
                new Color(0.5f, 0.5f, 1.0f),
                new Color(1.0f, 0.5f, 0.5f),
                new Color(0.5f, 1.0f, 0.5f)
        };

        this.brush = wireframe ? Brush.WIRE_FRAME : Brush.FRONT_SHADED;
        for (int i = 0; i < 3; i++) {
            Color color = getMaterialColor(i);
            getShape(i).setMaterial(Material.paint(color, brush));
        }

        Vector axis1 = new Vector(0.0, 0.5, 0.0);
        Vector axis2 = new Vector(0.75, 0.1, 0.333);

        for (int i = 0, j = 0; i < 3; i++, j += Math.PI * 0.666667) {
            Shape<?> shape = getShape(i);
            animate(shape.getTransformation(), timeScale, axis1, axis2);
            arrangeInCircle(shape.getTransformation(), 0.5, j);
            animate(shape.getTransformation(), timeScale, Vector.X, new Vector(0.15, 0.03, 1.0));
        }
    }

    private Shape<?> buildShape0(final float divisionCount) {
        Shape<?> shape = null;
        String name = null;
        switch (category) {
            case PLATONIC_SOLIDS:
                Tetrahedron tetrahedron = new Tetrahedron().scale(0.333);
                CompositeShape compositeShape = new CompositeShape();
                for (int i = 0; i < tetrahedron.facetCount(); i++) {
                    compositeShape.addShape(new TessellatedFacet(tetrahedron.getFacet(i), Math.round(divisionCount)));
                }
                compositeShape.inheritMaterialToContainedShapes();
                shape = compositeShape;
                name = "Tetrahedron";
                break;
            case FLAT_SURFACES:
                shape = duplicateAndFlip(new DiscSector(Math.round(divisionCount * 2), 0.5, 0.3, 2.2)
                                                 .translate(Vector.X.scale(-0.25)));
                name = "Wedge";
                break;
            case CURVED_SURFACES:
                shape = duplicateAndFlip(new Tube(Math.round(divisionCount), 0.25, 0.5));
                name = "Tube";
                break;
            case ROUND_SOLIDS:
                int segments = Math.round(divisionCount * 2);
                Disc top = new Disc(segments, 0.25).translate(Vector.Z.scale(0.25));
                Disc bottom = top.scale(-1).flip();
                Tube cylnder = new Tube(segments, 0.25, 0.5);
                CompositeShape compositeShape2 = new CompositeShape();
                compositeShape2.addShape(top);
                compositeShape2.addShape(bottom);
                compositeShape2.addShape(cylnder);
                compositeShape2.inheritMaterialToContainedShapes();
                shape = compositeShape2;
                name = "Cylinder";
                break;
            case EXTRUDED_SHAPES:
                shape = new Star(0.25, 0.1, 5, 0.0625);
                name = "Star";
                break;
            case INTERESTING_SHAPES:
                if (divisionCount > 1.0f) {
                    DetailLevel detailLevel = divisionCount < 4.0f
                                                          ? DetailLevel.LOW
                                                          : divisionCount < 16.0f
                                                            ? DetailLevel.MEDIUM_LOW
                                                            : DetailLevel.MEDIUM;
                    shape = new Teapot(detailLevel).scale(0.15);
                } else {
                    shape = new QuadSequence((Faceted)(new Teapot(DetailLevel.LOW).translate(Vector.Z.scale(-1.0)).scale(
                            0.15)));
                }
                name = "Teapot";
                break;
            case TEXTURES:
                shape = new ImageQuad(getTexture(), ImageQuad.Mode.SHADED_WITH_THRESHOLD_TRANSPARENCY_50, 0.0f, 0.0f, 0.5f, 0.5f).scale(0.6667);
                name = "Square-Ring";
                break;
        }
        names[0] = name;
        return shape;
    }

    private Shape<?> buildShape1(final float divisionCount) {
        Shape<?> shape = null;
        String name = null;
        DetailLevel detailLevel = divisionCount < 4.0f
                                              ? DetailLevel.LOW
                                              : divisionCount < 16.0f
                                                ? DetailLevel.MEDIUM_LOW
                                                : DetailLevel.MEDIUM;
        switch (category) {
            case PLATONIC_SOLIDS:
                Box cube = new Box().scale(0.5);
                CompositeShape compositeShape = new CompositeShape();
                for (int i = 0, l = cube.facetCount(); i < l; i++) {
                    compositeShape.addShape(new TessellatedFacet(cube.getFacet(i), Math.round(divisionCount * 0.75f)));
                }
                compositeShape.inheritMaterialToContainedShapes();
                shape = compositeShape;
                name = "Cube";
                break;
            case FLAT_SURFACES:
                shape = duplicateAndFlip(new Ring(Math.round(divisionCount * 2), 0.3, 0.18));
                name = "Ring";
                break;
            case CURVED_SURFACES:
                shape = duplicateAndFlip(new Dome(new Point(0,0,0.25),
                                                  new Point(0.20,0,-0.25),
                                                  new Point(0.20,0,0),
                                                  new Point(0,0.20,-0.25),
                                                  Math.round(divisionCount)).scale(0.6667));
                name = "Dome";
                break;
            case ROUND_SOLIDS:
                shape = new Torus(Math.round(divisionCount * 2), 0.25, 0.1, false);
                name = "Torus";
                break;
            case EXTRUDED_SHAPES:
                shape = new Cog(6, 0.25, 0.1875, detailLevel);
                name = "Cog";
                break;
            case INTERESTING_SHAPES:
                if (divisionCount > 1.0f) {
                    shape = new Heart(detailLevel).scale(0.35);
                } else {
                    shape = new QuadSequence((Faceted)(new Heart(DetailLevel.LOW).scale(0.35)));
                }
                name = "Heart";
                break;
            case TEXTURES:
                shape = new ImageQuad(getTexture(), ImageQuad.Mode.SHADED_WITH_THRESHOLD_TRANSPARENCY_50, 0.0f, 0.5f, 0.5f, 1.0f).scale(0.6667);
                name = "Triangular-Ring";
                break;
        }
        names[1] = name;
        return shape;
    }

    private Shape<?> buildShape2(final float divisionCount) {
        Shape<?> shape = null;
        String name = null;
        DetailLevel detailLevel;
        if (divisionCount >= 32.0f) {
            detailLevel = DetailLevel.HIGH;
        } else if (divisionCount >= 16.0f) {
            detailLevel = DetailLevel.MEDIUM_HIGH;
        } else if (divisionCount >= 8.0f) {
            detailLevel = DetailLevel.MEDIUM;
        } else if (divisionCount >= 4.0f) {
            detailLevel = DetailLevel.MEDIUM_LOW;
        } else {
            detailLevel = DetailLevel.LOW;
        }
        switch (category) {
            case PLATONIC_SOLIDS:
                Icosahedron icosahedron = new Icosahedron().scale(0.333);
                CompositeShape compositeShape = new CompositeShape();
                for (int i = 0, l = icosahedron.facetCount(); i < l; i++) {
                    TessellatedFacet tessellatedFacet = new TessellatedFacet(icosahedron.getFacet(i), Math.round(divisionCount * 0.5f)) {
                        @Override
                        protected void project(GeometryBuffer buffer) {
                            super.project(new DelegatingGeometryBuffer(buffer) {
                                public void vertex(Point point) {
                                    super.vertex(point);
                                }

                                @Override
                                public void vertex(double x, double y, double z) {
                                    vertex(new Point(x, y, z));
                                }
                            });
                        }
                    };
                    compositeShape.addShape(tessellatedFacet);
                }
                compositeShape.inheritMaterialToContainedShapes();
                shape = compositeShape;
                name = "Icosahedron";
                break;
            case FLAT_SURFACES:
                shape = duplicateAndFlip(new Polygon(Vector.Z,
                                                     new Point(-4,0,0),
                                                     new Point(-2,3.5,0),
                                                     new Point(2,3.5,0),
                                                     new Point(4,0,0),
                                                     new Point(2,-3.5,0),
                                                     new Point(-2,-3.5,0)).scale(0.1));
                name = "Hex";
                break;
            case CURVED_SURFACES:
                Point[] points = {
                    new Point(0,0, 0), new Point(1,0, 1), new Point(2,0,-1), new Point(3,0, 0),
                    new Point(0,1, 1), new Point(1,1,-1), new Point(2,1, 1), new Point(3,1,-1),
                    new Point(0,2,-1), new Point(1,2, 1), new Point(2,2,-1), new Point(3,2, 1),
                    new Point(0,3, 0), new Point(1,3,-1), new Point(2,3, 1), new Point(3,3, 0)
                };
                shape = duplicateAndFlip(new BezierPatch(points, detailLevel)
                                                 .translate(new Vector(-1.5, -1.5, 0))
                                                 .scale(0.2));
                name = "Bezier";
                break;
            case ROUND_SOLIDS:
                shape = new GeodesicSphere(Math.round(divisionCount * 0.25f)).scale(0.75);
                name = "Sphere";
                break;
            case EXTRUDED_SHAPES:
                shape = new Crescent(0.25, 0.125, divisionCount < 4.0f
                                                  ? DetailLevel.LOW
                                                  : divisionCount < 16.0f
                                                    ? DetailLevel.MEDIUM_LOW
                                                    : DetailLevel.MEDIUM);
                name = "Crescent";
                break;
            case INTERESTING_SHAPES:
                shape = new Scroll(detailLevel).scale(0.5);
                name = "Scroll";
                break;
            case TEXTURES:
                shape = new ImageQuad(getTexture(), ImageQuad.Mode.SHADED_WITH_THRESHOLD_TRANSPARENCY_50, 0.5f, 0.0f, 1.0f, 0.5f).scale(0.6667);
                name = "Pentagonal-Ring";
                break;
        }
        names[2] = name;
        return shape;
    }

    private Texture getTexture() {
        if (texture == null) {
            texture = new TextureFactory().loadClasspathResource("images/demo-texture.png").setForceAlpha(true).build();
        }
        return texture;
    }

    private Shape<?> duplicateAndFlip(FlippableShape<?> shape) {
        CompositeShape compositeShape = new CompositeShape(shape, shape.flip());
        compositeShape.inheritMaterialToContainedShapes();
        return compositeShape;
    }

    public Brush getBrush() {
        return brush;
    }

    public Color getMaterialColor(int shapeNum) {
        return colors[shapeNum];
    }

    public Shape<?> getShape(int shapeNum) {
        return shapes[shapeNum];
    }

    public String getShapeName(int shapeNum) {
        return names[shapeNum];
    }

    public Shape<?> getOrigin() {
        return origin;
    }

    public int size() {
        return 3;
    }

    @SuppressWarnings({ "unchecked", "ConstantConditions" })
    public Iterator<Shape<?>> iterator() {
        List shapes = Arrays.asList(this.shapes);
        return ((List<Shape<?>>)shapes).iterator();
    }

    public static void animate(Transformation transformation, double timeScale, final Vector axis1, final Vector axis2) {
        transformation.decorate(new DemoObjects.AnimatedDecorator(timeScale) {
            @Override
            public boolean isStatic() {
                return false;
            }

            @Override
            protected void apply(double seconds, GeometryBuffer buffer) {
                buffer.rotate(axis1, seconds * Math.PI * 0.25);
                buffer.rotate(axis2, seconds * Math.PI * 0.33333333);
            }
        }, new Transformation.Op("ani", "ts=%.2f,axis1=%s,axis2=%s", timeScale, axis1, axis2));
    }

    public static void arrangeInCircle(Transformation transformation, double radius, double angle) {
        transformation.translate(Vector.X.scale(radius));
        transformation.rotate(Axis.Z, angle);
    }

    /**
     * Creates a vector from one point to another
     * @param a The point to start from
     * @param b The point to end at
     * @return A vector: a &rarr; b
     */
    public static Vector toVector(Point a, Point b) {
        return new Vector(b.x - a.x, b.y - a.y, b.z - a.z);
    }

    private static DynamicDetailSource<Shape<?>> buildDynamicDetailSource(final ObjectCategory objectCategory,
                                                                          final int shapeNum,
                                                                          final double timescale,
                                                                          final boolean wireframe) {
        return new AbstractDynamicDetailSource<Shape<?>>() {
            public Shape<?> produceAt(DetailLevel detailLevel) {
                int divisions = objectCategory == ObjectCategory.PLATONIC_SOLIDS ? 0 : detailLevel.steps;
                return new DemoObjects(timescale, divisions, objectCategory, wireframe).shapes[shapeNum];
            }
        };
    }

    public abstract static class AnimatedDecorator extends DynamicDecorator {

        private static final long BASE_TIME = System.currentTimeMillis();

        private final double timeScale;

        public AnimatedDecorator(double timeScale) {
            this.timeScale = timeScale;
        }

        public void apply(long nowMS, GeometryBuffer buffer) {
            nowMS += 1000L;
            apply((nowMS - BASE_TIME) * timeScale / 1000.0, buffer);
        }

        protected abstract void apply(double seconds, GeometryBuffer buffer);
    }
}
