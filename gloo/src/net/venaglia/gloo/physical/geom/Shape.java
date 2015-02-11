package net.venaglia.gloo.physical.geom;

import net.venaglia.common.util.Named;
import net.venaglia.gloo.physical.bounds.Bounded;
import net.venaglia.common.util.Lock;
import net.venaglia.common.util.Series;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.projection.Transformable;

/**
 * User: ed
 * Date: 8/3/12
 * Time: 7:34 PM
 */
public interface Shape<T extends Shape<T>> extends Bounded, Transformable, Element<T>, Series<Point>, Projectable, Named {

    /**
     * @return The normal vector for the point at the specified index, or null
     *     if this shape does not represent a surface.
     */
    Vector getNormal(int index);

    Lock getLock();

    T setMaterial(Material material);

    Material getMaterial();
}
