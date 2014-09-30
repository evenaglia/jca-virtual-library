package net.venaglia.realms.common.map.world;

import static java.lang.Double.longBitsToDouble;
import static net.venaglia.realms.common.map.things.annotations.AnnotationDrivenThingProcessor.generateSerializer;

import net.venaglia.common.util.Factory;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.serializer.SerializerDebugger;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeDefinition;
import net.venaglia.realms.common.map.data.binaries.BinaryTypeRegistry;
import net.venaglia.realms.common.map.things.annotations.Property;
import net.venaglia.realms.common.map.things.surface.Zone;
import net.venaglia.realms.common.map.things.surface.ZonePosition;
import net.venaglia.realms.common.map.world.ref.AcreDetailRef;
import net.venaglia.realms.common.map.world.topo.Topography;
import net.venaglia.realms.common.map.world.topo.TopographyDef;
import net.venaglia.realms.common.util.Visitor;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.MatrixXForm;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.util.matrix.Matrix_4x4;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;

/**
 * User: ed
 * Date: 1/28/13
 * Time: 8:44 AM
 */
public class AcreDetail extends WorldElement {

    public static final String MIMETYPE = "world/acre";
    public static final BinaryTypeDefinition<AcreDetail> DEFINITION;

    static {
        Factory<AcreDetail> factory = new Factory<AcreDetail>() {
            public AcreDetail createEmpty() {
                return new AcreDetail();
            }
        };
        Visitor<AcreDetail> postProcess = new Visitor<AcreDetail>() {
            public void visit(AcreDetail value) throws RuntimeException {
                value.acreTopographyRef = new TopographyDef(value.acreTopographyDef);
            }
        };
        SerializerStrategy<AcreDetail> serializer = generateSerializer(AcreDetail.class, factory, null, postProcess);
        DEFINITION = BinaryTypeDefinition.build(AcreDetail.class, MIMETYPE, serializer);
        BinaryTypeRegistry.add(DEFINITION);
    }

    private static final double HALF_PI = Math.PI * 0.5;

    @Property
    private GeoPoint center;
    @Property
    private GeoPoint[] vertices;
    private transient XForm toLocal;
    private transient XForm toGlobal;
    @Property
    int[] neighborIds;
    private Collection<AcreDetailRef> neighbors;
    @Property
    private long[] acreTopographyDef;
    @Property
    private long[] seamFirstVertexIds; // length = 9 * (pentagonal ? 5 : 6)
    @Property
    private long[] zoneFirstVertexIds; // length = 4 * (pentagonal ? 5 : 6)
    private transient Ref<Topography> acreTopographyRef;
    private transient EnumMap<ZonePosition,Ref<Zone>> zoneRef;
    @Property
    private float ruggedness = 1.0f; // 1 = plains; 10 = rolling hills; 100 = mountains

    public AcreDetail() {
    }

    public GeoPoint getCenter() {
        return center;
    }

    public void setCenter(GeoPoint center) {
        this.center = center;
        this.toGlobal = null;
        this.toLocal = null;
    }

    public GeoPoint[] getVertices() {
        return vertices;
    }

    public void setVertices(GeoPoint[] vertices) {
        this.vertices = vertices;
    }

    public XForm getToLocal() {
        if (toLocal == null && center != null) {
            toLocal = new MatrixXForm(Matrix_4x4.identity()
                                                .product(Matrix_4x4.rotate(Axis.Z, -center.longitude))
                                                .product(Matrix_4x4.rotate(Axis.X, center.latitude + HALF_PI))
                                                .product(Matrix_4x4.translate(Axis.Y,
                                                                              -GeoSpec.APPROX_RADIUS_METERS.get())));
        }
        return toLocal;
    }

    public XForm getToGlobal() {
        if (toGlobal == null && center != null) {
            toGlobal = new MatrixXForm(Matrix_4x4.identity()
                                                 .product(Matrix_4x4.translate(Axis.Y, GeoSpec.APPROX_RADIUS_METERS.get()))
                                                 .product(Matrix_4x4.rotate(Axis.X, -center.latitude - HALF_PI))
                                                 .product(Matrix_4x4.rotate(Axis.Z, center.longitude)));
        }
        return toGlobal;
    }

    public Collection<AcreDetailRef> getNeighbors() {
        if (neighbors == null) {
            WorldMap worldMap = WorldMap.INSTANCE.get();
            neighbors = new ArrayList<AcreDetailRef>(neighborIds.length);
            for (int neighborId : neighborIds) {
                neighbors.add(new AcreDetailRef(neighborId, worldMap.getBinaryStore()));
            }
        }
        return neighbors;
    }

    public void setNeighborIds(int[] neighborIds) {
        this.neighborIds = neighborIds;
    }

    public void setAcreTopographyDef(long[] acreTopographyDef) {
        this.acreTopographyDef = acreTopographyDef;
    }

    public void setSeamFirstVertexIds(long[] seamFirstVertexIds) {
        this.seamFirstVertexIds = seamFirstVertexIds;
    }

    public void setZoneFirstVertexIds(long[] zoneFirstVertexIds) {
        this.zoneFirstVertexIds = zoneFirstVertexIds;
    }

    public float getRuggedness() {
        return ruggedness;
    }

