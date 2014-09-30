package net.venaglia.realms.common.map.data.binaries;

import net.venaglia.realms.common.util.cache.BasicWorkingCache;

/**
 * User: ed
 * Date: 4/11/14
 * Time: 5:04 PM
 */
public class BinaryCache extends BasicWorkingCache<BinaryResource> {

    private final BinarySource source;

    public BinaryCache(BinarySource source) {
        this.source = source;
    }

    @Override
    protected BinaryResource miss(Long id) {
        BinaryResource resource = source.createEmpty();
        source.populate(id, resource);
        return resource;
    }

    public BinaryResource insert(BinaryResource resource, long locatorId) {
        // todo: the returned object needs to be added to the cache.
        return source.insert(resource, locatorId);
    }

    public BinaryResource update(BinaryResource resource, long locatorId, byte[] data) {
        // todo: the returned object is typically not the same object passed in, so update the cache entry.
        return source.update(resource, locatorId, data);
    }

    public void delete(BinaryResource resource, long locatorId) {
        source.delete(resource, locatorId);
    }

    public BinaryResource lookupByLocator(String mimetype, long locatorId) {
        Long id = source.lookupIdByLocator(mimetype, locatorId);
        return id == null ? null : get(id);
    }
}
