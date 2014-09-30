package net.venaglia.realms.common.map;

import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.data.binaries.BinaryType;

/**
 * User: ed
 * Date: 4/30/14
 * Time: 5:51 PM
 */
public interface BinaryStore {

    BinaryResource getBinaryResource(Long id);

    BinaryResource getBinaryResource(String mimetype, long locatorId);

    BinaryResource createBinaryResource(BinaryType type, long locatorId, byte[] data);

    BinaryResource updateBinaryResource(BinaryResource resource, long locatorId, byte[] data);

    void destroyBinaryResource(BinaryResource resource, long locatorId);

    void freeBinaryResource(BinaryResource resource);
}
