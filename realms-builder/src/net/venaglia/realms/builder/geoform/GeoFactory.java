package net.venaglia.realms.builder.geoform;

import static net.venaglia.realms.builder.geoform.SectorDebugger.AcreVertexCategory.*;
import static net.venaglia.realms.common.util.work.ProgressExportingRunnable.ProgressExporter;
import static net.venaglia.realms.spec.GeoSpec.GLOBAL_SECTORS;
import static net.venaglia.realms.spec.GeoSpec.SECTORS;
import static net.venaglia.realms.spec.GeoSpec.SECTOR_DIVISIONS;
import static net.venaglia.realms.spec.GeoSpec.SUMMARY;

import net.venaglia.common.util.Consumer;
import net.venaglia.common.util.RangeBasedLongSet;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.gloo.physical.geom.*;
import net.venaglia.gloo.physical.geom.primitives.Icosahedron;
import net.venaglia.realms.builder.utils.MutableBounds;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.BinaryStore;
import net.venaglia.realms.common.map.VertexStore;
import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.topo.VertexBlock;
import net.venaglia.realms.common.util.work.ProgressExportingRunnable;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.spec.map.*;
import net.venaglia.common.util.ProgressMonitor;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.realms.common.util.work.Results;
import net.venaglia.realms.common.util.work.WorkManager;
import net.venaglia.realms.common.util.work.WorkQueue;
import net.venaglia.realms.common.util.work.WorkSourceAdapter;
import net.venaglia.realms.common.util.work.WorkSourceKey;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 7/17/12
 * Time: 7:42 PM
 */
public class GeoFactory {

    private static final boolean VALIDATE_ZONE_VERTEX_COUNTS = true;
    private static final boolean VALIDATE_ZONE_BOUNDARY_COUNTS = true;
    private static final boolean VALIDATE_ACRE_INNER_VERTEX_COUNTS = true;
    private static final boolean VALIDATE_ACRE_BOUNDARY_CORNER_VERTEX_COUNTS = true;
    private static final boolean VALIDATE_ACRE_BOUNDARY_MIDPOINT_VERTEX_COUNTS = true;

    public final ProgressMonitor progressMonitor;

    private final WorkManager workManager;
    private final AtomicBoolean success = new AtomicBoolean();
    private final AtomicReference<File> globalVerticesFile = new AtomicReference<File>();

    public GeoFactory() {
        workManager = new WorkManager("GeoFactory");
        progressMonitor = workManager.getProgressMonitor();
    }

