package net.venaglia.realms.builder.terraform;

import static net.venaglia.realms.builder.terraform.sets.BoundaryAcreSetBuilder.AcreOrder.*;
import static net.venaglia.realms.builder.terraform.sets.BoundaryAcreSetBuilder.StandardNeighborPredicate.*;

import net.venaglia.common.util.IntIterator;
import net.venaglia.common.util.impl.RangeIterator;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.util.ColorGradient;
import net.venaglia.realms.builder.terraform.sets.AcreSetBuilder;
import net.venaglia.realms.common.map.world.AcreIdSet;
import net.venaglia.realms.builder.terraform.sets.AcreSet;
import net.venaglia.realms.builder.terraform.sets.BasicAcreSetNavigator;
import net.venaglia.realms.builder.terraform.sets.BoundaryAcreSetBuilder;
import net.venaglia.realms.builder.view.AcreView;
import net.venaglia.realms.builder.view.AcreViews;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.spec.GeoSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: ed
 * Date: 1/31/15
 * Time: 3:16 PM
 */
public class Cartography implements Runnable {

    private final AcreSet allAcres;
    private final AcreView.AcreViewGeometry geometry;
    private final AcreViews<AcreView> acreViews;
    private final Collection<AcreView> myAcreViews = new ArrayList<AcreView>();
    private final AcreTags acreTags = new AcreTags();
    private final Runnable cleanup;

    private int[] neighborIds = new int[6];
    private int neighborsModCount = 0;

    public Cartography(AcreSet allAcres, AcreView.AcreViewGeometry geometry, AcreViews<AcreView> acreViews) {
        this.allAcres = allAcres;
        this.geometry = geometry;
        this.acreViews = acreViews;
        this.cleanup = new Runnable() {
            @Override
            public void run() {
                for (AcreView view : myAcreViews) {
                    acreViews.removeView(view);
                }
            }
        };
    }

    private void newTintedAcreView(AcreSet region, Color tint, String regionType, int seq) {
        String name = regionType + " Region[" + seq + "] " + region.size() + " acres";
        ElevationAcreView acreView = new TintedRegionAcreView(name, region, tint);
        myAcreViews.add(acreView);
        acreViews.addAndShowView(acreView);
    }

    private Collection<AcreSet> findRegions(Region regionType) {
        AcreNavigator unsortedAcreNavigator = new AcreSetBuilder(allAcres, null, GeoSpec.ACRES.iGet());
        for (AcreDetail acre : allAcres) {
            float elevation = acre.getElevation();
            if (regionType.partition(elevation)) {
                unsortedAcreNavigator.push(acre);
            }
        }
        AcreSet unsortedAcres = unsortedAcreNavigator.done();
        Collection<AcreSet> regions = new ArrayList<AcreSet>(32);
        while (!unsortedAcres.isEmpty()) {
            AcreDetail startingAcre = getStartingAcre(unsortedAcres, regionType);
            final AcreSet region = walkOneRegion(startingAcre, unsortedAcres, regionType);
            regions.add(region);
//            newTintedAcreView(region, Color.RED, regionType.toString(), regions.size());
        }
        return regions;
    }

    private AcreDetail getStartingAcre(AcreSet unsortedAcres, Region regionType) {
        AcreDetail startingAcre = null;
        for (AcreDetail acre : unsortedAcres) {
            if (startingAcre == null) {
                startingAcre = acre;
            } else {
                startingAcre = regionType.accumulate(acre, startingAcre);
            }
        }
        unsortedAcres.remove(startingAcre);
        return startingAcre;
    }

    private AcreSet walkOneRegion(AcreDetail start, AcreSet unsortedAcres, Region regionType) {
        IntIterator seed = new RangeIterator(start.getId(), start.getId() + 1);
        AcreNavigator navigator = new AcreSetBuilder(allAcres, seed, GeoSpec.ACRES.iGet());
        while (navigator.hasNext()) {
            navigator.next();
            for (AcreDetail neighbor : navigator.neighbors()) {
                if (regionType.partition(neighbor.getElevation())) {
                    unsortedAcres.remove(neighbor);
                    navigator.push(neighbor);
                }
            }
        }
        return navigator.done();
    }

    private void findPeaks(AcreSet land) {
        nextAcre:
        for (AcreNavigator navigator = land.navigateReadOnly(); navigator.hasNext(); ) {
            AcreDetail acre = navigator.next();
            float elevation = acre.getElevation();
            for (AcreDetail neighbor : navigator.neighbors()) {
                if (neighbor.getElevation() > elevation) {
                    continue nextAcre;
                }
            }
            acreTags.setTag(acre, AcreTags.Tag.GEOGRAPHIC_PEAK);
        }
    }

