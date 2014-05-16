package net.venaglia.realms.common.map.things.surface;

import net.venaglia.common.util.ThreadSingletonSource;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;

/**
 * User: ed
 * Date: 4/20/14
 * Time: 4:46 PM
 */
public class ZoneSequencer {

    public static final ThreadSingletonSource<ZoneSequencer> SOURCE = ThreadSingletonSource.forType(ZoneSequencer.class);

    int pointsIndex;
    private double[] pointsData;
    int vectorsIndex;
    private double[] vectorsData;
    int colorsIndex;
    private float[] colorsData;

    public ZoneSequencer load(double[] pointsData, double[] vectorsData, float[] colorsData) {
        this.pointsIndex = 0;
        this.pointsData = pointsData;
        this.vectorsIndex = 0;
        this.vectorsData = vectorsData;
        this.colorsIndex = 0;
        this.colorsData = colorsData;
        return this;
    }

    public void unload() {
        this.pointsIndex = -1;
        this.pointsData = null;
        this.vectorsIndex = -1;
        this.vectorsData = null;
        this.colorsIndex = -1;
        this.colorsData = null;
    }

    public void readPoints(Point[] points) {
        for (int i = pointsIndex, j = 0, k = points.length;j < k; i += 3, j++) {
            double x = pointsData[i];
            double y = pointsData[i + 1];
            double z = pointsData[i + 2];
            points[j] = Double.isNaN(x) ? null : new Point(x, y, z);
            pointsIndex += 3;
        }
    }

    public void readVectors(Vector[] vectors) {
        for (int i = vectorsIndex, j = 0, k = vectors.length;j < k; i += 3, j++) {
            double a = vectorsData[i];
            double b = vectorsData[i + 1];
            double c = vectorsData[i + 2];
            vectors[j] = Double.isNaN(a) ? null : new Vector(a, b, c);
            vectorsIndex += 3;
        }
    }

    public void readColors(Color[] colors) {
        for (int i = colorsIndex, j = 0, k = colors.length; j < k; i += 3, j++) {
            float r = colorsData[i];
            float g = colorsData[i + 1];
            float b = colorsData[i + 2];
            colors[j] = Float.isNaN(r) ? null : new Color(r, g, b);
            colorsIndex += 3;
        }
    }

    public void load(Point[] points, Vector[] vectors, Color[] colors) {
        pointsData = new double[points.length * 3];
        for (int i = 0, j = 0; i < points.length; i++) {
            Point p = points[i];
            if (p == null) {
                pointsData[j++] = Double.NaN;
                pointsData[j++] = Double.NaN;
                pointsData[j++] = Double.NaN;
            } else {
                pointsData[j++] = p.x;
                pointsData[j++] = p.y;
                pointsData[j++] = p.z;
            }
        }
        vectorsData = new double[vectors.length * 3];
        for (int i = 0, j = 0; i < vectors.length; i++) {
            Vector v = vectors[i];
            if (v == null) {
                vectorsData[j++] = Double.NaN;
                vectorsData[j++] = Double.NaN;
                vectorsData[j++] = Double.NaN;
            } else {
                vectorsData[j++] = v.i;
                vectorsData[j++] = v.j;
                vectorsData[j++] = v.k;
            }
        }
        colorsData = new float[colors.length * 3];
        for (int i = 0, j = 0; i < colors.length; i++) {
            Color c = colors[i];
            if (c == null) {
                colorsData[j++] = Float.NaN;
                colorsData[j++] = Float.NaN;
                colorsData[j++] = Float.NaN;
            } else {
                colorsData[j++] = c.r;
                colorsData[j++] = c.g;
                colorsData[j++] = c.b;
            }
        }
    }

    public double[] getPointsData() {
        return pointsData;
    }

    public double[] getVectorsData() {
        return vectorsData;
    }

    public float[] getColorsData() {
        return colorsData;
    }
}
