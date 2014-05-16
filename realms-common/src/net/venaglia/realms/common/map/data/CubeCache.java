package net.venaglia.realms.common.map.data;

import net.venaglia.realms.common.map.Source;
import net.venaglia.realms.common.util.cache.BasicWorkingCache;

/**
 * User: ed
 * Date: 3/29/14
 * Time: 7:35 AM
 */
public class CubeCache extends BasicWorkingCache<CubeImpl> {

    protected final Source<CubeImpl> source;

    public CubeCache(Source<CubeImpl> source) {
        this.source = source;
    }

    @Override
    protected CubeImpl miss(Long id) {
        CubeImpl cube = source.createEmpty();
        source.populate(id, cube);
        return cube;
    }
}
