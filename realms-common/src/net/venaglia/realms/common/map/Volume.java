package net.venaglia.realms.common.map;

import net.venaglia.gloo.physical.bounds.BoundingVolume;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 7:30 PM
 */
public interface Volume {

    BoundingVolume<?> getBounds();
}
