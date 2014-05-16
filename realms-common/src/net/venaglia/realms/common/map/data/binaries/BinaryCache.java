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

    public BinaryResource insert(BinaryResource resource) {
        return source.insert(resource);
    }

    public BinaryResource update(BinaryResource resource, byte[] data) {
        return source.update(resource, data);
    }

    public void delete(BinaryResource resource) {
        source.delete(resource);
    }

    public BinaryResource lookupByLocator(String mimetype, long locatorId) {
        Long id = source.lookupIdByLocator(mimetype, locatorId);
        return id == null ? null : get(id);
    }
}
