package net.venaglia.realms.common.map.db_x;

import net.venaglia.gloo.util.SpatialMap;

/**
 * User: ed
 * Date: 3/3/13
 * Time: 11:08 AM
 */
public interface SpatialIndex<E> extends SpatialMap<E> {

    boolean isReadOnly();

    boolean hasUncommittedChanges();

    void commitChanges();

}
