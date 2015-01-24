package net.venaglia.realms.builder.terraform;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.realms.builder.terraform.flow.FlowPointData;
import net.venaglia.realms.builder.terraform.flow.FlowQuery;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.GeoPoint;

/**
* User: ed
* Date: 10/14/14
* Time: 9:10 PM
*/
class AcreQuery implements FlowQuery {

    private final AcreDetail acreDetail;
    private final TransferBufferProvider transferBufferProvider;
    private final int transferBufferLeft;
    private final int transferBufferRight;
    private final Point center;
    private final AcreDetail[] neighbors;
    private final Point[] vertices;
    private final double[] vertexDistances;
    private final double[] sectorArea;
    private final double totalArea;
    private final double radius;

    AcreQuery(AcreDetail acreDetail,
              AcreDetail[] neighbors,
              TransferBufferProvider transferBufferProvider,
              int transferBufferLeft,
              int transferBufferRight) {
        this.acreDetail = acreDetail;
        this.transferBufferProvider = transferBufferProvider;
        this.transferBufferLeft = transferBufferLeft;
        this.transferBufferRight = transferBufferRight;
        GeoPoint[] myVertices = acreDetail.getVertices();
        int vertexCount = myVertices.length;
        this.neighbors = neighbors;
        this.vertices = new Point[vertexCount];
        this.sectorArea = new double[vertexCount];
        double totalArea = 0;
        this.vertexDistances = new double[vertexCount];
        double radii[] = new double[vertexCount];
        this.radius = GeoSpec.APPROX_RADIUS_METERS.get();
        center = acreDetail.getCenter().toPoint(radius);
        for (int idx = 0; idx < vertexCount; idx++) {
            Point v = myVertices[idx].toPoint(radius);
            this.vertices[idx] = v;
            radii[idx] = Vector.computeDistance(center, v);
        }
        for (int idx = 0; idx < vertexCount; idx++) {
            int next = (idx + 1) % vertexCount;
            double a = radii[idx];
            double b = radii[next];
            double c = vertexDistances[idx] = Vector.computeDistance(this.vertices[idx], this.vertices[next]);
            totalArea += sectorArea[idx] = triangleArea(a, b, c);
        }
        this.totalArea = totalArea;
        // typical 1 acre hex: side 40.0m, long width 80.0m, short height 69.3m
    }

    AcreQuery(AcreDetail acreDetail,
              AcreDetail[] neighbors) {
        this(acreDetail, neighbors, null, 0, 0);
    }

    public GeoPoint getPoint() {
        return acreDetail.getCenter();
    }

    public double getRadius() {
        return radius;
    }

    public double getScale() {
        return 32; // approximate radius of an acre
    }

    public void processDataForPoint(FlowPointData data) {
        Point p = center.translate(data.getMagnitudeVector(Vector.VECTOR_XFORM_VIEW));
        p = p.scale(radius / Vector.computeDistance(p.x, p.y, p.z));
        setTransferCenter(p);
        int l = neighbors.length;
        double[] spokes = new double[l];
        for (int i = 0; i < l; i++) {
            Point c = vertices[i];
            spokes[i] = Vector.computeDistance(p, c);
        }
        for (int i = 0; i < l; i++) {
            int j = (i + 1) % l;
            double area = triangleArea(spokes[i], spokes[j], vertexDistances[i]);
            double drawAmount = 0.333333333 * (area - sectorArea[i]) / (totalArea - sectorArea[i]);
            setTransferAmount(j, drawAmount, area);
        }
        double samplePressure = Math.max(0.0, data.getPressure() * 0.125 + 0.25);
//        float targetPressure = (float)Math.pow(samplePressure, 1.5);
        float targetPressure = (float)samplePressure;
        float previousPressure = acreDetail.getPressure();
        acreDetail.setPressure((targetPressure * 3.0f + previousPressure * 125.0f) / 128.0f);
    }

    protected void setTransferAmount(int index, double amount, double area) {
        TransferBuffer transferBuffer = transferBufferProvider.getTransferBufferFor(transferBufferLeft,
                                                                                    transferBufferRight,
                                                                                    acreDetail,
                                                                                    neighbors[index]);
        transferBuffer.setTransfer(amount);
        // save the mineral content in the scratchpad buffer using touch()
        if (amount > 0) {
            transferBuffer.getAcre1MineralContent(null).touch();
        } else {
            transferBuffer.getAcre2MineralContent(null).touch();
        }
    }

    protected void setTransferCenter(Point transferCenter) {
        // no-op
    }

    private double triangleArea(double a, double b, double c) {
        double s = (a + b + c) * 0.5;
        return Math.sqrt(s * (s - a) * (s - b) * (s - c));
    }
}
