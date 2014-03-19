package net.venaglia.realms.common.map_x.db.impl.spatial;

import java.nio.ByteBuffer;

/**
* User: ed
* Date: 8/2/13
* Time: 9:07 PM
*/
interface Node<V> {

    int SIZE = 64; // 64 bytes
    int BOUNDARY_NODE_INDEX = 0;
    int ROOT_NODE_INDEX = 1;
    int ROOT_FREE_NODE_INDEX = 2;

    EntryType getType();

    VoxelNode<V> getParent();

    Integer getIndex();

    void readFrom(ByteBuffer buffer);

    void writeTo(ByteBuffer buffer);
}
