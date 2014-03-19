package net.venaglia.realms.common.map_x.db.impl.spatial;

/**
 * User: ed
 * Date: 8/5/13
 * Time: 6:10 PM
 */
interface MutableDirty {

    boolean isDirty();

    void setDirty();

    void clearDirty();
}
