package net.venaglia.realms.builder.terraform;

import static net.venaglia.gloo.physical.geom.Vector.computeDistance;

import net.venaglia.common.util.extensible.ExtendedPropertyProvider;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.realms.builder.terraform.flow.FlowPointData;
import net.venaglia.realms.builder.terraform.flow.FlowQuery;
import net.venaglia.realms.common.map.GlobalVertexLookup;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.ref.AcreLookup;
import net.venaglia.realms.spec.map.GeoPoint;

/**
* User: ed
* Date: 10/14/14
* Time: 9:10 PM
*/
class AcreQuery implements FlowQuery {

    private static double[] radii = new double[6];

    private final Shared shared;
    private final int acreId;
    private final int transferBufferLeft;
    private final int transferBufferRight;
    private final double cx, cy, cz;
    private final double totalArea;

    AcreQuery(Shared shared,
              int acreId,
              int transferBufferLeft,
              int transferBufferRight) {
        this.shared = shared;
        this.acreId = acreId;
        this.transferBufferLeft = transferBufferLeft;
        this.transferBufferRight = transferBufferRight;
        double totalArea = 0;
        AcreState acreState = shared.withAcreId(acreId);
        DoubleSlice sectorArea = acreState.getSectorArea();
        DoubleSlice vertexDistances = acreState.getVertexDistances();
        long[] acreTopographyDef = acreState.getAcreDetail().getAcreTopographyDef();
        int vertexCount = acreTopographyDef.length == 19 ? 6 : 5;
        double[] array = acreState.getCenterVertex();
        cx = array[0] * shared.radius;
        cy = array[1] * shared.radius;
        cz = array[2] * shared.radius;
        for (int idx = 0; idx < vertexCount; idx++) {
            array = shared.vertexLookup.get((int)acreTopographyDef[idx + 1 + vertexCount], acreState.getArrayView());
            radii[idx] = computeDistance(cx - array[0] * shared.radius,
                                         cy - array[1] * shared.radius,
                                         cz - array[2] * shared.radius);
        }
        for (int idx = 0; idx < vertexCount; idx++) {
            int next = (idx + 1) % vertexCount;
            double[] a1 = acreState.getCornerVertex(idx);
            double[] a2 = acreState.getCornerVertex(next);
            double a = radii[idx];
            double b = radii[next];
            double c = computeDistance(a1[0] - a2[0], a1[1] - a2[1], a1[2] - a2[2]) * shared.radius;
            vertexDistances.set(idx, c);
            double area = triangleArea(a, b, c);
            sectorArea.set(idx, area);
            totalArea += area;
        }
        this.totalArea = totalArea;
        // typical 1 acre hex: side 40.0m, long width 80.0m, short height 69.3m
    }

    AcreQuery(Shared shared, int acreId) {
        this(shared, acreId, 0, 0);
    }

    public GeoPoint getPoint() {
        return GeoPoint.fromPoint(cx, cy, cz);
    }

    public double getRadius() {
        return shared.radius;
    }

    public double getScale() {
        return 32; // approximate radius of an acre
    }

    public void processDataForPoint(FlowPointData data) {
        AcreState acreState = shared.withAcreId(acreId);
        AcreDetail acreDetail = acreState.getAcreDetail();
        DoubleSlice sectorArea = acreState.getSectorArea();
        DoubleSlice vertexDistances = acreState.getVertexDistances();
        double[] array = data.getMagnitudeVector(acreState.getArrayView());
        double px = this.cx + array[0];
        double py = this.cy + array[1];
        double pz = this.cz + array[2];
        double scale = shared.radius / computeDistance(px, py, pz);
        px *= scale;
        py *= scale;
        pz *= scale;
        setTransferCenter(px, py, pz);
        int l = acreState.getNeighborCount();
        double[] spokes = new double[l];
        for (int i = 0; i < l; i++) {
            array = acreState.getCornerVertex(i);
            spokes[i] = computeDistance(px - array[0] * shared.radius,
                                        py - array[1] * shared.radius,
                                        pz - array[2] * shared.radius);
        }
        for (int i = 0; i < l; i++) {
            int j = (i + 1) % l;
            double area = triangleArea(spokes[i], spokes[j], vertexDistances.get(i));
            double drawAmount = 0.333333333 * (area - sectorArea.get(i)) / (totalArea - sectorArea.get(i));
            setTransferAmount(j, drawAmount, area, acreDetail, acreState.getNeighbor(j));
        }
        double samplePressure = Math.sin(data.getPressure() * 10) * 0.125 + 0.25;
        float targetPressure = (float)samplePressure;
        float previousPressure = shared.pressureProvider.getFloat(acreDetail, 0.0f);
        float newPressure = (targetPressure * 3.0f + previousPressure * 125.0f) / 128.0f;
        shared.pressureProvider.setFloat(acreDetail, newPressure);
    }

