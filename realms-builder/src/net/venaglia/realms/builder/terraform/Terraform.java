package net.venaglia.realms.builder.terraform;

import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.CompositeShape;
import net.venaglia.gloo.util.ColorGradient;
import net.venaglia.gloo.view.View3D;
import net.venaglia.realms.builder.terraform.flow.FlowObserver;
import net.venaglia.realms.builder.terraform.flow.FlowQuery;
import net.venaglia.realms.builder.terraform.flow.FlowQueryInterface;
import net.venaglia.realms.builder.terraform.flow.FlowSimulator;
import net.venaglia.realms.builder.terraform.flow.TectonicVectorArrow;
import net.venaglia.realms.builder.terraform.impl.AbstractTransferBuffer;
import net.venaglia.realms.builder.view.AcreView;
import net.venaglia.realms.builder.view.AcreViewer;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.chemistry.elements.MaterialElement;
import net.venaglia.realms.common.map.BinaryStore;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.WorldMapImpl;
import net.venaglia.realms.common.util.work.Results;
import net.venaglia.realms.common.util.work.WorkManager;
import net.venaglia.realms.common.util.work.WorkQueue;
import net.venaglia.realms.common.util.work.WorkSourceAdapter;
import net.venaglia.realms.common.util.work.WorkSourceKey;
import net.venaglia.realms.spec.GeoSpec;
import net.venaglia.realms.common.map.WorldMap;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 7:12 PM
 */
public class Terraform implements FlowObserver {

    public static final int NUM_TRANSFER_WORKERS = 64;

    private final Iterable<? extends FlowQuery> queries;
    private final AcreView.AcreViewGeometry geometry;
    private final FlowSimulator flowSimulator;
    private final TransferBufferProviderImpl transferBufferProvider;
    private final Collection<TransferWorker> transferWorkers;

    private AbstractTransferBuffer[] transferBuffers;
    private long[] mineralContentBuffer;
    private View3D acreView;
    private AcreView[] acreViews;
    private Runnable clearScratchpad;

