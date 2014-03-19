package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.realms.common.map.db_x.DB;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 8/2/13
* Time: 9:08 PM
* To change this template use File | Settings | File Templates.
*/
class VoxelNode<V> extends AbstractNode<V> {

    protected final double x0;
    protected final double x_;
    protected final double x1;
    protected final double y0;
    protected final double y_;
    protected final double y1;
    protected final double z0;
    protected final double z_;
    protected final double z1;

    protected Map<Octant,Node> children;

    VoxelNode(double x0,
              double x1,
              double y0,
              double y1,
              double z0,
              double z1,
              IO<V> io,
              VoxelNode<V> parent,
              Integer nodeIndex) {
        super(nodeIndex, parent, io);
        this.x0 = x0;
        this.x_ = (x0 + x1) * 0.5;
        this.x1 = x1;
        this.y0 = y0;
        this.y_ = (y0 + y1) * 0.5;
        this.y1 = y1;
        this.z0 = z0;
        this.z_ = (z0 + z1) * 0.5;
        this.z1 = z1;
    }

    public EntryType getType() {
        return EntryType.VOXEL_NODE;
    }

    public Map<Octant,Node> getChildren() {
        ensureLoaded();
        return children;
    }

    public Node getChild(Octant octant) {
        ensureLoaded();
        return children.get(octant);
    }

    public void readFrom(ByteBuffer buffer) {
        EnumMap<Octant,Node> entries = new EnumMap<Octant,Node>(Octant.class);
        for (Octant octant : Octant.values()) {
            int entryIndex = buffer.getInt();
            EntryType type = EntryType.decode(buffer.get());
            Node node = createNode(octant, entryIndex, type);
            if (node != null) {
                entries.put(octant, node);
            }
        }
        children = entries;
    }

    public void writeTo(ByteBuffer buffer) {
        for (Octant octant : Octant.values()) {
            Node node = children.get(octant);
            if (node != null) {
                buffer.putInt(node.getIndex());
                buffer.put(node.getType().encode());
            } else {
                buffer.putInt(0);
                buffer.put(EntryType.NULL_NODE.encode());
            }
        }
    }

    private Node createNode(Octant octant, int entryIndex, EntryType type) {
        switch (type) {
            case ROOT_NODE:
                throw new IllegalArgumentException();
            case VOXEL_NODE:
                switch (octant) {
                    case LO_X__LO_Y__LO_Z:
                        return createDirectoryNodeImpl(x0, x_, y0, y_, z0, z_, entryIndex);
                    case LO_X__LO_Y__HI_Z:
                        return createDirectoryNodeImpl(x0, x_, y0, y_, z_, z1, entryIndex);
                    case LO_X__HI_Y__LO_Z:
                        return createDirectoryNodeImpl(x0, x_, y_, y1, z0, z_, entryIndex);
                    case LO_X__HI_Y__HI_Z:
                        return createDirectoryNodeImpl(x0, x_, y_, y1, z_, z1, entryIndex);
                    case HI_X__LO_Y__LO_Z:
                        return createDirectoryNodeImpl(x_, x1, y0, y_, z0, z_, entryIndex);
                    case HI_X__LO_Y__HI_Z:
                        return createDirectoryNodeImpl(x_, x1, y0, y_, z_, z1, entryIndex);
                    case HI_X__HI_Y__LO_Z:
                        return createDirectoryNodeImpl(x_, x1, y_, y1, z0, z_, entryIndex);
                    case HI_X__HI_Y__HI_Z:
                        return createDirectoryNodeImpl(x_, x1, y_, y1, z_, z1, entryIndex);
                }
                throw new IllegalArgumentException();
            case VALUE_NODE:
                return new ValueNode<V>(entryIndex, this, null);
            case FREE_NODE:
                throw new IllegalArgumentException();
        }
        return null;
    }

    private VoxelNode createDirectoryNodeImpl(double a0,
                                                  double a1,
                                                  double b0,
                                                  double b1,
                                                  double c0,
                                                  double c1,
                                                  Integer nodeIndex) {
        switch (getDepth() + 1) {
            case 1:
            case 2:
            case 3:
            case 5:
            case 8:
            case 13:
            case 21:
                return new VoxelNode2<V>(a0, a1, b0, b1, c0, c1, io, this, nodeIndex);
            default:
                return new VoxelNode<V>(a0, a1, b0, b1, c0, c1, io, this, nodeIndex);
        }
    }

    public int getDepth() {
        return parent.getDepth() + 1;
    }

    DB<V> getDB() {
        return parent.getDB();
    }

    FreeIndexBuffer<V> getFreeIndexBuffer() {
        return parent.getFreeIndexBuffer();
    }

    static byte[] getInitialData() {
        byte[] bytes = new byte[SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        new VoxelNode<Object>(0,0,0,0,0,0,null,null,null).writeTo(buffer);
        return bytes;
    }
}
