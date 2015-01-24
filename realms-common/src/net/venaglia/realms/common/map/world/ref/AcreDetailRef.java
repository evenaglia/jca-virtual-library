package net.venaglia.realms.common.map.world.ref;

import net.venaglia.common.util.Identifiable;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.realms.common.map.BinaryStore;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.world.AcreDetail;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/29/14
 * Time: 5:48 PM
 */
public class AcreDetailRef extends AbstractCachingRef<AcreDetail> implements Identifiable {

    private final int acreId;
    private final BinaryStore binaryStore;

    public AcreDetailRef(int acreId, BinaryStore binaryStore) {
        this.acreId = acreId;
        this.binaryStore = binaryStore;
    }

    @Override
    protected AcreDetail getImpl() {
        BinaryResource resource = binaryStore.getBinaryResource(AcreDetail.MIMETYPE, acreId);
        if (resource == null) {
            return null;
        }
        return AcreDetail.DEFINITION.getSerializer().deserialize(ByteBuffer.wrap(resource.getData()));
    }

    public Long getId() {
        return (long)acreId;
    }
}
