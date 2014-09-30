package net.venaglia.realms.builder.geoform;

import static net.venaglia.realms.builder.geoform.SectorDebugger.AcreVertexCategory.*;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.debug.OutputGraph;
import net.venaglia.gloo.util.debug.OutputTextBuffer;
import net.venaglia.gloo.util.debug.ProjectedOutputGraph;
import net.venaglia.gloo.util.impl.OctreeMap;
import net.venaglia.realms.spec.map.Acre;
import net.venaglia.realms.spec.map.GeoPoint;
import net.venaglia.realms.spec.map.Sector;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * User: ed
 * Date: 7/8/14
 * Time: 5:22 PM
 */
public class SectorDebugger {

    public final Sector sector;
    public final VertexCountAccessor vca;
    public final ProjectedOutputGraph<GeoPoint> debug;

    private final SpatialMap<Stat> stats;
    private final Set<AcreVertexCategory> categories;
    private Color[] rainbow;
    private Color[] rainbow2;

    public SectorDebugger(Sector sector, VertexCountAccessor vca, AcreVertexCategory... categories) {
        this.sector = sector;
        this.vca = vca;
        OutputGraph debug = new OutputGraph("Sector debugger: " + sector, new Dimension(1280, 1024), 0.5, 0.5, 750.0);
        this.debug = debug.project(sector.getRelativeCoordinateReference());
        this.stats = new OctreeMap<Stat>(new BoundingSphere(Point.ORIGIN, 1024.0));
        this.rainbow = new Color[12];
        this.rainbow2 = new Color[12];
        for (int i = 0; i < 12; i++) {
            this.rainbow[i] = new Color(colorSine(i*2, 0.0), colorSine(i*2, 1.0), colorSine(i*2, 2.0), 0.5f);
            this.rainbow2[i] = this.rainbow[i].darker().darker().darker().darker();
        }
        this.categories = EnumSet.noneOf(AcreVertexCategory.class);
        Collections.addAll(this.categories, categories);
    }

    public void showIt() {
        debug.addLine(Color.MAGENTA.darker().darker().darker(), sector.points[0], sector.points[1], sector.points[2], sector.points[0]);
        for (Acre acre : sector.getInnerAcres()) {
            debug.addLine(Color.GRAY, acre.points[0], acre.points[1], acre.points[2], acre.points[3], acre.points[4], acre.points[5], acre.points[0]);
        }
        for (Acre acre : sector.getSharedAcres()) {
            if (acre.points.length == 5) {
                debug.addLine(Color.GRAY, acre.points[0], acre.points[1], acre.points[2], acre.points[3], acre.points[4], acre.points[0]);
            } else {
                debug.addLine(Color.GRAY, acre.points[0], acre.points[1], acre.points[2], acre.points[3], acre.points[4], acre.points[5], acre.points[0]);
            }
        }
        int[] counts = new int[6];

        showStats(EnumSet.of(INNER), true);
        showStats(EnumSet.of(ACRE_SEAMS, ZONE_SEAMS), true);
        showStats(EnumSet.of(SPOKE_MIDPOINTS, EDGE_MIDPOINTS, CORNERS, CENTER), false);

        for (SpatialMap.Entry<Stat> entry : stats) {
            counts[Math.min(entry.get().dataPoints.length, 5)]++;
        }
        String[] arity = "never,once,twice,3 times,4 times,5 times".split(",");
        for (int i = 1; i < counts.length; i++) {
            System.out.printf("%d %s occurring %s\n", counts[i], counts[i] == 1 ? "point" : "points", arity[i]);
        }
    }

