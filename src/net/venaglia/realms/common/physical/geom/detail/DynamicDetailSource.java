package net.venaglia.realms.common.physical.geom.detail;

import net.venaglia.realms.common.physical.bounds.Bounded;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.projection.Projectable;

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
