package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.decorators.Transformation;
import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.Faceted;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.common.util.Lock;
import net.venaglia.common.util.impl.ThreadSafeLock;

import java.util.Arrays;
import java.util.Comparator;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 8:42 AM
 *
 * Utility class that can take a faceted object with smooth surface normals and render each surface
 * as a true facet with one normal per facet.
 */
public abstract class FacetSequence implements Shape<FacetSequence>, Faceted {

    protected final Facet.Type type;
    protected final Facet[] facets;
    protected final Vector[] normals;
    protected final Lock lock = new ThreadSafeLock();

    protected Transformation transformation;
    protected Material material = Material.DEFAULT;
    protected BoundingVolume<?> boundingVolume;

    public FacetSequence(Faceted baseObject) {
        int l = baseObject.facetCount();
        facets = new Facet[l];
        normals = new Vector[l];
        Facet.Type type = null;
        for (int i = 0; i < l; i++) {
            Facet facet = baseObject.getFacet(i);
            if (type == null) {
                type = facet.type;
            } else if (type != Facet.Type.MIXED && type != facet.type) {
                type = Facet.Type.MIXED;
            }
            facets[i] = facet;
        }
        this.type = type;
        Arrays.sort(facets, new Comparator<Facet>() {
            public int compare(Facet a, Facet b) {
                return a.type.compareTo(b.type);
            }
        });
    }

    public int facetCount() {
        return facets.length;
    }

    public Facet.Type getFacetType() {
        return type;
    }

    public Facet getFacet(int index) {
        return facets[index];
    }

    protected BoundingVolume<?> createBoundingVolume() {
        return null;
    }

    public BoundingVolume<?> getBounds() {
        if (boundingVolume == null) {
            boundingVolume = createBoundingVolume();
        }
        return boundingVolume;
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        // todo
    }
}
