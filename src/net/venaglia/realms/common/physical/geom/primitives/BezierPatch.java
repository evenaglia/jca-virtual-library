package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.FlippableShape;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractQuadFacetedType;
import net.venaglia.realms.common.projection.GeometryBuffer;

/**
 * User: ed
 * Date: 3/10/13
 * Time: 1:41 PM
 */
public class BezierPatch extends AbstractQuadFacetedType<BezierPatch> implements FlippableShape<BezierPatch> {

    private static final double SMALL_ENOUGH_TO_BE_0 = 1.0 / 1024.0;
    private static final double BIG_ENOUGH_TO_BE_1 = 1023.0 / 1024.0;

    private final ControlPoints controlPoints;
    private final DetailLevel detailLevel;
    private final Vector[] normals;

    public BezierPatch(Point[] controlPoints, DetailLevel detailLevel) {
        this(new ControlPoints(controlPoints), detailLevel);
    }

    public BezierPatch(ControlPoints controlPoints, DetailLevel detailLevel) {
        super(buildPoints(controlPoints, detailLevel));
        this.controlPoints = controlPoints;
        this.detailLevel = detailLevel;
        this.normals = buildNormals();
    }

    private static Point[] buildPoints(ControlPoints controlPoints, DetailLevel detailLevel) {
        Point[] points = new Point[detailLevel.steps * detailLevel.steps];
        int k = 0;
        for (int j = 0; j < detailLevel.steps; j++) {
            double v = detailLevel.fraction * j;
            for (int i = 0; i < detailLevel.steps; i++) {
                double u = detailLevel.fraction * i;
                points[k++] = controlPoints.samplePoint(u, v);
            }
        }
        return points;
    }

    @Override
    protected void findFacetPoints(int index, FacetBuilder facetBuilder) {
        int quads = detailLevel.steps - 1;
        int i = index % quads;
        int j = index / quads;
        int k = i + j * detailLevel.steps;
        int l = k + detailLevel.steps;
        facetBuilder.usePoints(k, l, l + 1, k + 1);
    }

    public int facetCount() {
        int i = detailLevel.steps - 1;
        return i * i;
    }

    private Vector[] buildNormals() {
        Vector[] normals = new Vector[detailLevel.steps * detailLevel.steps];
        int k = 0;
        for (int j = 0; j < detailLevel.steps; j++) {
            double v = detailLevel.fraction * j;
            for (int i = 0; i < detailLevel.steps; i++) {
                double u = detailLevel.fraction * i;
                normals[k++] = controlPoints.sampleNormal(u, v);
            }
        }
        return normals;
    }

    @Override
    protected BezierPatch build(Point[] points, XForm xForm) {
        return new BezierPatch(controlPoints.build(xForm), detailLevel);
    }

    public BezierPatch flip() {
        return new BezierPatch(controlPoints.flip(), detailLevel);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        int size = detailLevel.steps;
        int l = 0;
        int r = size;
        for (int j = 1; j < size; j++) {
            buffer.start(GeometryBuffer.GeometrySequence.QUAD_STRIP);
            buffer.normal(normals[l]);
            buffer.vertex(points[l]);
            buffer.normal(normals[r]);
            buffer.vertex(points[r]);
            l++; r++;
            for (int i = 1; i < size; i++) {
                buffer.normal(normals[l]);
                buffer.vertex(points[l]);
                buffer.normal(normals[r]);
                buffer.vertex(points[r]);
                l++; r++;
            }
            buffer.end();
        }
    }

    public Vector getNormal(int index) {
        return normals[index];
    }

    private static class ControlPoints {

        private final Point cp00, cp10, cp20, cp30;
        private final Point cp01, cp11, cp21, cp31;
        private final Point cp02, cp12, cp22, cp32;
        private final Point cp03, cp13, cp23, cp33;

        private ControlPoints(Point[] controlPoints) {
            cp00 = controlPoints[0];
            cp01 = controlPoints[1];
            cp02 = controlPoints[2];
            cp03 = controlPoints[3];
            cp10 = controlPoints[4];
            cp11 = controlPoints[5];
            cp12 = controlPoints[6];
            cp13 = controlPoints[7];
            cp20 = controlPoints[8];
            cp21 = controlPoints[9];
            cp22 = controlPoints[10];
            cp23 = controlPoints[11];
            cp30 = controlPoints[12];
            cp31 = controlPoints[13];
            cp32 = controlPoints[14];
            cp33 = controlPoints[15];
        }

        private ControlPoints(Point cp00, Point cp10, Point cp20, Point cp30,
                              Point cp01, Point cp11, Point cp21, Point cp31,
                              Point cp02, Point cp12, Point cp22, Point cp32,
                              Point cp03, Point cp13, Point cp23, Point cp33) {
            this.cp00 = cp00;
            this.cp10 = cp10;
            this.cp20 = cp20;
            this.cp30 = cp30;
            this.cp01 = cp01;
            this.cp11 = cp11;
            this.cp21 = cp21;
            this.cp31 = cp31;
            this.cp02 = cp02;
            this.cp12 = cp12;
            this.cp22 = cp22;
            this.cp32 = cp32;
            this.cp03 = cp03;
            this.cp13 = cp13;
            this.cp23 = cp23;
            this.cp33 = cp33;
        }

