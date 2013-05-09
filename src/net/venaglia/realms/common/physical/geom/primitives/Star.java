package net.venaglia.realms.common.physical.geom.primitives;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.demo.SingleShapeDemo;

/**
 * User: ed
 * Date: 4/15/13
 * Time: 5:45 PM
 */
public class Star extends ExtrudedShape<Star> {

    public Star(double majorRadius, double minorRadius, int points, double thickness) {
        super(buildControlPoints(majorRadius, minorRadius, points), thickness, null);
    }

    public Star(Point[] points,
                Vector[] normals) {
        super(points, normals, null);
    }

    private static double[] buildControlPoints(double majorRadius, double minorRadius, int points) {
        if (points < 3) {
            throw new IllegalArgumentException("Must specify at least 3 points: " + points);
        }
        int p = points * 2;
        double[] controlPoints = new double[p * 2];
        int k = 0;
        for (int i = 0; i < p; i++) {
            double angle = Math.PI * 2.0 * i / p;
            double r = (i & 1) == 0 ? majorRadius : minorRadius;
            controlPoints[k++] = Math.sin(angle) * r;
            controlPoints[k++] = Math.cos(angle) * r;
        }
        return controlPoints;
    }

    @Override
    protected Star build(Point[] points, Vector[] normals, XForm xForm) {
        return new Star(points, normals);
    }

    public static void main(String[] args) {
        Color offWhite = new Color(1.0f, 0.9f, 0.8f);
        Star star = new Star(0.5, 0.25, 5, 0.25).rotate(Axis.X, 0.85);
        new SingleShapeDemo(star, offWhite, SingleShapeDemo.Mode.SHADED).start();
    }
}