    private void findFloors(AcreSet water) {
        nextAcre:
        for (AcreNavigator navigator = water.navigateReadOnly(); navigator.hasNext(); ) {
            AcreDetail acre = navigator.next();
            float elevation = acre.getElevation();
            for (AcreDetail neighbor : navigator.neighbors()) {
                if (neighbor.getElevation() < elevation) {
                    continue nextAcre;
                }
            }
            acreTags.setTag(acre, AcreTags.Tag.LAKE_BOTTOM);
        }
    }

    private AcreIdSet findByTag(AcreSet region, AcreTags.Tag tag) {
        AcreIdSet result = new AcreIdSet();
        for (AcreNavigator navigator = region.navigateReadOnly(); navigator.hasNext(); ) {
            AcreDetail acre = navigator.next();
            if (acreTags.hasTag(acre, tag)) {
                result.add(acre.getId());
            }
        }
        return result;
    }

    private void findCoastalBoundaries(AcreSet land) {
        for (AcreNavigator navigator = land.navigateReadOnly(); navigator.hasNext(); ) {
            AcreDetail acre = navigator.next();
            for (AcreDetail neighbor : navigator.neighbors()) {
                if (!land.contains(neighbor)) {
                    acreTags.setTag(acre, AcreTags.Tag.COASTAL_BOUNDARY);
                    break;
                }
            }
        }
    }

    private void findRidgeLinesAndHeadwaters(AcreSet land) {
        AcreNavigator peaks = new BasicAcreSetNavigator(findByTag(land, AcreTags.Tag.GEOGRAPHIC_PEAK), land);
//        int[] ridgeBuffer = new int[land.size()];
//        Collection<AcreIdSet> ridgeLines = new ArrayList<>(land.size() >> 4 + 2);

        while (peaks.hasNext()) {
//            int ridgeIndex = 0;
            AcreDetail acre = peaks.next();
            int prevAcreId = acre.getId();
            while (acre != null) {
//                ridgeBuffer[ridgeIndex++] = acre.getId();
                float elevation = acre.getElevation();
                AcreDetail next = null;
                for (AcreDetail neighbor : neighbors(acre)) {
                    if (neighbor.getElevation() <= elevation && neighbor.getId() != prevAcreId && land.contains(neighbor)) {
                        next = next == null ? neighbor : Region.LAND.accumulate(next, neighbor);
                    }
                }
                acre = next;
                if (acre != null) {
                    if (!acreTags.setTag(acre, AcreTags.Tag.RIDGE_LINE)) {
                        acreTags.setTag(acre, AcreTags.Tag.RIVER_HEAD);
                        break;
                    }
                    prevAcreId = acre.getId();
                }
            }
//            int[] ridgeLine = new int[ridgeIndex];
//            System.arraycopy(ridgeBuffer, 0, ridgeLine, 0, ridgeIndex);
//            ridgeLines.add(AcreIdSet.wrap(ridgeLine));
        }
//        return ridgeLines;
    }

    private Collection<AcreIdSet> findRivers(AcreSet land) {
        AcreNavigator heads = new BasicAcreSetNavigator(findByTag(land, AcreTags.Tag.RIVER_HEAD), land);
        int[] waterwayBuffer = new int[land.size()];
        Collection<AcreIdSet> waterways = new ArrayList<>(land.size() >> 4 + 2);

        while (heads.hasNext()) {
            int waterwayIndex = 0;
            AcreDetail acre = heads.next();
            int prevAcreId = acre.getId();
            while (acre != null) {
                waterwayBuffer[waterwayIndex++] = acre.getId();
                float elevation = acre.getElevation();
                AcreDetail next = null;
                for (AcreDetail neighbor : neighbors(acre)) {
                    if (neighbor.getElevation() <= elevation && neighbor.getId() != prevAcreId && land.contains(neighbor)) {
                        next = next == null ? neighbor : Region.LAND.accumulate(next, neighbor);
                    }
                }
                acre = next;
                if (acre != null) {
                    if (!acreTags.setTag(acre, AcreTags.Tag.WATERWAY)) {
                        acreTags.setTag(acre, AcreTags.Tag.CONFLUENCE);
                        break;
                    }
                    prevAcreId = acre.getId();
                }
            }
            int[] waterway = new int[waterwayIndex];
            System.arraycopy(waterwayBuffer, 0, waterway, 0, waterwayIndex);
            waterways.add(AcreIdSet.wrap(waterway));
        }
        return waterways;
    }

    private Collection<AcreSet> findWaterways(AcreSet land) {
        Collection<AcreSet> waterways = new ArrayList<>();
        for (AcreDetail riverHead : land.asSeries(findByTag(land, AcreTags.Tag.RIVER_HEAD))) {
            AcreNavigator navigator = new BoundaryAcreSetBuilder(riverHead, land, LOWEST_FIRST, ALWAYS);
            while (navigator.hasNext()) {
                AcreDetail acre = navigator.next();
                navigator.push(acre);
                if (!acreTags.setTag(acre, AcreTags.Tag.WATERWAY)) {
                    acreTags.setTag(acre, AcreTags.Tag.CONFLUENCE);
                    break;
                }
                if (acreTags.hasTag(acre, AcreTags.Tag.COASTAL_BOUNDARY)) {
                    break;
                }
            }
            AcreSet waterway = navigator.done();
            waterways.add(waterway);
//            newTintedAcreView(waterway, Color.BLUE, "WATERWAY", waterways.size());
        }
        return waterways;
    }

