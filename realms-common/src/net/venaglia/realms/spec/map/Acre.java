package net.venaglia.realms.spec.map;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.primitives.TriangleSequence;
import net.venaglia.realms.common.map.world.AcreDetail;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 7/15/12
 * Time: 10:59 AM
 */
public class Acre extends AbstractCartographicElement {

    public enum Flavor {
        INNER1(Material.makeFrontShaded(Color.RED), Material.makeFrontShaded(Color.GREEN), true),
        INNER2(Material.makeFrontShaded(Color.GREEN), Material.makeFrontShaded( Color.RED), true),
        INNER3(Material.makeFrontShaded(Color.BLUE), Material.makeFrontShaded( Color.BLUE), true),
        DUAL_SECTOR(Material.makeFrontShaded(Color.BLUE), Material.makeFrontShaded( Color.BLUE), false),
        MULTI_SECTOR(Material.makeFrontShaded(Color.BLUE), Material.makeFrontShaded( Color.BLUE), false);

        public final boolean inner;

        private final Material material;
        private final Material invertMaterial;

        private Flavor(Material material, Material invertMaterial, boolean inner) {
            this.material = material;
            this.invertMaterial = invertMaterial;
            this.inner = inner;
        }

        public Material getMaterial(boolean invert) {
            return invert ? invertMaterial : material;
        }
    }

    public static final AtomicInteger SEQ = new AtomicInteger();

    public final Flavor flavor;

    public GeoPoint center;
    public int packId = -1;
    public int[] packNeighbors;

    /**
     * def[0] : center vertex id
     * def[1..6] : edge midpoint vertex ids
     * def[7..12] : corner vertex ids
     * def[13..18] : spoke midpoint vertex ids
     */
    public long[] topographyDef;

    /**
     * starts[0..4] : zone seam vertex start ids
     * starts[5..6] : acre seam vertex ids
     * ...
     * starts[35..39] : zone seam vertex start ids
     * starts[40..41] : acre seam vertex ids
     */
    public long[] seamStartVertexIds;

    /**
     * starts[0..23] : zone inner vertex start ids
     */
    public long[] zoneStartVertexIds;

    public Acre(int seq, Sector sector, Flavor flavor, GeoPoint center, GeoPoint... points) {
        super(computeId(assertLessThan(seq, 14950), 18, 0, sector.id) | ACRE_BIT,
              sector,
              points.length,
              points);
        assert points.length == 6;
        assert flavor != null && flavor != Flavor.MULTI_SECTOR;
        this.flavor = flavor;
        this.center = center;
        if (sector.acres != null) {
            sector.acres.add(this, center.toPoint(1000.0));
        }
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
        if (sector1.acres != null) {
            sector1.acres.add(this, p);
        }
        if (sector2.acres != null) {
            sector2.acres.add(this, p);
        }
        if (others != null) {
            for (Sector s : others) {
                if (s.acres != null) {
                    s.acres.add(this, p);
                }
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

    public void applyPackDataToGraphAcre(AcreDetail ga) {
        ga.setId(packId);
        ga.setCenter(center);
        ga.setVertices(points.clone());
        ga.setNeighborIds(packNeighbors.clone());
        ga.setAcreTopographyDef(topographyDef);
        ga.setSeamFirstVertexIds(seamStartVertexIds);
        ga.setZoneFirstVertexIds(zoneStartVertexIds);
    }

    @Override
    public int countGeoPoints() {
        return super.countGeoPoints() + 1;
    }

    @Override
    public GeoPoint getGeoPoint(int index) {
        return index == 0 ? center : super.getGeoPoint(index - 1);
    }

    @Override
    public void setGeoPoint(int index, GeoPoint geoPoint) {
        if (index == 0) {
            center = geoPoint;
        } else {
            super.setGeoPoint(index - 1, geoPoint);
        }
    }

    @Override
    public RelativeCoordinateReference getRelativeCoordinateReference() {
        // find the four corners of this acre
        Point a, b, c, d;
        Point t1, t2, b1, b2, p1, p2;
        if (points == null) {
            throw new NullPointerException("points");
        }
        switch (points.length) {
            case 5:
                b1 = points[0].toPoint(1000.0);
                b2 = points[1].toPoint(1000.0);
                t1 = points[3].toPoint(1000.0);
                // special case, these pentagonal acres are equilateral
                t2 = t1.translate(Vector.betweenPoints(b1, b2));
                p1 = points[2].toPoint(1000.0);
                p2 = points[4].toPoint(1000.0);
                break;
            case 6:
                b1 = points[0].toPoint(1000.0);
                b2 = points[1].toPoint(1000.0);
                t1 = points[3].toPoint(1000.0);
                t2 = points[4].toPoint(1000.0);
                p1 = points[2].toPoint(1000.0);
                p2 = points[5].toPoint(1000.0);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        a = findClosestPointOnLine(t1, t2, p1);
        b = findClosestPointOnLine(t1, t2, p2);
        c = findClosestPointOnLine(b1, b2, p1);
        d = findClosestPointOnLine(b1, b2, p2);
        return new RelativeCoordinateReference(a, b, c, d);
    }

    public Point findClosestPointOnLine(Point onLine_pointA, Point onLine_pointB, Point nearbyPoint) {
        Vector a = Vector.betweenPoints(onLine_pointA, nearbyPoint);
        Vector u = Vector.betweenPoints(onLine_pointA, onLine_pointB).normalize();
        return onLine_pointA.translate(u.scale(a.dot(u)));
    }
}
