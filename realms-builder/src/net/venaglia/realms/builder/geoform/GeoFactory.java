package net.venaglia.realms.builder.geoform;

import static net.venaglia.realms.spec.GeoSpec.*;

import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.elements.GraphAcre;
import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.geom.*;
import net.venaglia.gloo.physical.geom.primitives.Icosahedron;
import net.venaglia.realms.spec.map.*;
import net.venaglia.common.util.ProgressMonitor;
import net.venaglia.common.util.Ref;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.impl.OctreeMap;
import net.venaglia.realms.common.util.work.Results;
import net.venaglia.realms.common.util.work.WorkManager;
import net.venaglia.realms.common.util.work.WorkQueue;
import net.venaglia.realms.common.util.work.WorkSourceAdapter;
import net.venaglia.realms.common.util.work.WorkSourceKey;
import net.venaglia.realms.spec.GeoSpec;

import java.awt.Dimension;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 7:42 PM
 */
public class GeoFactory {

    private static final double radius = 1000.0;

    public final ProgressMonitor progressMonitor;

    private final WorkManager workManager;
    private final WorkSourceKey<Globe> globe = WorkSourceKey.create("globe", Globe.class);

    public GeoFactory() {
        workManager = new WorkManager("GeoFactory");
        progressMonitor = workManager.getProgressMonitor();
    }

