package net.venaglia.gloo.util.debug;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * User: ed
 * Date: 8/15/14
 * Time: 5:20 PM
 */
public class ProjectedOutputGraph<P> {

    private final OutputGraph out;
    private final OutputGraphProjection<P> projection;

    public ProjectedOutputGraph(OutputGraph out, OutputGraphProjection<P> projection) {
        if (out == null) throw new NullPointerException("out");
        if (projection == null) throw new NullPointerException("projection");
        this.out = out;
        this.projection = projection;
    }

    public void clear() {
        out.clear();
    }

    public void addPoint(Color color, String label, P point) {
        double[] buffer = projectOne(point);
        out.addPoint(color, label, buffer[0], buffer[1]);
    }

    public void addPixels(Color color, P... points) {
        out.addPixels(color, projectAll(points));
    }

    public void addCircle(Color color, String label, P center, int fixedRadius) {
        double[] buffer = projectOne(center);
        out.addCircle(color, label, buffer[0], buffer[1], fixedRadius);
    }

    public void addLine(Color color, P... points) {
        out.addLine(color, projectAll(points));
    }

    public void addArrow(Color color, P... points) {
        out.addArrow(color, projectAll(points));
    }

    public void addPoly(Color color, P... points) {
        out.addPoly(color, projectAll(points));
    }

    public void addLabel(Color color, String label, P point) {
        double[] buffer = projectOne(point);
        out.addLabel(color, label, buffer[0], buffer[1]);
    }

    public void addImage(Color color, BufferedImage image, String label, P point) {
        double[] buffer = projectOne(point);
        out.addImage(color, image, label, buffer[0], buffer[1]);
    }

    public ProjectedRegion<P> addMouseOver(String text) {
        return new MyProjectedRegion(new OutputTextBuffer().append(text));
    }

    public ProjectedRegion<P> addMouseOver(OutputTextBuffer textBuffer) {
        return new MyProjectedRegion(textBuffer);
    }

    private double[] projectOne(P point) {
        double[] buffer = {0,0};
        projection.project(point, buffer, 0);
        return buffer;
    }

    private double[] projectAll(P[] points) {
        double[] buffer = new double[points.length * 2];
        for (int i = 0, j = 0, l = points.length; i < l; i++, j += 2) {
            projection.project(points[i], buffer, j);
        }
        return buffer;
    }

    private Area projectRegion(Iterable<P> points) {
        List<P> pointList = new ArrayList<P>();
        for (P point : points) {
            pointList.add(point);
        }
        if (pointList.size() < 3) {
            return null;
        }
        double[] buffer = new double[pointList.size() * 2];
        for (int i = 0, j = 0, l = pointList.size(); i < l; i++, j += 2) {
            projection.project(pointList.get(i), buffer, j);
        }
        Area region = new Area();
        int x[] = new int[3], y[] = new int[3];
        for (int i = 2, j = 0, l = pointList.size(); i < l; i++, j += 2) {
            for (int a = 0, b = j; a < 3; a++) {
                x[a] = (int)buffer[b++];
                y[a] = (int)buffer[b++];
            }
            region.add(new Area(new Polygon(x, y, 3)));
        }
        return region;
    }

    private class MyProjectedRegion extends ProjectedRegion<P> {

        private final OutputTextBuffer text;

        public MyProjectedRegion(OutputTextBuffer text) {
            super(projection);
            this.text = text;
        }

        @Override
        void onClose() {
            out.addMouseOver(text, toRegion());
        }
    }
}
