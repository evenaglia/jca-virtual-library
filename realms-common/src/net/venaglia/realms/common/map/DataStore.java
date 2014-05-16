package net.venaglia.realms.common.map;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.map.data.CommonDataSources;
import net.venaglia.realms.common.util.Visitor;

import java.util.UUID;

/**
 * User: ed
 * Date: 3/20/14
 * Time: 5:19 PM
 */
public interface DataStore {

    void init();

    UUID getInstanceUuid();

    CommonDataSources getCommonDataSources();

    void intersect(BoundingVolume<?> bounds, Visitor<Cube> visitor);
}