    private void showStats(Set<AcreVertexCategory> categories, boolean pixel) {
        categories.retainAll(this.categories);
        if (categories.isEmpty()) {
            return;
        }
        for (SpatialMap.Entry<Stat> entry : stats) {
            Stat stat = entry.get();
            int n = stat.dataPoints.length;
            int m = Math.min(n, 5);
            if (categories.contains(stat.getCategory())) {
                if (pixel) {
                    debug.addPixels(rainbow2[m], stat.geoPoint);
                } else {
                    debug.addPoint(rainbow[m], String.valueOf(n), stat.geoPoint);
                    if (stat.dataPoints.length > 0) {
                        long expectedVertexId = vca.getVertexId(stat.geoPoint);
                        if (expectedVertexId == -1L) {
                            debug.addCircle(Color.RED, null, stat.geoPoint, 6);
                        }
                        boolean success = true;
                        for (DataPoint dataPoint : stat.dataPoints) {
                            if (dataPoint.vertexId != expectedVertexId) {
                                success = false;
                                break;
                            }
                        }
                        if (!success) {
                            OutputTextBuffer text = new OutputTextBuffer();
                            text.setColor(Color.YELLOW).append("Expecting: ").setColor(Color.WHITE).append(expectedVertexId);
                            for (DataPoint dataPoint : stat.dataPoints) {
                                text.setColor(Color.YELLOW).append("\n    Found: ");
                                text.setColor(dataPoint.vertexId == expectedVertexId ? Color.GREEN.darker() : Color.RED);
                                text.append(dataPoint.vertexId);
                            }
                            debug.addCircle(Color.RED, null, stat.geoPoint, 4);
                            debug.addMouseOver(text).fromLastElement();
                        } else {
                            debug.addCircle(Color.GREEN, null, stat.geoPoint, 4);
                        }
                    }
                }
            }
        }
    }

    private float colorSine(int depth, double part) {
        return (float)(Math.sin((depth * -1.0 + Math.PI / 2.0) + Math.PI * part * 0.6666666667) * 0.5 + 0.5);
    }

    public void addVertex(AcreVertexCategory category, Point point, long id, Object ref, String format, Object... args) {
        assert point != null;
        GeoPoint geoPoint = GeoPoint.fromPoint(point);
        Stat stat = stats.get(point, 0.00005);
        if (stat == null) {
            stat = new Stat(geoPoint, point);
            stats.add(stat, point);
        }
        stat.consume(id, ref, String.format(format, args), category);
    }

    public void addVertices(AcreVertexCategory category, Point[] points, long id, Object ref, VertexNameSequence sequence) {
        for (Point point : points) {
            assert point != null;
            GeoPoint geoPoint = GeoPoint.fromPoint(point);
            Stat stat = stats.get(point, 0.00005);
            if (stat == null) {
                stat = new Stat(geoPoint, point);
                stats.add(stat, point);
            }
            stat.consume(id, ref, sequence.next(), category);
        }
    }

    public enum AcreVertexCategory {
        CENTER, CORNERS, EDGE_MIDPOINTS, SPOKE_MIDPOINTS, ACRE_SEAMS, ZONE_SEAMS, INNER
    }

    public void addAcreVertices(Acre acre) {
        Point[] points = AcreSeamSeq.buildAcrePoints(acre);
        if (categories.contains(CENTER)) {
            addVertex(CENTER, points[0], acre.topographyDef[0], acre, "ctr");
        }
        int l = acre.points.length;
        if (categories.contains(CORNERS)) {
            for (int i = 0, j = 2; i < l; i++, j += 3) {
                addVertex(CORNERS, points[j], acre.topographyDef[i + l + 1], acre, "cor[%d]", i);
            }
        }
        if (categories.contains(EDGE_MIDPOINTS)) {
            for (int i = 0, j = 3; i < l; i++, j += 3) {
                addVertex(EDGE_MIDPOINTS, points[j], acre.topographyDef[i + 1], acre, "mid[%d]", i);
            }
        }
        if (categories.contains(SPOKE_MIDPOINTS)) {
            for (int i = 0, j = 1; i < l; i++, j += 3) {
                addVertex(SPOKE_MIDPOINTS, points[j], acre.topographyDef[i + l * 2 + 1], acre, "spo[%d]", i);
            }
        }
        boolean doAcreSeams = categories.contains(ACRE_SEAMS);
        boolean doZoneSeams = categories.contains(ZONE_SEAMS);
        if (doAcreSeams || doZoneSeams) {
            for (int i = 0, j = -2, k = acre.seamStartVertexIds.length; i < k; i++) {
                j = addSeamVertices(acre, points, doAcreSeams, doZoneSeams, i, j);
            }
        }
        if (categories.contains(INNER)) {
            for (int i = 0, j = -2, k = acre.zoneStartVertexIds.length; i < k; i++) {
                j = addZoneVertices(acre, points, i, j);
            }
        }
    }

