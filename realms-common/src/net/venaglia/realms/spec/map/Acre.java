package net.venaglia.realms.spec.map;

import net.venaglia.realms.common.map.WorldMap;
import net.venaglia.realms.common.map.elements.GraphAcre;
import net.venaglia.realms.common.map.ref.GraphAcreRef;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.primitives.TriangleSequence;
import net.venaglia.common.util.Ref;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 7/15/12
 * Time: 10:59 AM
 */
public class Acre extends AbstractCartographicElement {

    public enum Flavor {
        INNER1(Material.makeFrontShaded(Color.RED), Material.makeFrontShaded(Color.GREEN)),
        INNER2(Material.makeFrontShaded(Color.GREEN), Material.makeFrontShaded( Color.RED)),
        INNER3(Material.makeFrontShaded(Color.BLUE), Material.makeFrontShaded( Color.BLUE)),
        DUAL_SECTOR(Material.makeFrontShaded(Color.BLUE), Material.makeFrontShaded( Color.BLUE)),
        MULTI_SECTOR(Material.makeFrontShaded(Color.BLUE), Material.makeFrontShaded( Color.BLUE));

        public final Material material;
        private final Material invertMaterial;

        private Flavor(Material material, Material invertMaterial) {
            this.material = material;
            this.invertMaterial = invertMaterial;
        }

        public Material getMaterial(boolean invert) {
            return invert ? invertMaterial : material;
        }
    }

    public static final AtomicInteger SEQ = new AtomicInteger();

    public final Flavor flavor;
    public final GeoPoint center;

    public int packId = -1;
    public int[] packNeighbors;

    public Acre(int seq, Sector sector, Flavor flavor, GeoPoint center, GeoPoint... points) {
        super(computeId(assertLessThan(seq, 14950), 18, 0, sector.id) | ACRE_BIT,
              sector,
              points.length,
              points);
        assert points.length == 6;
        assert flavor != null && flavor != Flavor.MULTI_SECTOR;
        this.flavor = flavor;
        this.center = center;
        sector.acres.add(this, center.toPoint(1000.0));
        SEQ.incrementAndGet();
    }

    public Acre(int seq, Sector sector1, Sector sector2, Iterable<Sector> others, GeoPoint center, GeoPoint... points) {
        super(computeDoubleSectorId(assertLessThan(seq, 33), sector1, sector2),
              commonParent(sector1, sector2),
              points.length,
              points);
        assert points.length == 6 || Arrays.asList(sector1.getParent().getParent().points).contains(center);
        this.center = center;
        Point p = center.toPoint(1000.0);
        sector1.acres.add(this, p);
        sector2.acres.add(this, p);
        if (others != null) {
            for (Sector s : others) {
                s.acres.add(this, p);
            }
            flavor = Flavor.MULTI_SECTOR;
        } else {
            flavor = Flavor.DUAL_SECTOR;
        }
        SEQ.incrementAndGet();
    }

    private static AbstractCartographicElement commonParent(Sector sector1, Sector sector2) {
        GlobalSector parent1 = sector1.getParent();
        GlobalSector parent2 = sector2.getParent();
        if (parent1 == parent2) {
            return parent1;
        }
        return parent1.getParent();
    }

    private static AbstractCartographicElement findGlobe(Sector sector) {
        return sector.getParent().getParent();
    }

    private static long computeDoubleSectorId(int seq, Sector sector1, Sector sector2) {
        if (sector1.id == sector2.id) throw new IllegalArgumentException();
        if (sector1.id > sector2.id) return computeDoubleSectorId(seq, sector2, sector1);
        return sector1.id | (sector2.id >> 16) | seq & 0x7F | ACRE_BIT;
    }

    private static int assertLessThan(int seq, int max) {
        assert seq >= 0;
        assert seq < max;
        return seq;
    }

    @Override
    public Shape get3DShape(double radius) {
        Shape shape = super.get3DShape(radius);
        AbstractCartographicElement parent = getParent();
        boolean inverted = parent instanceof Sector && ((Sector)parent).isInverted();
        shape.setMaterial(flavor.getMaterial(inverted));
        return shape;
    }

    public Shape get3DShape(double radius, int side) {
//        Shape shape = super.get3DShape(radius);
        // point 1: not in the right place
        // point 2: new even visible; a line, maybe?
        // point 3: new even visible; a line, maybe?
        Shape shape = new TriangleSequence(center.toPoint(radius),
                                           points[side % points.length].toPoint(radius),
                                           points[(side + 1) % points.length].toPoint(radius));
        AbstractCartographicElement parent = getParent();
        boolean inverted = parent instanceof Sector && ((Sector)parent).isInverted();
        shape.setMaterial(flavor.getMaterial(inverted));
        return shape;
    }

    public void applyPackDataToGraphAcre(GraphAcre ga, double radius, WorldMap worldMap) {
        ga.setId(packId);
        ga.setCenter(center);

        Collection<Ref<GraphAcre>> neighbors = ga.getNeighbors();
        neighbors.clear();
        for (int neighborID : packNeighbors) {
            neighbors.add(new GraphAcreRef(neighborID, worldMap));
        }

        GeoPoint[] verteces = ga.getVertices();
        int length = points.length;
        if (verteces.length != length) {
            verteces = new GeoPoint[length];
        }
        System.arraycopy(points, 0, verteces, 0, length);
    }
}