    private void buildTagViews(final AtomicBoolean running) {
        for (AcreTags.Tag tag : AcreTags.Tag.values()) {
            Color highlightColor;
            switch (tag) {
                case COASTAL_BOUNDARY:
                    highlightColor = Color.CYAN; break;
                case GEOGRAPHIC_PEAK:
                    highlightColor = Color.WHITE; break;
                case RIDGE_LINE:
                    highlightColor = Color.GRAY_50; break;
                case RIVER_HEAD:
                    highlightColor = Color.CYAN; break;
                case WATERWAY:
                    highlightColor = Color.BLUE; break;
                case CONFLUENCE:
                    highlightColor = Color.MAGENTA; break;
                case LAKE_BOTTOM:
                    highlightColor = Color.BLACK; break;
                case LAKE_BOUNDARY:
                    highlightColor = Color.CYAN; break;
                default:
                    throw new IllegalStateException();
            }
            AcreView view = new ElevationAcreView(tag.name(), geometry, allAcres) {
                final ColorGradient highlight = gradient.tint(highlightColor, 0.75f);

                @Override
                public boolean isStatic() {
                    return !running.get();
                }

                @Override
                protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
                    float elevation = acreLookup.get(acreId).getElevation();
                    float v = Math.max(0.0f, Math.min(1.0f, elevation * 0.5f + 0.5f));
                    ColorGradient gradient = acreTags.hasTag(acreLookup.get(acreId), tag) ? highlight : this.gradient;
                    gradient.applyColor(v, acreRenderer);
                }
            };
            myAcreViews.add(view);
            acreViews.addAndShowView(view);
        }
    }

    @Override
    public void run() {
        System.out.println("Mapping the continents and islands...");
        AtomicBoolean running = new AtomicBoolean(true);
        Collection<AcreSet> islands = findRegions(Region.LAND);
        Collection<AcreSet> seas = findRegions(Region.SEA);
        buildTagViews(running);
        for (AcreSet land : islands) {
            findPeaks(land);
            findFloors(land);
            findCoastalBoundaries(land);
            findRidgeLinesAndHeadwaters(land);
            findWaterways(land);
        }
        running.set(false);
//        cleanup.run();
    }

    private Iterable<AcreDetail> neighbors(final AcreDetail acre) {
        return new Iterable<AcreDetail>() {
            @Override
            public Iterator<AcreDetail> iterator() {
                return new Iterator<AcreDetail>() {

                    private final int n = acre.getNeighborIds(neighborIds);
                    private final int c = ++neighborsModCount;

                    private int i = 0;

                    @Override
                    public boolean hasNext() {
                        if (c != neighborsModCount) {
                            throw new IllegalStateException();
                        }
                        return i < n;
                    }

                    @Override
                    public AcreDetail next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        return allAcres.get(neighborIds[i++]);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }
        };
    }

    private String textCount(int count, String nounSingular, String nounPlural) {
        return String.format("%d %s", count, count == 1 ? nounSingular : nounPlural);
    }

    private enum Region {
        LAND {
            @Override
            boolean partition(float elevation) {
                return elevation > 0;
            }

            @Override
            AcreDetail accumulate(AcreDetail a, AcreDetail b) {
                return a.getElevation() > b.getElevation() ? a : b;
            }
        },
        SEA {
            @Override
            boolean partition(float elevation) {
                return elevation <= 0;
            }

            @Override
            AcreDetail accumulate(AcreDetail a, AcreDetail b) {
                return a.getElevation() < b.getElevation() ? a : b;
            }
        };

        abstract boolean partition(float elevation);

        abstract AcreDetail accumulate(AcreDetail a, AcreDetail b);
    }

    private class TintedRegionAcreView extends ElevationAcreView {

        private final ColorGradient tintedGradient;
        private final AcreIdSet tintAcres;

        public TintedRegionAcreView(String name, AcreSet region, Color tint) {
            super(name, Cartography.this.geometry, Cartography.this.allAcres);
            tintedGradient = gradient.tint(tint, 0.5f).highPerformance();
            tintAcres = region.getAcreIds();
        }

        @Override
        protected void renderAcre(int acreId, AcreRenderer acreRenderer) {
            float elevation = acreLookup.get(acreId).getElevation();
            float v = Math.max(0.0f, Math.min(1.0f, elevation * 0.5f + 0.5f));
            ColorGradient g = tintAcres.contains(acreId) ? tintedGradient : gradient;
            g.applyColor(v, acreRenderer);
        }
    }
}
