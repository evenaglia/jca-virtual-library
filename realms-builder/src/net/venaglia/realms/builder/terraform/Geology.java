package net.venaglia.realms.builder.terraform;

import static net.venaglia.realms.builder.terraform.flow.FlowSimulator.TectonicDensity.*;
import static net.venaglia.realms.builder.view.AcreView.makeGradient;

import net.venaglia.common.util.IntIterator;
import net.venaglia.common.util.extensible.ExtendedPropertyKey;
import net.venaglia.gloo.physical.geom.CompositeShape;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.util.ColorGradient;
import net.venaglia.realms.builder.terraform.flow.FlowObserver;
import net.venaglia.realms.builder.terraform.flow.FlowQuery;
import net.venaglia.realms.builder.terraform.flow.FlowQueryInterface;
import net.venaglia.realms.builder.terraform.flow.FlowSimulator;
import net.venaglia.realms.builder.terraform.flow.TectonicVectorArrow;
import net.venaglia.realms.builder.terraform.impl.AbstractTransferBuffer;
import net.venaglia.realms.builder.terraform.sets.AcreDetailExtendedPropertyProvider;
import net.venaglia.realms.common.map.GlobalVertexLookup;
import net.venaglia.realms.common.map.world.ref.AcreLookup;
import net.venaglia.realms.builder.terraform.sets.AcreSet;
import net.venaglia.realms.builder.view.AcreView;
import net.venaglia.realms.builder.view.AcreViews;
import net.venaglia.realms.common.chemistry.elements.MaterialElement;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.spec.GeoSpec;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

/**
 * User: ed
 * Date: 1/29/15
 * Time: 8:06 AM
 */
public class Geology implements FlowObserver, Runnable {

    public static final int NUM_TRANSFER_WORKERS = 64;

    private final FlowSimulator flowSimulator;
    private final TransferBufferProviderImpl transferBufferProvider;
    private final List<? extends FlowQuery> queries;
    private final Collection<TransferWorker> transferWorkers;
    private final int frameLimit;

    private AbstractTransferBuffer[] transferBuffers;
    private long[] mineralContentBuffer;
    private Runnable clearScratchpad;
    private Runnable cleanup;
    private AcreDetailExtendedPropertyProvider.FloatProvider pressureProvider;
    private int frameCount = 0;

