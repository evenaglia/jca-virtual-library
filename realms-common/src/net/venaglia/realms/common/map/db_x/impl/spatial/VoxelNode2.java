package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.realms.common.map.db_x.DB;

/**
 * User: ed
 * Date: 8/5/13
 * Time: 8:30 AM
 */
class VoxelNode2<V> extends VoxelNode<V> {

    private final int depth;
    private final DB<V> db;
    private final FreeIndexBuffer<V> freeIndexBuffer;

    VoxelNode2(double x0,
               double x1,
               double y0,
               double y1,
               double z0,
               double z1,
               IO<V> io,
               VoxelNode<V> parent,
               Integer nodeIndex) {
        super(x0, x1, y0, y1, z0, z1, io, parent, nodeIndex);
        this.depth = super.getDepth();
        this.db = super.getDB();
        this.freeIndexBuffer = super.getFreeIndexBuffer();
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    DB<V> getDB() {
        return db;
    }

    @Override
    FreeIndexBuffer<V> getFreeIndexBuffer() {
        return freeIndexBuffer;
    }
}
