package net.venaglia.realms.spec.map;

import net.venaglia.common.util.Series;
import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.bounds.SimpleBoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.util.OctreeVoxel;
import net.venaglia.gloo.util.impl.AbstractSpatialMap;
import net.venaglia.gloo.util.impl.OctreeMap;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
* User: ed
* Date: 9/4/14
* Time: 5:18 PM
*/
public class GlobalPointMap extends OctreeMap<GeoPoint> {

    private File hibernationFile = null;

    public GlobalPointMap() {
        super(new BoundingSphere(Point.ORIGIN, 1024.0), 16, 12);
    }

    @Override
    public boolean contains(double x, double y, double z) {
        checkHiberhate();
        return super.contains(x, y, z);
    }

    @Override
    public int intersect(SimpleBoundingVolume region, Consumer<GeoPoint> consumer) {
        checkHiberhate();
        return super.intersect(region, consumer);
    }

    @Override
    public BoundingVolume<?> getBounds() {
        checkHiberhate();
        return super.getBounds();
    }

    @Override
    protected boolean addImpl(AbstractEntry<GeoPoint> entry) throws UnsupportedOperationException {
        checkHiberhate();
        return super.addImpl(entry);
    }

    @Override
    protected boolean addImpl(AbstractEntry<GeoPoint> entry, double x, double y, double z)
            throws UnsupportedOperationException {
        checkHiberhate();
        return super.addImpl(entry, x, y, z);
    }

    @Override
    protected boolean removeImpl(AbstractEntry<GeoPoint> entry)
            throws UnsupportedOperationException {
        checkHiberhate();
        return super.removeImpl(entry);
    }

    @Override
    protected boolean moveImpl(AbstractEntry<GeoPoint> entry, double x, double y, double z)
            throws IndexOutOfBoundsException, UnsupportedOperationException {
        checkHiberhate();
        return super.moveImpl(entry, x, y, z);
    }

    @Override
    public int size() {
        checkHiberhate();
        return super.size();
    }

    @Override
    public void clear() {
        checkHiberhate();
        super.clear();
    }

    public void fastClear() {
        checkHiberhate();
        fastClearImpl();
    }

    private void fastClearImpl() {
        // truncate to free memory
        reconstruct(Collections.<ReconstructOperation<GeoPoint>>singleton(new ReconstructOperation<GeoPoint>() {
            public char getCode() {
                return OPERATION_CODE_END;
            }

            public OctreeVoxel getVoxel() {
                return null;
            }

            public Entry<GeoPoint> getEntry() {
                return null;
            }
        }).iterator());
    }

    @Override
    public Iterator<Entry<GeoPoint>> iterator() {
        checkHiberhate();
        return super.iterator();
    }

    @Override
    public NodeView<GeoPoint> getNodeView() {
        checkHiberhate();
        return super.getNodeView();
    }

    @Override
    public void project(long nowMS, GeometryBuffer buffer) {
        if (hibernationFile == null) {
            super.project(nowMS, buffer);
        }
    }

    @Override
    public void projectImpl(long nowMS, GeometryBuffer buffer) {
        if (hibernationFile == null) {
            super.projectImpl(nowMS, buffer);
        }
    }

    @Override
    public boolean isEmpty() {
        checkHiberhate();
        return super.isEmpty();
    }

    @Override
    public GeoPoint get(Point p) {
        checkHiberhate();
        return super.get(p);
    }

    @Override
    public GeoPoint get(double x, double y, double z) {
        checkHiberhate();
        return super.get(x, y, z);
    }

    @Override
    public GeoPoint get(Point p, double r) {
        checkHiberhate();
        return super.get(p, r);
    }

    @Override
    protected BasicEntry<GeoPoint> getEntry(Point p, double r) {
        checkHiberhate();
        return super.getEntry(p, r);
    }

    @Override
    public GeoPoint get(double x, double y, double z, double r) {
        checkHiberhate();
        return super.get(x, y, z, r);
    }

    @Override
    public int intersect(SimpleBoundingVolume region, BasicConsumer<GeoPoint> consumer) {
        checkHiberhate();
        return super.intersect(region, consumer);
    }

