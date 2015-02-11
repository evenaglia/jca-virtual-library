package net.venaglia.realms.builder.terraform;

import net.venaglia.common.util.Predicate;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.realms.builder.terraform.sets.AcreSet;
import net.venaglia.realms.builder.terraform.sets.AllAcresSet;
import net.venaglia.realms.builder.view.AcreView;
import net.venaglia.realms.builder.view.AcreViewer;
import net.venaglia.realms.builder.view.AcreViewer2D;
import net.venaglia.realms.builder.view.AcreViews;
import net.venaglia.realms.common.Configuration;
import net.venaglia.realms.common.map.BinaryStore;
import net.venaglia.realms.common.map.GlobalVertexLookup;
import net.venaglia.realms.common.map.data.binaries.BinaryResource;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.common.map.world.WorldMapImpl;
import net.venaglia.realms.common.map.world.topo.TerraformVertexStore;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 7:12 PM
 */
public class Terraform {

    private final boolean _3D = true;
    private final int count;
    private final AcreSet allAcres;
    private final AcreView.AcreViewGeometry geometry;
    private final ElevationAcreView elevation;
    private AcreViews<AcreView> acreViews;
    private AcreViews<Shape<?>> overlayShapes;
    private GlobalVertexLookup vertexLookup;

    public Terraform(WorldMap worldMap) {
        count = GeoSpec.ACRES.iGet();
        final AcreDetail[] allAcresArray = new AcreDetail[count];
        this.vertexLookup = ((TerraformVertexStore)worldMap.getVertexStore()).getGlobalVertexLookup();
        this.geometry = getGeometry(worldMap, allAcresArray);
        for (AcreDetail acre : allAcresArray) {
            acre.setElevation(-0.5f);
        }
        allAcres = new AllAcresSet(allAcresArray);
        acreViews = new AcreViews<AcreView>();
        elevation = new ElevationAcreView("Elevation", geometry, allAcres);
        elevation.setStatic(false);
        acreViews.addView(elevation);
        overlayShapes = new AcreViews<Shape<?>>();
    }

    private AcreView.AcreViewGeometry getGeometry(final WorldMap worldMap, final AcreDetail[] allAcres) {
        final boolean loadWithParanoia = Configuration.PARANOIA_ON_ACRES.getBoolean();
        final Map<Integer,String> signatures = loadWithParanoia ? loadSignatures() : null;
        final AcreView.AcreViewGeometry geometry = new AcreView.AcreViewGeometry(vertexLookup);

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
        int frameLimit = calculateFrameLimit(count);
        new Geology(frameLimit, allAcres, geometry, vertexLookup, acreViews, overlayShapes).run();
        elevation.setStatic(true);
        new Cartography(allAcres, geometry, acreViews).run();
        System.out.println("[exit]");
    }

    private void prepareView() {
        double radius = GeoSpec.APPROX_RADIUS_METERS.get();
        if (_3D) {
            AcreViewer.view(acreViews,
                            overlayShapes,
                            radius,
                            "Planet Engine",
                            new Dimension(1280,1024));
        } else {
            AcreViewer2D.view(acreViews, radius, "Planet Engine", new Dimension(1280, 1024));
            AcreViewer2D.transform(geometry.getZoneVertices(), radius);
        }
    }

    public static int calculateFrameLimit(int count) {
        return (int)Math.round(Math.pow(count, 0.2) * 100);
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
        private final Predicate<String> fieldPredicate;

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
            this.fieldPredicate = new Predicate<String>() {

                private final Set<String> allow = new HashSet<>(Arrays.asList("neighborIds","acreTopographyDef","elevation","id"));

                @Override
                public boolean allow(String s) {
                    return allow.contains(s);
                }
            };
        }

        @Override
        public void run() {
            boolean loadWithParanoia = Configuration.PARANOIA_ON_ACRES.getBoolean();
            SerializerStrategy<AcreDetail> serializer = AcreDetail.DEFINITION.getSerializer();
            for (int i = base, l = allAcres.length; i < l; i += step) {
                BinaryResource resource = binaryStore.getBinaryResource(AcreDetail.MIMETYPE, i);
                ByteBuffer byteBuffer = ByteBuffer.wrap(resource.getData());
                AcreDetail acreDetail = serializer.deserializePartial(byteBuffer, fieldPredicate);
                assert i == acreDetail.getId();
                if (loadWithParanoia) {
                    String sig = signatures.get(i);
                    String fsc = acreDetail.formatForSanityCheck();
                    assert fsc.equals(sig);
                    signatures.remove(i);
                }
                allAcres[i] = acreDetail;
                geometry.visit(acreDetail);
                if ((count.getAndIncrement() & 16383) == 16383) {
                    System.out.print(".");
                }
            }
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
