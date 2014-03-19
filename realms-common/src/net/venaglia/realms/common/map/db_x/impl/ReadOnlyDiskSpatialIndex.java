package net.venaglia.realms.common.map.db_x.impl;

import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.db_x.DB;
import net.venaglia.realms.common.map.db_x.DBException;
import net.venaglia.realms.common.map.db_x.DatabaseOptions;
import net.venaglia.realms.common.map.db_x.IdProvider;
import net.venaglia.realms.common.map.db_x.SpatialIndex;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.OctreeVoxel;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.Series;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.gloo.util.impl.AbstractSpatialMap;
import net.venaglia.gloo.util.impl.OctreeMap;
import net.venaglia.realms.spec.GeoSpec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 3/3/13
 * Time: 11:07 AM
 */
public class ReadOnlyDiskSpatialIndex<V> extends AbstractSpatialMap<V> implements SpatialIndex<V> {

    private static final int ROOT_NODE_SIZE = (Double.SIZE >> 3) * 6 + (Integer.SIZE >> 3) + Node.SIZE;

    protected final String filename;
    protected final String banner;
    protected final int offset;
    protected final File file;
    protected final DB<V> db;

    protected RandomAccessFile raf;
    protected FileChannel channel;
    protected Node<V> root;
    protected int size;

    public ReadOnlyDiskSpatialIndex(Class<V> valueType, DB<V> db) {
        Map<String,Object> opts = buildOptions(valueType);
        this.filename = (String)opts.get("filename");
        String banner = (String)opts.get("banner");
        byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
        this.banner = banner;
        this.offset = align(bannerBytes.length + 1) + Node.SIZE;
        this.file = buildFile(this.filename);
        this.db = db;
    }