    @Override
    public Series<GeoPoint> asSeries() {
        checkHiberhate();
        return super.asSeries();
    }

    @Override
    public boolean add(GeoPoint object, Point p) {
        checkHiberhate();
        return super.add(object, p);
    }

    @Override
    public boolean add(GeoPoint obj, double x, double y, double z) {
        checkHiberhate();
        return super.add(obj, x, y, z);
    }

    @Override
    protected AbstractEntry<GeoPoint> createEntry(final GeoPoint obj, double x, double y, double z) {
        checkHiberhate();
        return new GlobalPointEntry(x, y, z, obj);
    }

    private void checkHiberhate() {
        if (hibernationFile != null) {
            throw new IllegalStateException("GlobalPointMap is in hibernation");
        }
    }

    public long getSeq(GeoPoint point) {
        BasicEntry<GeoPoint> entry = getEntry(point.toPoint(1000.0), 0.0005);
        return entry == null ? -1L : ((GlobalPointEntry)entry).getSeq();
    }

    public synchronized void hibernate() {
        if (hibernationFile == null) {
            boolean success = false;
            try {
                hibernationFile = File.createTempFile("GlobalPointMap.", ".bin");
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            ByteBuffer buffer = ByteBuffer.allocate(GlobalPointEntry.BYTE_BUFFER_SIZE + 1);
            hibernationFile.deleteOnExit();
            FileChannel channel = null;
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(hibernationFile);
                channel = out.getChannel();
                NodeView<GeoPoint> nodeView = super.getNodeView();
                if (!nodeView.isEmpty()) {
                    writeNode(nodeView, channel, buffer);
                }
                buffer.put((byte)ReconstructOperation.OPERATION_CODE_END);
                buffer.flip();
                channel.write(buffer);
                buffer.clear();
                success = true;
                fastClearImpl();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                closeQuietly(channel);
                closeQuietly(out);
                if (success) {
                    System.out.printf("Hibernated GlobalPointMap - %,d bytes written to %s\n",
                                      hibernationFile.length(),
                                      hibernationFile);
                } else {
                    hibernationFile.delete();
                    hibernationFile = null;
                }
            }
        }
    }

    private void writeNode(NodeView<GeoPoint> nodeView, FileChannel out, ByteBuffer buffer)
            throws IOException {
        if (nodeView.hasChildNodes()) {
            for (OctreeVoxel voxel : OctreeVoxel.values()) {
                NodeView<GeoPoint> childNode = nodeView.getChildNode(voxel);
                if (!childNode.isEmpty()) {
                    buffer.put((byte)ReconstructOperation.OPERATION_CODE_MOVE_TO_NEW_CHILD);
                    buffer.put((byte)voxel.ordinal());
                    buffer.flip();
                    out.write(buffer);
                    buffer.clear();
                    writeNode(childNode, out, buffer);
                    buffer.put((byte)ReconstructOperation.OPERATION_CODE_MOVE_TO_PARENT);
                    buffer.flip();
                    out.write(buffer);
                    buffer.clear();
                }
            }
        } else {
            int entryCount = nodeView.getEntryCount();
            for (int i = 0; i < entryCount; i++) {
                GlobalPointEntry entry = (GlobalPointEntry)nodeView.getEntry(i);
                buffer.put((byte)ReconstructOperation.OPERATION_CODE_ADD_ENTRY);
                entry.toBytes(buffer);
                buffer.flip();
                out.write(buffer);
                buffer.clear();
            }
        }
    }

