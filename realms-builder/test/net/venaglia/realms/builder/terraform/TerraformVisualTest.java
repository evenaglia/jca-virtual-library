package net.venaglia.realms.builder.terraform;

import static java.awt.Color.*;
import static net.venaglia.realms.builder.terraform.flow.FlowSimulator.TectonicDensity.LOW;

import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.common.util.Visitor;
import net.venaglia.common.util.extensible.ExtendedPropertyKey;
import net.venaglia.common.util.extensible.ExtensibleObjectRegistry;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.util.debug.OutputGraph;
import net.venaglia.gloo.util.debug.ProjectedOutputGraph;
import net.venaglia.realms.builder.terraform.flow.FlowPointContribution;
import net.venaglia.realms.builder.terraform.flow.FlowPointData;
import net.venaglia.realms.builder.terraform.flow.FlowQuery;
import net.venaglia.realms.builder.terraform.flow.FlowSimulator;
import net.venaglia.realms.builder.terraform.flow.Fragment;
import net.venaglia.realms.builder.terraform.sets.AcreDetailExtendedPropertyProvider;
import net.venaglia.realms.common.map.GlobalVertexLookup;
import net.venaglia.realms.common.map.VertexStore;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.ref.AcreLookup;
import net.venaglia.realms.common.map.world.topo.VertexChangeEventBus;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.spec.map.RelativeCoordinateReference;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * User: ed
 * Date: 10/17/14
 * Time: 8:31 AM
 */
public class TerraformVisualTest {

    private final int count;
    private final Set<String> debugParts;

    private Random random;
    private ProjectedOutputGraph<GeoPoint> debug;
    private double nominalFragmentRadius = 0;
    private double globeRadius = 0;
    private AcreDetail testAcre;
    private AcreDetail[] neighbors;
    private MyAcreQuery query;
    private FlowPointData flowPointData;
    private AcreDetailExtendedPropertyProvider.FloatProvider pressureProvider;

    public TerraformVisualTest(long seed, String parts) {
        count = 4322;
        debugParts = new HashSet<String>(Arrays.asList(parts.split(",")));
        random = new Random(seed);
        globeRadius = 2043;
        nominalFragmentRadius = Math.sqrt((globeRadius * globeRadius * 1.333333333333333) / count);
    }

    public void init() {
        AcreDetail[] samples = AcreDetail.getSampleAcreDetail();
        testAcre = samples[0];

        if (this.debug == null) {
            OutputGraph debug = new OutputGraph("Acre Flow", 1024, 0.5, 0.5, 400.0);
            debug.onClose(new Runnable() {
                public void run() {
                    System.exit(0);
                }
            });
            this.debug = debug.project(RelativeCoordinateReference.forAcre(testAcre).scale(1.0));
        }

        Map<Integer,AcreDetail> neighborsById = new HashMap<Integer,AcreDetail>();
        for (int i = 1; i < samples.length; i++) {
            AcreDetail sample = samples[i];
            neighborsById.put(sample.getId(), sample);
        }
        int[] neighborIds = new int[6];
        neighbors = new AcreDetail[testAcre.getNeighborIds(neighborIds)];
        for (int i = 0; i < neighbors.length; i++) {
            neighbors[i] = neighborsById.get(neighborIds[i]);
        }
//        // todo: this should not need to be, neighbors should already be in vertex order
//        neighbors = reorder(neighbors, testAcre.getVertices());

        ExtendedPropertyKey.FloatKey pressureKey = new ExtendedPropertyKey.FloatKey("pressure");
        pressureProvider = new AcreDetailExtendedPropertyProvider.FloatProvider(pressureKey);
        ExtensibleObjectRegistry.register(pressureProvider);
        AcreQuery.Shared shared = new AcreQuery.Shared(new MyAcreLookup(samples),
                                                       new MyGlobalVertexLookup(samples),
                                                       new AcreDetailExtendedPropertyProvider.FloatProvider(new ExtendedPropertyKey.FloatKey("pressure")),
                                                       null,
                                                       4086,
                                                       4322);
        query = new MyAcreQuery(shared, testAcre.getId());
        flowPointData = synthesizeFlowPointData(query.getPoint());
        query.processDataForPoint(flowPointData);
    }

    private AcreDetail[] reorder(AcreDetail[] neighbors, GeoPoint[] myVertices) {
        assert neighbors.length == myVertices.length;
        int length = neighbors.length;
        AcreDetail[] results = new AcreDetail[length];
        int usedMask = 0;
        int setMask = 0;
        for (int i = 0; i < length; i++) {
            GeoPoint a = myVertices[i];
            GeoPoint b = myVertices[(i + 1) % length];
            for (int j = 0; j < length; j++) {
                if (isThisNeighbor(a, b, neighbors[j])) {
                    results[i] = neighbors[j];
                    usedMask |= 1 << j;
                    setMask |= 1 << i;
                    break;
                }
            }
            assert results[i] != null;
        }
        assert usedMask == setMask;
        return results;
    }

