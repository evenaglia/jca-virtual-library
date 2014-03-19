package net.venaglia.realms.common.map_x.db.impl;

import net.venaglia.realms.common.map_x.db.DBException;
import net.venaglia.realms.common.map_x.db.DuplicateKeyException;
import net.venaglia.realms.common.map_x.db.IndexEntry;
import net.venaglia.realms.common.map_x.db.KeyNotFoundException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 11:12 PM
 */
public class ReadWriteDiskIndex extends AbstractDiskIndex {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private NavigableMap<Integer,IndexEntry> index = new TreeMap<Integer,IndexEntry>();
    private boolean uncommittedChanges = false;

    public ReadWriteDiskIndex(String banner, String name) throws IOException {
        super(banner, name, true);
    }

    public boolean hasUncommittedChanges() {
        return uncommittedChanges;
    }

    public IndexEntry add(Integer id, long offset, int length) {
        IndexEntry indexEntry;
        lock.writeLock().lock();
        try {
            if (index.containsKey(id)) {
                throw new DuplicateKeyException();
            }
            indexEntry = new IndexEntry(id, offset, length);
            index.put(id, indexEntry);
            uncommittedChanges = true;
        } finally {
            lock.writeLock().unlock();
        }
        return indexEntry;
    }

    public IndexEntry update(IndexEntry entry, long offset, int length) {
        Integer key = entry.getKey();
        IndexEntry newEntry;
        lock.writeLock().lock();
        try {
            if (!index.containsKey(key)) {
                throw new KeyNotFoundException();
            }
            newEntry = new IndexEntry(key, offset, length);
            index.put(key, newEntry);
            uncommittedChanges = true;
        } finally {
            lock.writeLock().unlock();
        }
        return newEntry;
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            index.clear();
            uncommittedChanges = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void commitChanges() {
        lock.writeLock().lock();
        try {
            Collection<IndexEntry> entries = index.values();
            raf.seek(0L);
            raf.write(banner.getBytes(Charset.forName("ISO-8859-1")));
            raf.write('\u001A');
            ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
            int lastKey = Integer.MIN_VALUE;
            for (IndexEntry indexEntry : entries) {
                buffer.clear();
                assert indexEntry.getKey() > lastKey;
                lastKey = indexEntry.getKey();
                buffer.putInt(lastKey);
                buffer.putLong(indexEntry.getOffset());
                buffer.putInt(indexEntry.getLength());
                raf.write(buffer.array());
            }
            size = entries.size();
            raf.setLength(offset + ((long)size) * RECORD_SIZE);
        } catch (IOException e) {
            throw new DBException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected IndexEntry readEntry(int i) {
        if (i < 0 || i >= size) {
            throw new ArrayIndexOutOfBoundsException(i);
        }
        lock.readLock().lock();
        try {
            return index.get(i);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Iterator<IndexEntry> iterator() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableCollection(index.values()).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    public IndexEntry get(Integer key) {
        lock.readLock().lock();
        try {
            return index.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int size() {
        lock.readLock().lock();
        try {
            return index.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
