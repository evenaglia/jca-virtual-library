package net.venaglia.realms.builder.terraform;

import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.realms.builder.terraform.flow.FlowObserver;
import net.venaglia.realms.builder.terraform.flow.FlowPointData;
import net.venaglia.realms.builder.terraform.flow.FlowQuery;
import net.venaglia.realms.builder.terraform.flow.FlowQueryInterface;
import net.venaglia.realms.builder.terraform.flow.FlowSimulator;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.chemistry.elements.MaterialElement;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.WorldMapImpl;
import net.venaglia.realms.common.map.world.ref.AcreDetailRef;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 7:12 PM
 */
public class Terraform implements FlowObserver {

    private final Iterable<? extends FlowQuery> queries;
    private final FlowSimulator flowSimulator;

    public Terraform(WorldMap worldMap) {
        this.queries = buildQueries(worldMap);
        long radius = GeoSpec.APPROX_RADIUS_METERS.get();
        int count = (int)(GeoSpec.ACRES.get() - GeoSpec.ONE_SECTOR_ACRES.get());
        this.flowSimulator = new FlowSimulator(radius, count, 60, 10.0);
        flowSimulator.setObserver(this);
    }

    private Iterable<Query> buildQueries(WorldMap worldMap) {
        boolean processingSmallGlobe = Configuration.GEOSPEC.getString("LARGE").equals("SMALL");
        Map<Integer,String> signatures = processingSmallGlobe ? loadSignatures() : null;
        int count = (int)GeoSpec.ACRES.get();
        List<Query> queries = new ArrayList<Query>(count);
        SerializerStrategy<AcreDetail> serializer = AcreDetail.DEFINITION.getSerializer();

        for (int i = 0; i < count; i++) {
            BinaryResource resource = worldMap.getBinaryStore().getBinaryResource(AcreDetail.MIMETYPE, i);
            ByteBuffer byteBuffer = ByteBuffer.wrap(resource.getData());
            AcreDetail acreDetail = serializer.deserialize(byteBuffer);
            assert i == acreDetail.getId();
            if (processingSmallGlobe) {
                String sig = signatures.get(i);
                String fsc = acreDetail.formatForSanityCheck();
                assert fsc.equals(sig);
                signatures.remove(i);
            }
            assert !processingSmallGlobe ||
            queries.add(new Query(acreDetail));
        }
        return queries;
    }

