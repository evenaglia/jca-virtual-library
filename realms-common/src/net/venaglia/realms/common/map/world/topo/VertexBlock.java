package net.venaglia.realms.common.map.world.topo;

import net.venaglia.common.util.Identifiable;
import net.venaglia.common.util.Visitor;
import net.venaglia.realms.common.map.VertexStore;
import net.venaglia.realms.common.map.data.binaries.BinaryType;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeRegistry;
import net.venaglia.realms.common.util.cache.Volatile;
import net.venaglia.realms.spec.GeoSpec;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

/**
 * User: ed
 * Date: 5/29/14
 * Time: 8:08 AM
 */
public class VertexBlock implements Identifiable, Volatile {

    private static final int VERTEX_BITS = Float.SIZE * 3 + Integer.SIZE; // 16 bytes

    public static final String MIMETYPE = "world/vertex";
    public static final BinaryType DEFINITION;
    public static final int VERTEX_MASK_BITS = 12;
    public static final int VERTEX_COUNT = 1 << VERTEX_MASK_BITS; // 4096
    public static final long INDEX_MASK = VERTEX_COUNT - 1; // 4095
    public static final long VERTEX_ID_MASK = ~INDEX_MASK;
    public static final int BUFFER_SIZE = VERTEX_COUNT * VERTEX_BITS / 8;

    public static final IdRange MANY_SHARED_VERTEX_STORES;
    public static final IdRange DUAL_SHARED_VERTEX_STORES;
    public static final IdRange NON_SHARED_VERTEX_STORES;

    private static final int STRIDE = VERTEX_BITS / 8; // 16 bytes

    private static final double PI = Math.PI;
    private static final double HALF_PI = PI * 0.5;
    private static final double HALF_PI_NEGATIVE = 0.0 - HALF_PI;

    static {
        DEFINITION = new BinaryType() {
            public Class<?> getJavaType() {
                return VertexBlock.class;
            }

            public String mimeType() {
                return MIMETYPE;
            }

            public Map<String,Object> generateMetadata(byte[] data) {
                return Collections.emptyMap();
            }

            public Map<String,Object> decodeMetadata(String encoded) {
                return Collections.emptyMap();
            }

            public String encodeMetadata(Map<String,Object> metadata) {
                return "";
            }
        };
        BinaryTypeRegistry.add(DEFINITION);

        MANY_SHARED_VERTEX_STORES = new IdRange(GeoSpec.POINTS_SHARED_MANY_ZONE.get());
        long padding = GeoSpec.ZONES.get() * (VERTEX_COUNT / 2 - GeoSpec.POINTS_NOT_SHARED_PER_ZONE.get());
        DUAL_SHARED_VERTEX_STORES = MANY_SHARED_VERTEX_STORES.next(GeoSpec.POINTS_SHARED_DUAL_ZONE.get(), padding);
        NON_SHARED_VERTEX_STORES = DUAL_SHARED_VERTEX_STORES.next(GeoSpec.POINTS_NOT_SHARED.get(), padding);
    }

    private Long id; // always a multiple of VERTEX_COUNT, individual elements addressed by LSB mask

    public VertexBlock(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    /**
     * VERTEX_COUNT elements, packed as:
     * [
     *   {
     *     4b (float) - elevation
     *     4b (float) - normal.longitude
     *     4b (float) - normal.latitude
     *     4b (rgbx)  - color
     *   }, ...
     * ]
     */
    private final ByteBuffer byteBuffer;

    void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void visit(int index, VertexStore.VertexConsumer consumer) {
        float elevation;
        float lon;
        float lat;
        int rgba;
        synchronized (byteBuffer) {
            byteBuffer.position(position(index));
            elevation = byteBuffer.getFloat();
            lon = byteBuffer.getFloat();
            lat = byteBuffer.getFloat();
            rgba = byteBuffer.getInt();
        }
        double c = Math.cos(lat);
        double i = Math.sin(lon) * c;
        double j = Math.cos(lon) * c;
        double k = Math.sin(lat);
        consumer.next(rgba, i, j, k, elevation);
    }

    public void set(int index, int rgba, double i, double j, double k, float elevation) {
        float lon;
        float lat;
        if (i == 0 && j == 0) {
            if (k == 0) {
                throw new IllegalArgumentException("Cannot use a zero length vector as a normal");
            } else {
                lon = (float)(k < 0 ? HALF_PI_NEGATIVE : HALF_PI);
                lat = 0;
            }
        } else {
            double longitude = Math.atan2(i, j);
            if (longitude > Math.PI) longitude -= 2.0 * Math.PI;
            double latitude = Math.atan2(k, Math.sqrt(i * i + j * j));
            if (latitude > HALF_PI) latitude -= Math.PI;
            lon = (float)longitude;
            lat = (float)latitude;
        }
        synchronized (byteBuffer) {
            byteBuffer.position(position(index));
            byteBuffer.putFloat(elevation);
            byteBuffer.putFloat(lon);
            byteBuffer.putFloat(lat);
            byteBuffer.putInt(rgba);
        }
    }

    void update(byte[] buffer) {
        assert buffer.length == byteBuffer.capacity();
        synchronized (byteBuffer) {
            byteBuffer.position(0);
            byteBuffer.get(buffer);
        }
    }

    private int position(int index) {
        return index * STRIDE;
    }

    public static void init() {
        // no-op, here to bootstrap the type into the BinaryTypeRegistry
    }

    public static class IdRange implements Comparable<IdRange> {
        public final long begin;
        public final long end;
        public final long expectedVertices;
        public final long maxVertices;

        private IdRange(long expectedVertices) {
            this(0L, expectedVertices | INDEX_MASK, expectedVertices);
        }

        private IdRange(long begin, long end, long expectedVertices) {
            this.begin = begin;
            this.end = end;
            this.expectedVertices = expectedVertices;
            this.maxVertices = end - begin;
        }

        private IdRange next(long expectedVertices, long padding) {
            long allocateFor = (expectedVertices + padding) | INDEX_MASK;
            return new IdRange(end + 1, allocateFor, expectedVertices);
        }

        public int compareTo(IdRange o) {
            return begin < o.begin ? -1 : begin > o.begin ? 1 : 0;
        }

        public void visitVertexIds(Visitor<Long> visitor) {
            for (long i = begin & VERTEX_ID_MASK, j = end & VERTEX_ID_MASK; i <= j; i += VERTEX_COUNT) {
                visitor.visit(i);
            }
        }
    }
}
