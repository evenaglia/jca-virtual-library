package net.venaglia.realms.common.map.world;

import static java.lang.Double.longBitsToDouble;
import static net.venaglia.realms.common.map.things.annotations.AnnotationDrivenThingProcessor.generateSerializer;

import net.venaglia.common.util.Factory;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.Visitor;
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

    /**
     * def[0] : center vertex id
     * def[1..6] : edge midpoint vertex ids
     * def[7..12] : corner vertex ids
     * def[13..18] : spoke midpoint vertex ids
     */
    @Property
    private long[] acreTopographyDef;

    /**
     * starts[0..4] : zone seam vertex start ids
     * starts[5..6] : acre seam vertex ids
     * ...
     * starts[35..39] : zone seam vertex start ids
     * starts[40..41] : acre seam vertex ids
     */
    @Property
    private long[] seamFirstVertexIds; // length = 9 * (pentagonal ? 5 : 6)

    /**
     * starts[0..23] : zone inner vertex start ids
     */
    @Property
    private long[] zoneFirstVertexIds; // length = 4 * (pentagonal ? 5 : 6)
    private transient Ref<Topography> acreTopographyRef;
    private transient EnumMap<ZonePosition,Ref<Zone>> zoneRef;
    @Property
    private float elevation = 0.0f; // -1.0 deep ocean, 0.0 swamp or coastline, 1.0 high mountains
    private float pressure = 0.0f;

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

    /**
     * Copies neighbor ids into the passed array, and returns the number of ids written
     * @param out Buffer to receive neighbor ids.
     * @return the number of ids written, either 5 or 6
     */
    public int getNeighborIds(int[] out) {
        System.arraycopy(neighborIds, 0, out, 0, neighborIds.length);
        return neighborIds.length;
    }

    public void setNeighborIds(int[] neighborIds) {
        this.neighborIds = neighborIds;
    }

    public long[] getAcreTopographyDef() {
        return acreTopographyDef;
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

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
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
        buffer.putFloat(elevation);             // 824 or 692
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
        AcreDetail sample = getSampleAcreDetail()[0];
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

    public String toSourceLiteral(String varName) {
        int c = neighborIds.length;
        StringBuilder builder = new StringBuilder(2500);
        builder.append("        ");
        if (varName.matches("[a-zA-Z$_]\\w*")) {
            builder.append("AcreDetail ");
        }
        builder.append("acre = new AcreDetail();\n");
        builder.append("        acre.setId(").append(id).append(");\n");
        builder.append("        acre.setCenter(").append(center.toSourceLiteral()).append(");\n");
        builder.append("        acre.setVertices(new GeoPoint[]{\n");
        for (int i = 0, l = vertices.length - 1; i <= l; i++) {
            GeoPoint vertex = vertices[i];
            builder.append("                ").append(vertex.toSourceLiteral());
            if (i < l) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("        });\n");
        builder.append("        acre.setNeighborIds(new int[]{ ")
               .append(toSourceLiteral(neighborIds, 0, c))
               .append(" });\n");
        builder.append("        acre.setAcreTopographyDef(new long[]{\n");
        builder.append("                ").append(toSourceLiteral(acreTopographyDef, 0, 1)).append("\n");
        for (int i = 1, l = acreTopographyDef.length; i < l; i += c) {
            builder.append("                ").append(toSourceLiteral(acreTopographyDef, i, c)).append("\n");
        }
        builder.append("        });\n");
        builder.append("        acre.setSeamFirstVertexIds(new long[]{\n");
        for (int i = 0, l = seamFirstVertexIds.length; i < l; i += 7) {
            builder.append("                ").append(toSourceLiteral(seamFirstVertexIds, i, 7)).append("\n");
        }
        builder.append("        });\n");
        builder.append("        acre.setZoneFirstVertexIds(new long[]{\n");
        for (int i = 0, l = zoneFirstVertexIds.length; i < l; i += c) {
            builder.append("                ").append(toSourceLiteral(zoneFirstVertexIds, i, c)).append("\n");
        }
        builder.append("        });\n");
        builder.append("        acre.setElevation(Float.intBitsToFloat(")
               .append(Float.floatToIntBits(elevation))
               .append("));\n");
        return builder.toString().replace("acre", varName);
    }

    private String toSourceLiteral(int[] values, int offset, int length) {
        if (offset + length > values.length) {
            throw new ArrayIndexOutOfBoundsException(offset + length);
        }
        StringBuilder builder = new StringBuilder(length * 16);
        for (int i = offset, j = 1; j <= length; i++, j++) {
            builder.append(values[i]);
            if (offset + j < values.length) {
                builder.append(',');
                if (j < length) builder.append(' ');
            }
        }
        return builder.toString();
    }

    private String toSourceLiteral(long[] values, int offset, int length) {
        if (offset + length > values.length) {
            throw new ArrayIndexOutOfBoundsException(offset + length);
        }
        StringBuilder builder = new StringBuilder(length * 16);
        for (int i = offset, j = 1; j <= length; i++, j++) {
            builder.append(values[i]).append('L');
            if (offset + j < values.length) {
                builder.append(',');
                if (j < length) builder.append(' ');
            }
        }
        return builder.toString();
    }

    public static AcreDetail[] getSampleAcreDetail() {
        AcreDetail[] acre = new AcreDetail[7];
        acre[0] = new AcreDetail();
        acre[0].setId(1400);
        acre[0].setCenter(new GeoPoint(longBitsToDouble(-4612328941586322293L), longBitsToDouble(-4620746769786686262L)));
        acre[0].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612332686590506937L), longBitsToDouble(-4620481214168053756L)),
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620559302886557850L)),
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620893629530942424L)),
                new GeoPoint(longBitsToDouble(-4612325408551135361L), longBitsToDouble(-4621210265915791352L)),
                new GeoPoint(longBitsToDouble(-4612451337360390071L), longBitsToDouble(-4621073838224515698L)),
                new GeoPoint(longBitsToDouble(-4612458675279891925L), longBitsToDouble(-4620647395159321547L))
        });
        acre[0].setNeighborIds(new int[]{ 1312, 1310, 2925, 2915, 1409, 1402 });
        acre[0].setAcreTopographyDef(new long[]{
                5616L,
                5298L, 5272L, 5281L, 5622L, 5621L, 5606L,
                5631L, 5294L, 5270L, 5617L, 5619L, 5609L,
                5604L, 5279L, 5280L, 5618L, 5620L, 5607L
        });
        acre[0].setSeamFirstVertexIds(new long[]{
                22639616L, 22639680L, 22639744L, 22639808L, 22639872L, 15323972608L, 15323972672L,
                22639936L, 22640000L, 22640064L, 22640128L, 22640192L, 15323911168L, 15323911232L,
                22640256L, 22640320L, 22640384L, 22640448L, 22640512L, 15323905024L, 15323905088L,
                22640576L, 22640640L, 22640704L, 22640768L, 22640832L, 15323907328L, 15323907392L,
                22640896L, 22640960L, 22641024L, 22641088L, 22641152L, 15323974912L, 15323974976L,
                22641216L, 22641280L, 22641344L, 22641408L, 22641472L, 15323975680L, 15323975744L
        });
        acre[0].setZoneFirstVertexIds(new long[]{
                387366912L, 387368960L, 387371008L, 387373056L, 387375104L, 387377152L,
                387379200L, 387381248L, 387383296L, 387385344L, 387387392L, 387389440L,
                387391488L, 387393536L, 387395584L, 387397632L, 387399680L, 387401728L,
                387403776L, 387405824L, 387407872L, 387409920L, 387411968L, 387414016L
        });
        acre[0].setElevation(Float.intBitsToFloat(0));

        acre[1] = new AcreDetail();
        acre[1].setId(1312);
        acre[1].setCenter(new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620318301039109490L)));
        acre[1].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612071601535407513L), longBitsToDouble(-4620235429607916382L)),
                new GeoPoint(longBitsToDouble(-4612075578168882598L), longBitsToDouble(-4620481214168053756L)),
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620559302886557850L)),
                new GeoPoint(longBitsToDouble(-4612332686590506937L), longBitsToDouble(-4620481214168053756L)),
                new GeoPoint(longBitsToDouble(-4612336663223982022L), longBitsToDouble(-4620235429607916382L)),
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620070470460372362L))
        });
        acre[1].setNeighborIds(new int[]{ 1309, 1310, 1400, 1402, 1399, 1307 });
        acre[1].setAcreTopographyDef(new long[]{
                1195L,
                5286L, 5295L, 5298L, 5633L, 1609L, 1194L,
                1193L, 5285L, 5294L, 5631L, 1608L, 1192L,
                1196L, 5293L, 5296L, 5632L, 1611L, 1197L
        });
        acre[1].setSeamFirstVertexIds(new long[]{
                22470656L, 22470720L, 22470784L, 22470848L, 22470912L, 15323124736L, 15323124800L,
                22470976L, 22471040L, 22471104L, 22471168L, 22471232L, 15323908096L, 15323908160L,
                22471296L, 22471360L, 22471424L, 22471488L, 22471552L, 15323909632L, 15323909696L,
                22471616L, 22471680L, 22471744L, 22471808L, 22471872L, 15323911168L, 15323911232L,
                22471936L, 22472000L, 22472064L, 22472128L, 22472192L, 15323977216L, 15323977280L,
                22472256L, 22472320L, 22472384L, 22472448L, 22472512L, 15323203072L, 15323203136L
        });
        acre[1].setZoneFirstVertexIds(new long[]{
                383041536L, 383043584L, 383045632L, 383047680L, 383049728L, 383051776L,
                383053824L, 383055872L, 383057920L, 383059968L, 383062016L, 383064064L,
                383066112L, 383068160L, 383070208L, 383072256L, 383074304L, 383076352L,
                383078400L, 383080448L, 383082496L, 383084544L, 383086592L, 383088640L
        });
        acre[1].setElevation(Float.intBitsToFloat(0));

        acre[2] = new AcreDetail();
        acre[2].setId(1310);
        acre[2].setCenter(new GeoPoint(longBitsToDouble(-4612079323173067243L), longBitsToDouble(-4620746769786686262L)));
        acre[2].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620559302886557850L)),
                new GeoPoint(longBitsToDouble(-4612075578168882598L), longBitsToDouble(-4620481214168053756L)),
                new GeoPoint(longBitsToDouble(-4611949589479497610L), longBitsToDouble(-4620647395159321547L)),
                new GeoPoint(longBitsToDouble(-4611956927398999464L), longBitsToDouble(-4621073838224515698L)),
                new GeoPoint(longBitsToDouble(-4612082856208254173L), longBitsToDouble(-4621210265915791350L)),
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620893629530942424L))
        });
        acre[2].setNeighborIds(new int[]{ 1312, 1309, 1313, 3259, 2925, 1400 });
        acre[2].setAcreTopographyDef(new long[]{
                5299L,
                5295L, 5290L, 5300L, 5276L, 5275L, 5272L,
                5294L, 5285L, 5288L, 5265L, 5273L, 5270L,
                5297L, 5291L, 5292L, 5301L, 5274L, 5271L
        });
        acre[2].setSeamFirstVertexIds(new long[]{
                22466816L, 22466880L, 22466944L, 22467008L, 22467072L, 15323905024L, 15323905088L,
                22467136L, 22467200L, 22467264L, 22467328L, 22467392L, 15323909632L, 15323909696L,
                22467456L, 22467520L, 22467584L, 22467648L, 22467712L, 15323908864L, 15323908928L,
                22467776L, 22467840L, 22467904L, 22467968L, 22468032L, 15323910400L, 15323910464L,
                22468096L, 22468160L, 22468224L, 22468288L, 22468352L, 15323905792L, 15323905856L,
                22468416L, 22468480L, 22468544L, 22468608L, 22468672L, 15323906560L, 15323906624L
        });
        acre[2].setZoneFirstVertexIds(new long[]{
                382943232L, 382945280L, 382947328L, 382949376L, 382951424L, 382953472L,
                382955520L, 382957568L, 382959616L, 382961664L, 382963712L, 382965760L,
                382967808L, 382969856L, 382971904L, 382973952L, 382976000L, 382978048L,
                382980096L, 382982144L, 382984192L, 382986240L, 382988288L, 382990336L
        });
        acre[2].setElevation(Float.intBitsToFloat(0));

        acre[3] = new AcreDetail();
        acre[3].setId(2925);
        acre[3].setCenter(new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4621348084140360882L)));
        acre[3].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612204132379694767L), longBitsToDouble(-4620893629530942424L)),
                new GeoPoint(longBitsToDouble(-4612082856208254173L), longBitsToDouble(-4621210265915791350L)),
                new GeoPoint(longBitsToDouble(-4612130214915333083L), longBitsToDouble(-4621716712640601931L)),
                new GeoPoint(longBitsToDouble(-4612278049844056452L), longBitsToDouble(-4621716712640601931L)),
                new GeoPoint(longBitsToDouble(-4612325408551135361L), longBitsToDouble(-4621210265915791352L))
        });
        acre[3].setNeighborIds(new int[]{ 1310, 3259, 3129, 2915, 1400 });
        acre[3].setAcreTopographyDef(new long[]{
                5338L,
                5275L, 5331L, 5342L, 5340L, 5281L,
                5270L, 5273L, 5329L, 5339L, 5617L,
                5278L, 5277L, 5344L, 5341L, 5282L
        });
        acre[3].setSeamFirstVertexIds(new long[]{
                25567616L, 25567680L, 25567744L, 25567808L, 25567872L, 15323907328L, 15323907392L,
                25567936L, 25568000L, 25568064L, 25568128L, 25568192L, 15323906560L, 15323906624L,
                25568256L, 25568320L, 25568384L, 25568448L, 25568512L, 15323917312L, 15323917376L,
                25568576L, 25568640L, 25568704L, 25568768L, 25568832L, 15323919616L, 15323919680L,
                25568896L, 25568960L, 25569024L, 25569088L, 25569152L, 15323920384L, 15323920448L
        });
        acre[3].setZoneFirstVertexIds(new long[]{
                462323712L, 462325760L, 462327808L, 462329856L, 462331904L,
                462333952L, 462336000L, 462338048L, 462340096L, 462342144L,
                462344192L, 462346240L, 462348288L, 462350336L, 462352384L,
                462354432L, 462356480L, 462358528L, 462360576L, 462362624L
        });
        acre[3].setElevation(Float.intBitsToFloat(0));

        acre[4] = new AcreDetail();
        acre[4].setId(2915);
        acre[4].setCenter(new GeoPoint(longBitsToDouble(-4612401350044068119L), longBitsToDouble(-4621585721633618685L)));
        acre[4].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612529284088578233L), longBitsToDouble(-4621456330338439495L)),
                new GeoPoint(longBitsToDouble(-4612451337360390071L), longBitsToDouble(-4621073838224515698L)),
                new GeoPoint(longBitsToDouble(-4612325408551135361L), longBitsToDouble(-4621210265915791352L)),
                new GeoPoint(longBitsToDouble(-4612278049844056452L), longBitsToDouble(-4621716712640601931L)),
                new GeoPoint(longBitsToDouble(-4612352719405567853L), longBitsToDouble(-4622098451290858844L)),
                new GeoPoint(longBitsToDouble(-4612478007988737249L), longBitsToDouble(-4621974629602383976L))
        });
        acre[4].setNeighborIds(new int[]{ 1409, 1400, 2925, 3129, 2924, 2917 });
        acre[4].setAcreTopographyDef(new long[]{
                5659L,
                5626L, 5622L, 5340L, 5336L, 5674L, 5670L,
                5665L, 5619L, 5617L, 5339L, 5673L, 5672L,
                5660L, 5623L, 5664L, 5661L, 5663L, 5662L
        });
        acre[4].setSeamFirstVertexIds(new long[]{
                25548416L, 25548480L, 25548544L, 25548608L, 25548672L, 15323982592L, 15323982656L,
                25548736L, 25548800L, 25548864L, 25548928L, 25548992L, 15323976448L, 15323976512L,
                25549056L, 25549120L, 25549184L, 25549248L, 25549312L, 15323974912L, 15323974976L,
                25549376L, 25549440L, 25549504L, 25549568L, 25549632L, 15323920384L, 15323920448L,
                25549696L, 25549760L, 25549824L, 25549888L, 25549952L, 15323918848L, 15323918912L,
                25550016L, 25550080L, 25550144L, 25550208L, 25550272L, 15323983360L, 15323983424L
        });
        acre[4].setZoneFirstVertexIds(new long[]{
                461832192L, 461834240L, 461836288L, 461838336L, 461840384L, 461842432L,
                461844480L, 461846528L, 461848576L, 461850624L, 461852672L, 461854720L,
                461856768L, 461858816L, 461860864L, 461862912L, 461864960L, 461867008L,
                461869056L, 461871104L, 461873152L, 461875200L, 461877248L, 461879296L
        });
        acre[4].setElevation(Float.intBitsToFloat(0));

        acre[5] = new AcreDetail();
        acre[5].setId(1409);
        acre[5].setCenter(new GeoPoint(longBitsToDouble(-4612581983350096603L), longBitsToDouble(-4620939465215775959L)));
        acre[5].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612593402511031936L), longBitsToDouble(-4620576001648127352L)),
                new GeoPoint(longBitsToDouble(-4612458675279891925L), longBitsToDouble(-4620647395159321547L)),
                new GeoPoint(longBitsToDouble(-4612451337360390071L), longBitsToDouble(-4621073838224515698L)),
                new GeoPoint(longBitsToDouble(-4612529284088578233L), longBitsToDouble(-4621456330338439495L)),
                new GeoPoint(longBitsToDouble(-4612661899927275020L), longBitsToDouble(-4621329202925711180L)),
                new GeoPoint(longBitsToDouble(-4612717384239428821L), longBitsToDouble(-4620807869650464085L))
        });
        acre[5].setNeighborIds(new int[]{ 1402, 1400, 2915, 2917, 2914, 1405 });
        acre[5].setAcreTopographyDef(new long[]{
                5627L,
                5615L, 5621L, 5626L, 5666L, 5650L, 5638L,
                5612L, 5609L, 5619L, 5665L, 5649L, 5652L,
                5629L, 5628L, 5630L, 5625L, 5624L, 5653L
        });
        acre[5].setSeamFirstVertexIds(new long[]{
                22656896L, 22656960L, 22657024L, 22657088L, 22657152L, 15323977984L, 15323978048L,
                22657216L, 22657280L, 22657344L, 22657408L, 22657472L, 15323973376L, 15323973440L,
                22657536L, 22657600L, 22657664L, 22657728L, 22657792L, 15323975680L, 15323975744L,
                22657856L, 22657920L, 22657984L, 22658048L, 22658112L, 15323976448L, 15323976512L,
                22658176L, 22658240L, 22658304L, 22658368L, 22658432L, 15323981824L, 15323981888L,
                22658496L, 22658560L, 22658624L, 22658688L, 22658752L, 15323980288L, 15323980352L
        });
        acre[5].setZoneFirstVertexIds(new long[]{
                387809280L, 387811328L, 387813376L, 387815424L, 387817472L, 387819520L,
                387821568L, 387823616L, 387825664L, 387827712L, 387829760L, 387831808L,
                387833856L, 387835904L, 387837952L, 387840000L, 387842048L, 387844096L,
                387846144L, 387848192L, 387850240L, 387852288L, 387854336L, 387856384L
        });
        acre[5].setElevation(Float.intBitsToFloat(0));

        acre[6] = new AcreDetail();
        acre[6].setId(1402);
        acre[6].setCenter(new GeoPoint(longBitsToDouble(-4612466461649095938L), longBitsToDouble(-4620404116047153891L)));
        acre[6].setVertices(new GeoPoint[]{
                new GeoPoint(longBitsToDouble(-4612474738822495949L), longBitsToDouble(-4620153734586342152L)),
                new GeoPoint(longBitsToDouble(-4612336663223982022L), longBitsToDouble(-4620235429607916382L)),
                new GeoPoint(longBitsToDouble(-4612332686590506937L), longBitsToDouble(-4620481214168053756L)),
                new GeoPoint(longBitsToDouble(-4612458675279891925L), longBitsToDouble(-4620647395159321547L)),
                new GeoPoint(longBitsToDouble(-4612593402511031936L), longBitsToDouble(-4620576001648127352L)),
                new GeoPoint(longBitsToDouble(-4612605531500237040L), longBitsToDouble(-4620328428641222999L))
        });
        acre[6].setNeighborIds(new int[]{ 1399, 1312, 1400, 1409, 1405, 1403 });
        acre[6].setAcreTopographyDef(new long[]{
                5608L,
                1612L, 5633L, 5606L, 5615L, 5614L, 1619L,
                1614L, 1608L, 5631L, 5609L, 5612L, 1620L,
                1617L, 1613L, 5605L, 5610L, 5613L, 5611L
        });
        acre[6].setSeamFirstVertexIds(new long[]{
                22643456L, 22643520L, 22643584L, 22643648L, 22643712L, 15323205376L, 15323205440L,
                22643776L, 22643840L, 22643904L, 22643968L, 22644032L, 15323203840L, 15323203904L,
                22644096L, 22644160L, 22644224L, 22644288L, 22644352L, 15323977216L, 15323977280L,
                22644416L, 22644480L, 22644544L, 22644608L, 22644672L, 15323972608L, 15323972672L,
                22644736L, 22644800L, 22644864L, 22644928L, 22644992L, 15323973376L, 15323973440L,
                22645056L, 22645120L, 22645184L, 22645248L, 22645312L, 15323974144L, 15323974208L
        });
        acre[6].setZoneFirstVertexIds(new long[]{
                387465216L, 387467264L, 387469312L, 387471360L, 387473408L, 387475456L,
                387477504L, 387479552L, 387481600L, 387483648L, 387485696L, 387487744L,
                387489792L, 387491840L, 387493888L, 387495936L, 387497984L, 387500032L,
                387502080L, 387504128L, 387506176L, 387508224L, 387510272L, 387512320L
        });
        acre[6].setElevation(Float.intBitsToFloat(0));
        return acre;
    }
}
