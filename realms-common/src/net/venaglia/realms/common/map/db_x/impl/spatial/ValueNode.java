package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.common.util.Ref;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.common.util.impl.SimpleRef;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
* Created with IntelliJ IDEA.
* User: ed
* Date: 8/2/13
* Time: 9:11 PM
* To change this template use File | Settings | File Templates.
*/
class ValueNode<V> extends AbstractNode<V> {

    protected double x1 = Double.NaN;
    protected double y1 = Double.NaN;
    protected double z1 = Double.NaN;
    protected Integer index1 = Integer.MIN_VALUE;
    protected Ref<Entry<V>> entry1;

    protected double x2 = Double.NaN;
    protected double y2 = Double.NaN;
    protected double z2 = Double.NaN;
    protected Integer index2 = Integer.MIN_VALUE;
    protected Ref<Entry<V>> entry2;

    protected ValueNode<V> prevValueNode;
    protected Ref<ValueNode<V>> nextValueNode;

    ValueNode(Integer nodeIndex, VoxelNode<V> parent, ValueNode<V> prev) {
        super(nodeIndex, parent);
        this.prevValueNode = prev;
    }

    private ValueNode() {
        super(null, null, null);
    }

    public EntryType getType() {
        return EntryType.VALUE_NODE;
    }

    public void readFrom(ByteBuffer buffer) {
        index1 = buffer.getInt();                       // 4 bytes read
        x1 = buffer.getDouble();                        // 12 bytes read
        y1 = buffer.getDouble();                        // 20 bytes read
        z1 = buffer.getDouble();                        // 28 bytes read
        index2 = buffer.getInt();                       // 32 bytes read
        x2 = buffer.getDouble();                        // 40 bytes read
        y2 = buffer.getDouble();                        // 48 bytes read
        z2 = buffer.getDouble();                        // 56 bytes read
        final int nextValueNodeIndex = buffer.getInt(); // 60 bytes read
        if (isNull(x1, y1, z1, index1)) {
            entry1 = null;
        } else {
            entry1 = new AbstractCachingRef<Entry<V>>() {
                @Override
                protected Entry<V> getImpl() {
                    return io.entryFor(ValueNode.this, false);
                }
            };
        }
        if (isNull(x2, y2, z2, index2)) {
            entry2 = null;
        } else {
            entry2 = new AbstractCachingRef<Entry<V>>() {
                @Override
                protected Entry<V> getImpl() {
                    return io.entryFor(ValueNode.this, true);
                }
            };
        }
        if (nextValueNodeIndex != 0) {
            nextValueNode = new AbstractCachingRef<ValueNode<V>>() {
                @Override
                protected ValueNode<V> getImpl() {
                    return new ValueNode<V>(nextValueNodeIndex, parent, ValueNode.this);
                }
            };
        }
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.putInt(index1).putDouble(x1).putDouble(y1).putDouble(z1);
        buffer.putInt(index2).putDouble(x2).putDouble(y2).putDouble(z2);
        if (nextValueNode != null) {
            buffer.putInt(nextValueNode.get().getIndex());
        } else {
            buffer.putInt(0);
        }
    }

    private boolean isNull(double x, double y, double z, Integer index) {
        return Double.isNaN(x) && Double.isNaN(y) && Double.isNaN(z) && index == Integer.MIN_VALUE;
    }

    boolean hasEntry1() {
        ensureLoaded();
        return entry1 != null;
    }

    Entry<V> getEntry1() {
        ensureLoaded();
        return entry1 == null ? null : entry1.get();
    }

    void setEntry1(Entry<V> entry) {
        ensureLoaded();
        dirty.setDirty();
        entry1 = entry == null ? null : new SimpleRef<Entry<V>>(entry);
    }

    boolean hasEntry2() {
        ensureLoaded();
        return entry2 != null;
    }

    Entry<V> getEntry2() {
        ensureLoaded();
        return entry2 == null ? null : entry2.get();
    }

    void setEntry2(Entry<V> entry) {
        ensureLoaded();
        dirty.setDirty();
        entry2 = entry == null ? null : new SimpleRef<Entry<V>>(entry);
    }

    boolean hasNext() {
        ensureLoaded();
        return nextValueNode != null;
    }

    ValueNode<V> getNext() {
        ensureLoaded();
        return nextValueNode != null ? nextValueNode.get() : null;
    }

    void setNext(ValueNode<V> next) {
        io.ensureWritable();
        dirty.setDirty();
        nextValueNode = SimpleRef.create(next);
    }

    ValueNode<V> getPrev() {
        return prevValueNode;
    }

    void setPrev(ValueNode<V> prev) {
        io.ensureWritable();
        dirty.setDirty();
        this.prevValueNode = prev;
    }

    static byte[] getInitialData() {
        byte[] bytes = new byte[SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        new ValueNode().writeTo(buffer);
        return bytes;
    }
}