    public void setRuggedness(float ruggedness) {
        this.ruggedness = ruggedness;
    }

    public Shape<?> getSurface(DetailLevel detailLevel) {
        // todo
        return null;
    }

    public String formatForSanityCheck() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(id);                      //   4
        buffer.putDouble(center.latitude);
        buffer.putDouble(center.longitude);     //  20
        boolean pentagonal = vertices.length == 5;
        for (GeoPoint vertex : vertices) {
            buffer.putDouble(vertex.latitude);
            buffer.putDouble(vertex.longitude);
        }                                       // 116 or 100
        for (int neighborId : neighborIds) {
            buffer.putInt(neighborId);
        }                                       // 140 or 120
        for (long l : acreTopographyDef) {
            buffer.putLong(l);
        }                                       // 292 or 248
        for (long l : seamFirstVertexIds) {
            buffer.putLong(l);
        }                                       // 628 or 528
        for (long l : zoneFirstVertexIds) {
            buffer.putLong(l);
        }                                       // 820 or 688
        buffer.putFloat(ruggedness);            // 824 or 692
        buffer.flip();
        StringBuilder out = new StringBuilder(buffer.limit() + 10);
        for (int i = 0, l = buffer.limit(); i < l; i++) {
            switch (i) {
                case 4:
                case 20:
                    out.append(':');
                    break;
                case 116:
                case 140:
                case 292:
                case 628:
                case 820:
                    if (!pentagonal) out.append(":");
                    break;
                case 100:
                case 120:
                case 248:
                case 528:
                case 688:
                    if (pentagonal) out.append(":");
                    break;
            }
            int b = ((int)buffer.get()) & 0xFF;
            out.append("0123456789abcdef".charAt((b >> 4) & 0xF));
            out.append("0123456789abcdef".charAt(b & 0xF));
        }
        return out.toString();
    }

    public static void init() {
        // no-op
    }

    public static void main(String[] args) throws IOException {
        AcreDetail sample = getSampleAcreDetail();
        String fsc = sample.formatForSanityCheck();

        SerializerDebugger debugger = new SerializerDebugger();
        SerializerStrategy<AcreDetail> serializer = AcreDetail.DEFINITION.getSerializer();
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        debugger.serialize(serializer, sample, buffer);
        debugger.writeReport(new PrintWriter(System.out, true));
        buffer.flip();
        byte[] test = new byte[buffer.limit()];
        buffer.get(test);

//        assert test.length == serializedSampleAcre.length;
//        assert Arrays.equals(serializedSampleAcre, test);

        System.out.println();
        System.out.println();
        System.out.println();
        buffer = ByteBuffer.wrap(test);
        debugger.reset();
        try {
            AcreDetail acreDetail = debugger.deserialize(serializer, buffer);
            assert fsc.equals(acreDetail.formatForSanityCheck());
        } finally {
            debugger.writeReport(new PrintWriter(System.out, true));
        }
    }

    private static AcreDetail getSampleAcreDetail() {
        AcreDetail sample = new AcreDetail();
        sample.setCenter(new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(4609566381344195822L)));
        sample.setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4610530227853135648l), longBitsToDouble(4609639443272320738L)),
                new GeoPoint(longBitsToDouble(-4615033827480506144L), longBitsToDouble(4609639443272320738L)),
                new GeoPoint(longBitsToDouble(-4615033827480506144L), longBitsToDouble(4609522382912331833L)),
                new GeoPoint(longBitsToDouble(-4613274214787029681L), longBitsToDouble(4609460555567388109L)),
                new GeoPoint(longBitsToDouble(-4611410034199873878L), longBitsToDouble(4609460555567388109L)),
                new GeoPoint(longBitsToDouble(-4610530227853135648L), longBitsToDouble(4609522382912331833L))
        });
        sample.setNeighborIds(new int[]{ 3, 879, 456, 15, 223, 12 });
        sample.setAcreTopographyDef(new long[]{
                32247,
                32253, 43462, 43464, 32251, 32235, 32257,
                32252, 43468, 43461, 32227, 32234, 32241,
                32256, 32255, 43463, 32248, 32250, 32249
        });
        sample.setSeamFirstVertexIds(new long[]{
                19951616, 19951680, 19951744, 19951808, 19951872, 15329083648L, 15329083712L,
                19951936, 19952000, 19952064, 19952128, 19952192, 15329084416L, 15329084480L,
                19952256, 19952320, 19952384, 19952448, 19952512, 15331237888L, 15331237952L,
                19952576, 19952640, 19952704, 19952768, 19952832, 15331238656L, 15331238720L,
                19952896, 19952960, 19953024, 19953088, 19953152, 15329082880L, 15329082944L,
                19953216, 19953280, 19953344, 19953408, 19953472, 15329080576L, 15329080640L
        });
        sample.setZoneFirstVertexIds(new long[]{
                318554112, 318556160, 318558208, 318560256, 318562304, 318564352,
                318566400, 318568448, 318570496, 318572544, 318574592, 318576640,
                318578688, 318580736, 318582784, 318584832, 318586880, 318588928,
                318590976, 318593024, 318595072, 318597120, 318599168, 318601216
        });
        sample.setRuggedness(Float.intBitsToFloat(1065353216));
        return sample;
    }
}
