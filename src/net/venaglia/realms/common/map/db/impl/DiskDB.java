package net.venaglia.realms.common.map.db.impl;

import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.db.DB;
import net.venaglia.realms.common.map.db.DBException;
import net.venaglia.realms.common.map.db.DatabaseOptions;
import net.venaglia.realms.common.map.db.DuplicateKeyException;
import net.venaglia.realms.common.map.db.Index;
import net.venaglia.realms.common.map.db.IndexEntry;
import net.venaglia.realms.common.map.db.NotWritableException;
import net.venaglia.realms.common.map.db.Serializer;
import net.venaglia.realms.common.util.Pair;
import net.venaglia.realms.common.util.Tuple2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 7:46 AM
 */
public class DiskDB<V> implements DB<V> {

    protected final Class<V> valueType;
    protected final String filename;
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final String banner;
    protected final int offset;
    protected final Mode mode;
    protected final byte[] padding;
    protected final int allignment;
    protected final File file;
    protected final Serializer<V> serializer;

    protected AbstractDiskIndex index;
    protected RandomAccessFile raf;
    protected FileChannel channel;

    public DiskDB(Class<V> valueType, Mode mode, Serializer<V> serializer) {
        this.valueType = valueType;
        Map<String,Object> opts = buildOptions(valueType);
        this.filename = (String)opts.get("filename");
        String banner = (String)opts.get("banner");
        byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
        this.allignment = (Integer)opts.get("alignment");
        this.offset = align(bannerBytes.length + 1);
        this.banner = banner;
        this.mode = mode;
        if (mode.isReadOnly()) {
            this.padding = null;
        } else {
            this.padding = new byte[(Integer)opts.get("reservedSize")];
            Arrays.fill(padding, (byte)0);
        }
        this.file = buildFile(filename);
        this.serializer = serializer;
    }

    public void open() throws IOException {
        if (!this.file.exists() && !mode.isReadOnly()) {
            createEmptyDBFile();
        }
        this.index = mode.isAppendable()
                     ? new ReadWriteDiskIndex(banner, filename)
                     : new ReadOnlyDiskIndex(banner, filename);
        this.raf = new RandomAccessFile(file, mode.fileMode());
        this.channel = raf.getChannel();
    }

    public void close() throws IOException {
        raf.close();
        channel.close();
        index.close();
    }

    private Map<String,Object> buildOptions(Class<V> type) {
        Map<String,Object> opts = new HashMap<String,Object>();
        DatabaseOptions hints = type.getAnnotation(DatabaseOptions.class);
        opts.put("filename", type.getSimpleName());
        opts.put("banner", String.format("All %ss", type.getSimpleName()));
        if (hints != null) {
            putIfNotEmpty(opts, "banner", hints.banner());
            putIfNotEmpty(opts, "filename", hints.filename());
            opts.put("reservedSize", Math.max(hints.alignment(), hints.reservedSize()));
            opts.put("alignment", hints.alignment());
        } else {
            opts.put("reservedSize", 64);
            opts.put("alignment", 64);
        }
        return opts;
    }

    private void putIfNotEmpty(Map<String,Object> opts, String key, String value) {
        if (value != null && value.length() != 0) {
            opts.put(key, value);
        }
    }

    private void createEmptyDBFile() throws IOException {
        byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
        File dir = this.file.getParentFile();
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new DBException("Unable to create directory: " + dir);
            }
        }
        FileOutputStream out = new FileOutputStream(this.file);
        try {
            byte[] b = new byte[this.offset];
            Arrays.fill(b, (byte)0);
            System.arraycopy(bannerBytes, 0, b, 0, bannerBytes.length);
            b[bannerBytes.length] = 26;
            out.write(b);
        } finally {
            out.close();
        }
    }

    protected V read(long off, int len) throws IOException {
        lock.readLock().lock();
        try {
            raf.seek(off);
            ByteBuffer byteBuffer = view(off + Long.SIZE, len);
            return serializer.deserialize(byteBuffer);
        } finally {
            lock.readLock().unlock();
        }
    }

    public File getFile() {
        return file;
    }

    public int size() {
        return index.size();
    }

    public Iterator<Tuple2<Integer,V>> iterator() {
        final Iterator<IndexEntry> indexIterator = index.iterator();
        return new Iterator<Tuple2<Integer, V>>() {
            public boolean hasNext() {
                return indexIterator.hasNext();
            }

            public Tuple2<Integer,V> next() {
                IndexEntry indexEntry = indexIterator.next();
                try {
                    V value = read(indexEntry.getOffset(), indexEntry.getLength());
                    return new Pair<Integer,V>(indexEntry.getKey(), value);
                } catch (IOException e) {
                    throw new DBException(e);
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean hasUncommittedChanges() {
        return index.hasUncommittedChanges();
    }

    public void commitChanges() {
        index.commitChanges();
    }

    public boolean contains(Integer key) {
        return index.contains(key);
    }

    public V get(Integer key) {
        IndexEntry indexEntry = index.get(key);
        if (indexEntry == null) {
            return null;
        } else {
            try {
                return read(indexEntry.getOffset(), indexEntry.getLength());
            } catch (IOException e) {
                throw new DBException(e);
            }
        }
    }

    public void put(Integer key, V value) {
        if (!mode.isAppendable()) {
            throw new NotWritableException();
        }
        IndexEntry indexEntry = index.get(key);
        if (indexEntry == null && !mode.isAppendable()) {
            throw new DuplicateKeyException();
        }
        lock.writeLock().lock();
        try {
            byte[] bytes = serializer.serialize(value);
            int length = align(Math.max(bytes.length, padding.length));
            long offset;
            if (indexEntry == null) {
                index.add(key, length, bytes.length);
                offset = raf.length();
            } else if (indexEntry.getLength() < length) {
                throw new DBException(String.format("Cannot replace entry for %d: %db required, %db available",
                                                    key,
                                                    length,
                                                    indexEntry.getLength()));
            } else {
                offset = indexEntry.getOffset();
            }
            assert padding.length >= bytes.length;
            writeImpl(bytes, offset, length);
        } catch (IOException e) {
            throw new DBException(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        if (mode == Mode.FULLY_MUTABLE) {
            try {
                this.raf.close();
                createEmptyDBFile();
                this.raf = new RandomAccessFile(file, mode.fileMode());
                this.channel = raf.getChannel();
            } catch (IOException e) {
                throw new DBException(e);
            }
        } else {
            throw new NotWritableException();
        }
    }

    private int align(int length) {
        int slack = length % allignment;
        return slack == 0 ? length : length + (allignment - slack);
    }

    private void writeImpl(byte[] bytes, long offset, int length) throws IOException {
        raf.seek(offset);
        raf.write(bytes);
        int bytesWritten = bytes.length;
        while (length > bytesWritten) {
            int toWrite = Math.min(padding.length, length - bytesWritten);
            raf.write(padding, 0, toWrite);
            bytesWritten += toWrite;
        }
    }

    protected ByteBuffer view(long off, long len) throws IOException {
        FileChannel.MapMode mode = this.mode.isReadOnly()
                                   ? FileChannel.MapMode.READ_ONLY
                                   : FileChannel.MapMode.READ_WRITE;
        return channel.map(mode, off, len);
    }

    public boolean isReadOnly() {
        return mode.isReadOnly();
    }

    @Override
    public String toString() {
        return "DiskDB<" + valueType.getSimpleName() +">[" + index.size() + "]";
    }

    protected static File buildFile(String name) {
        File dir = new File("db", Configuration.DATABASE_DIRECTORY.getString());
        return new File(dir, name + ".data");
    }
}
