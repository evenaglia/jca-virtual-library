package net.venaglia.gloo.physical.geom.base;

import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.Point;

/**
 * User: ed
 * Date: 9/3/12
 * Time: 10:46 PM
 */
public abstract class AbstractPolygonFacetedType<T extends AbstractPolygonFacetedType<T>> extends AbstractFacetedShape<T> {

    protected AbstractPolygonFacetedType(Point[] points) {
        super(points);
    }

    public final Facet.Type getFacetType() {
        return Facet.Type.POLY;
    }
}