    private int addSeamVertices(Acre acre, Point[] points, boolean doAcreSeams, boolean doZoneSeams, int i, int j) {
        int segment = i % 7;
        if (segment == 0) {
            j += 3;
        }
        if (!(segment < 5 ? doZoneSeams : doAcreSeams)) {
            return j;
        }
        int a = 0, b = 0;
        switch (segment) {
            case 0: a = 0;     b = j;     break;
            case 1: a = j;     b = j + 1; break;
            case 2: a = j;     b = j + 2; break;
            case 3: a = j;     b = j + 3; break;
            case 4: a = j + 2; b = j + 3; break;
            case 5: a = j + 1; b = j + 2; break;
            case 6: a = j + 2; b = j + 4; break;
        }
        while (b >= points.length) {
            b -= points.length - 1;
        }
        Point[] midpoints = AcreSeamSeq.buildMidpoints(points[a], points[b], 64);
        String code = segment < 5 ? "z_s" : "a_s";
        int seq = (segment < 5 ? (i/7) * 5 : (i/7) * 2 - 5) + segment;
        VertexNameSequence nameSequence = new VertexNameSequence(code, seq * 64);
        addVertices(segment < 5 ? AcreVertexCategory.ZONE_SEAMS : ACRE_SEAMS,
                    midpoints, acre.seamStartVertexIds[i], acre, nameSequence);
        return j;
    }

    private int addZoneVertices(Acre acre, Point[] points, int i, int j) {
        int segment = i % 4;
        if (segment == 0) {
            j += 3;
        }
        int a = 0, b = 0, c = 0;
        switch (segment) {
            case 0: a = j;     b = j + 3; c = 0;     break;
            case 1: a = j + 1; b = j + 2; c = j;     break;
            case 2: a = j + 3; b = j;     c = j + 2; break;
            case 3: a = j + 2; b = j + 4; c = j + 3; break;
        }
        while (a >= points.length) {
            a -= points.length - 1;
        }
        while (b >= points.length) {
            b -= points.length - 1;
        }
        while (c >= points.length) {
            c -= points.length - 1;
        }
        Point[] midpoints = AcreSeamSeq.buildInnerPoints(points[a], points[b], points[c], 64);
        VertexNameSequence nameSequence = new VertexNameSequence("ver", i * 2048);
        addVertices(AcreVertexCategory.INNER, midpoints, acre.zoneStartVertexIds[i], acre, nameSequence);
        return j;
    }

    public interface VertexCountAccessor {
        long[] getAllVertexIds(long start, int limit);
        int getCount(long vertexId);
        long getVertexId(GeoPoint point);
    }

    private static class Stat {
        final GeoPoint geoPoint;
        final Point point;
        DataPoint[] dataPoints = {};

        private Stat(GeoPoint geoPoint, Point point) {
            this.geoPoint = geoPoint;
            this.point = point;
        }

        void consume(long id, Object ref, String name, AcreVertexCategory category) {
            DataPoint[] dataPoints = new DataPoint[this.dataPoints.length + 1];
            System.arraycopy(this.dataPoints, 0, dataPoints, 0, this.dataPoints.length);
            dataPoints[this.dataPoints.length] = new DataPoint(id, ref, name, category);
            this.dataPoints = dataPoints;
        }

        AcreVertexCategory getCategory() {
            AcreVertexCategory category = null;
            for (DataPoint dataPoint : dataPoints) {
                category = category == null || category.compareTo(dataPoint.category) < 0
                           ? dataPoint.category
                           : category;
            }
            return category;
        }
    }

    private static class DataPoint {

        final long vertexId;
        final Object ref;
        final String name;
        final AcreVertexCategory category;

        private DataPoint(long vertexId, Object ref, String name, AcreVertexCategory category) {
            this.vertexId = vertexId;
            this.ref = ref;
            this.name = name;
            this.category = category;
        }
    }

    private static class VertexNameSequence implements Iterator<String> {

        private final String code;

        private int index;

        private VertexNameSequence(String code, int startIndex) {
            this.code = code;
            this.index = startIndex;
        }

        public boolean hasNext() {
            return true;
        }

        public String next() {
            return String.format("%s[%d]", code, index++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
