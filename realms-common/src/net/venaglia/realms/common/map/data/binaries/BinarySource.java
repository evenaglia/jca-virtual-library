package net.venaglia.realms.common.map.data.binaries;

import net.venaglia.realms.common.map.Source;

/**
 * User: ed
 * Date: 4/14/14
 * Time: 5:45 PM
 */
public interface BinarySource extends Source<BinaryResource> {

    BinaryResource insert (BinaryResource resource);

    BinaryResource update(BinaryResource resource, byte[] data);

    void delete(BinaryResource resource);

    Long lookupIdByLocator(String mimetype, long locatorId);
}
