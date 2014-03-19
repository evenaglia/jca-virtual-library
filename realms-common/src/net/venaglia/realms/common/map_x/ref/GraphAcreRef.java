package net.venaglia.realms.common.map_x.ref;

import net.venaglia.realms.common.map_x.WorldMap;
import net.venaglia.realms.common.map_x.db.DB;
import net.venaglia.realms.common.map_x.elements.GraphAcre;

/**
 * User: ed
 * Date: 1/28/13
 * Time: 8:47 AM
 */
public class GraphAcreRef extends AbstractWorldElementRef<GraphAcre> {

    public GraphAcreRef(int acreId, WorldMap worldMap) {
        super(acreId, worldMap);
    }

    @Override
    protected DB<GraphAcre> getDB() {
        return worldMap.graph;
    }
}
