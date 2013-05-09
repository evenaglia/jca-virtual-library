package net.venaglia.realms.common.physical.geom.complex;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Faceted;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.primitives.Icosahedron;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.Lock;
import net.venaglia.realms.common.util.impl.ThreadSafeLock;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 10/19/12
 * Time: 8:55 AM
 */
final public class GeodesicSphere implements Shape<GeodesicSphere>, Faceted {

    protected final BoundingSphere bounds;
    protected final TessellatedFacet[] facets;
    protected final int pointsPerFacet;
    protected final int facetsPerFacet;
    protected final Lock lock = new ThreadSafeLock();

    protected Transformation transformation;
    protected Material material = Material.DEFAULT;

    public GeodesicSphere(int divisionCount) {
        if (divisionCount <= 0) {
            throw new IllegalArgumentException("Division count must be > 0: " + divisionCount);
        }
        this.bounds = new BoundingSphere(Point.ORIGIN, 0.5);
        Faceted base = new Icosahedron();
        facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = new SphericalTessellatedFacet(base.getFacet(i), divisionCount);
        }
        pointsPerFacet = facets[0].size();
        facetsPerFacet = facets[0].facetCount();
    }

    private GeodesicSphere(BoundingSphere bounds, TessellatedFacet[] facets, Transformation transformation) {
        this.bounds = bounds;
        this.facets = facets;
        pointsPerFacet = facets[0].size();
        facetsPerFacet = facets[0].facetCount();
        if (transformation != null) {
            getTransformation().transform(transformation);
        }
    }

    protected TessellatedFacet getFacetForPointIndex(int index) {
        return facets[index / pointsPerFacet];
    }

    protected TessellatedFacet getFacetForFacetIndex(int index) {
        return facets[index / facetsPerFacet];
    }

    public int facetCount() {
        return facetsPerFacet * 20;
    }

    public Facet.Type getFacetType() {
        return Facet.Type.TRIANGLE;
    }

    public Facet getFacet(int index) {
        return getFacetForFacetIndex(index).getFacet(index % facetsPerFacet);
    }

    public Vector getNormal(int index) {
        return getFacetForPointIndex(index).getNormal(index % pointsPerFacet);
    }

    public Transformation getTransformation() {
        if (transformation == null) {
            transformation = new Transformation(lock);
        }
        return transformation;
    }

    public Lock getLock() {
        return lock;
    }

    public GeodesicSphere setMaterial(Material material) {
        this.material = material;
        return this;
    }

    public Material getMaterial() {
        return material;
    }

    public BoundingVolume<?> getBounds() {
        return bounds;
    }

    public GeodesicSphere scale(double magnitude) {
        TessellatedFacet[] facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = this.facets[i].scale(magnitude);
        }
        return new GeodesicSphere(bounds.scale(magnitude), facets, transformation);
    }

    public GeodesicSphere scale(Vector magnitude) {
        TessellatedFacet[] facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = this.facets[i].scale(magnitude);
        }
        return new GeodesicSphere(bounds.scale(magnitude), facets, transformation);
    }

    public GeodesicSphere translate(Vector magnitude) {
        TessellatedFacet[] facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = this.facets[i].translate(magnitude);
        }
        return new GeodesicSphere(bounds.translate(magnitude), facets, transformation);
    }

    public GeodesicSphere rotate(Vector x, Vector y, Vector z) {
        TessellatedFacet[] facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = this.facets[i].rotate(x, y, z);
        }
        return new GeodesicSphere(bounds.rotate(x, y, z), facets, transformation);
    }

    public GeodesicSphere rotate(Axis axis, double angle) {
        TessellatedFacet[] facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = this.facets[i].rotate(axis, angle);
        }
        return new GeodesicSphere(bounds.rotate(axis, angle), facets, transformation);
    }

    public GeodesicSphere transform(XForm xForm) {
        TessellatedFacet[] facets = new TessellatedFacet[20];
        for (int i = 0; i < 20; i++) {
            facets[i] = this.facets[i].transform(xForm);
        }
        return new GeodesicSphere(bounds.transform(xForm), facets, transformation);
    }

    public GeodesicSphere copy() {
        return new GeodesicSphere(bounds, this.facets.clone(), transformation);
    }

    public int size() {
        return pointsPerFacet * 20;
    }

    public Iterator<Point> iterator() {
        return new Iterator<Point>() {

            private int i = 0;
            private int l = size();
            private Iterator<Point> iter = facets[0].iterator();

            public boolean hasNext() {
                return i < l;
            }

            public Point next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                i++;
                if (i % pointsPerFacet == 0) {
                    iter = getFacetForPointIndex(i).iterator();
                }
                return iter.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        if (transformation == null) {
            material.apply(nowMS, buffer);
            project(buffer);
        } else {
            buffer.pushTransform();
            transformation.apply(nowMS, buffer);
            material.apply(nowMS, buffer);
            project(buffer);
            buffer.popTransform();
        }
    }

    public void project(GeometryBuffer buffer) {
        for (int i = 0; i < 20; i++) {
            facets[i].project(buffer);
        }
    }

    private class SphericalTessellatedFacet extends TessellatedFacet {

        private final Vector[] normals;

        public SphericalTessellatedFacet(Facet facet, int divisionCount) {
            super(facet, divisionCount);
            normals = new Vector[points.length];
            for (int i = 0, l = points.length; i < l; i++) {
                Point p = points[i];
                Vector r = new Vector(p.x, p.y, p.z);
                double magnitude = 0.5 / r.l;
                points[i] = p.scale(magnitude);
                normals[i] = Vector.betweenPoints(points[i], p.scale(magnitude * 2.0));
            }
        }

        protected SphericalTessellatedFacet(Point[] points, Vector[] normals, int[][] strips, int divisions, Facet.Type type) {
            super(points, strips, divisions, type);
            this.normals = normals;
        }

        @Override
        protected TessellatedFacet build(Point[] points, XForm xForm) {
            if (!xForm.isSymmetric()) {
                throw new UnsupportedOperationException("Asymmetric scaling of a GeodesicSphere is not supported.");
            }
            return new SphericalTessellatedFacet(assertLength(points, this.points.length),
                                                 assertLength(normals, this.points.length),
                                                 strips, divisions, type);
        }

        @Override
        public Vector getNormal(int index) {
            return Vector.betweenPoints(bounds.center, points[index]).normalize();
        }

        @Override
        protected void project(GeometryBuffer buffer) {
            GeometryBuffer.GeometrySequence geometrySequence = type == Facet.Type.TRIANGLE
                                                                     ? GeometryBuffer.GeometrySequence.TRIANGLE_STRIP
                                                                     : GeometryBuffer.GeometrySequence.QUAD_STRIP;
            for (int[] strip : strips) {
                buffer.start(geometrySequence);
                for (int i : strip) {
                    buffer.normal(normals[i]);
                    buffer.vertex(points[i]);
                }
                buffer.end();
            }
        }
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        GeodesicSphere sphere = new GeodesicSphere(3);
        new SingleShapeDemo(sphere, offWhite, SingleShapeDemo.Mode.SHADED).start();

    }
}
