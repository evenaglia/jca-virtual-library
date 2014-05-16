package net.venaglia.realms.common.map.data;

import net.venaglia.realms.common.map.UniqueIdSource;

/**
 * User: ed
 * Date: 4/15/14
 * Time: 9:04 AM
 */
public interface IdSourceProvider {
    UniqueIdSource getIdSource(Sequence sequence);
}