    public Globe createGlobe() {
        final Globe globeInstance = Globe.INSTANCE;

        // globe building steps
        final WorkSourceKey<GlobalSector[]> globalSectorsKey = WorkSourceKey.create("global sectors", GlobalSector[].class);
        final WorkSourceKey<Collection<Sector>> sectorsKey = WorkSourceKey.create("sectors", Collection.class);
        final WorkSourceKey<Map<GeoPoint,Sector[]>> combinePointsKey = WorkSourceKey.create("combine points", Collection.class);
        final WorkSourceKey<Collection<AcreBuilder>> acreBuildersKey = WorkSourceKey.create("build acres", Collection.class);
        final WorkSourceKey<TopographyIncrements> acreGraphKey = WorkSourceKey.create("acre graph", TopographyIncrements.class);
        final WorkSourceKey<AcreSeamSeq> seamSequenceKey = WorkSourceKey.create("acre seam sequence", Void.class);
        final WorkSourceKey<Void> topographyKey = WorkSourceKey.create("topography", Void.class);
        final WorkSourceKey<File> globalVertexKey = WorkSourceKey.create("collect global vertices", File.class);
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
        }, 30);
        workManager.addWorkSource(new WorkSourceAdapter<Map<GeoPoint,Sector[]>>(combinePointsKey, sectorsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<Sector> sectors = dependencies.getResult(sectorsKey);
                resultBuffer.set(initGlobeStep3(workQueue, globeInstance, sectors));
            }
        }, 180);
        workManager.addWorkSource(new WorkSourceAdapter<Collection<AcreBuilder>>(acreBuildersKey, sectorsKey, combinePointsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<Sector> sectors = dependencies.getResult(sectorsKey);
                Map<GeoPoint,Sector[]> sectorMap = dependencies.getResult(combinePointsKey);
                resultBuffer.set(initGlobeStep4(workQueue, sectorMap, sectors));
            }
        }, 40);
        workManager.addWorkSource(new WorkSourceAdapter<TopographyIncrements>(acreGraphKey, acreBuildersKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<AcreBuilder> acreBuilders = dependencies.getResult(acreBuildersKey);
                initGlobeStep5(workQueue, acreBuilders);
            }
        }, 10);
        workManager.addWorkSource(new WorkSourceAdapter<AcreSeamSeq>(seamSequenceKey, acreGraphKey, acreBuildersKey, sectorsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                final Collection<Sector> sectors = dependencies.getResult(sectorsKey);
                initGlobeStep6(workQueue, sectors, globeInstance, resultBuffer);
            }
        }, 50);
        workManager.addWorkSource(new WorkSourceAdapter<Void>(topographyKey, seamSequenceKey, acreGraphKey, acreBuildersKey, sectorsKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                Collection<AcreBuilder> acreBuilders = dependencies.getResult(acreBuildersKey);
                AcreSeamSeq acreSeamSeq = dependencies.getResult(seamSequenceKey);
                initGlobeStep7(workQueue, acreBuilders, acreSeamSeq, globeInstance.pointMap);
            }
        }, 60);
        workManager.addWorkSource(new WorkSourceAdapter<File>(globalVertexKey, topographyKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                final File vertices = initGlobeStep8(globeInstance.pointMap);
                globalVerticesFile.set(vertices);
                resultBuffer.set(vertices);
            }
        }, 10);

        // validation steps
        final WorkSourceKey<ValidateResult> acreValidateKey = WorkSourceKey.create("validate acre graph", Void.class);
        final WorkSourceKey<Void> vertexValidateKey = WorkSourceKey.create("validate vertex ids", List.class);
        final WorkSourceKey<Globe> globalVertexValidateKey = WorkSourceKey.create("validate global vertex ids", List.class);
        workManager.addWorkSource(new WorkSourceAdapter<ValidateResult>(acreValidateKey, globalVertexKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                ValidateResult validateResult = new ValidateResult();
                validateGlobeStep1(workQueue, globeInstance, validateResult);
                resultBuffer.set(validateResult);
            }
        }, 30);
        workManager.addWorkSource(new WorkSourceAdapter<Void>(vertexValidateKey, acreValidateKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                System.out.println("Validating Vertex Ids...");
                ValidateResult validateResult = dependencies.getResult(acreValidateKey);
                validateGlobeStep2(workQueue, globeInstance, validateResult);
            }
        }, 160);
        workManager.addWorkSource(new WorkSourceAdapter<Globe>(globalVertexValidateKey, globalVertexKey, vertexValidateKey, acreValidateKey) {
            public void addWork(WorkQueue workQueue, Results dependencies) {
                ValidateResult validateResult = dependencies.getResult(acreValidateKey);
                validateGlobeStep3(workQueue, globeInstance, validateResult);
                resultBuffer.set(globeInstance);
            }
        }, 30);

        // finish up
        try {
            return workManager.getResults().getResult(globalVertexValidateKey); // blocks until completed
        } finally {
            ValidateResult validateResult = workManager.getResults().getResult(acreValidateKey);
            success.set(validateResult.errorCount.get() == 0);
            int good = validateResult.totalCount.get() - validateResult.errorCount.get();
            System.out.printf("Validated (%d/%d good, %d bad)%n",
                              good,
                              validateResult.totalCount.get(),
                              validateResult.errorCount.get());
        }
    }

    public boolean isSuccess() {
        return success.get();
    }

    private AcreSeamSeq assignZoneSeamIds(Globe globe,
                                          long seqStart,
                                          ProgressExporter progressExporter) {
        AcreSeamSeq seq = new AcreSeamSeq(seqStart);
        AcreSeamSeq.Accessor seqAccessor = seq.getAccessor();
        for (GlobalSector gs : globe.sectors) {
            for (Sector s : gs.getSectors()) {
                for (Acre acre : s.getInnerAcres()) {
                    for (Point p : AcreSeamSeq.buildMidpoints(acre)) {
                        seqAccessor.touch(p);
                        progressExporter.oneMore();
                    }
                }
                for (Acre acre : s.getSharedAcres()) {
                    for (Point p : AcreSeamSeq.buildMidpoints(acre)) {
                        seqAccessor.touch(p);
                        progressExporter.oneMore();
                    }
                }
            }
        }
        seq.freeze();
        return seq;
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

    private Map<GeoPoint,Sector[]> initGlobeStep3(final WorkQueue workQueue,
                                                  final Globe globe,
                                                  final Collection<Sector> sectors) {
        final Map<GeoPoint,Sector[]> sectorMap = new HashMap<GeoPoint,Sector[]>(131072);
        workQueue.addWorkUnit(new ProgressExportingRunnable() {

            private ProgressExporter progressExporter;

            public void setProgressExporter(ProgressExporter progressExporter) {
                this.progressExporter = progressExporter;
            }

            public void run() {
                System.out.println("Combining points...");
                long trianglesPerSector = SECTOR_DIVISIONS.get() * SECTOR_DIVISIONS.get();
                long expectedSize = GLOBAL_SECTORS.get() + SECTORS.get() + trianglesPerSector * SECTORS.get();
                final List<GeoPointBasedElement> everything = new ArrayList<GeoPointBasedElement>((int)expectedSize);
                Collections.addAll(everything, globe.sectors);
                progressExporter.exportProgress(1, expectedSize * 2 + sectors.size() * 2 + 1);
                everything.addAll(sectors);
                for (Sector s : sectors) {
                    s.edgesBuilt = new AtomicInteger();
                    assert s.triangles.length == trianglesPerSector;
                    Collections.addAll(everything, s.triangles);
                    progressExporter.oneMore();
                }
                assert everything.size() == expectedSize;
                combinePoints(everything, globe, progressExporter);
                assert globe.pointMap.size() == (int)(GeoSpec.POINTS_SHARED_MANY_ZONE.get());
                System.out.printf("Total global points: %,d\n", globe.pointMap.size());
                for (Sector s : sectors) {
                    addSectorByPoint(sectorMap, s, s.points[0]);
                    addSectorByPoint(sectorMap, s, s.points[1]);
                    addSectorByPoint(sectorMap, s, s.points[2]);
                    progressExporter.oneMore();
                }
                assert sectorMapErrors(globe, sectorMap).isEmpty();
            }
        });
        return sectorMap;
    }

    private Collection<AcreBuilder> initGlobeStep4(WorkQueue workQueue,
                                                   Map<GeoPoint, Sector[]> sectorMap,
                                                   Collection<Sector> sectors) {
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

    private void initGlobeStep5(final WorkQueue workQueue,
                                final Collection<AcreBuilder> acreBuilders) {
        for (AcreBuilder acreBuilder : acreBuilders) {
            workQueue.addWorkUnit(acreBuilder.secondRun());
        }
    }

    private void initGlobeStep6(final WorkQueue workQueue,
                                final Collection<Sector> sectors,
                                final Globe globeInstance,
                                final AtomicReference<AcreSeamSeq> resultBuffer) {
        workQueue.addWorkUnit(new ProgressExportingRunnable() {
            private ProgressExporter progressExporter;

            public void setProgressExporter(ProgressExporter progressExporter) {
                this.progressExporter = progressExporter;
            }

            public void run() {
                long total = (sectors.size() +
                              GeoSpec.ONE_SECTOR_ACRES.get() +
                              GeoSpec.TWO_SECTOR_ACRES.get() * 2 +
                              GeoSpec.PENTAGONAL_ACRES.get() * 5 +
                              GeoSpec.SIX_SECTOR_ACRES.get() * 6);
                progressExporter.exportProgress(0, total);
                for (Sector s : sectors) {
                    s.clearTriangles();
                    progressExporter.oneMore();
                }
                packAcres(globeInstance);
                System.out.println("Assigning Global Vertex IDs...");
                int seq = 0;
                for (SpatialMap.Entry<GeoPoint> entry : globeInstance.pointMap) {
                    ((GlobalPointMap.GlobalPointEntry)entry).setSeq(seq++);
                }
                TopographyIncrements topographyIncrements = AcreBuilder.getTopographyIncrements(seq);
                System.out.println(topographyIncrements);
                AcreSeamSeq acreSeamSeq = assignZoneSeamIds(globeInstance,
                                                            topographyIncrements.zoneSeamSeqStart,
                                                            progressExporter);
                resultBuffer.set(acreSeamSeq);
            }
        });
    }

    private void initGlobeStep7(final WorkQueue workQueue,
                                final Collection<AcreBuilder> acreBuilders,
                                final AcreSeamSeq acreSeamSeq,
                                final GlobalPointMap globalPointMap) {
        System.out.println("Building Initial Topography...");
        for (AcreBuilder acreBuilder : acreBuilders) {
            workQueue.addWorkUnit(acreBuilder.thirdRun(acreSeamSeq.getAccessor(), globalPointMap));
        }
    }

    private File initGlobeStep8(final GlobalPointMap globalPointMap) {
        System.out.println("Packing global vertices...");
        final int count = GeoSpec.POINTS_SHARED_MANY_ZONE.iGet();
        final int step = 2 * (Double.SIZE >> 3);
        final byte[] bytes = new byte[count * step];
        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (SpatialMap.Entry<GeoPoint> entry : globalPointMap) {
            int seq = ((GlobalPointMap.GlobalPointEntry)entry).getSeq();
            buffer.position(seq * step);
            GeoPoint point = entry.get();
            buffer.putDouble(point.longitude);
            buffer.putDouble(point.latitude);
        }
        assert buffer.remaining() == 0;

        System.out.printf("Writing global vertices to temp file... (%,d bytes)\n", count * step);
        buffer.flip();
        File verticesFile = null;
        FileOutputStream out = null;
        try {
            verticesFile = File.createTempFile("global.", ".vtx");
            verticesFile.deleteOnExit();
            out = new FileOutputStream(verticesFile);
            out.getChannel().write(buffer);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                // don't care;
            }
        }
        return verticesFile;
    }

    private void validateGlobeStep1(WorkQueue workQueue,
                                    Globe globeInstance,
                                    ValidateResult validateResult) {
        for (int i = 0; i < 20; i++) {
            GlobalSector gs = globeInstance.sectors[i];
            for (Sector s : gs.getSectors()) {
                for (Acre a : s.getInnerAcres()) {
                    assert a != null;
                }
                for (Acre a : s.getSharedAcres()) {
                    assert a != null;
                }
            }
        }
        final Set<GeoPoint> globalPoints = new HashSet<GeoPoint>(globeInstance.pointMap.size());
        for (SpatialMap.Entry<GeoPoint> entry : globeInstance.pointMap) {
            globalPoints.add(entry.get());
        }
        globeInstance.pointMap.fastClear();
        System.out.println("Validating Acres...");
        for (int i = 0; i < 20; i++) {
            GlobalSector gs = globeInstance.sectors[i];
            for (Sector s : gs.getSectors()) {
                for (Acre acre : s.getInnerAcres()) {
                    validateResult.totalCount.addAndGet(acre.points.length);
                    AcreValidator acreValidator = new AcreValidator(acre,
                                                                    globeInstance,
                                                                    globalPoints,
                                                                    validateResult.errorCount);
                    workQueue.addWorkUnit(acreValidator);
                }
                for (Acre acre : s.getSharedAcres()) {
                    validateResult.totalCount.addAndGet(acre.points.length);
                    AcreValidator acreValidator = new AcreValidator(acre,
                                                                    globeInstance,
                                                                    globalPoints,
                                                                    validateResult.errorCount);
                    workQueue.addWorkUnit(acreValidator);
                }
            }
        }
    }

    private void validateGlobeStep2(WorkQueue workQueue,
                                    Globe globeInstance,
                                    final ValidateResult validateResult) {
        final long[] expectedCounts = calculateSingleSectorVertexCounts(false);
        final long[] expectedCountsWithPentagonalAcre = calculateSingleSectorVertexCounts(true);
        for (int i = 0; i < 20; i++) {
            GlobalSector gs = globeInstance.sectors[i];
            for (final Sector s : gs.getSectors()) {
                SingleSectorVertexValidator validator = new SingleSectorVertexValidator(s) {
                    @Override
                    protected long[] getExpectedCounts() {
                        return containsPentagonalAcre ? expectedCountsWithPentagonalAcre : expectedCounts;
                    }

                    @Override
                    protected void recordError(Object message) {
                        int errors = validateResult.errorCount.incrementAndGet();
                        if (errors < 50) {
                            super.recordError(message);
                        }
                    }

                    @Override
                    protected String getInstanceName() {
                        return String.format("Sector%s", s);
                    }
                };
                workQueue.addWorkUnit(validator);
            }
        }
    }

    private void validateGlobeStep3(WorkQueue workQueue,
                                    Globe globe,
                                    ValidateResult validateResult) {
        workQueue.addWorkUnit(new GlobalVertexValidator(globe, validateResult));
    }


    private long[] calculateSingleSectorVertexCounts(boolean pentagonal) {
        long sectorDivisions = GeoSpec.SECTOR_DIVISIONS.get();
        long innerVerticesPerAcre = 0;
        if (VALIDATE_ZONE_BOUNDARY_COUNTS) {
            innerVerticesPerAcre += 63 * 30; // 30 seams, not on the acre boundary
        }
        if (VALIDATE_ZONE_VERTEX_COUNTS) {
            innerVerticesPerAcre += 1953 * 24; // inner vertices
        }
        if (VALIDATE_ACRE_INNER_VERTEX_COUNTS) {
            innerVerticesPerAcre += 6 + 1; // spoke midpoints & center point
        }

        long boundaryVerticesPerAcreSide = 0;
        if (VALIDATE_ZONE_BOUNDARY_COUNTS) {
            boundaryVerticesPerAcreSide += 63 * 2; // zone seams
        }
        if (VALIDATE_ACRE_BOUNDARY_MIDPOINT_VERTEX_COUNTS) {
            boundaryVerticesPerAcreSide += 1; // edge midpoints
        }

        long concaveBorderVertices = 0;
        long convexBorderVertices = 0;
        if (VALIDATE_ACRE_BOUNDARY_CORNER_VERTEX_COUNTS) {
            concaveBorderVertices += sectorDivisions * 2;
        }

        long boundaryAcreSides = (sectorDivisions + 2) * 3 + sectorDivisions;
        long redAcres = sectorDivisions / 3 * (sectorDivisions / 3 + 1) / 2;
        long greenAcres = redAcres - (sectorDivisions / 3);
        long innerAcres = (sectorDivisions * sectorDivisions - (sectorDivisions - 2) * 3) / 6;
        long totalAcres = sectorDivisions + innerAcres;
        long sharedAcreSeams = redAcres * 6 + greenAcres * 3 - sectorDivisions;
        long tripleReferencedAcreCorners = 0;
        if (VALIDATE_ACRE_BOUNDARY_CORNER_VERTEX_COUNTS) {
            tripleReferencedAcreCorners += redAcres * 6 - concaveBorderVertices;
            convexBorderVertices += boundaryAcreSides - concaveBorderVertices;
        }

        long[] counts = VertexValidator.createZeroCountsArray();
        counts[1] = totalAcres * innerVerticesPerAcre + // + 90/240 ?? why ??
                    boundaryAcreSides * boundaryVerticesPerAcreSide +
                    convexBorderVertices;
        counts[2] = sharedAcreSeams * boundaryVerticesPerAcreSide +
                    concaveBorderVertices;
        counts[3] = tripleReferencedAcreCorners;

        if (pentagonal) {
            if (VALIDATE_ZONE_VERTEX_COUNTS) {
                counts[1] -= 1953 * 4; // inner vertices
            }
            if (VALIDATE_ZONE_BOUNDARY_COUNTS) {
                counts[1] -= 63 *7; // zone seams
            }
            if (VALIDATE_ACRE_BOUNDARY_MIDPOINT_VERTEX_COUNTS) {
                counts[1] -= 1;  // edge midpoint
            }
            if (VALIDATE_ACRE_BOUNDARY_CORNER_VERTEX_COUNTS) {
                counts[1] -= 1;  // corner point
            }
            if (VALIDATE_ACRE_INNER_VERTEX_COUNTS) {
                counts[1] -= 1;  // spoke midpoint
            }
        }
        return counts;
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

    protected static void combinePoints(Collection<GeoPointBasedElement> elements, Globe globe) {
        combinePoints(elements, globe, null);
    }

    private static void combinePoints(Collection<GeoPointBasedElement> elements,
                                      Globe globe,
                                      ProgressExporter progressExporter) {
        int index = 0;
        GeoPoint[] globalPoints = new GeoPoint[(int)(GeoSpec.POINTS_SHARED_MANY_ZONE.get())];
        SpatialMap<GeoPoint> pointMap = globe.pointMap;
        PointRewriter pointRewriter = new PointRewriter();
        MutableBounds bounds = new MutableBounds();
        for (GeoPointBasedElement e : elements) {
            for (int i = 0, pointsLength = e.countGeoPoints(); i < pointsLength; i++) {
                GeoPoint geoPoint = e.getGeoPoint(i);
                bounds.load(geoPoint);
                pointRewriter.set(e, i);
                pointMap.intersect(bounds, pointRewriter);
                if (pointRewriter.rewriteCount() == 0) {
                    globalPoints[index++] = geoPoint;
                    pointMap.add(geoPoint, bounds.getCenterPoint());
                }
            }
            if (progressExporter != null) {
                progressExporter.oneMore();
            }
        }
        Comparator<GeoPoint> comparator = new Comparator<GeoPoint>() {
            public int compare(GeoPoint p1, GeoPoint p2) {
                int cmp = Double.compare(p1.latitude, p2.latitude);
                return cmp == 0 ? Double.compare(p1.longitude, p2.longitude) : cmp;
            }
        };
        Arrays.sort(globalPoints, comparator);
        for (GeoPointBasedElement e : elements) {
            for (int i = 0, pointsLength = e.countGeoPoints(); i < pointsLength; i++) {
                GeoPoint geoPoint = e.getGeoPoint(i);
                index = Arrays.binarySearch(globalPoints, geoPoint, comparator);
                assert index >= 0 && index < globalPoints.length && geoPoint.equals(globalPoints[index]);
                e.setGeoPoint(i, globalPoints[index]);
            }
            if (progressExporter != null) {
                progressExporter.oneMore();
            }
        }
        Arrays.fill(globalPoints, null); // just being nice
    }

    public void packAcres(Globe globe) {
        Map<Long,Acre> acresById = globe.acresById;
        for (GlobalSector gs : globe.sectors) {
            for (Sector s : gs.getSectors()) {
                for (Acre a : s.getInnerAcres()) {
                    assert a.packNeighbors == null;
                    a.packNeighbors = new int[a.points.length];
                    acresById.put(a.id, a);
                }
                for (Acre a : s.getSharedAcres()) {
                    if (a.packNeighbors == null) {
                        a.packNeighbors = new int[a.points.length];
                        acresById.put(a.id, a);
                    }
                }
            }
        }
        int seq = 0;
        for (Map.Entry<Long,Acre> e : acresById.entrySet()) {
            e.getValue().packId = seq++;
        }
    }

    public void packNeighbors(Map<Long,Acre> newIds) {
        for (Map.Entry<Long,Acre> e : newIds.entrySet()) {
            Acre a = e.getValue();
            long[] neighbors = a.neighbors;
            int[] packNeighbors = new int[neighbors.length];
            a.packNeighbors = packNeighbors;
            for (int i = 0; i < neighbors.length; i++) {
                Acre acre = newIds.get(neighbors[i]);
                assert acre != null;
                packNeighbors[i] = acre.packId;
            }
        }
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
        Configuration.TERAFORMING.setBoolean(true);
        final double radius = 1000.0;
        System.out.println(SUMMARY);
        final GeoFactory geoFactory = new GeoFactory();
        final AtomicBoolean buildCompleted = new AtomicBoolean(false);
        new Thread("ProgressMonitor") {

            private final long interval = Math.max(100, Long.parseLong(System.getProperty("progress.interval", "5000")));

            {
                this.setDaemon(true);
            }

            @Override
            public void run() {
                synchronized (this) {
                    try {
                        this.wait(interval);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
                while (!buildCompleted.get()) {
                    ProgressMonitor pm = geoFactory.progressMonitor;
                    String name = pm.getCurrentStepName();
                    if (name == null) {
                        name = "???";
                    }
                    System.out.println(String.format("%6.3f%% -- in step \"%s\" (%d of %d)", pm.getProgress() * 100.0, name, pm.getCurrentStepNumber(), pm.getNumberOfSteps()));
                    synchronized (this) {
                        try {
                            this.wait(interval);
                        } catch (InterruptedException e) {
                            // don't care
                        }
                    }
                }
            }
        }.start();
        final Globe globe = geoFactory.createGlobe();
        final boolean success = geoFactory.isSuccess();
        buildCompleted.set(true);

        final Dimension windowSize = new Dimension(1280,1024);

        final WriteSummary writeSummary;
        final AcreListener newAcreListener = "SMALL".equals(Configuration.GEOSPEC.getString()) &&
                                             !Configuration.DATABASE_HARMLESS.getBoolean()
                                             ? new AcreListener() : null;
        if (success) {
            boolean saveParanoiaFile = Configuration.PARANOIA_ON_ACRES.getBoolean();
            final ConcurrentMap<String,Boolean> signatures =
                    saveParanoiaFile ? new ConcurrentHashMap<String,Boolean>(8192) : null;
            WorkSourceKey<WriteSummary> writeCount = WorkSourceKey.create("summary", Void.class);
            geoFactory.workManager.addWorkSource(new WorkSourceAdapter<WriteSummary>(writeCount) {

                final WriteSummary writeSummary = new WriteSummary();

                public void addWork(WorkQueue workQueue, Results dependencies) {
                    System.out.println("Saving to disk...   (each '.' represents 1024 acres)");
                    BinaryStore binaryStore = WorldMap.INSTANCE.get().getBinaryStore();
                    geoFactory.packNeighbors(globe.acresById);
                    List<Acre> acres = new ArrayList<Acre>(globe.acresById.values());
                    for (int i = 0, l = acres.size(); i < l; i += 100) {
                        int j = Math.min(i + 100, l);
                        workQueue.addWorkUnit(new AcreWriter(acres.subList(i, j),
                                                             binaryStore,
                                                             newAcreListener,
                                                             writeSummary));
                    }
                    resultBuffer.set(writeSummary);
                }
            });
            writeSummary = geoFactory.workManager.getResults().getResult(writeCount);
            System.out.println();
            new VertexWriter(writeSummary, geoFactory.globalVerticesFile.get(), WorldMap.INSTANCE.get().getVertexStore()).run();
            if (saveParanoiaFile && !Configuration.DATABASE_HARMLESS.getBoolean()) {
                writeSignatures(signatures.keySet());
            }
            GeoViewer.view(globe, radius, "The world", windowSize);
        } else {
            writeSummary = new WriteSummary();
        }
        System.out.printf("\nWrote %,d acres, %,d vertices, %,d bytes\n",
                          writeSummary.acreCount.get(),
                          writeSummary.vertexCount.get(),
                          writeSummary.byteCount.get());
        if (newAcreListener != null && newAcreListener.captureSuccessful()) {
            System.out.println("-----------------------------------");
            AcreDetail[] capturedAcres = newAcreListener.getCapturedAcres();
            for (int i = 0; i < capturedAcres.length; i++) {
                AcreDetail detail = capturedAcres[i];
                System.out.println(detail.toSourceLiteral("acre[" + i + "]"));
            }
        }
    }

    private static void writeSignatures(Set<String> signatures) {
        String fileName = "validate.signatures.txt";
        FileWriter out = null;
        try {
            out = new FileWriter(fileName);
            for (String sig : signatures) {
                out.write(sig);
                out.write("\n");
            }
        } catch (IOException e) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    // don't care
                }
            }
        }
        System.out.printf("\nWrote %,d KB to %s", Math.round(new File(fileName).length() / 1000.0), fileName);
    }

    private static class ValidateResult {
        public final AtomicInteger totalCount = new AtomicInteger();
        public final AtomicInteger errorCount = new AtomicInteger();
    }

    private static class WriteSummary {
        public final AtomicInteger acreCount = new AtomicInteger();
        public final AtomicInteger vertexCount = new AtomicInteger();
        public final AtomicLong byteCount = new AtomicLong();
    }

    private static class AcreValidator implements Runnable {

        private final Acre acre;
        private final Globe globe;
        private final Set<GeoPoint> globalPoints;
        private final AtomicInteger errorCount;

        public AcreValidator(Acre acre, Globe globe, Set<GeoPoint> globalPoints, AtomicInteger errorCount) {
            this.acre = acre;
            this.globe = globe;
            this.globalPoints = globalPoints;
            this.errorCount = errorCount;
        }

        public void run() {
            long[] neighbors = acre.neighbors;
            for (int i = 0, l = neighbors.length; i < l; i++) {
                long neighborID = neighbors[i];
                if (neighborID != 0) {
                    AbstractCartographicElement obj = globe.findByID(neighborID);
                    if (obj == null) {
                        System.err.printf("Could not find neighbor acre by id: %s --> %s\n",
                                          acre.getIDString(),
                                          AbstractCartographicElement.toIdString(neighborID));
                    } else {
                        assert obj instanceof Acre;
                        for (GeoPoint p : obj.points) {
                            assert globalPoints.contains(p);
                        }
                        boolean reciprocate = false;
                        for (long neighbor : obj.neighbors) {
                            if (neighbor == acre.id) {
                                reciprocate = true;
                                break;
                            }
                        }
                        if (!reciprocate) {
                            System.err.printf("Returned Acre does not reciprocate neighbor ids: %s !--> %s\n",
                                              obj, acre);
                        }
                        if (!containsBothPoints(obj, acre.points[i], acre.points[(i + 1) % l])) {
                            int foundAt = -1;
                            for (int j = 0; j < l; j++) {
                                if (j == i) {
                                    continue; // no need to look here
                                }
                                if (containsBothPoints(obj, acre.points[i], acre.points[(i + 1) % l])) {
                                    foundAt = j;
                                    break;
                                }
                            }
                            if (foundAt >= 0) {
                                System.err.printf("Neighbor is not at expected position: acre[%s].neighbors[%d] !--> %s, was found at neighbors[%d]\n",
                                                  acre.getIDString(),
                                                  i,
                                                  AbstractCartographicElement.toIdString(neighborID),
                                                  foundAt);
                            } else {
                                System.err.printf("Neighbor is not at expected position: acre[%s].neighbors[%d] !--> %s, and does not share two points\n",
                                                  acre.getIDString(),
                                                  i,
                                                  AbstractCartographicElement.toIdString(neighborID));
                            }
                        } else if (reciprocate) {
                            continue;
                        }
                    }
                }
                errorCount.incrementAndGet();
            }
            int l = acre.points.length;
            assert acre.topographyDef != null && acre.topographyDef.length == l * 3 + 1;
            assert acre.seamStartVertexIds != null;
            assert acre.seamStartVertexIds.length == l * 7;
            assert validAcreSeamStarts(acre.seamStartVertexIds) : toString(chopDown(7, acre.seamStartVertexIds));
            assert acre.zoneStartVertexIds != null && acre.zoneStartVertexIds.length == l * 4;
        }

        private boolean containsBothPoints(AbstractCartographicElement obj, GeoPoint pointA, GeoPoint pointB) {
            for (GeoPoint pointP : obj.points) {
                if (pointP.equals(pointA)) {
                    pointA = null;
                    if (pointB == null) break;
                }
                if (pointP.equals(pointB)) {
                    pointB = null;
                    if (pointA == null) break;
                }
            }
            return pointA == null && pointB == null;
        }

        private String toString(long[][] longs) {
            StringBuilder buffer = new StringBuilder();
            buffer.append('\n');
            for (long[] arr : longs) {
                if (buffer.length() > 1) {
                    buffer.append(",\n");
                }
                buffer.append(Arrays.toString(arr));
            }
            return buffer.toString();
        }

        private boolean validAcreSeamStarts(long[] seamStartVertexIds) {
            long maxInner = seamStartVertexIds[0];
            long minOuter = seamStartVertexIds[5];
            for (int i = 0, k = 0, l = seamStartVertexIds.length; i < l; i += 7) {
                for (int j = 0; j < 5; j++) {
                    maxInner = Math.max(maxInner, seamStartVertexIds[k++]);
                }
                for (int j = 5; j < 7; j++) {
                    minOuter = Math.min(minOuter, seamStartVertexIds[k++]);
                }
            }
            return maxInner < minOuter;
        }

        private long[][] chopDown(int chopAt, long[] values) {
            assert chopAt > 0 && values.length % chopAt == 0;
            long[][] result = new long[values.length / chopAt][];
            for (int i = 0, j = 0, l = values.length; j < l; i++, j += chopAt) {
                result[i] = new long[Math.min(chopAt, l - j)];
                System.arraycopy(values, j, result[i], 0, result.length);
            }
            return result;
        }
    }

    private static abstract class AcreVertexValidator extends VertexValidator {

        private static final long zoneDivisions = 64;
        private static final long innerVerticesPerZone = ((zoneDivisions + 1) * (zoneDivisions + 2)) / 2 - zoneDivisions * 3;

        protected boolean debugMe;

        protected void addZoneBoundaryVertexIds(Acre acre) {
            boolean debug = debugMe && (acre.id == 290288384147457L || acre.id == 290292544569353L);
            if (debug) {
                System.out.println("Acre " + acre.id + ":");
            }
            assert acre.topographyDef.length == acre.points.length * 3 + 1;
            int boundary = acre.points.length * 2 + 1;
            if (VALIDATE_ACRE_BOUNDARY_MIDPOINT_VERTEX_COUNTS) {
                for (int i = 1, l = acre.points.length + 1; i < l; i++) {
                    add(acre.topographyDef[i]); // corner points and edge midpoints
                    if (debug) {
                        System.out.println("\ttopographyDef[" + i + "]=" + acre.topographyDef[i]);
                    }
                }
            }
            if (VALIDATE_ACRE_BOUNDARY_CORNER_VERTEX_COUNTS) {
                for (int i = acre.points.length + 1; i < boundary; i++) {
                    add(acre.topographyDef[i]); // corner points and edge midpoints
                    if (debug) {
                        System.out.println("\ttopographyDef[" + i + "]=" + acre.topographyDef[i]);
                    }
                }
            }
            if (VALIDATE_ACRE_INNER_VERTEX_COUNTS) {
                add(acre.topographyDef[0]); // center point
                for (int i = boundary, l = acre.topographyDef.length; i < l; i++) {
                    add(acre.topographyDef[i]); // spoke midpoints
                }
            }
            if (VALIDATE_ZONE_BOUNDARY_COUNTS) {
                assert acre.seamStartVertexIds.length == acre.points.length * 7;
                for (long id : acre.seamStartVertexIds) {
                    addAll(id, 63);
                }
            }
        }

        protected void addInnerZoneVertexIds(Acre acre) {
            if (VALIDATE_ZONE_VERTEX_COUNTS) {
                assert acre.zoneStartVertexIds.length == acre.points.length * 4;
                for (long id : acre.zoneStartVertexIds) {
                    addAll(id, (int)innerVerticesPerZone);
                }
            }
        }
    }

    private static abstract class SingleSectorVertexValidator extends AcreVertexValidator {

        private final Sector sector;

        private SingleSectorVertexValidator(Sector sector) {
            this.sector = sector;
            this.debugMe = false; // sector.id == 0x80500000000L;
        }

        protected boolean containsPentagonalAcre = false;
        protected SectorDebugger debugger;

        @Override
        protected void processVertices() {
            for (Acre acre : sector.getInnerAcres()) {
                processAcre(acre);
            }
            for (Acre acre : sector.getSharedAcres()) {
                processAcre(acre);
            }
        }

        private void processAcre(Acre acre) {
            if (acre.points.length == 5) {
                containsPentagonalAcre = true;
            }
            addZoneBoundaryVertexIds(acre);
            addInnerZoneVertexIds(acre);
        }

        @Override
        protected void showDetailedCounts(SectorDebugger.VertexCountAccessor vca) {
            debugger = new SectorDebugger(sector, vca, EDGE_MIDPOINTS);
            for (Acre acre : sector.getInnerAcres()) {
                debugger.addAcreVertices(acre);
            }
            for (Acre acre : sector.getSharedAcres()) {
                debugger.addAcreVertices(acre);
            }
            debugger.showIt();
        }
    }

    private static class GlobalVertexValidator extends AcreVertexValidator implements ProgressExportingRunnable {

        private final Globe globe;
        private final ValidateResult validateResult;

        private ProgressExporter progressExporter;

        public GlobalVertexValidator(Globe globe, ValidateResult validateResult) {
            this.globe = globe;
            this.validateResult = validateResult;
        }

        public void setProgressExporter(ProgressExporter progressExporter) {
            this.progressExporter = progressExporter;
        }

        @Override
        protected void processVertices() {
            int total = GeoSpec.SECTORS.iGet();
            int count = 0;
            for (GlobalSector globalSector : globe.sectors) {
                for (Sector sector : globalSector.getSectors()) {
                    for (Acre acre : sector.getSharedAcres()) {
                        if (!acre.flavor.inner) {
                            addZoneBoundaryVertexIds(acre);
                        }
                    }
                    progressExporter.exportProgress(++count, total);
                }
            }
        }

        @Override
        protected void recordError(Object message) {
            if (validateResult.errorCount.incrementAndGet() < 50) {
                super.recordError(message);
            }
        }

        @Override
        protected long[] getExpectedCounts() {
            // we are only tallying zone boundary vertices of shared acres

            long zoneDivisions = 64;
            long verticesPerSeam = zoneDivisions - 1;
            long seamVerticesPerAcre = 7 * verticesPerSeam * 6;
            long majorVerticesPerAcre = 6 + 12 + 1; // corners + midpoints + center
            long verticesPerAcre = seamVerticesPerAcre + majorVerticesPerAcre;
            long verticesPerPentagonalAcre = (verticesPerAcre - 1) * 5 / 6 + 1;

            long duplicates2 = GeoSpec.TWO_SECTOR_ACRES.get() * verticesPerAcre;
            long duplicates5 = GeoSpec.PENTAGONAL_ACRES.get() * verticesPerPentagonalAcre;
            long duplicates6 = GeoSpec.SIX_SECTOR_ACRES.get() * verticesPerAcre;

            long[] counts = VertexValidator.createZeroCountsArray();
            counts[1] = COUNT_NOT_CHECKED; // don't count
            counts[2] = duplicates2;
            counts[5] = duplicates5;
            counts[6] = duplicates6;
            return counts;
        }
    }

    private static class AcreWriter implements Runnable {

        private final List<Acre> acres;
        private final BinaryStore binaryStore;
        private final WriteSummary writeSummary;
        private final Consumer<AcreDetail> newAcreListener;
        private final boolean dataWriteEnabled;
        private SerializerStrategy<AcreDetail> serializer;
        private ByteBuffer buffer;

        public AcreWriter(List<Acre> acres,
                          BinaryStore binaryStore,
                          Consumer<AcreDetail> newAcreListener,
                          WriteSummary writeSummary) {
            this.acres = acres;
            this.binaryStore = binaryStore;
            this.newAcreListener = newAcreListener;
            this.writeSummary = writeSummary;
            this.dataWriteEnabled = !Configuration.DATABASE_HARMLESS.getBoolean(false);
        }

        public void run() {
            serializer = AcreDetail.DEFINITION.getSerializer();
            buffer = ByteBuffer.allocate(1024 * 1024);
            try {
                for (Acre acre : acres) {
                    buffer.clear();
                    save(acre);
                }
            } finally {
                serializer = null;
                buffer = null;
            }
        }

        private void save(final Acre acre) {
            AcreDetail detail = new AcreDetail();
            acre.applyPackDataToGraphAcre(detail);
            serializer.serialize(detail, buffer);
            buffer.flip();
            byte[] data = new byte[buffer.limit()];
            buffer.get(data);
            writeSummary.byteCount.addAndGet(data.length);
            if (dataWriteEnabled) {
                binaryStore.createBinaryResource(AcreDetail.DEFINITION, detail.getId(), data);
            }
            if ((writeSummary.acreCount.incrementAndGet() & 0x3FF) == 0) {
                System.out.print('.');
            }
            if (newAcreListener != null) {
                newAcreListener.consume(detail);
            }
        }
    }

    private static class AcreListener implements Consumer<AcreDetail> {

        private final AcreDetail[] capturedAcres;
        private final Map<Integer,Integer> captureAcresToArray;

        private AcreListener() {
            capturedAcres = new AcreDetail[7];
            captureAcresToArray = new HashMap<Integer,Integer>();
        }

        public AcreDetail[] getCapturedAcres() {
            return capturedAcres;
        }

        public boolean captureSuccessful() {
            for (AcreDetail capturedAcre : capturedAcres) {
                if (capturedAcre == null) {
                    return false;
                }
            }
            return true;
        }

        public synchronized void consume(AcreDetail value) {
            if (captureAcresToArray.isEmpty()) {
                capturedAcres[0] = value;
                int[] ids = new int[6];
                if (value.getNeighborIds(ids) == 6) {
                    for (int i = 0; i < 6; i++) {
                        captureAcresToArray.put(ids[i], i + 1);
                    }
                }
            } else {
                Integer index = captureAcresToArray.get(value.getId());
                if (index != null) {
                    capturedAcres[index] = value;
                }
            }
        }
    }

    private static class VertexWriter implements Runnable {

        private final WriteSummary writeSummary;
        private final File globalVerticesFile;
        private final VertexStore vertexStore;
        private final int count;
        private final double radius;

        public VertexWriter(WriteSummary writeSummary, File globalVerticesFile, VertexStore vertexStore) {
            this.writeSummary = writeSummary;
            this.globalVerticesFile = globalVerticesFile;
            this.vertexStore = vertexStore;
            this.count = GeoSpec.POINTS_SHARED_MANY_ZONE.iGet();
            this.radius = GeoSpec.APPROX_RADIUS_METERS.get();
        }

        @Override
        public void run() {
            final int count = GeoSpec.POINTS_SHARED_MANY_ZONE.iGet();
            final int step = 2 * (Double.SIZE >> 3);
            System.out.printf("Reading global vertices to temp file... (%,d bytes)\n", count * step);
            final ByteBuffer buffer = ByteBuffer.allocate(count * step);
            try {
                FileInputStream in = new FileInputStream(globalVerticesFile);
                in.getChannel().read(buffer);
                in.close();
                globalVerticesFile.delete();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            assert buffer.remaining() == 0;

            buffer.flip();
            System.out.println("Saving to disk...   (each '.' represents 1024 vertices)");
            RangeBasedLongSet ids = new RangeBasedLongSet();
            int j, k, n = 0;
            double x, y, z;
            double c, latitude, longitude;
            for (int i = 0; i < count; i += VertexBlock.VERTEX_COUNT) {
                k = Math.min(i + VertexBlock.VERTEX_COUNT, count) - 1;
                ids.clear();
                ids.addAll(i, k);
                VertexStore.VertexWriter vertexWriter = vertexStore.write(ids);
                for (j = i; j <= k; j++) {
                    longitude = buffer.getDouble();
                    latitude = buffer.getDouble();
                    c = Math.cos(latitude);
                    x = Math.sin(longitude) * c;
                    y = Math.cos(longitude) * c;
                    z = Math.sin(latitude);
                    vertexWriter.next(0xFFFFFF, x * radius, y * radius, z * radius, 0.0f);
                    if ((++n & 0x03FF) == 0) {
                        System.out.print(".");
                    }
                }
                vertexWriter.done();
            }
            assert n == count;
            writeSummary.vertexCount.addAndGet(n);
            writeSummary.byteCount.addAndGet(n * 16);
            vertexStore.flushChanges();
        }
    }
}