    private Map<Integer,String> loadSignatures() {
        HashMap<Integer,String> signatures = new HashMap<Integer,String>(8192);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader("validate.signatures.txt"));
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                Integer id = Integer.parseInt(line.substring(0, 8), 16);
                assert !signatures.containsKey(id);
                signatures.put(id, line);
            }
        } catch (IOException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    // don't care
                }
            }
            return null;
        }
        return signatures;
    }

    public void run() {
        flowSimulator.run();
    }

    public void frame(FlowQueryInterface queryInterface) {
        queryInterface.query(queries);
    }

    private static class Query implements FlowQuery {

        private final AcreDetail acreDetail;
        private final Point center;
        private final AcreDetail[] neighbors;
        private final Point[] vertices;
        private final double[] vertexDistances;
        private final double[] sectorArea;
        private final double totalArea;

        private long[] mineralStrata;

        private Query(AcreDetail acreDetail) {
            this.acreDetail = acreDetail;
            Collection<AcreDetailRef> neighborRefs = acreDetail.getNeighbors();
            GeoPoint[] myVertices = acreDetail.getVertices();
            int vertexCount = myVertices.length;
            this.neighbors = new AcreDetail[vertexCount];
            this.vertices = new Point[vertexCount];
            this.sectorArea = new double[vertexCount];
            double totalArea = 0;
            this.vertexDistances = new double[vertexCount];
            double radii[] = new double[vertexCount];
            int idx = 0;
            double radius = GeoSpec.APPROX_RADIUS_METERS.get();
            center = acreDetail.getCenter().toPoint(radius);
            for (AcreDetailRef neighborRef : neighborRefs) {
                AcreDetail neighbor = neighborRef.get();
                this.neighbors[idx] = neighbor;
                Point v = myVertices[idx].toPoint(radius);
                this.vertices[idx] = v;
                radii[idx] = Vector.computeDistance(center, v);
                idx++;
            }
            for (idx = 0; idx < vertexCount; idx++) {
                int next = (idx + 1) % vertexCount;
                double a = radii[idx];
                double b = radii[next];
                double c = vertexDistances[idx] = Vector.computeDistance(this.vertices[idx], this.vertices[next]);
                totalArea += sectorArea[idx] = triangleArea(a, b, c);
            }
            this.totalArea = totalArea;
            // typical 1 acre hex: side 40m, long width 80m, short height 70m
        }

        public GeoPoint getPoint() {
            return acreDetail.getCenter();
        }

        public void processDataForPoint(FlowPointData data) {
            Point p = center.translate(data.getMagnitude());
            int l = neighbors.length;
            double[] spokes = new double[l];
            for (int i = 0; i < l;) {
                Point c = vertices[i];
                spokes[i] = Vector.computeDistance(p, c);
            }
            for (int i = 0; i < l;) {
                int j = (i + 1) % l;
                double area = triangleArea(spokes[i], spokes[j], vertexDistances[i]);
                double drawAmount = (area - sectorArea[i]) / (totalArea - sectorArea[i]);
                draw(neighbors[i], acreDetail, drawAmount);
            }
            acreDetail.setRuggedness(acreDetail.getRuggedness() * 127.0f + computeRoughness(data.getPressure()) * 0.0078125f);
        }

        private float computeRoughness(double pressure) {
            double roughness = Math.exp(pressure) * 0.10;
            return (float)roughness;
        }

        private void draw(AcreDetail from, AcreDetail to, double amount) {
            // todo
        }

        private double triangleArea(double a, double b, double c) {
            double s = (a + b + c) * 0.5;
            return Math.sqrt(s * (s - a) * (s - b) * (s - c));
        }
    }

    private static class MineralContent {

        private static final ThreadLocal<MineralContent[]> THREAD_LOCAL = new ThreadLocal<MineralContent[]>() {
            @Override
            protected MineralContent[] initialValue() {
                return new MineralContent[] {
                        new MineralContent(),
                        new MineralContent(),
                        new MineralContent(),
                        new MineralContent()
                };
            }
        };
        private static final long[] MASKS = {
                0x0000000000000FFFl,
                0x0000000000FFF000l,
                0x0000000FFF000000l,
                0x0000FFF000000000l,
                0x0FFF000000000000l
        };
        private static final long[] UNMASKS = {
                0xFFFFFFFFFFFFF000l,
                0xFFFFFFFFFF000FFFl,
                0xFFFFFFF000FFFFFFl,
                0xFFFF000FFFFFFFFFl,
                0xF000FFFFFFFFFFFFl
        };
        private static final int[] STARTS = {
                0, 12, 24, 36, 48
        };

        private MineralContent() { }

        private long[] buffer;
        private int index;

        public float getPercent(MaterialElement element) {
            return getPart(element.ordinal()) / 4096.0f;
        }

        public void setPercent(MaterialElement element, float pct) {
            setPart(element.ordinal(), Math.round(pct * 4096.0f));
        }

        public float getTotal() {
            long l = buffer[index];
            int sum = 0;
            sum += l & 0xFFF;
            l >>= 12;
            sum += l & 0xFFF;
            l >>= 12;
            sum += l & 0xFFF;
            l >>= 12;
            sum += l & 0xFFF;
            l >>= 12;
            sum += l & 0xFFF;
            return sum / 4096.0F;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        private int getPart(int part) {
            return (int)((buffer[index] & MASKS[part]) >> STARTS[part]);
        }

        private void setPart(int part, int value) {
            long update = (value & 0xFFF) << STARTS[part];
            buffer[index] = (buffer[index] & UNMASKS[part]) | update;
        }

        public static MineralContent get(long[] buffer, int index, int instance) {
            MineralContent mineralContent = THREAD_LOCAL.get()[instance];
            mineralContent.buffer = buffer;
            mineralContent.index = index;
            return mineralContent;
        }
    }

    public static void main(String[] args) {
        new Terraform(new WorldMapImpl()).run();
    }
}