    public Globe createGlobe() {
        final Globe globeInstance = Globe.INSTANCE;
        final WorkSourceKey<GlobalSector[]> globalSectorsKey = WorkSourceKey.create("global sectors", GlobalSector[].class);
        final WorkSourceKey<Collection<Sector>> sectorsKey = WorkSourceKey.create("sectors", Collection.class);
        final WorkSourceKey<Collection<AcreBuilder>> acreBuildersKey = WorkSourceKey.create("acre builders", Collection.class);
        final WorkSourceKey<Void> acreGraphKey = WorkSourceKey.create("acre graph", Void.class);
        workManager.addWorkSource(new WorkSourceAdapter<GlobalSector[]>(globalSectorsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                resultBuffer.set(initGlobeStep1(workQueue, globeInstance));
            }
        }, 1);
        workManager.addWorkSource(new WorkSourceAdapter<Collection<Sector>>(sectorsKey, globalSectorsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                GlobalSector[] globalSectors = dependencies.getResult(globalSectorsKey);
                resultBuffer.set(initGlobeStep2(workQueue, globalSectors));
            }
        }, 80);
        workManager.addWorkSource(new WorkSourceAdapter<Collection<AcreBuilder>>(acreBuildersKey, sectorsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<Sector> sectors = dependencies.getResult(sectorsKey);
                resultBuffer.set(initGlobeStep3(workQueue, globeInstance, sectors));
            }
        }, 60);
        workManager.addWorkSource(new WorkSourceAdapter<Void>(acreGraphKey, acreBuildersKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<AcreBuilder> acreBuilders = dependencies.getResult(acreBuildersKey);
                initGlobeStep4(workQueue, acreBuilders);
            }
        }, 12);
        workManager.addWorkSource(new WorkSourceAdapter<Globe>(globe, sectorsKey, acreGraphKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<Sector> sectors = dependencies.getResult(sectorsKey);
                for (Sector s : sectors) {
                    s.clearTriangles();
                }
                resultBuffer.set(globeInstance);
            }
        });
        return workManager.getResults().getResult(globe);
    }

    private GlobalSector[] initGlobeStep1(WorkQueue workQueue, Globe globe) {
        System.out.println("Building Sectors...");
        Icosahedron icosahedron = new Icosahedron();
        GeoPoint[] points = new GeoPoint[12];
        for (int i = 0; i < 12; i++) {
            points[i] = GeoPoint.fromPoint(icosahedron.points[i]);
        }
        final GlobalSector[] globalSectors = globe.sectors;
        for (int i = 0; i < 20; i++) {
            Facet facet = icosahedron.getFacet(i);
            assert facet.type == Facet.Type.TRIANGLE;
            GlobalSector gs = new GlobalSector(i,
                                               globe,
                                               points[indexOf(icosahedron.points, facet.a)],
                                               points[indexOf(icosahedron.points, facet.b)],
                                               points[indexOf(icosahedron.points, facet.c)]);
            gs.setInit(gs.new Initializer(facet.a, facet.b, facet.c, (i % 2) == 0, workQueue));
            globalSectors[i] = gs;
        }
        for (GlobalSector gs : globalSectors) {
            for (int n = 0; n < 3; n++) {
                GeoPoint p1 = gs.points[n];
                GeoPoint p2 = gs.points[(n + 1) % 3];
                for (GlobalSector t : globalSectors) {
                    for (int m = 0; m < 3; m++) {
                        if (t.points[m] == p1 && (t.points[(m + 1) % 3] == p2 || t.points[(m + 2) % 3] == p2)) {
                            gs.neighbors[n] = t.id;
                        }
                    }
                }
            }
        }
        for (GlobalSector gs : globalSectors) {
            for (int n = 0; n < 3; n++) {
                if (gs.neighbors[n] == 0) {
                    throw new AssertionError();
                }
            }
        }
        for (GlobalSector gs : globalSectors) {
            workQueue.addWorkUnit(gs.getInit());
        }
        return globalSectors;
    }

    private Collection<Sector> initGlobeStep2(WorkQueue workQueue, GlobalSector[] globalSectors) {
        System.out.println("Building Triangles...");
        final Collection<Sector> allSectors = new ArrayList<Sector>(26000);
        for (GlobalSector gs : globalSectors) {
            Collections.addAll(allSectors, gs.getSectors());
        }
        assert allSectors.size() == SECTORS.get();
        for (GlobalSector gs : globalSectors) {
            for (Sector s : gs.getSectors()) {
                workQueue.addWorkUnit(s.getInit());
            }
        }
        return allSectors;
    }

    private Collection<AcreBuilder> initGlobeStep3(WorkQueue workQueue, Globe globe, Collection<Sector> sectors) {
        System.out.println("Combining points...");
        long size = GLOBAL_SECTORS.get() + SECTORS.get() * (SECTOR_DIVISIONS.get() * 6 - 2);
        List<GeoPointBasedElement> everything = new ArrayList<GeoPointBasedElement>((int)size);
        Collections.addAll(everything, globe.sectors);
        everything.addAll(sectors);
        CartographicElementView elementView = CartographicElementView.getFor((int)SECTOR_DIVISIONS.get());
        for (Sector s : sectors) {
            s.edgesBuilt = new AtomicInteger();
            List<Sector.Triangle> border = elementView.get(s.triangles,
                                                           CartographicElementView.Position.Border,
                                                           null,
                                                           false);
            for (Sector.Triangle t : border) {
                everything.add(t);
            }
        }
        combinePoints(everything);
        Map<GeoPoint,Sector[]> sectorMap = new HashMap<GeoPoint,Sector[]>(131072);
        for (Sector s : sectors) {
            addSectorByPoint(sectorMap, s, s.points[0]);
            addSectorByPoint(sectorMap, s, s.points[1]);
            addSectorByPoint(sectorMap, s, s.points[2]);
        }
        assert sectorMapErrors(globe, sectorMap).isEmpty();
        System.out.println("Building Acres...");
        final Collection<AcreBuilder> acreBuilders = new ArrayList<AcreBuilder>(sectors.size());
        for (final Sector s : sectors) {
            AcreBuilder acreBuilder = new AcreBuilder((int)SECTOR_DIVISIONS.get(),
                                                      true,
                                                      s,
                                                      sectorMap,
                                                      workQueue,
                                                      null);
            acreBuilders.add(acreBuilder);
            workQueue.addWorkUnit(acreBuilder);
        }
        return acreBuilders;
    }

    private void initGlobeStep4(final WorkQueue workQueue,
                                final Collection<AcreBuilder> acreBuilders) {
        for (AcreBuilder acreBuilder : acreBuilders) {
            workQueue.addWorkUnit(acreBuilder.secondRun());
        }
    }

    public static Map<GeoPoint,Sector[]> sectorMapErrors(Globe globe, Map<GeoPoint, Sector[]> sectorMap) {
        Set<GeoPoint> pointsWith5Sectors = new HashSet<GeoPoint>(Arrays.asList(globe.points));
        Map<GeoPoint,Sector[]> invalidPoints = new HashMap<GeoPoint,Sector[]>();
        for (Map.Entry<GeoPoint,Sector[]> entry : sectorMap.entrySet()) {
            int c = 0;
            for (Sector s : entry.getValue()) {
                if (s != null) c++;
            }
            if (c != 6) {
                if (c != 5 || !pointsWith5Sectors.contains(entry.getKey())) {
                    invalidPoints.put(entry.getKey(), entry.getValue());
                }
            }
        }
        if (!invalidPoints.isEmpty()) {
            for (Map.Entry<GeoPoint,Sector[]> entry : invalidPoints.entrySet()) {
                int c = 0;
                for (Sector s : entry.getValue()) {
                    if (s != null) c++;
                }
                int expected = pointsWith5Sectors.contains(entry.getKey()) ? 5 : 6;
                StringBuilder buffer = new StringBuilder();
                buffer.append("Point is expected to be referenced by ")
                      .append(expected)
                      .append(" sectors, but is only referenced by ")
                      .append(c)
                      .append("\n");
                buffer.append("\tPoint: ")
                      .append(entry.getKey());
                for (Sector s : entry.getValue()) {
                    if (s != null) {
                        buffer.append("\n\t-> ").append(s);
                    }
                }
                System.err.println(buffer);
            }
        }
        return invalidPoints;
    }

    public static void addSectorByPoint(Map<GeoPoint,Sector[]> map, Sector sector, GeoPoint point) {
        Sector[] sectors = map.get(point);
        if (sectors == null) {
            sectors = new Sector[6];
            map.put(point, sectors);
            sectors[0] = sector;
            return;
        }
        for (int i = 1; i < 6; i++) {
            if (sectors[i] == null) {
                sectors[i] = sector;
                return;
            }
        }
        List<Object> list = new ArrayList<Object>(7);
        Collections.addAll(list, sectors);
        list.add(sector);
        StringBuilder buffer = new StringBuilder();
        buffer.append("Too many sectors reference point: ");
        buffer.append(point);
        for (Object o : list) {
            buffer.append("\n\t\t");
            buffer.append(o);
        }
        throw new AssertionError(buffer.toString());
    }

    private int indexOf(Point[] points, Point point) {
        for (int i = 0; i < points.length; i++) {
            if (points[i] == point) {
                return i;
            }
        }
        return -1;
    }

    protected static void combinePoints(Collection<GeoPointBasedElement> elements) {
        SpatialMap<GeoPoint> pointMap = new OctreeMap<GeoPoint>(new BoundingSphere(Point.ORIGIN, 1024.0));
        PointRewriter pointRewriter = new PointRewriter();
        for (GeoPointBasedElement e : elements) {
            for (int i = 0, pointsLength = e.countGeoPoints(); i < pointsLength; i++) {
                GeoPoint geoPoint = e.getGeoPoint(i);
                Point p = geoPoint.toPoint(1000.0);
                pointRewriter.set(e, i);
                pointMap.intersect(new BoundingSphere(p, 0.00005), pointRewriter);
                if (pointRewriter.rewriteCount() == 0) {
                    pointMap.add(geoPoint, p);
                }
            }
        }
    }

    public Collection<Acre> packAcres(Globe globe) {
        Map<Long,Acre> newIds = new TreeMap<Long,Acre>();
        for (GlobalSector gs : globe.sectors) {
            for (Sector s : gs.getSectors()) {
                for (Acre a : s.getInnerAcres()) {
                    assert a.packNeighbors == null;
                    a.packNeighbors = new int[a.neighbors.length];
                    newIds.put(a.id, a);
                }
                for (Acre a : s.getSharedAcres()) {
                    if (a.packNeighbors == null) {
                        a.packNeighbors = new int[a.neighbors.length];
                        newIds.put(a.id, a);
                    }
                }
            }
        }
        int seq = 0;
        for (Map.Entry<Long,Acre> e : newIds.entrySet()) {
            e.getValue().packId = seq++;
        }
        for (Map.Entry<Long,Acre> e : newIds.entrySet()) {
            long[] neighbors = e.getValue().neighbors;
            int[] packNeighbors = new int[neighbors.length];
            e.getValue().packNeighbors = packNeighbors;
            for (int i = 0; i < neighbors.length; i++) {
                Acre acre = newIds.get(neighbors[i]);
                assert acre != null;
                packNeighbors[i] = acre.packId;
            }
        }
        return newIds.values();
    }

    public void writeToDisk(Collection<Acre> acres) throws IOException {
        double radius = GeoSpec.APPROX_RADIUS_METERS.get();
        WorldMap worldMap = new WorldMap();
        worldMap.graph.clear();
        GraphAcre ga = new GraphAcre();
        ga.setNeighbors(new ArrayList<Ref<GraphAcre>>());
        ga.setVertices(new GeoPoint[6]);
        for (Acre acre : acres) {
            acre.applyPackDataToGraphAcre(ga, radius, worldMap);
            worldMap.graph.put(ga.getId(), ga);
        }
        worldMap.graph.commitChanges();
        System.out.println("Wrote " + worldMap.graph + " to " + worldMap.graph.getFile());
    }

    private static class PointRewriter implements SpatialMap.Consumer<GeoPoint> {
        private GeoPointBasedElement element;
        private int index;
        private int count = 0;

        private PointRewriter() {
        }

        private void set(GeoPointBasedElement element, int index) {
            this.element = element;
            this.index = index;
            this.count = 0;
        }

        public void found(SpatialMap.Entry<GeoPoint> entry, double x, double y, double z) {
            element.setGeoPoint(index, entry.get());
            count++;
        }

        private int rewriteCount() {
            return count;
        }
    }

    public static void main(String[] args) {
        System.out.println(SUMMARY);
        final GeoFactory geoFactory = new GeoFactory();
        final AtomicBoolean buildCompleted = new AtomicBoolean(false);
        new Thread("ProgressMonitor") {
            {
                this.setDaemon(true);
            }

            @Override
            public void run() {
                synchronized (this) {
                    try {
                        this.wait(1000);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
                while (!buildCompleted.get()) {
                    ProgressMonitor pm = geoFactory.progressMonitor;
                    System.out.println(String.format("%6.3f%% -- %d of %d (%,d acres built)", pm.getProgress() * 100.0, pm.getCurrentStepNumber(), pm.getNumberOfSteps(), Acre.SEQ.get()));
                    synchronized (this) {
                        try {
                            this.wait(500);
                        } catch (InterruptedException e) {
                            // don't care
                        }
                    }
                }
            }
        }.start();
        final Globe globe = geoFactory.createGlobe();
        ProgressMonitor pm = geoFactory.progressMonitor;
        buildCompleted.set(true);
        System.out.println(String.format("%6.3f%% -- %d of %d (%,d acres built)", pm.getProgress() * 100.0, pm.getCurrentStepNumber(), pm.getNumberOfSteps(), Acre.SEQ.get()));

//        Dimension windowSize = new Dimension(1024,768);
        Dimension windowSize = new Dimension(1280,1024);
//        Dimension windowSize = new Dimension(2048,1536);
        GeoViewer.view(globe, radius, "The world", windowSize);

        for (int i = 0; i < 20; i++) {
            GlobalSector gs = globe.sectors[i];
            for (Sector s : gs.getSectors()) {
                for (Acre a : s.getInnerAcres()) {
                    assert a != null;
                }
                for (Acre a : s.getSharedAcres()) {
                    assert a != null;
                }
            }
        }

        final AtomicInteger errorCount = new AtomicInteger();
        final AtomicInteger totalCount = new AtomicInteger();
        WorkSourceKey<Void> validateKey = WorkSourceKey.create("validate graph", Void.class);
        WorkSourceKey<Void> summaryKey = WorkSourceKey.create("summary", Void.class);
        geoFactory.workManager.addWorkSource(new WorkSourceAdapter<Void>(validateKey, geoFactory.globe) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                for (int i = 0; i < 20; i++) {
                    GlobalSector gs = globe.sectors[i];
                    for (Sector s : gs.getSectors()) {
                        for (SpatialMap.Entry<Acre> entry : s.acres) {
                            final Acre acre = entry.get();
                            totalCount.addAndGet(acre.neighbors.length);
                            workQueue.addWorkUnit(new Validator(acre, globe, errorCount));
                        }
                    }
                }
            }
        });
        geoFactory.workManager.addWorkSource(new WorkSourceAdapter<Void>(summaryKey, validateKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                int good = totalCount.get() - errorCount.get();
                System.out.println("Done!!! (" + good + "/" + totalCount.get() + " good)");
                if (errorCount.get() == 0) {
                    try {
                        geoFactory.writeToDisk(geoFactory.packAcres(globe));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        geoFactory.workManager.getResults().getResult(summaryKey);
    }

    private static class Validator implements Runnable {

        private final Acre acre;
        private final Globe globe;
        private final AtomicInteger errorCount;

        public Validator(Acre acre, Globe globe, AtomicInteger errorCount) {
            this.acre = acre;
            this.globe = globe;
            this.errorCount = errorCount;
        }

        public void run() {
            outer:
            for (long neighborID : acre.neighbors) {
                if (neighborID != 0) {
                    AbstractCartographicElement obj = globe.findByID(neighborID);
                    if (obj == null) {
                        System.err
                                .println("Could not find neighbor acre by id: " + acre.getIDString() + " --> " + AbstractCartographicElement
                                        .toIdString(neighborID));
                    } else {
                        assert obj instanceof Acre;
                        for (long l : obj.neighbors) {
                            if (l == acre.id) {
                                continue outer;
                            }
                        }
                        System.err
                                .println("Returned Acre does not reciprocate neighbor ids: " + obj + " !--> " + acre);
                    }
                }
                errorCount.incrementAndGet();
            }
        }
    }

}
