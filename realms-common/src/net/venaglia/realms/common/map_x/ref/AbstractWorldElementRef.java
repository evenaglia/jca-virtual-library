package net.venaglia.realms.common.map_x.ref;

import net.venaglia.realms.common.map_x.WorldMap;
import net.venaglia.realms.common.map_x.db.DB;
import net.venaglia.realms.common.map_x.elements.WorldElement;
import net.venaglia.common.util.impl.AbstractCachingRef;

/**
 * User: ed
 * Date: 2/26/13
 * Time: 11:13 PM
 */
public abstract class AbstractWorldElementRef<T extends WorldElement> extends AbstractCachingRef<T> {

    protected final int id;
    protected final WorldMap worldMap;

    protected AbstractWorldElementRef(int id, WorldMap worldMap) {
        this.id = id;
        this.worldMap = worldMap;
    }

    public T getImpl() {
        return getDB().get(id);
    }

    protected abstract DB<T> getDB();
}
