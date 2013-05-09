package net.venaglia.realms.common.physical.geom.base;

import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Point;

/**
 * User: ed
 * Date: 9/3/12
 * Time: 10:46 PM
 */
public abstract class AbstractQuadFacetedType<T extends AbstractQuadFacetedType<T>> extends AbstractFacetedShape<T> {

    protected AbstractQuadFacetedType(Point[] points) {
        super(points);
    }

    public final Facet.Type getFacetType() {
        return Facet.Type.QUAD;
    }
}
