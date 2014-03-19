package net.venaglia.realms.common.map.db_x.impl.spatial;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 8/7/13
 * Time: 5:45 PM
 */
class NullNode<V> extends AbstractNode<V> {

    public NullNode(Integer nodeIndex, RootNode<V> parent) {
        super(nodeIndex, parent);
    }

    public EntryType getType() {
        return EntryType.NULL_NODE;
    }

    public void readFrom(ByteBuffer buffer) {
        // no-op
    }

    public void writeTo(ByteBuffer buffer) {
        // no-op
    }

    static byte[] getInitialData() {
        return new byte[SIZE];
    }
}
