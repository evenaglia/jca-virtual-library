package net.venaglia.realms.common.map.data.binaries;

import net.venaglia.realms.common.map.Source;

/**
 * User: ed
 * Date: 4/14/14
 * Time: 5:45 PM
 */
public interface BinarySource extends Source<BinaryResource> {

    BinaryResource insert (BinaryResource resource, long locatorId);

    BinaryResource update(BinaryResource resource, long locatorId, byte[] data);

    void delete(BinaryResource resource, long locatorId);

    Long lookupIdByLocator(String mimetype, long locatorId);
}
