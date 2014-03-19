package net.venaglia.realms.common.map_x.elements;

import net.venaglia.realms.common.map_x.WorldMap;
import net.venaglia.realms.common.map_x.db.IdProvider;

import java.io.Serializable;

/**
 * User: ed
 * Date: 2/25/13
 * Time: 5:39 PM
 */
public abstract class WorldElement implements Serializable {

    public static final IdProvider<WorldElement> ID_PROVIDER = new IdProvider<WorldElement>() {
        public int getId(WorldElement value) {
            return value.getId();
        }
    };

    protected int id;
    protected WorldMap worldMap;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setWorldMap(WorldMap worldMap) {
        this.worldMap = worldMap;
    }
}