    public Terraform(WorldMap worldMap) {
        this.flowSimulator = new FlowSimulator(25, 8000, 1000, 10.0);
        this.flowSimulator.setObserver(this);
        this.transferBufferProvider = new TransferBufferProviderImpl();
        this.transferWorkers = new ArrayList<TransferWorker>(NUM_TRANSFER_WORKERS);
        int count = (int)GeoSpec.ACRES.get();
        List<AcreQuery> queries = new ArrayList<AcreQuery>(count);
        final AcreDetail[] allAcres = new AcreDetail[count];

        this.geometry = getGeometry(worldMap, allAcres);

        this.transferBuffers = new AbstractTransferBuffer[(int)GeoSpec.ACRE_SEAMS.get() * 2];
        final long[] scratchpad = new long[transferBuffers.length];
        buildTransferBuffers(allAcres, scratchpad);
        Arrays.sort(transferBuffers);

        buildAcreQueries(queries, allAcres);
        Random seed = new SecureRandom();
        for (int i = 0; i < NUM_TRANSFER_WORKERS; i++) {
            transferWorkers.add(new TransferWorker(i, allAcres, new Random(seed.nextLong())));
        }
        mineralContentBuffer = new long[count];
        MineralContentFactory.fillRandom(mineralContentBuffer);

        System.out.printf("Built queries for %d acres\n", count);
        acreViews = new AcreView[]{
                new ElementAcreView(geometry, MaterialElement.EARTH, makeGradient(0.5f, 0.3f, 0.0f)),
                new ElementAcreView(geometry, MaterialElement.WATER, makeGradient(0.1f, 0.3f, 0.8f)),
                new ElementAcreView(geometry, MaterialElement.AIR, makeGradient(0.4f, 0.5f, 0.9f)),
                new ElementAcreView(geometry, MaterialElement.FIRE, makeGradient(1.0f, 0.4f, 0.0f)),
                new ElementAcreView(geometry, MaterialElement.PLASMA, makeGradient(0.7f, 0.2f, 0.9f)),
                new AcreView(geometry, "Elevation") {

                    private ColorGradient gradient = new ColorGradient(new Color(0.0f,0.0f,0.25f), new Color(1.0f,1.0f,1.0f))
                            .addStop(0.10f, new Color(0.0f, 0.0f, 0.4f))
                            .addStop(0.35f, new Color(0.0f, 0.2f, 1.0f))
                            .addStop(0.49f, new Color(0.6f, 0.9f, 1.0f))
                            .addStop(0.50f, new Color(1.0f,0.9f,0.8f))
                            .addStop(0.51f, new Color(0.0f, 0.8f, 0.1f))
                            .addStop(0.65f, new Color(0.4f, 0.1f, 0.0f))
                            .addStop(0.90f, new Color(0.8f, 0.8f, 0.9f));

                    @Override
                    protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
                        float log = allAcres[acreId].getElevation();
                        float v = Math.max(0.0f, Math.min(1.0f, log * 0.5f + 0.5f));
                        gradient.applyColor(v, acreRenderer);
                    }
                },
                new AcreView(geometry, "Pressure") {

                    private ColorGradient gradient = makeGradient(0.7f, 0.5f, 0.0f);

                    @Override
                    protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
                        gradient.applyColor(allAcres[acreId].getPressure(), acreRenderer);
                    }
                }
        };
        this.clearScratchpad = new Runnable() {
            public void run() {
                Arrays.fill(scratchpad, MineralContent.CLEAN_VALUE);
            }
        };
        this.queries = queries;
    }

    private void buildAcreQueries(List<AcreQuery> queries, AcreDetail[] allAcres) {
        int[] neighborIds = new int[6];
        int count = 0;
        System.out.println("Building acre queries...");
        for (AcreDetail acreDetail : allAcres) {
            int c = acreDetail.getNeighborIds(neighborIds);
            AcreDetail[] neighbors = new AcreDetail[c];
            for (int idx = 0; idx < c ; idx++) {
                int neighborId = neighborIds[idx];
                neighbors[idx] = allAcres[neighborId];
            }
            queries.add(new AcreQuery(acreDetail,
                                       neighbors,
                                       transferBufferProvider,
                                       transferBufferProvider.getLeft(acreDetail),
                                       transferBufferProvider.getRight(acreDetail)));
            if ((count++ & 1023) == 1023) {
                System.out.print(".");
            }
        }
        System.out.println();
    }

    private void buildTransferBuffers(final AcreDetail[] allAcres,final long[] scratchpad) {
        int[] neighborIds = new int[6];
        int tbi = 0;
        for (AcreDetail acreDetail : allAcres) {
            int acreId = acreDetail.getId();
            int c = acreDetail.getNeighborIds(neighborIds);
            for (int idx = 0; idx < c ; idx++) {
                int neighborId = neighborIds[idx];
                final int scratchpadIndex = tbi;
                transferBuffers[tbi] = new AbstractTransferBuffer(acreId, neighborId, tbi) {
                    @Override
                    protected AcreDetail getAcre(int id) {
                        return allAcres[id];
                    }

                    @Override
                    protected MineralContent getMineralContent(int id, MineralContentFactory.Neighbor neighbor) {
                        return MineralContentFactory.get(mineralContentBuffer, id, scratchpad, scratchpadIndex, neighbor);
                    }
                };
                tbi++;
            }
        }
        assert tbi == transferBuffers.length;
    }

    private AcreView.AcreViewGeometry getGeometry(final WorldMap worldMap, final AcreDetail[] allAcres) {
        final boolean loadWithParanoia = Configuration.PARANOIA_ON_ACRES.getBoolean();
        final Map<Integer,String> signatures = loadWithParanoia ? loadSignatures() : null;
//        SerializerStrategy<AcreDetail> serializer = AcreDetail.DEFINITION.getSerializer();
        final AcreView.AcreViewGeometry geometry = new AcreView.AcreViewGeometry(worldMap.getVertexStore(),
                                                                                 (int)GeoSpec.ACRES.get(),
                                                                                 GeoSpec.POINTS_SHARED_MANY_ZONE.get(),
                                                                                 GeoSpec.APPROX_RADIUS_METERS.get());

        final AtomicInteger count = new AtomicInteger();
        WorkSourceKey<Void> key = WorkSourceKey.create("geometry", Void.class);
        WorkManager workManager = new WorkManager("Acre loader");
        System.out.println("Reading acres...");
        workManager.addWorkSource(new WorkSourceAdapter<Void>(key) {
            @Override
            public void addWork(WorkQueue workQueue, Results dependencies) {
                for (int i = 0; i < 64; i++) {
                    GeometryAcreReader reader = new GeometryAcreReader(i,
                                                                       64,
                                                                       allAcres,
                                                                       signatures,
                                                                       geometry,
                                                                       worldMap.getBinaryStore(),
                                                                       count);
                    workQueue.addWorkUnit(reader);
                }
            }
        });
        workManager.getResults().getResult(key);
        System.out.println();
        geometry.close();
        return geometry;
    }

    private ColorGradient makeGradient (float r, float g, float b) {
        Color c = new Color(r, g, b);
        ColorGradient gradient = new ColorGradient(Color.BLACK, Color.WHITE);
        gradient.addStop(0.5f, c);
        return new ColorGradient(gradient.get(0.2f), gradient.get(0.8f)).addStop(0.5f, c);
    }

    private Map<Integer,String> loadSignatures() {
        HashMap<Integer,String> signatures = new HashMap<Integer,String>(8192);
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader("validate.signatures.txt"));
            for (String line = in.readLine(); line != null; line = in.readLine()) {
                Integer id = Integer.parseInt(line.substring(0, 8), 16);
                assert !signatures.containsKey(id);
                signatures.put(id, line);
            }
        } catch (IOException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    // don't care
                }
            }
            return null;
        }
        return signatures;
    }

    public void run() {
        prepareView();
        System.out.println("Starting planetary core...");
        flowSimulator.start();
        flowSimulator.waitUntilRunning();
        System.out.println("Adding some magma...");
        flowSimulator.waitUntilStable();
        System.out.println("Forming oceans and continents...");
        flowSimulator.waitUntilDone();
    }

    private void prepareView() {
        double radius = GeoSpec.APPROX_RADIUS_METERS.get();
        CompositeShape arrows = new CompositeShape();
        for (TectonicVectorArrow arrow : flowSimulator.getTectonicArrows(radius)) {
            arrows.addShape(arrow);
        }
        acreView = AcreViewer.view(acreViews,
                                   radius,
                                   "Planet Engine",
                                   new Dimension(1280,1024),
                                   arrows);
    }

    private int frameCount = 0;

    public void frame(FlowQueryInterface queryInterface) {
        if (queryInterface.isStable()) {
            clearScratchpad.run();
            queryInterface.query(queries);
            queryInterface.runNext(transferWorkers);
            if ((++frameCount % 100) == 0) {
                System.out.println("Frame " + frameCount);
            }
        }
    }

    public static void main(String[] args) {
        Configuration.TERAFORMING.setBoolean(true);
        Terraform terraform = new Terraform(new WorldMapImpl());
        terraform.run();
    }

    private static class GeometryAcreReader implements Runnable {

        private final int base;
        private final int step;
        private final AcreDetail[] allAcres;
        private final Map<Integer,String> signatures;
        private final AcreView.AcreViewGeometry geometry;
        private final BinaryStore binaryStore;
        private final AtomicInteger count;

        private GeometryAcreReader(int base,
                                   int step,
                                   AcreDetail[] allAcres,
                                   Map<Integer, String> signatures,
                                   AcreView.AcreViewGeometry geometry,
                                   BinaryStore binaryStore,
                                   AtomicInteger count) {
            this.base = base;
            this.step = step;
            this.allAcres = allAcres;
            this.signatures = signatures;
            this.geometry = geometry;
            this.binaryStore = binaryStore;
            this.count = count;
        }

        @Override
        public void run() {
            boolean loadWithParanoia = Configuration.PARANOIA_ON_ACRES.getBoolean();
            SerializerStrategy<AcreDetail> serializer = AcreDetail.DEFINITION.getSerializer();
            for (int i = base, l = allAcres.length; i < l; i += step) {
                BinaryResource resource = binaryStore.getBinaryResource(AcreDetail.MIMETYPE, i);
                ByteBuffer byteBuffer = ByteBuffer.wrap(resource.getData());
                AcreDetail acreDetail = serializer.deserialize(byteBuffer);
                assert i == acreDetail.getId();
                if (loadWithParanoia) {
                    String sig = signatures.get(i);
                    String fsc = acreDetail.formatForSanityCheck();
                    assert fsc.equals(sig);
                    signatures.remove(i);
                }
                allAcres[i] = acreDetail;
                geometry.visit(acreDetail);
                if ((count.getAndIncrement() & 1023) == 1023) {
                    System.out.print(".");
                }
            }
        }
    }

    private class TransferBufferProviderImpl implements TransferBufferProvider {

        public int getLeft(AcreDetail acre) {
            int i = binarySearch(acre.getId(), Integer.MIN_VALUE, 0, transferBuffers.length - 1);
            return -(i + 1);
        }

        public int getRight(AcreDetail acre) {
            int i = binarySearch(acre.getId(), Integer.MAX_VALUE, 0, transferBuffers.length - 1);
            return -(i + 1);
        }

        public TransferBuffer getTransferBufferFor(int left, int right, AcreDetail acre, AcreDetail neighbor) {
            int acreId = acre.getId();
            int neighborId = neighbor.getId();
            int position = binarySearch(acreId, neighborId, left, right);
            assert position >= 0;
            return transferBuffers[position];
        }

        private int binarySearch(int acreId1, int acreId2, int low, int high) {
            while (low <= high) {
                int mid = (low + high) >>> 1;
                AbstractTransferBuffer midVal = transferBuffers[mid];
                int cmp = midVal.compareTo(acreId1, acreId2);
                if (cmp < 0)
                    low = mid + 1;
                else if (cmp > 0)
                    high = mid - 1;
                else
                    return mid; // key found
            }
            return -(low + 1);  // key not found.
        }
    }

    private class TransferWorker implements Runnable {

        private final int offset;
        private final AcreDetail[] allAcres;
        private final Random random;

        TransferWorker(int offset, AcreDetail[] allAcres, Random random) {
            this.offset = offset;
            this.allAcres = allAcres;
            this.random = random;
        }

        public void run() {
            TransferBuffer transferBuffer_a, transferBuffer_b;
            MaterialElement[] materialElements = MaterialElement.values();
            MineralContent[] mineralContents = new MineralContent[6];
            int[] neighborIds = new int[6];
            float[] percentages = new float[6 * materialElements.length];
            float[] pct = new float[materialElements.length];
            for (int acreId = offset, l = allAcres.length; acreId < l; acreId += NUM_TRANSFER_WORKERS) {
                AcreDetail acreDetail = allAcres[acreId];
                int neighborCount = acreDetail.getNeighborIds(neighborIds);
                Arrays.fill(percentages, 0);
                float sumNeighborElevation = 0.0f;
                for (int i = 0, j = 0; i < neighborCount; i++, j += materialElements.length) {
                    AcreDetail neighbor = allAcres[neighborIds[i]];
                    MineralContentFactory.Neighbor whichNeighbor = MineralContentFactory.Neighbor.byIndex(i);
                    transferBuffer_a = transferBufferProvider.getTransferBufferFor(0,
                                                                                   transferBuffers.length,
                                                                                   acreDetail,
                                                                                   neighbor);
                    transferBuffer_b = transferBufferProvider.getTransferBufferFor(0,
                                                                                   transferBuffers.length,
                                                                                   neighbor,
                                                                                   acreDetail);
                    sumNeighborElevation += neighbor.getElevation();
                    float transfer = (float)(transferBuffer_a.getTransfer() - transferBuffer_b.getTransfer()) * 0.5f;
                    boolean reverse = (transfer < 0);
                    if (reverse) {
                        MineralContent mineralContent = transferBuffer_a.getAcre2MineralContent(whichNeighbor);
                        mineralContents[i] = mineralContent;
                        accumulate(-transfer, percentages, j, materialElements, mineralContent);
                    } else {
                        MineralContent mineralContent = transferBuffer_a.getAcre1MineralContent(whichNeighbor);
                        mineralContents[i] = mineralContent;
                        accumulate(transfer, percentages, j, materialElements, mineralContent);
                    }
                    assert mineralContents[i] != null;
                }
                assert validPercentages(percentages);
                MineralContent composite = MineralContentFactory.get(mineralContentBuffer, acreId, mineralContentBuffer, acreId, null);
                doTransfer(composite, neighborCount, mineralContents, percentages, pct, materialElements);

                float averageNeighborElevation = sumNeighborElevation / neighborCount;
                float targetElevation = computeTargetElevation(composite);
                float erosionElevation = computeErosionTargetElevation(acreDetail.getElevation());
                float pressureElevation = computePressureElevation(acreDetail.getPressure());
                // todo: adjust these weights for larger planets
                acreDetail.setElevation(erosionElevation * (92.0f / 128.0f) +
                                        pressureElevation * (10.0f / 128.0f) +
                                        averageNeighborElevation * (25.0f / 128.0f) +
                                        targetElevation * (1.0f / 128.0f));
            }
        }

        private float computePressureElevation(float acrePressure) {
            float p = acrePressure - 0.3f;
            return p * (p < 0.0f ? 2.0f : 3.0f);
        }

        private float computeTargetElevation(MineralContent composite) {
            float earth = composite.getPercent(MaterialElement.EARTH);
            float water = composite.getPercent(MaterialElement.WATER);
            float nominalElevation = earth + water * -2.0f;
            float air = composite.getPercent(MaterialElement.AIR);
            float fire = composite.getPercent(MaterialElement.FIRE);
            float extraElevation = (air + fire * 2.0f);
            return nominalElevation + extraElevation * extraElevation * extraElevation;
        }

        private float computeErosionTargetElevation(float currentElevation) {
            boolean aboveSeaLevel = currentElevation >= 0;
            if (aboveSeaLevel ? currentElevation < 0.01f : currentElevation > -0.01f) {
                return currentElevation;
            }
            float baseline = aboveSeaLevel ? 0.01f : -0.01f;
            float erosionTarget = aboveSeaLevel ? 0.05f : -0.05f;
            return (currentElevation - baseline) * (61.0f / 64.0f) + erosionTarget * (3.0f / 64.0f) + baseline;
        }

        private final int flowDenominator = 128;
        private final float flowMultiplier = 1.0f / flowDenominator;

        private void doTransfer(MineralContent composite,
                                int neighborCount,
                                MineralContent[] mineralContents,
                                float[] percentages,
                                float[] pct,
                                MaterialElement[] materialElements) {
            float totalNewPct = 0.0f;
            for (MaterialElement element : materialElements) {
                float totalElementPct = 0;
                for (int i = 0, j = element.ordinal(); i < neighborCount; i++, j+= materialElements.length) {
                     // percentage of this element, from a single neighbor
                    totalElementPct += percentages[j] * mineralContents[i].getPercent(element);
                }
                assert totalElementPct <= 1.0f;
                float jitter = Math.min(0.03125f, Math.max(-0.03125f, ((float)random.nextGaussian()) * flowMultiplier));
                float backPressure = (composite.getPercent(element) - 0.2f) * flowMultiplier;
                float myPct = 1.0f - totalElementPct;
                float newPct = composite.getPercent(element) * myPct + totalElementPct + jitter - backPressure;
                pct[element.ordinal()] = newPct;
                totalNewPct += newPct;
            }

            // normalize my content back to 100%
            float mul = 1.0f / totalNewPct;
            for (MaterialElement element : materialElements) {
                composite.setPercent(element, pct[element.ordinal()] * mul);
            }
        }

        private boolean validPercentages(float[] percentages) {
            float sum = 0;
            for (float pct : percentages) {
                sum += pct;
            }
            return sum <= 1.0f;
        }

        private void accumulate(float transfer,
                                float[] percentages,
                                int percentagesOffset,
                                MaterialElement[] materialElements,
                                MineralContent mineralContent) {
            for (int i = 0, j = percentagesOffset; i < materialElements.length; i++, j++) {
                percentages[j] += transfer * mineralContent.getPercent(materialElements[i]);
            }
        }
    }

    private class ElementAcreView extends AcreView {

        private final MaterialElement element;
        private final ColorGradient colorGradient;

        public ElementAcreView(AcreViewGeometry geometry, MaterialElement element, ColorGradient colorGradient) {
            super(geometry, element.name().charAt(0) + element.name().substring(1).toLowerCase());
            this.element = element;
            this.colorGradient = colorGradient;
        }

        @Override
        protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
            MineralContent mineralContent = MineralContentFactory.get(mineralContentBuffer,
                                                                      acreId,
                                                                      mineralContentBuffer,
                                                                      acreId,
                                                                      null);
            float percent = mineralContent.getPercent(element);
            float n = Math.min(1.0f, Math.max(0.0f, percent));
            colorGradient.applyColor(n, acreRenderer);
        }
    }
}

