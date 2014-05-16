package net.venaglia.realms.common.map;

import net.venaglia.common.util.Factory;

/**
 * User: ed
 * Date: 3/29/14
 * Time: 11:11 AM
 */
public interface Source<OBJ> extends Factory<OBJ> {

    void populate(Long id, OBJ obj);
}
