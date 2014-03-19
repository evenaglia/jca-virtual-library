package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.common.util.Ref;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.common.util.impl.SimpleRef;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.NoSuchElementException;

/**
* User: ed
* Date: 8/2/13
* Time: 9:19 PM
*/
class FreeNode<V> extends AbstractNode<V> {

    private int[] freeIndexes;
    private short used;
    private FreeNode<V> prevFreeNode;
    private Ref<FreeNode<V>> nextFreeNode;

    FreeNode(Integer myIndex, RootNode<V> root, FreeNode<V> prev) {
        super(myIndex, root);
        this.prevFreeNode = prev;
    }

    public EntryType getType() {
        return EntryType.FREE_NODE;
    }

    public void readFrom(ByteBuffer buffer) {
        freeIndexes = new int[15];
        final int nextFreeNodeIndex = buffer.asIntBuffer().get(freeIndexes).get();
        if (nextFreeNodeIndex != 0) {
            nextFreeNode = new AbstractCachingRef<FreeNode<V>>() {
                @Override
                protected FreeNode<V> getImpl() {
                    return new FreeNode<V>(nextFreeNodeIndex, (RootNode<V>)parent, FreeNode.this);
                }
            };
        }
        int used = 0;
        for (int i = 15; i >= 0; i--) {
            used = used << 1 | (freeIndexes[i] != 0 ? 1 : 0);
        }
        this.used = (short)(used & 0x7F);
    }

    public void writeTo(ByteBuffer buffer) {
        IntBuffer b = buffer.asIntBuffer();
        b.put(freeIndexes);
        b.put(nextFreeNode == null ? 0 : nextFreeNode.get().getIndex());
    }

    boolean hasNext() {
        ensureLoaded();
        return nextFreeNode != null;
    }

    FreeNode<V> getNext() {
        ensureLoaded();
        return nextFreeNode == null ? null : nextFreeNode.get();
    }

    void setNext(FreeNode<V> next) {
        io.ensureWritable();
        this.nextFreeNode = SimpleRef.create(next);
    }

    FreeNode<V> getPrev() {
        return prevFreeNode;
    }

    void setPrev(FreeNode<V> prev) {
        io.ensureWritable();
        this.prevFreeNode = prev;
    }

    boolean isEmpty() {
        return used == 0;
    }

    boolean isFull() {
        return used == 0x7FFF;
    }

    Integer take() {
        io.ensureWritable();
        int idx = whichBitIsSet(used);
        if (idx < 0) throw new NoSuchElementException();
        try {
            return freeIndexes[idx];
        } finally {
            freeIndexes[idx] = 0;
            used &= ~(1 << idx);
        }
    }

    void offer(Integer index) {
        io.ensureWritable();
        if (index == null || index == 0) {
            throw new IllegalArgumentException();
        }
        int idx = whichBitIsSet((short)(0x3FFF & ~used));
        if (idx < 0) throw new NoSuchElementException();
        freeIndexes[idx] = index;
        used |= (1 << idx);
    }

    private static int whichBitIsSet(short bits) {
        if (bits == 0) {
            return -1;
        }
        short comb = -1;
        for (int i = 0; i < 5;) {
            switch (bits & comb) {
                case 0x0001:
                    return 0;
                case 0x0002:
                    return 1;
                case 0x0004:
                    return 2;
                case 0x0008:
                    return 3;
                case 0x0010:
                    return 4;
                case 0x0020:
                    return 5;
                case 0x0040:
                    return 6;
                case 0x0080:
                    return 7;
                case 0x0100:
                    return 8;
                case 0x0200:
                    return 9;
                case 0x0400:
                    return 10;
                case 0x0800:
                    return 11;
                case 0x1000:
                    return 12;
                case 0x2000:
                    return 13;
                case 0x4000:
                    return 14;
                case -32768:
                    return 15;
                case 0x0000:
                    comb = (short)(~comb);
                    continue;
                default:
                    bits &= comb;
                    i++;
                    break;
            }
            switch (i) {
                case 1:
                    comb = 0x5555;
                    break;
                case 2:
                    comb = 0x3333;
                    break;
                case 3:
                    comb = 0x0F0F;
                    break;
                case 4:
                    comb = 0x00FF;
                    break;
            }
        }
        throw new IllegalStateException();
    }

    FreeNode<V> allocateNew(Integer index, RootNode<V> root) {
        FreeNode<V> freeNode = new FreeNode<V>(index, root, this);
        freeNode.loadWithInitialData();
        if (nextFreeNode != null) {
            FreeNode<V> next = nextFreeNode.get();
            next.setPrev(freeNode);
            freeNode.setNext(next);
        }
        setNext(freeNode);
        return freeNode;
    }

    static byte[] getInitialData() {
        byte[] bytes = new byte[SIZE];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.nativeOrder());
        new FreeNode<Object>(null,null,null).writeTo(buffer);
        return bytes;
    }

//    public static void main(String[] args) {
//        for (int i = 0; i < 65536; i++) {
//            int b = whichBitIsSet((short)i);
//            if (b < 0) {
//                if (i == 0) continue;
//                throw new AssertionFailure();
//            }
//            if ((i & (1 << b)) == 0) {
//                throw new AssertionFailure();
//            }
//        }
//    }
}