        Point samplePoint(double u, double v) {
            double u_ = 1.0 - u;
            double v_ = 1.0 - v;
            return calc(u, u_,
                        calc(v, v_, cp00, cp01, cp02, cp03),
                        calc(v, v_, cp10, cp11, cp12, cp13),
                        calc(v, v_, cp20, cp21, cp22, cp23),
                        calc(v, v_, cp30, cp31, cp32, cp33));
        }

        Vector sampleNormal(double u, double v) {
            double u_ = 1.0 - u;
            double v_ = 1.0 - v;
            boolean uLow = u < 0.5;
            boolean vLow = v < 0.5;
            Vector s = calcVector(u, u_,
                                  calc(v, v_, cp00, cp01, cp02, cp03),
                                  calc(v, v_, cp10, cp11, cp12, cp13),
                                  calc(v, v_, cp20, cp21, cp22, cp23),
                                  calc(v, v_, cp30, cp31, cp32, cp33),
                                  vLow ? cp03 : cp00, vLow ? cp33 : cp30);
            Vector t = calcVector(v, v_,
                                  calc(u, u_, cp00, cp10, cp20, cp30),
                                  calc(u, u_, cp01, cp11, cp21, cp31),
                                  calc(u, u_, cp02, cp12, cp22, cp32),
                                  calc(u, u_, cp03, cp13, cp23, cp33),
                                  uLow ? cp30 : cp00, uLow ? cp33 : cp03);
            return t.cross(s).normalize();
        }

        private Point calc(double p, double p_, Point a, Point b, Point c, Point d) {

            if (p <= SMALL_ENOUGH_TO_BE_0) {
                return a;
            }
            if (p >= BIG_ENOUGH_TO_BE_1) {
                return d;
            }

            double x0 = a.x * p_ + b.x * p;
            double x1 = b.x * p_ + c.x * p;
            double x2 = c.x * p_ + d.x * p;
            double x3 = x0 * p_ + x1 * p;
            double x4 = x1 * p_ + x2 * p;
            double x5 = x3 * p_ + x4 * p;

            double y0 = a.y * p_ + b.y * p;
            double y1 = b.y * p_ + c.y * p;
            double y2 = c.y * p_ + d.y * p;
            double y3 = y0 * p_ + y1 * p;
            double y4 = y1 * p_ + y2 * p;
            double y5 = y3 * p_ + y4 * p;

            double z0 = a.z * p_ + b.z * p;
            double z1 = b.z * p_ + c.z * p;
            double z2 = c.z * p_ + d.z * p;
            double z3 = z0 * p_ + z1 * p;
            double z4 = z1 * p_ + z2 * p;
            double z5 = z3 * p_ + z4 * p;

            return new Point(x5, y5, z5);
        }

        Vector calcVector(double p, double p_, Point a, Point b, Point c, Point d,
                          Point m, Point n) {

            if (a.equals(b) && a.equals(c) && a.equals(d)) {
                return Vector.betweenPoints(m, n);
            }

            if (p <= SMALL_ENOUGH_TO_BE_0) {
                return Vector.betweenPoints(a, b);
            }
            if (p >= BIG_ENOUGH_TO_BE_1) {
                return Vector.betweenPoints(c, d);
            }

            double x0 = a.x * p_ + b.x * p;
            double x1 = b.x * p_ + c.x * p;
            double x2 = c.x * p_ + d.x * p;
            double x3 = x0 * p_ + x1 * p;
            double x4 = x1 * p_ + x2 * p;

            double y0 = a.y * p_ + b.y * p;
            double y1 = b.y * p_ + c.y * p;
            double y2 = c.y * p_ + d.y * p;
            double y3 = y0 * p_ + y1 * p;
            double y4 = y1 * p_ + y2 * p;

            double z0 = a.z * p_ + b.z * p;
            double z1 = b.z * p_ + c.z * p;
            double z2 = c.z * p_ + d.z * p;
            double z3 = z0 * p_ + z1 * p;
            double z4 = z1 * p_ + z2 * p;

            return new Vector(x4 - x3, y4 - y3, z4 - z3);
        }

        ControlPoints build(XForm xForm) {
            return new ControlPoints(
                    xForm.apply(cp00), xForm.apply(cp10), xForm.apply(cp20), xForm.apply(cp30),
                    xForm.apply(cp01), xForm.apply(cp11), xForm.apply(cp21), xForm.apply(cp31),
                    xForm.apply(cp02), xForm.apply(cp12), xForm.apply(cp22), xForm.apply(cp32),
                    xForm.apply(cp03), xForm.apply(cp13), xForm.apply(cp23), xForm.apply(cp33)
            );
        }

        ControlPoints flip() {
            return new ControlPoints(cp00, cp01, cp02, cp03,
                                     cp10, cp11, cp12, cp13,
                                     cp20, cp21, cp22, cp23,
                                     cp30, cp31, cp32, cp33);
        }
    }
}
