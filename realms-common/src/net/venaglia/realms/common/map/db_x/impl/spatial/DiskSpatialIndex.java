package net.venaglia.realms.common.map.db_x.impl.spatial;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.impl.AbstractSpatialMap;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.db_x.DB;
import net.venaglia.realms.common.map.db_x.DatabaseOptions;
import net.venaglia.realms.common.map.db_x.NotWritableException;
import net.venaglia.realms.spec.GeoSpec;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 7/31/13
 * Time: 8:36 AM
 */
public class DiskSpatialIndex<V> extends AbstractSpatialMap<V> implements Flushable, Closeable {

    /*
     * A disk based spatial index is similar to a file system. The most
     * significant feature is that the actual serialized object data is not
     * stored in the file, but rather only the keys of that data. Actual data
     * storage is delegated to a DB implementation.
     *
     * The file header consists of a banner followed by a 0x1A character, then
     * padded to the next multiple of 64 bytes.
     *
     * The remainder of the file is divided into 64 bytes blocks. The first
     * block (node index 0) contains the maximum bounds of this spatial index.
     * The second block (node index 1) contains the root voxel. The third block
     * (node index 2) is the initial block used to identify any unused blocks.
     *
     * All other blocks in the file are allocated and referenced dynamically.
     */

    protected final String filename;
    protected final String banner;
    protected final int offset;
    protected final File file;
    protected final boolean readonly;
    protected final DB<V> db;
    protected final IO<V> io;
    protected final AtomicInteger lastIndex = new AtomicInteger();
    protected final Map<Integer,Node> dirtyNodes;

    private final int maxConcurrentOps = 1;
    private final BlockingDeque<ByteBuffer> buffers = new LinkedBlockingDeque<ByteBuffer>(maxConcurrentOps);

    protected RandomAccessFile raf;
    protected FileChannel channel;
    protected BoundaryNode boundary;
    protected RootNode<V> root;
    protected FreeIndexBuffer freeIndexBuffer;

    public DiskSpatialIndex(Class<V> valueType, DB<V> db) {
        Map<String,Object> opts = buildOptions(valueType);
        this.filename = (String)opts.get("filename");
        String banner = (String)opts.get("banner");
        byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
        this.banner = banner;
        this.offset = align(bannerBytes.length + 1) + Node.SIZE;
        this.file = buildFile(this.filename);
        this.readonly = db.isReadOnly();
        this.db = db;
        this.io = new IOImpl();
        this.dirtyNodes = readonly ? null : new LinkedHashMap<Integer, Node>();
    }

    private static Map<String,Object> buildOptions(Class<?> type) {
        Map<String,Object> opts = new HashMap<String,Object>();
        DatabaseOptions hints = type.getAnnotation(DatabaseOptions.class);
        opts.put("filename", type.getSimpleName());
        opts.put("banner", String.format("All %ss", type.getSimpleName()));
        if (hints != null) {
            putIfNotEmpty(opts, "banner", hints.banner());
            putIfNotEmpty(opts, "filename", hints.filename());
        }
        return opts;
    }

    private static void putIfNotEmpty(Map<String,Object> opts, String key, String value) {
        if (value != null && value.length() != 0) {
            opts.put(key, value);
        }
    }

    static int align(int length) {
        int slack = length % Node.SIZE;
        return slack == 0 ? length : length + (Node.SIZE - slack);
    }

    static File buildFile(String name) {
        GeoSpec.getGeoIdentity(); // force GeoSpec to initialize
        File dir = new File("db", Configuration.DATABASE_DIRECTORY.getString());
        return new File(dir, name + ".3dex");
    }

