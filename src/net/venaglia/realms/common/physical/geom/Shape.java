package net.venaglia.realms.common.physical.geom;

import net.venaglia.realms.common.physical.bounds.Bounded;
import net.venaglia.realms.common.util.Lock;
import net.venaglia.realms.common.util.Series;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.decorators.Transformation;
import net.venaglia.realms.common.projection.Projectable;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:34 PM
 */
public interface Shape<T extends Shape<T>> extends Bounded, Element<T>, Series<Point>, Projectable {

    /**
     * @return The normal vector for the point at the specified index, or null
     *     if this shape does not represent a surface.
     */
    Vector getNormal(int index);

    /**
     * @return A Transformation object that may be used to apply
     *     transformations to this shape.
     */
    Transformation getTransformation();

    Lock getLock();

    T setMaterial(Material material);

    Material getMaterial();
}
