package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.demo.SingleShapeDemo;

/**
 * User: ed
 * Date: 4/15/13
 * Time: 9:09 PM
 */
public class Cog extends ExtrudedShape<Cog> {

    private static final double BEVEL_1 = Math.PI * 0.25;
    private static final double BEVEL_2 = Math.PI * 0.75;

    public final int teeth;

    public Cog(int teeth, double radius, double thickness, DetailLevel detailLevel) {
        super(buildControlPoints(teeth, radius), thickness, detailLevel);
        this.teeth = teeth;
    }

    private Cog(Point[] points,
                Vector[] normals,
                DetailLevel detailLevel,
                int teeth) {
        super(points, normals, detailLevel);
        this.teeth = teeth;
    }

    private static double[] buildControlPoints(int teeth, double radius) {
        if (teeth < 6) {
            throw new IllegalArgumentException("A cog needs at least 6 teeth: " + teeth);
        }
        if (teeth > 128) {
            throw new IllegalArgumentException("A cog cannot have more than 128 teeth: " + teeth);
        }
        double r1 = Math.PI * radius * 0.6667 / teeth;
        double r2 = radius - r1;
        r1 *= 0.125;
        double r3 = r1 * radius / r2;
        double toothSpacing = Math.PI * 2.0 / teeth;
        double ta = toothSpacing * 0.1875;
        double tb = toothSpacing * 0.3125;
        double[] controlPoints = new double[3 * 4 * 2 * teeth];
        int k = 0;
        for (int i = 0; i < teeth; i++) {
            double t0 = toothSpacing * i;
            double t1 = t0 - tb;
            double t2 = t0 - ta;
            double t3 = t0 + ta;
            double t4 = t0 + tb;
            k = addControlPoints(controlPoints, k, t1, t1 + BEVEL_1, r2, r1);
            k = addControlPoints(controlPoints, k, t2, t2 + BEVEL_1, radius, r3);
            k = addControlPoints(controlPoints, k, t3, t3 + BEVEL_2, radius, r3);
            k = addControlPoints(controlPoints, k, t4, t4 + BEVEL_2, r2, r1);
        }
//        net.venaglia.gloo.util.debug.OutputGraph out =
//                new net.venaglia.gloo.util.debug.OutputGraph("cog", 1024, 0, 0, 768.0);
//        for (int i = 0; i <= controlPoints.length; i += 6) {
//            out.addLine(java.awt.Color.DARK_GRAY,
//                        controlPoints[i % controlPoints.length],
//                        controlPoints[(i + 1) % controlPoints.length],
//                        controlPoints[(i + 6) % controlPoints.length],
//                        controlPoints[(i + 7) % controlPoints.length]);
//        }
//        for (int i = 2; i < controlPoints.length; i += 6) {
//            out.addLine(java.awt.Color.RED, controlPoints[i - 2], controlPoints[i - 1], controlPoints[i], controlPoints[i + 1]);
//            out.addPoint(java.awt.Color.RED, null, controlPoints[i], controlPoints[i + 1]);
//        }
//        for (int i = 4; i < controlPoints.length; i += 6) {
//            out.addLine(java.awt.Color.RED, controlPoints[i], controlPoints[i + 1], controlPoints[(i + 2) % controlPoints.length], controlPoints[(i + 3) % controlPoints.length]);
//            out.addPoint(java.awt.Color.RED, null, controlPoints[i], controlPoints[i + 1]);
//        }
//        for (int i = 0; i < controlPoints.length; i += 6) {
//            out.addPoint(java.awt.Color.WHITE, String.valueOf(i / 6 + 1), controlPoints[i], controlPoints[i + 1]);
//        }
        return controlPoints;
    }

    private static int addControlPoints(double[] controlPoints, int k, double angle, double bevelAngle, double radius, double r) {
        double x = Math.sin(angle) * radius;
        double y = Math.cos(angle) * radius;
        double i = Math.sin(bevelAngle) * r;
        double j = Math.cos(bevelAngle) * r;
        int l = controlPoints.length;
        controlPoints[(k + l - 2) % l] = x - i;
        controlPoints[(k + l - 1) % l] = y - j;
        //noinspection PointlessArithmeticExpression
        controlPoints[(k + 0) % l] = x;
        controlPoints[(k + 1) % l] = y;
        controlPoints[(k + l + 2) % l] = x + i;
        controlPoints[(k + l + 3) % l] = y + j;
        return k + 6;
    }

    @Override
    protected Cog build(Point[] points, Vector[] normals, XForm xForm) {
        return new Cog(points, normals, detailLevel, teeth);
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Cog cog = new Cog(6, 0.5, 0.25, DetailLevel.LOW).rotate(Axis.X, 0.85);
//        Cog cog = new Cog(6, 0.5, 0.25, BezierPatch.DetailLevel.LOW).rotate(Axis.X, 0.85);
//        Cog cog = new Cog(6, 0.5, 0.25, BezierPatch.DetailLevel.MEDIUM).rotate(Axis.X, 0.85);
        new SingleShapeDemo(cog, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