/*
Notes on stage 2 terraforming:

Acre marks:
  coastal boundary
  geographic peak
  river head
  lake bottom
  lake boundary

Acre lines:
  coastal boundary
  ridges
  waterways
  lake boundaries

Identify each continent or island in the world, identify all acres on a coastline
  Mark all acres with a positive elevation, having a neighbor with a negative elevation, as coastal boundary

Identify the highest elevation on each continent or island, these are the geographic peaks
From the center of each geographic peak, starting with the highest
  Build a line, starting from this acre
  Subsequent acres are the highest elevation, strictly less or equal than the current acre, not already on the line
  Repeat, stopping once when you reach a coastal boundary, or no neighbor meets the criteria
  From the center of the highest acre, pick the highest neighbor not already on the line, and build a second line
  Repeat, stopping once when you reach a coastal boundary, or no neighbor meets the criteria
  This defines a continental divide, when starting on the highest acre
For every continent or island, repeating until exhausted
  Find the next highest elevation not already on a line
  Subsequent acres are the highest elevation, strictly less or equal than the current acre, not already on this line
  Repeat, stopping once when you reach a coastal elevation, or another line is intersected
  If another line is intersected, mark this acre as river head, we'll come back to it later
  This identifies all ridges in the world
Identify the highest points on each continent, and all acres that were marked as a river-head while building ridge lines.
  Build a line, starting from these acres
  Subsequent acres are the lowest elevation, strictly less or equal than the current acre, not already on the line
  Repeat, stopping once you reach a coastal boundary, or no neighbor meets the criteria
  If a waterway crosses a ridge??? todo: deal with this, but how? Fail for now
  If a coastal boundary was not reached, mark the terminating acre as a lake bottom
  This defines all the waterways in the world
Find all lake bottoms
  Find the lowest path between acres to reach a river, lake or coastal boundary
  Walk all neighbors contiguous to the lake bottom, strictly lower than the highest point on the lowest path
  Mark all contiguous acres, that have a neighbor that is not part of this lake, as lake boundary
  Mark remaining contiguous acres as lake bottom
For all vertices, centered on an acre
  Compute and apply the elevation, based on the raw elevation from stage 1 terraforming
For all vertices shared between two acres and adjacent spoke vertices, over which a ridge line crosses
  Compute the intermediate elevation, favoring the higher of the two acres
For all vertices shared between two acres and adjacent spoke vertices, over which a waterway crosses
  Compute the intermediate elevation, favoring the lower of the two acres
For all other vertices shared between two acres and their adjacent spoke vertices
  Compute the intermediate elevation, favoring the average of the two acres
For all vertices shared between three acres
  ??? // resume here
Save all
  continents and islands
  ridges
  waterways
  lake boundaries
*/
