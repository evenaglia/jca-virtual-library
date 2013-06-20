package net.venaglia.realms.common.map.db;

import net.venaglia.common.util.Pair;
import net.venaglia.common.util.Tuple2;

import java.io.Serializable;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 5:49 PM
 */
public final class IndexEntry implements Comparable<IndexEntry>, Serializable, Tuple2<Integer,IndexEntry> {

    private final Integer key;
    private final long offset;
    private final int length;

    public IndexEntry(Integer key, long offset, int length) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        this.key = key;
        this.offset = offset;
        this.length = length;
    }

    public Integer getA() {
        return key;
    }

    public IndexEntry getB() {
        return this;
    }

    public Tuple2<IndexEntry,Integer> reverse() {
        return new Pair<IndexEntry,Integer>(this, key);
    }

    public Integer getKey() {
        return key;
    }

    public long getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

    public int compareTo(IndexEntry indexEntry) {
        return key.compareTo(indexEntry.getKey());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexEntry that = (IndexEntry)o;
        return key.equals(that.key);

    }

    @Override
    public int hashCode() {
        return key;
    }

    @Override
    public String toString() {
        return String.format("IndexEntry[%d:%d-%d;l=%d]", key, offset, offset + length, length);
    }
}