    protected void setTransferAmount(int index, double amount, double area, AcreDetail acreDetail, AcreDetail neighbor) {
        TransferBuffer transferBuffer = shared.transferBufferProvider
                                              .getTransferBufferFor(transferBufferLeft,
                                                                    transferBufferRight,
                                                                    acreDetail,
                                                                    neighbor);
        transferBuffer.setTransfer(amount);
        // save the mineral content in the scratchpad buffer using touch()
        if (amount > 0) {
            transferBuffer.getAcre1MineralContent(null).touch();
        } else {
            transferBuffer.getAcre2MineralContent(null).touch();
        }
    }

    protected void setTransferCenter(double x, double y, double z) {
        // no-op
    }

    private double triangleArea(double a, double b, double c) {
        double s = (a + b + c) * 0.5;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }

    public static class Shared {
        final AcreLookup acreLookup;
        final GlobalVertexLookup vertexLookup;
        final ExtendedPropertyProvider.FloatProvider<AcreDetail> pressureProvider;
        final TransferBufferProvider transferBufferProvider;
        final double radius;
        final int acres;

        public Shared(AcreLookup acreLookup,
                      GlobalVertexLookup vertexLookup,
                      ExtendedPropertyProvider.FloatProvider<AcreDetail> pressureProvider,
                      TransferBufferProvider transferBufferProvider,
                      double radius,
                      int acres) {
            this.acreLookup = acreLookup;
            this.vertexLookup = vertexLookup;
            this.pressureProvider = pressureProvider;
            this.transferBufferProvider = transferBufferProvider;
            this.radius = radius;
            this.acres = acres;
            this.vertexDistances = new double[this.acres * 6];
            this.sectorArea = new double[this.acres * 6];
        }

        private final double[] vertexDistances;
        private final double[] sectorArea;
        private final ThreadLocal<AcreState> acreState = new ThreadLocal<AcreState>() {
            @Override
            protected AcreState initialValue() {
                return new AcreState(Shared.this);
            }
        };

        AcreState withAcreId(int acreId) {
            return acreState.get().setAcreId(acreId);
        }
    }

    static class AcreState {

        private final AcreLookup acreLookup;
        private final GlobalVertexLookup vertexLookup;
        private final int[] neighborIds = new int[6];
        private final XForm.View<double[]> arrayView;

        private int acreId;
        private AcreDetail acreDetail;
        private int neighborCount;
        private long[] acreTopographyDef;
        private DoubleSlice vertexDistancesSlice;
        private DoubleSlice sectorAreaSlice;

        AcreState(Shared shared) {
            this.acreLookup = shared.acreLookup;
            this.vertexLookup = shared.vertexLookup;
            this.vertexDistancesSlice = new DoubleSlice(shared.vertexDistances);
            this.sectorAreaSlice = new DoubleSlice(shared.sectorArea);
            this.arrayView = new XForm.View<double[]>() {

                                private int which = 0;
                                private double[][] buffer = {{0,0,0},{0,0,0}};

                                @Override
                                public double[] convert(double x, double y, double z, double w) {
                                    double[] buffer = this.buffer[which];
                                    which = 1 - which;
                                    buffer[0] = x;
                                    buffer[1] = y;
                                    buffer[2] = z;
                                    return buffer;
                                }
                            };
        }

        AcreState setAcreId(int acreId) {
            this.acreId = acreId;
            this.acreDetail = acreLookup.get(acreId);
            this.neighborCount = acreDetail.getNeighborIds(neighborIds);
            this.acreTopographyDef = acreDetail.getAcreTopographyDef();
            return this;
        }

        AcreDetail getAcreDetail() {
            return acreDetail;
        }

        DoubleSlice getVertexDistances() {
            return vertexDistancesSlice.init(acreId * 6, neighborCount);
        }

        DoubleSlice getSectorArea() {
            return sectorAreaSlice.init(acreId * 6, neighborCount);
        }

        int getNeighborCount() {
            return neighborCount;
        }

        AcreDetail getNeighbor(int index) {
            assert index >= 0 && index < neighborCount;
            return acreLookup.get(neighborIds[index]);
        }

        double[] getCenterVertex() {
            return vertexLookup.get((int)acreTopographyDef[0], arrayView);
        }

        double[] getCornerVertex(int idx) {
            assert idx >= 0 && idx < neighborCount;
            return vertexLookup.get((int)acreTopographyDef[idx + 1 + neighborCount], arrayView);
        }

        XForm.View<double[]> getArrayView() {
            return arrayView;
        }
    }

    static class DoubleSlice {

        private final double[] backingArray;

        private int start;
        private int size;

        public DoubleSlice(double[] backingArray) {
            this.backingArray = backingArray;
        }

        int size() {
            return size;
        }

        double get(int i) {
            assert i >= 0 && i < size;
            return backingArray[start + i];
        }

        void set(int i, double v) {
            assert i >= 0 && i < size;
            backingArray[start + i] = v;
        }

        DoubleSlice init(int start, int size) {
            this.start = start;
            this.size = size;
            return this;
        }

        @Override
        public String toString() {
            StringBuilder buffer = new StringBuilder(64);
            buffer.append('[');
            for (int i = 0; i < size; i++) {
                if (i > 0) {
                    buffer.append(',');
                }
                buffer.append(String.format("%.3f", backingArray[start + i]));
            }
            buffer.append(']');
            return buffer.toString();
        }
    }
}
