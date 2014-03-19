package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Axis;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * User: ed
 * Date: 8/8/13
 * Time: 11:09 PM
 */
class BoundaryNode<V> extends AbstractNode<V> {

    protected double x0;
    protected double x1;
    protected double y0;
    protected double y1;
    protected double z0;
    protected double z1;

    BoundaryNode(IO<V> io) {
        super(BOUNDARY_NODE_INDEX, null, io);
    }

    BoundaryNode(BoundingVolume<?> bounds) {
        this((IO<V>)null);
        super.loadWithInitialData();
        x0 = bounds.min(Axis.X);
        x1 = bounds.max(Axis.X);
        y0 = bounds.min(Axis.Y);
        y1 = bounds.max(Axis.Y);
        z0 = bounds.min(Axis.Z);
        z1 = bounds.max(Axis.Z);
    }

    public EntryType getType() {
        return EntryType.BOUNDARY_NODE;
    }

    public void readFrom(ByteBuffer buffer) {
        x0 = buffer.getDouble(); // 8 bytes read
        y0 = buffer.getDouble(); // 16 bytes read
        z0 = buffer.getDouble(); // 24 bytes read
        x1 = buffer.getDouble(); // 32 bytes read
        y1 = buffer.getDouble(); // 40 bytes read
        z1 = buffer.getDouble(); // 48 bytes read
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.putDouble(x0);
        buffer.putDouble(y0);
        buffer.putDouble(z0);
        buffer.putDouble(x1);
        buffer.putDouble(y1);
        buffer.putDouble(z1);
    }

    double min(Axis axis) {
        ensureLoaded();
        return axis.of(x0, y0, z0);
    }

    double max(Axis axis) {
        ensureLoaded();
        return axis.of(x1, y1, z1);
    }

    @Override
    protected void loadWithInitialData() {
        throw new UnsupportedOperationException();
    }

    static byte[] getInitialData() {
        BoundaryNode<Object> boundaryNode = new BoundaryNode<Object>((IO<Object>)null);
        boundaryNode.x0 = Double.NaN;
        boundaryNode.y0 = Double.NaN;
        boundaryNode.z0 = Double.NaN;
        boundaryNode.x1 = Double.NaN;
        boundaryNode.y1 = Double.NaN;
        boundaryNode.z1 = Double.NaN;
        byte[] bytes = new byte[SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        boundaryNode.writeTo(buffer);
        return bytes;
    }
}