    private boolean isThisNeighbor(GeoPoint a, GeoPoint b, AcreDetail neighbor) {
        assert !a.equals(b);
        int ixA = -1;
        int ixB = -1;
        GeoPoint[] vertices = neighbor.getVertices();
        for (int i = 0, l = vertices.length; i < l; i++) {
            GeoPoint vertex = vertices[i];
            if (vertex.equals(a)) {
                if (ixB >= 0) {
                    return true;
                }
                ixA = i;
            } else if (vertex.equals(b)) {
                if (ixA >= 0) {
                    return true;
                }
                ixB = i;
            }
        }
        return false;
    }

    private FlowPointData synthesizeFlowPointData(final GeoPoint point) {
        final FlowPointData[] result = {null};
        FlowSimulator flowSimulator = new FlowSimulator(25, 8000, 1000, 10, LOW, random.nextLong(), true);
        System.out.println("Simulating flow...");
        flowSimulator.startThenStopOnceStable();
        flowSimulator.runOneQuery(new FlowQuery() {
            public GeoPoint getPoint() {
                return point;
            }

            public double getRadius() {
                return globeRadius;
            }

            public double getScale() {
                return 32;
            }

            public void processDataForPoint(FlowPointData data) {
                result[0] = data.immutableCopy();
            }
        });
        assert result[0] != null;
        assert result[0].getFlowPointContributions() != null;
        return result[0];
    }

//    private Vector randomVector(double scale) {
//        return new Vector(random.nextGaussian() * scale,
//                          random.nextGaussian() * scale,
//                          random.nextGaussian() * scale);
//    }

    public void show() {
        debug.clear();
        if (debugParts.contains("transfer-lines")) {
            GeoPoint transferCenter = GeoPoint.fromPoint(query.getTransferCenter());
            for (int i = 0; i < neighbors.length; i++) {
                debug.addLine(MAGENTA, transferCenter, testAcre.getVertices()[i]);
            }
            // todo: compute midpoints between the acre center and each neighbor's center
            // todo: draw arrows showing the amount of transfer between the neighboring acres
        }

        if (debugParts.contains("acres")) {
            for (int i = 0; i < neighbors.length; i++) {
                AcreDetail neighbor = neighbors[i];
                debug.addLine(LIGHT_GRAY, close(neighbor.getVertices()));
                debug.addLabel(LIGHT_GRAY, String.format("neighbor %d\nid: %d", i, neighbor.getId()), neighbor.getCenter());
            }
            debug.addLine(YELLOW, close(testAcre.getVertices()));
        }
        if (debugParts.contains("vertices")) {
            for (int i = 0, j = 1, l = neighbors.length; i < l; i++, j = (j + 1) % l) {
                debug.addPoint(YELLOW, String.format("%d", i), testAcre.getVertices()[i]);
            }
        }

        if (debugParts.contains("contributions")) {
            double total = 0.0;
            for (FlowPointContribution contribution : flowPointData.getFlowPointContributions()) {
                total += contribution.getContribution();
            }
            int flowPointSeq = 1;
            for (FlowPointContribution contribution : flowPointData.getFlowPointContributions()) {
                Fragment fragment = contribution.getFragment();
                Point center = fragment.getCenterXYZ(Point.POINT_XFORM_VIEW);
                GeoPoint c = GeoPoint.fromPoint(center);
                debug.addPoint(BLUE, String.format("Flow %d", flowPointSeq++), c);
                drawArrow(CYAN, c, contribution.getVectorIJK(Vector.VECTOR_XFORM_VIEW).scale(globeRadius));
                double fraction = contribution.getContribution() / total;
                debug.addCircle(CYAN, String.format("%.4f%%", fraction * 100.0), c, (int)Math.round(fraction * 32));
            }
        }

        GeoPoint neighborsCenter = neighbors[0].getCenter();
        GeoPoint point = flowPointData.getGeoPoint();
        if (debugParts.contains("pressure")) {
            double m = flowPointData.getMagnitude();
            double p = flowPointData.getPressure();
            debug.addCircle(GREEN, null, point, (int)Math.round(m));
            debug.addLabel(GREEN, String.format("m=%.2f", m), GeoPoint.midPoint(point, neighborsCenter, 0.555));
            debug.addCircle(RED, null, point, (int)Math.round(m * Math.pow(2.0, p)));
            debug.addLabel(RED, String.format("p=%.2f", p), GeoPoint.midPoint(point, neighborsCenter, 0.525));
        }
        if (debugParts.contains("query")) {
            debug.addPoint(WHITE, "Query", point);
            drawArrow(WHITE, point, flowPointData.getMagnitudeVector(Vector.VECTOR_XFORM_VIEW));
        }

        if (debugParts.contains("transfer-amounts")) {
            for (int i = 0, j = 1, l = neighbors.length; i < l; i++, j = (j + 1) % l) {
                double transferAmount = query.getTransferAmount(j);
                double area = query.getArea(j);
                AcreDetail neighbor = neighbors[i];
                Color color = transferAmount == 0 ? YELLOW : transferAmount < 0 ? RED : GREEN;
                GeoPoint midpoint = GeoPoint.midPoint(query.getPoint(), neighbor.getCenter(), 0.4);
                String label = String.format("%d xfer:%d%%\narea:%dm²",
                                             i,
                                             Math.abs(Math.round(transferAmount * 100.0)),
                                             Math.round(area));
                debug.addLabel(color, label, midpoint);
            }
            // todo: draw arrows showing the amount of transfer between the neighboring acres
        }
        // todo: visualize the flow transfer
        // todo: draw computed sum flow information
    }