    public Geology(int frameLimit,
                   final AcreSet allAcres,
                   AcreView.AcreViewGeometry geometry,
                   GlobalVertexLookup vertexLookup,
                   final AcreViews<AcreView> acreViews,
                   final AcreViews<Shape<?>> overlayShapes) {
        int count = GeoSpec.ACRES.iGet();
        this.frameLimit = frameLimit;
        FlowSimulator.TectonicDensity density;
        if (count < 10000) {
            density = LOW;
        } else if (count < 100000) {
            density = MEDIUM;
        } else {
            density = HIGH;
        }
        this.flowSimulator = new FlowSimulator(25, 8000, 1000, 10.0, density);
        this.flowSimulator.setObserver(this);
        this.transferBufferProvider = new TransferBufferProviderImpl();
        this.transferWorkers = new ArrayList<TransferWorker>(NUM_TRANSFER_WORKERS);
        ExtendedPropertyKey.FloatKey pressureKey = new ExtendedPropertyKey.FloatKey("pressure");
        this.pressureProvider = new AcreDetailExtendedPropertyProvider.FloatProvider(pressureKey);
        this.transferBuffers = new AbstractTransferBuffer[GeoSpec.ACRE_SEAMS.iGet() * 2];
        final long[] scratchpad = new long[transferBuffers.length];
        buildTransferBuffers(allAcres, scratchpad);
        Arrays.sort(transferBuffers);
        List<AcreQuery> queries = new ArrayList<AcreQuery>(count);
        buildAcreQueries(queries, allAcres, vertexLookup);
        Random seed = new SecureRandom();
        for (int i = 0; i < NUM_TRANSFER_WORKERS; i++) {
            transferWorkers.add(new TransferWorker(i, allAcres, count, new Random(seed.nextLong())));
        }
        mineralContentBuffer = new long[count];
        MineralContentFactory.fillRandom(mineralContentBuffer);
        this.clearScratchpad = new Runnable() {
            public void run() {
                Arrays.fill(scratchpad, MineralContent.CLEAN_VALUE);
            }
        };
        this.queries = queries;
        acreViews.addView(
                new AcreView(geometry, "Pressure") {

                    private ColorGradient gradient = makeGradient(0.7f, 0.5f, 0.0f);

                    @Override
                    protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
                        float value = pressureProvider == null
                                      ? 0.5f
                                      : pressureProvider.getFloat(allAcres.get(acreId), 0.0f);
                        gradient.applyColor(value, acreRenderer);
                    }
                }
        );
        acreViews.addView(new ElementAcreView(geometry, MaterialElement.EARTH, makeGradient(0.5f, 0.3f, 0.0f)));
        acreViews.addView(new ElementAcreView(geometry, MaterialElement.WATER, makeGradient(0.1f, 0.3f, 0.8f)));
        acreViews.addView(new ElementAcreView(geometry, MaterialElement.AIR, makeGradient(0.4f, 0.5f, 0.9f)));
        acreViews.addView(new ElementAcreView(geometry, MaterialElement.FIRE, makeGradient(1.0f, 0.4f, 0.0f)));
        acreViews.addView(new ElementAcreView(geometry, MaterialElement.PLASMA, makeGradient(0.7f, 0.2f, 0.9f)));
        double radius = GeoSpec.APPROX_RADIUS_METERS.get();
        CompositeShape arrows = new CompositeShape();
        arrows.setName("Arrows");
        for (TectonicVectorArrow arrow : flowSimulator.getTectonicArrows(radius)) {
            arrows.addShape(arrow);
        }
        overlayShapes.addView(arrows);
        cleanup = new Runnable() {
            @Override
            public void run() {
                flowSimulator.destroy();
                queries.clear();
                acreViews.removeView("Earth");
                acreViews.removeView("Water");
                acreViews.removeView("Air");
                acreViews.removeView("Fire");
                acreViews.removeView("Plasma");
                acreViews.removeView("Pressure");
                overlayShapes.removeView("Arrows");
                pressureProvider = null; // delete the extended property
            }
        };
    }

    private void buildAcreQueries(List<AcreQuery> queries, AcreSet allAcres, GlobalVertexLookup vertexLookup) {
        int count = 0;
        AcreQuery.Shared shared = new AcreQuery.Shared(allAcres,
                                                       vertexLookup,
                                                       pressureProvider,
                                                       transferBufferProvider,
                                                       GeoSpec.APPROX_RADIUS_METERS.get(),
                                                       GeoSpec.ACRES.iGet());
        System.out.println("Building acre queries...");
        for (IntIterator iterator = allAcres.getAcreIds().iterator(); iterator.hasNext();) {
            int acreId = iterator.next();
            queries.add(new AcreQuery(shared,
                                       acreId,
                                       transferBufferProvider.getLeft(acreId),
                                       transferBufferProvider.getRight(acreId)));
            if ((count++ & 1023) == 1023) {
                System.out.print(".");
            }
        }
//        int count = 0;
//        int[] neighborIds = new int[6];
//        for (AcreNavigator<?> navigator = allAcres.navigateReadOnly(); navigator.hasNext();) {
//            AcreDetail acreDetail = navigator.next();
//            int c = acreDetail.getNeighborIds(neighborIds);
//            AcreDetail[] neighbors = new AcreDetail[c];
//            for (int idx = 0; idx < c ; idx++) {
//                int neighborId = neighborIds[idx];
//                neighbors[idx] = allAcres.get(neighborId);
//            }
//            queries.add(new AcreQuery(pressureProvider,
//                                      acreDetail,
//                                      neighbors,
//                                      transferBufferProvider,
//                                      transferBufferProvider.getLeft(acreDetail),
//                                      transferBufferProvider.getRight(acreDetail)));
//            if ((count++ & 1023) == 1023) {
//                System.out.print(".");
//            }
//        }
        System.out.println();
    }

    private void buildTransferBuffers(final AcreSet allAcres,final long[] scratchpad) {
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
                        return allAcres.get(id);
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

    public void frame(FlowQueryInterface queryInterface) {
        if (queryInterface.isStable()) {
            if ((++frameCount % 64) == 0) {
                System.out.print(".");
            }
            clearScratchpad.run();
            queryInterface.query(queries);
            queryInterface.runNext(transferWorkers);
            if (frameCount >= frameLimit) {
                flowSimulator.stop();
                System.out.println();
            }
        }
    }

    public void run() {
        System.out.println("Starting planetary core...");
        flowSimulator.start();
        flowSimulator.waitUntilRunning();
        System.out.println("Adding some magma...");
        flowSimulator.waitUntilStable();
        System.out.println("Forming oceans and continents... (" + frameLimit + " frames)");
        flowSimulator.waitUntilDone();
        cleanup.run();
    }

    private class TransferBufferProviderImpl implements TransferBufferProvider {

        public int getLeft(int acreId) {
            int i = binarySearch(acreId, Integer.MIN_VALUE, 0, transferBuffers.length - 1);
            return -(i + 1);
        }

        public int getRight(int acreId) {
            int i = binarySearch(acreId, Integer.MAX_VALUE, 0, transferBuffers.length - 1);
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
        private final AcreLookup allAcres;
        private final Random random;
        private final int acreCount;

        TransferWorker(int offset, AcreLookup allAcres, int acreCount, Random random) {
            this.offset = offset;
            this.allAcres = allAcres;
            this.random = random;
            this.acreCount = acreCount;
        }

        public void run() {
            TransferBuffer transferBuffer_a, transferBuffer_b;
            MaterialElement[] materialElements = MaterialElement.values();
            float[] naturalOccurrence = getNormalizedNaturalOccurrence(materialElements);
            MineralContent[] mineralContents = new MineralContent[6];
            int[] neighborIds = new int[6];
            float[] percentages = new float[6 * materialElements.length];
            float[] pct = new float[materialElements.length];
            for (int acreId = offset, l = acreCount; acreId < l; acreId += NUM_TRANSFER_WORKERS) {
                AcreDetail acreDetail = allAcres.get(acreId);
                int neighborCount = acreDetail.getNeighborIds(neighborIds);
                Arrays.fill(percentages, 0);
                float sumNeighborElevation = 0.0f;
                for (int i = 0, j = 0; i < neighborCount; i++, j += materialElements.length) {
                    AcreDetail neighbor = allAcres.get(neighborIds[i]);
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
                assert validPercentages(percentages); // fixme: sometimes fails on LARGE planet
                MineralContent composite = MineralContentFactory.get(mineralContentBuffer, acreId, mineralContentBuffer, acreId, null);
                doTransfer(composite, neighborCount, mineralContents, percentages, pct, materialElements, naturalOccurrence);

                float averageNeighborElevation = sumNeighborElevation / neighborCount;
                float targetElevation = computeTargetElevation(composite);
                float erosionElevation = computeErosionTargetElevation(acreDetail.getElevation());
                float pressureElevation = computePressureElevation(pressureProvider.getFloat(acreDetail, 0.0f));
                acreDetail.setElevation(erosionElevation * (92.0f / 128.0f) +
                                        pressureElevation * (10.0f / 128.0f) +
                                        averageNeighborElevation * (25.0f / 128.0f) +
                                        targetElevation * (1.0f / 128.0f));
            }
        }

        private float[] getNormalizedNaturalOccurrence(MaterialElement[] materialElements) {
            float[] naturalOccurrence = new float[materialElements.length];
            float sumNaturalOccurrence = 0.0f;
            for (int i = 0; i < materialElements.length; i++) {
                MaterialElement element = materialElements[i];
                naturalOccurrence[i] = element.getNaturalOccurrence();
                sumNaturalOccurrence += element.getNaturalOccurrence();
            }
            for (int i = 0; i < naturalOccurrence.length; i++) {
                naturalOccurrence[i] = naturalOccurrence[i] / sumNaturalOccurrence;
            }
            return naturalOccurrence;
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
                                MaterialElement[] materialElements,
                                float[] naturalOccurrence) {
            float totalNewPct = 0.0f;
            for (MaterialElement element : materialElements) {
                float totalElementPct = 0;
                for (int i = 0, j = element.ordinal(); i < neighborCount; i++, j+= materialElements.length) {
                     // percentage of this element, from a single neighbor
                    totalElementPct += percentages[j] * mineralContents[i].getPercent(element);
                }
                assert totalElementPct <= 1.0f;
                float occurance = naturalOccurrence[element.ordinal()];
                float jitterLimit = 0; // occurance / 0.0625f;
                float jitter = ((float)random.nextGaussian()) * flowMultiplier * occurance;
                jitter = Math.min(jitterLimit, Math.max(-jitterLimit, jitter));
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

        private double sumPercentages(float[] percentages) {
            float sum = 0;
            for (float pct : percentages) {
                sum += pct;
            }
            return sum;
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