    public synchronized void resuscitate() {
        if (hibernationFile != null) {
            FileChannel channel = null;
            FileInputStream out = null;
            try {
                out = new FileInputStream(hibernationFile);
                channel = out.getChannel();
                reconstruct(new ReconstructIterator(channel));
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                closeQuietly(channel);
                closeQuietly(out);
            }
            hibernationFile.delete();
            hibernationFile = null;
            System.out.printf("Resuscitated GlobalPointMap - %,d entries\n", size());
        }
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // don't care
            }
        }
    }

    public static class GlobalPointEntry extends AbstractSpatialMap.AbstractEntry<GeoPoint> {

        static final int BYTE_BUFFER_SIZE = (Double.SIZE * 5 + Integer.SIZE) / 8;

        private final double longitude;
        private final double latitude;

        private int seq = Integer.MIN_VALUE;

        public GlobalPointEntry(double x, double y, double z, GeoPoint obj) {
            super(x, y, z);
            this.longitude = obj.longitude;
            this.latitude = obj.latitude;
        }

        private GlobalPointEntry(double x, double y, double z, double longitude, double latitude) {
            super(x, y, z);
            this.longitude = longitude;
            this.latitude = latitude;
        }

        public GeoPoint get() {
            return new GeoPoint(longitude, latitude);
        }

        public int getSeq() {
            return seq;
        }

        public void setSeq(int seq) {
            this.seq = seq;
        }

        void toBytes(ByteBuffer buffer) {
            buffer.putDouble(x);
            buffer.putDouble(y);
            buffer.putDouble(z);
            buffer.putDouble(longitude);
            buffer.putDouble(latitude);
            buffer.putInt(seq);
        }

        static GlobalPointEntry fromBytes(ByteBuffer buffer) {
            if (buffer.remaining() < BYTE_BUFFER_SIZE) {
                return null;
            }
            GlobalPointEntry entry = new GlobalPointEntry(
                    buffer.getDouble(),
                    buffer.getDouble(),
                    buffer.getDouble(),
                    buffer.getDouble(),
                    buffer.getDouble()
            );
            entry.setSeq(buffer.getInt());
            return entry;
        }
    }

    private static class ReconstructIterator implements Iterator<ReconstructOperation<GeoPoint>> {

        private final FileChannel channel;
        private final ByteBuffer oneByteBuffer;
        private final ByteBuffer entryBuffer;
        private final ReconstructOperation<GeoPoint> op;

        private boolean eof = false;
        private char opCode;
        private OctreeVoxel voxel;
        private GlobalPointEntry entry;

        public ReconstructIterator(FileChannel channel) {
            this.channel = channel;
            this.oneByteBuffer = ByteBuffer.allocate(1);
            this.entryBuffer = ByteBuffer.allocate(GlobalPointEntry.BYTE_BUFFER_SIZE);
            this.op = new ReconstructOperation<GeoPoint>() {
                public char getCode() {
                    return opCode;
                }

                public OctreeVoxel getVoxel() {
                    return voxel;
                }

                public Entry<GeoPoint> getEntry() {
                    return entry;
                }
            };
        }

        public boolean hasNext() {
            return !eof;
        }

        public ReconstructOperation<GeoPoint> next() {
            if (eof) {
                throw new NoSuchElementException();
            }
            opCode = (char)getOneByte();
            voxel = null;
            entry = null;
            switch (opCode) {
                case ReconstructOperation.OPERATION_CODE_MOVE_TO_NEW_CHILD:
                    voxel = OctreeVoxel.values()[getOneByte()];
                    break;
                case ReconstructOperation.OPERATION_CODE_MOVE_TO_PARENT:
                    break;
                case ReconstructOperation.OPERATION_CODE_ADD_ENTRY:
                    entry = readEntry();
                    break;
                case ReconstructOperation.OPERATION_CODE_END:
                    eof = true;
                    break;
                default:
                    eof = true;
            }
            return op;
        }

        private int getOneByte() {
            try {
                channel.read(oneByteBuffer);
                oneByteBuffer.flip();
                return ((int)oneByteBuffer.get()) & 0xFF;
            } catch (IOException e) {
                eof = true;
                return -1;
            } finally {
                oneByteBuffer.clear();
            }
        }

        private GlobalPointEntry readEntry() {
            try {
                channel.read(entryBuffer);
                entryBuffer.flip();
                return GlobalPointEntry.fromBytes(entryBuffer);
            } catch (IOException e) {
                eof = true;
                return null;
            } finally {
                entryBuffer.clear();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
