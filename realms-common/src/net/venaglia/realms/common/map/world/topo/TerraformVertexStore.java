package net.venaglia.realms.common.map.world.topo;

import static net.venaglia.realms.common.map.world.topo.VertexBlock.BUFFER_SIZE;
import static net.venaglia.realms.common.map.world.topo.VertexBlock.DEFINITION;
import static net.venaglia.realms.common.map.world.topo.VertexBlock.VERTEX_COUNT;

 import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.BinaryStore;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.spec.GeoSpec;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * User: ed
 * Date: 6/11/14
 * Time: 8:35 AM
 *
 * VertexStore to be used when terraforming. This implementation stores all multi zone points, but no others
 */
public class TerraformVertexStore extends AbstractVertexStore {

    private final ConcurrentNavigableMap<Long,VertexBlock> allSharedBlocks = new ConcurrentSkipListMap<Long,VertexBlock>();
    private final BinaryStore binaryStore;

    public TerraformVertexStore(BinaryStore binaryStore) {
        this.binaryStore = binaryStore;
        int totalVertices = (int)GeoSpec.POINTS_SHARED_MANY_ZONE.get();
        int totalBlocks = (totalVertices + VERTEX_COUNT - 1) / VERTEX_COUNT;
        long blockId = 0L;
        VertexBlock.init();
        System.out.println("Reading vertex blocks...");
        for (int i = 0; i < totalBlocks; i++, blockId += VERTEX_COUNT) {
            BinaryResource binaryResource = binaryStore.getBinaryResource(VertexBlock.MIMETYPE, blockId);
            VertexBlock vertexBlock;
            if (binaryResource == null) {
                byte[] data = new byte[BUFFER_SIZE];
                vertexBlock = new VertexBlock(ByteBuffer.wrap(data));
                vertexBlock.setId(blockId);
            } else {
                vertexBlock = new VertexBlock(ByteBuffer.wrap(binaryResource.getData()));
                vertexBlock.setId(blockId);
                binaryStore.freeBinaryResource(binaryResource);
            }
            allSharedBlocks.put(vertexBlock.getId(), vertexBlock);
            if ((i & 1023) == 1023) {
                System.out.print(".");
            }
        }
        System.out.println();
    }

    @Override
    protected void preFetch(Set<Long> blockIds) {
        // no-op, everything is already loaded
    }

    @Override
    protected VertexBlock getBlock(Long id) {
        return allSharedBlocks.get(id);
    }

    public void flushChanges() {
        boolean readonly = Configuration.DATABASE_HARMLESS.getBoolean();
        byte[] buffer = new byte[BUFFER_SIZE];
        for (VertexBlock vertexBlock : allSharedBlocks.values()) {
            BinaryResource binaryResource = binaryStore.getBinaryResource(VertexBlock.MIMETYPE, vertexBlock.getId());
            boolean insert = binaryResource == null && !readonly;
            boolean update = binaryResource != null && !readonly;
            if (update) {
                binaryStore.freeBinaryResource(binaryResource);
            }
            vertexBlock.update(buffer);
            if (update) {
                binaryResource = binaryStore.updateBinaryResource(binaryResource, vertexBlock.getId(), buffer);
                binaryStore.freeBinaryResource(binaryResource);
            } else if (insert) {
                binaryResource = binaryStore.createBinaryResource(DEFINITION, vertexBlock.getId(), buffer);
                binaryStore.freeBinaryResource(binaryResource);
            }
        }
    }
}