    public static <V> void createEmptyFile(Class<V> valueType, BoundingVolume<?> bounds) throws IOException {
        Map<String,Object> opts = buildOptions(valueType);
        String filename = (String)opts.get("filename");
        String banner = (String)opts.get("banner");
        byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
        int offset = align(bannerBytes.length + 1) + Node.SIZE;
        File file = buildFile(filename);
        if (file.exists()) {
            throw new IOException("Refusing to overwrite an existing file with an empty one: " + file);
        }
        int l = offset + Node.SIZE * 3; // file offset + 3 blocks (boundary + root + free)
        byte[] data = new byte[l];
        wrap(data, 0, offset).put(bannerBytes).put((byte)26);

        BoundaryNode boundary = new BoundaryNode<V>(bounds);
        boundary.writeTo(wrap(data, offset, Node.SIZE));
        RootNode<V> root = new RootNode<V>(boundary, null, null);
        root.loadWithInitialData();
        root.writeTo(wrap(data, offset + Node.SIZE, Node.SIZE));
        FreeNode<V> free = new FreeNode<V>(Node.ROOT_FREE_NODE_INDEX, root, null);
        free.loadWithInitialData();
        free.writeTo(wrap(data, offset + Node.SIZE * 2, Node.SIZE));

        OutputStream out = new FileOutputStream(file);
        out.write(data);
        out.close();
    }

    public void open() throws IOException {
        if (!this.file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        long len = file.length() - offset;
        if (len % Node.SIZE != 0) {
            throw new IOException("File size is not as expected, file is probably corrupt.");
        }
        long l = len / Node.SIZE;
        if (l > Integer.MAX_VALUE) {
            throw new IOException("Database is too big, it cannot contain more than " + Integer.MAX_VALUE + "entries");
        }
        lastIndex.set((int)l);
        if (raf != null) {
            return; // already open
        }
        for (int i = 0; i < maxConcurrentOps; i++) {
            ByteBuffer buffer = ByteBuffer.allocate(Node.SIZE);
            buffer.order(ByteOrder.nativeOrder());
            buffers.add(buffer);
        }
        raf = new RandomAccessFile(file, "r");
        channel = raf.getChannel();
        boundary = new BoundaryNode<V>(io);
        root = new RootNode<V>(boundary, io, db);
        FreeNode<V> free = new FreeNode<V>(Node.ROOT_FREE_NODE_INDEX, root, null);
        freeIndexBuffer = new FreeIndexBuffer<V>(root, free, io);
    }

    private static ByteBuffer wrap(byte[] data, int start, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, start, length);
        buffer.order(ByteOrder.nativeOrder());
        return buffer;
    }

    public void close() throws IOException {
        flush();
        raf.close();
        channel.close();
    }

    public void flush() throws IOException {
        //To change body of created methods use File | Settings | File Templates.
    }

    public int intersect(BoundingVolume<?> region, Consumer<V> consumer) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean contains(double x, double y, double z) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BoundingVolume<?> getBounds() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int size() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Iterator<SpatialMap.Entry<V>> iterator() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private class IOImpl extends IO<V> {

//        private

        @Override
        void read(Node node) {
            if (node == null) {
                throw new NullPointerException("node");
            }
            ByteBuffer buffer = null;
            try {
                buffer = buffers.take();
                buffer.reset();
                channel.read(buffer, position(node.getIndex()));
                buffer.flip();
                node.readFrom(buffer);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                returnBufferToPool(buffer);
            }
        }

        @Override
        boolean readonly() {
            return readonly;
        }

        @Override
        void write(Node node) {
            if (readonly) {
                throw new NotWritableException();
            }
            if (node == null) {
                throw new NullPointerException("node");
            }
            ByteBuffer buffer = null;
            try {
                buffer = buffers.take();
                buffer.reset();
                node.writeTo(buffer);
                buffer.flip();
                channel.write(buffer, position(node.getIndex()));
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                returnBufferToPool(buffer);
            }
        }

        private void returnBufferToPool(ByteBuffer buffer) {
            if (buffer != null) {
                try {
                    buffers.put(buffer);
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
            }
        }

        @Override
        net.venaglia.realms.common.map.db_x.impl.spatial.Entry<V> entryFor(ValueNode node, boolean highValue) {
            return new net.venaglia.realms.common.map.db_x.impl.spatial.Entry<V>(db, node, highValue);
        }

        @Override
        NullNode<V> appendNullNode() {
            Integer nodeIndex = lastIndex.getAndIncrement();
            return new NullNode<V>(nodeIndex, root);
        }

        @Override
        void queueDirty(Node<V> node) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        private long position(int nodeIndex) {
            long offset = ((long)nodeIndex) * Node.SIZE;
            return offset + DiskSpatialIndex.this.offset;
        }
    }
}