    public void open() throws IOException {
        if (!this.file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        raf = new RandomAccessFile(file, "r");
        channel = raf.getChannel();
        int[] size = {0};
        this.root = readRootNode(size);
        this.size = size[0];
    }

    public void close() throws IOException {
        raf.close();
        channel.close();
    }

    private Map<String,Object> buildOptions(Class<V> type) {
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

    private void putIfNotEmpty(Map<String,Object> opts, String key, String value) {
        if (value != null && value.length() != 0) {
            opts.put(key, value);
        }
    }

    public boolean contains(double x, double y, double z) {
        return root.contains(x, y, z);
    }

    public int intersect(BoundingVolume<?> region, Consumer<V> consumer) {
        return root.intersect(region, consumer);
    }

    public BoundingVolume<?> getBounds() {
        return root.getBounds();
    }

    public int size() {
        return size;
    }

    public Iterator<Entry<V>> iterator() {
        final Iterator<RefEntry<V>> iterator = root.iterator();
        return new Iterator<Entry<V>>() {
            public boolean hasNext() {
                return iterator.hasNext();
            }

            public Entry<V> next() {
                return iterator.next();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static int align(int length) {
        int slack = length % Node.SIZE;
        return slack == 0 ? length : length + (Node.SIZE - slack);
    }

    public boolean isReadOnly() {
        return true;
    }

    public boolean hasUncommittedChanges() {
        return false;
    }

    public void commitChanges() {
        // no-op
    }

    protected Node<V> readRootNode(int[] size) {
        long off = ((long)offset) - Node.SIZE;
        try {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, off, ROOT_NODE_SIZE);
            int[] children = new int[8];
            buffer.position(0);
            double[] coords = new double[6];
            buffer.asDoubleBuffer().get(coords);
            size[0] = buffer.getInt();
            buffer.asIntBuffer().get(children);
            List<NodeRef<V>> childNodes = forChildren(null, children);
            return new Node<V>(childNodes, null, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
        } catch (IOException e) {
            throw new DBException(e);
        }
    }

    protected Node<V> readNode(int block, Node<V> parent, double x0, double x1, double y0, double y1, double z0, double z1) {
        long off = ((long)offset) + block * Node.SIZE;
        try {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, off, Node.SIZE);
            int[] children = new int[8];
            buffer.asIntBuffer().get(children);
            if (children[0] == -1) {
                int entryBlock = children[1];
                int count = children[2];
                List<RefEntry<V>> refEntries = readRefEntries(entryBlock, count);
                return new Node<V>(null, refEntries, x0, x1, y0, y1, z0, z1);
            } else {
                List<NodeRef<V>> childNodes = forChildren(parent, children);
                return new Node<V>(childNodes, null, x0, x1, y0, y1, z0, z1);
            }
        } catch (IOException e) {
            throw new DBException(e);
        }
    }

    protected List<RefEntry<V>> readRefEntries(int block, int count) {
        List<RefEntry<V>> entries = new ArrayList<RefEntry<V>>(count);
        long off = ((long)offset) + block * Node.SIZE;
        long len = ((long)count) * RefEntry.SIZE;
        try {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, off, len);
            double[] coords = new double[3];
            for (int i = 0; i < count; i++) {
                buffer.asDoubleBuffer().get(coords);
                entries.add(new RefEntry<V>(buildRef(buffer.getInt()), coords[0], coords[1], coords[2]));
            }
        } catch (IOException e) {
            throw new DBException(e);
        }
        return entries;
    }

    protected Ref<V> buildRef(final int id) {
        return new Ref<V>() {
            public V get() {
                return db.get(id);
            }
        };
    }

    private List<NodeRef<V>> forChildren(Node<V> parent, int[] childBlocks) {
        List<NodeRef<V>> result = new ArrayList<NodeRef<V>>(8);
        for (OctreeVoxel voxel : OctreeVoxel.values()) {
            result.add(new NodeRef<V>(parent, childBlocks[voxel.ordinal()], voxel, this));
        }
        return result;
    }

    public Writer<V> buildWriter(IdProvider<? super V> idProvider) {
        return new WriterImpl<V>(idProvider);
    }

    protected static File buildFile(String name) {
        GeoSpec.getGeoIdentity(); // force GeoSpec to initialize
        File dir = new File("db", Configuration.DATABASE_DIRECTORY.getString());
        return new File(dir, name + ".3dex");
    }

    protected static class Node<V> implements Series<RefEntry<V>> {
        public static final int SIZE = (Integer.SIZE >> 3) * 8; // bytes

        private final List<RefEntry<V>> entries;
        private final List<NodeRef<V>> children;
        private final double x0, x_, x1;
        private final double y0, y_, y1;
        private final double z0, z_, z1;

        public Node(List<NodeRef<V>> children, List<RefEntry<V>> entries,
                    double x0, double x1, double y0, double y1, double z0, double z1) {
            this.children = children == null ? Collections.<NodeRef<V>>emptyList() : children;
            this.entries = entries == null ? Collections.<RefEntry<V>>emptyList() : entries;
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

        public int size() {
            return entries.size();
        }

        public Iterator<RefEntry<V>> iterator() {
            return new Iterator<RefEntry<V>>() {

                private Iterator<RefEntry<V>> iterator = entries.iterator();
                private Iterator<NodeRef<V>> childIterator = children.iterator();

                public boolean hasNext() {
                    while (iterator != null && !iterator.hasNext()) {
                        if (childIterator.hasNext()) {
                            iterator = childIterator.next().get().iterator();
                        } else {
                            childIterator = null;
                            iterator = null;
                        }
                    }
                    return iterator != null;
                }

                public RefEntry<V> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return iterator.next();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        public BoundingVolume<?> getBounds() {
            return new BoundingBox(new Point(x0, y0, z0), new Point(x1, y1, z1));
        }

        public boolean contains(double x, double y, double z) {
            if (x < x0 || x > x1 || y < y0 || y > y1 || z < z0 || z > z1) {
                return false;
            }
            for (RefEntry<V> entry : entries) {
                if (entry.isAt(x, y, z)) {
                    return true;
                }
            }
            return false;
        }

        public int intersect(BoundingVolume<?> region, Consumer<V> consumer) {
            int c = 0;
            for (RefEntry<V> entry : entries) {
                if (includes(region, entry)) {
                    consume(entry, consumer);
                    c++;
                }
            }
            for (NodeRef<V> child : children) {
                if (child.overlaps(x0, x1, y0, y1, z0, z1)) {
                    c += child.get().intersect(region, consumer);
                }
            }
            return c;
        }
    }

    private static class NodeRef<V> extends AbstractCachingRef<Node<V>> implements Ref<Node<V>> {

        private final Node<V> parent;
        private final int block;
        private final ReadOnlyDiskSpatialIndex<V> outer;
        private final double x0, x1, y0, y1, z0, z1;

        private NodeRef(Node<V> parent, int block, OctreeVoxel voxel, ReadOnlyDiskSpatialIndex<V> outer) {
            this.parent = parent;
            this.block = block;
            this.outer = outer;
            this.x0 = voxel.splitX(parent.x0, parent.x_);
            this.x1 = voxel.splitX(parent.x_, parent.x1);
            this.y0 = voxel.splitX(parent.y0, parent.y_);
            this.y1 = voxel.splitX(parent.y_, parent.y1);
            this.z0 = voxel.splitX(parent.z0, parent.z_);
            this.z1 = voxel.splitX(parent.z_, parent.z1);
        }

        protected Node<V> getImpl() {
            return outer.readNode(block, parent, x0, x1, y0, y1, z0, z1);
        }

        protected BoundingVolume<?> getBounds() {
            return new BoundingBox(new Point(x0, y0, z0), new Point(x1, y1, z1));
        }

        protected boolean includes(double x, double y, double z) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
        }

        protected boolean overlaps(double x0, double x1, double y0, double y1, double z0, double z1) {
            return x1 >= this.x0 && x0 <= this.x1 && y1 >= this.y0 && y0 <= this.y1 && z1 >= this.z0 && z0 <= this.z1;
        }
    }

    private static class RefEntry<V> extends AbstractEntry<V> {

        public static final int SIZE = (Double.SIZE >> 3) * 3 + (Integer.SIZE >> 3); // bytes

        private final Ref<V> ref;

        private RefEntry(Ref<V> ref, double x, double y, double z) {
            super(x, y, z);
            this.ref = ref;
        }

        @Override
        public V get() {
            return ref.get();
        }

        private boolean isAt(double x, double y, double z) {
            return x == this.x && y == this.y && z == this.z;
        }
    }

    public interface Writer<V> {
        void write(OctreeMap<V> map) throws IOException;
    }

    private class WriterImpl<V> implements Writer<V> {

        private final IdProvider<? super V> idProvider;

        private ByteBuffer zero;
        private ByteBuffer buffer;
        private long offest;

        WriterImpl(IdProvider<? super V> idProvider) {
            this.idProvider = idProvider;
        }

        public void write(OctreeMap<V> map) throws IOException {
            int maxSize = align(RefEntry.SIZE * map.getMaxElementsPerVoxel());
            this.zero = ByteBuffer.wrap(new byte[maxSize]);
            this.buffer = ByteBuffer.allocate(maxSize);
            if (map.isEmpty()) {
                throw new IllegalArgumentException("cannot write an empty map");
            }

            byte[] bannerBytes = banner.getBytes(Charset.forName("ISO-8859-1"));
            int offset = align(bannerBytes.length + 1) + Node.SIZE;
            File file = buildFile(filename);
            File dir = file.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Unable to create directory: " + dir);
                }
            }

            offest = Integer.MIN_VALUE;
            FileOutputStream out = new FileOutputStream(file);
            out.write(bannerBytes);
            out.write(0x1A); // EOF character
            FileChannel channel = out.getChannel();
            writePadding(channel, offset - bannerBytes.length - 1);
            this.offest = 0;
            writeNode(channel, map.getNodeView());
        }

        private int writeNode(ByteChannel out, OctreeMap.NodeView<V> node) throws IOException {
            if (node.isEmpty()) {
                return -1;
            }
            buffer.clear();
            if (node.hasChildNodes()) {
                for (OctreeVoxel voxel : OctreeVoxel.values()) {
                    buffer.putInt(writeNode(out, node.getChildNode(voxel)));
                }
            } else {
                int size = node.getEntryCount();
                for (int i = 0; i < size; i++) {
                    Entry<V> entry = node.getEntry(i);
                    buffer.putDouble(entry.getAxis(Axis.X));
                    buffer.putDouble(entry.getAxis(Axis.Y));
                    buffer.putDouble(entry.getAxis(Axis.Z));
                    buffer.putInt(idProvider.getId(entry.get()));
                }
                buffer.flip();
                int entryBlock = writeBlock(out, buffer);
                buffer.clear();
                buffer.putInt(-1);
                buffer.putInt(entryBlock);
                buffer.putInt(size);
            }
            buffer.flip();
            return writeBlock(out, buffer);
        }

        private int writeBlock(ByteChannel out, ByteBuffer buffer) throws IOException {
            int block = (int)(offest / Node.SIZE);
            assert offest % Node.SIZE == 0;
            int bytes = out.write(buffer);
            offest += bytes;
            padBlock(out, bytes);
            return block;
        }

        private void padBlock(ByteChannel out, int dataBytes) throws IOException {
            int slack = dataBytes % Node.SIZE;
            if (slack > 0) {
                writePadding(out, Node.SIZE - slack);
            }
        }

        private void writePadding(ByteChannel out, int bytes) throws IOException {
            while (bytes > 0) {
                zero.position(0);
                int capacity = zero.capacity();
                if (bytes > capacity) {
                    zero.limit(capacity);
                    out.write(zero);
                    bytes -= capacity;
                } else {
                    zero.limit(bytes);
                    out.write(zero);
                    bytes = 0;
                }
            }
            offest += bytes;
        }
    }
}
