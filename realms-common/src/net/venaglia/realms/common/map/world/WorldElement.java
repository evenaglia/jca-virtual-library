package net.venaglia.realms.common.map.world;

import net.venaglia.realms.common.map.things.annotations.Property;

import java.io.Serializable;

/**
 * User: ed
 * Date: 2/25/13
 * Time: 5:39 PM
 */
public abstract class WorldElement implements Serializable {

    @Property
    protected int id;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
