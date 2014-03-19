package net.venaglia.realms.common.map_x.db.impl.spatial;

import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.realms.common.map_x.db.DB;

/**
* User: ed
* Date: 8/2/13
* Time: 9:08 PM
*/
class RootNode<V> extends VoxelNode<V> {

    private final DB<V> db;
    private final FreeIndexBuffer<V> freeIndexBuffer;

    RootNode(BoundaryNode boundary, IO<V> io, DB<V> db) {
        super(boundary.min(Axis.X), boundary.max(Axis.X),
              boundary.min(Axis.Y), boundary.max(Axis.Y),
              boundary.min(Axis.Z), boundary.max(Axis.Z),
              io, null, ROOT_NODE_INDEX);
        this.db = db;
        FreeNode<V> freeNode = new FreeNode<V>(ROOT_FREE_NODE_INDEX, this, null);
        this.freeIndexBuffer = new FreeIndexBuffer<V>(this, freeNode, io);
    }

    public EntryType getType() {
        return EntryType.ROOT_NODE;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    public DB<V> getDB() {
        return db;
    }

    @Override
    FreeIndexBuffer<V> getFreeIndexBuffer() {
        return freeIndexBuffer;
    }
}