    private GeoPoint[] close(GeoPoint[] vertices) {
        if (vertices.length < 3) {
            return vertices;
        }
        GeoPoint[] points = new GeoPoint[vertices.length + 1];
        System.arraycopy(vertices, 0, points, 0, vertices.length);
        points[vertices.length] = vertices[0];
        return points;
    }

    private void drawArrow(Color color, GeoPoint basePoint, Vector... vectors) {
        GeoPoint[] points = new GeoPoint[vectors.length + 1];
        int j = 0;
        PointOnSphere pos = new PointOnSphere(basePoint, globeRadius);
        points[j++] = pos.current();
        for (Vector vector : vectors) {
            pos.translate(vector);
            points[j++] = pos.current();
        }
        debug.addArrow(color, points);
    }

    public static void main(String[] args) throws Exception {
        long seed = 1234L;
        TerraformVisualTest terraformVisualTest = new TerraformVisualTest(seed, "acres,contributions,vertices,pressure,query,transfer-lines,transfer-amounts");
//        for (;;) {
            terraformVisualTest.init();
            terraformVisualTest.show();
            Thread.sleep(1000L);
//        }
    }

    public static class PointOnSphere {

        private final double radius;

        private GeoPoint geoPoint;
        private Point p;

        public PointOnSphere(GeoPoint geoPoint, double radius) {
            this.geoPoint = geoPoint;
            this.radius = radius;
            p = geoPoint.toPoint(radius);
        }

        public GeoPoint current() {
            if (geoPoint == null) {
                geoPoint = GeoPoint.fromPoint(p);
            }
            return geoPoint;
        }

        public void translate(Vector vector) {
            Point point = p.translate(vector);
            double m = radius / Vector.computeDistance(point.x, point.y, point.z);
            p = point.scale(m);
            geoPoint = null;
        }
    }

    private static class MyAcreQuery extends AcreQuery {

        private final double[] transferAmounts;
        private final double[] areas;
        private Point transferCenter;

        public MyAcreQuery(Shared shared, int acreId) {
            super(shared, acreId);
            AcreDetail testAcre = shared.acreLookup.get(acreId);
            transferAmounts = new double[testAcre.getVertices().length];
            areas = new double[testAcre.getVertices().length];
        }

        public double getTransferAmount(int index) {
            return transferAmounts[index];
        }

        public double getArea(int index) {
            return areas[index];
        }

        @Override
        protected void setTransferAmount(int index,
                                         double amount,
                                         double area,
                                         AcreDetail acreDetail,
                                         AcreDetail neighbor) {
            transferAmounts[index] = amount;
            areas[index] = area;
        }

        public Point getTransferCenter() {
            return transferCenter;
        }

        @Override
        protected void setTransferCenter(double x, double y, double z) {
            this.transferCenter = new Point(x, y, z);
        }
    }

    private static class MyAcreLookup implements AcreLookup {

        private final Map<Integer,AcreDetail> acreDetailMap = new TreeMap<>();

        private MyAcreLookup(AcreDetail[] acreDetails) {
            for (AcreDetail acre : acreDetails) {
                acreDetailMap.put(acre.getId(), acre);
            }
        }

        @Override
        public AcreDetail get(int key) {
            return acreDetailMap.get(key);
        }
    }

    private static class MyGlobalVertexLookup extends GlobalVertexLookup {

        private final Map<Integer,Point> pointMap = new TreeMap<>();

        private MyGlobalVertexLookup(AcreDetail[] acreDetails) {
            super(new VertexStore() {
                @Override
                public void read(RangeBasedLongSet vertexIds, VertexConsumer consumer) {
                    // no-op
                }

                @Override
                public VertexWriter write(RangeBasedLongSet vertexIds) {
                    return null;
                }

                @Override
                public void flushChanges() {
                    // no-op
                }

                @Override
                public VertexChangeEventBus getChangeEventBus() {
                    return null;
                }

                @Override
                public void visitVertexIds(RangeBasedLongSet set, Visitor<Long> visitor) {
                    // no-op
                }
            });
            for (AcreDetail acre : acreDetails) {
                long[] acreTopographyDef = acre.getAcreTopographyDef();
                pointMap.put((int)acreTopographyDef[0], acre.getCenter().toPoint(1.0));
                for (int i = 0, l = acreTopographyDef.length == 19 ? 6 : 5, j = l + 1; i < l; i++, j++) {
                    pointMap.put((int)acreTopographyDef[j], acre.getVertices()[i].toPoint(1.0));
                }
            }
        }

        @Override
        public Point get(int index) {
            return pointMap.get(index);
        }

        @Override
        public <V> V get(int index, XForm.View<V> view) {
            Point p = get(index);
            return view.convert(p.x, p.y, p.z, 1);
        }
    }
}
