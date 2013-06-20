package net.venaglia.gloo.physical.geom.detail;

import net.venaglia.gloo.physical.bounds.Bounded;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.projection.Projectable;

/**
 * User: ed
 * Date: 4/25/13
 * Time: 8:09 AM
 */
public interface DynamicDetailSource<T extends Projectable & Bounded> {

    float getSizeFactor();

    T produceAt(DetailLevel detailLevel);

    Shape<?> getTarget();
}
