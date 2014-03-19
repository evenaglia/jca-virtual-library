package net.venaglia.realms.common.map_x.db.impl;

import net.venaglia.realms.common.map_x.db.IndexEntry;
import net.venaglia.realms.common.map_x.db.NotWritableException;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 7:42 AM
 */
public class ReadOnlyDiskIndex extends AbstractDiskIndex {

    protected ReadOnlyDiskIndex(String banner, String name)
            throws IOException {
        super(banner, name,  false);
    }

    public boolean hasUncommittedChanges() {
        return false;
    }

    public IndexEntry add(Integer id, long offset, int length) {
        throw new NotWritableException("Write not supported, index is read-only");
    }

    public IndexEntry update(IndexEntry entry, long offset, int length) {
        throw new NotWritableException("Write not supported, index is read-only");
    }

    public void clear() {
        throw new NotWritableException("Clear not supported, index is read-only");
    }

    public void commitChanges() {
        // no-op
    }

    protected IndexEntry readEntry(int i) {
        if (i < 0 || i >= size) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        long index = ((long)i) * RECORD_SIZE + offset;
        try {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, index, RECORD_SIZE);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            return new IndexEntry(buffer.getInt(), buffer.getLong(), buffer.getInt());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Iterator<IndexEntry> iterator() {
        return new Iterator<IndexEntry>() {

            private int index = 0;

            public boolean hasNext() {
                return index < size;
            }

            public IndexEntry next() {
                if (index >= size) {
                    throw new NoSuchElementException();
                }
                return readEntry(index++);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    protected IndexEntry binarySearch(int find, int low, int high) {
        int mid;
        while (low <= high) {
            mid = (low + high) / 2;
            IndexEntry indexEntry = readEntry(mid);
            int test = indexEntry.getKey();
            if (test < find) {
                low = mid + 1;
            } else if (test > find) {
                high = mid - 1;
            } else {
                if (high == mid) return indexEntry;
                high = mid;
            }
        }
        return null; // not found
    }

    public IndexEntry get(Integer key) {
        return key == null ? null : binarySearch(key, 0, size - 1);
    }

    public int size() {
        return size;
    }
}
