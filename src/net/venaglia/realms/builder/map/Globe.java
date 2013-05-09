package net.venaglia.realms.builder.map;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.primitives.Icosahedron;

/**
 * User: ed
 * Date: 7/15/12
 * Time: 10:58 AM
 *
 * Represents the entire globe of the world.
 *
 * todo
 */
public class Globe extends AbstractCartographicElement {

    public static final Globe INSTANCE;

    static {
        Icosahedron icosahedron = new Icosahedron();
        GeoPoint[] points = new GeoPoint[12];
        for (int i = 0; i < 12; i++) {
            points[i] = GeoPoint.fromPoint(icosahedron.points[i]);
        }
        INSTANCE = new Globe(points);
    }

    public final GlobalSector[] sectors = new GlobalSector[20]; // length = 20; geodesic icosahedron

//    private final SpatialMap<Acre> acres =

    private Globe(GeoPoint[] points) {
        super(0, null, 0, points);
    }

    public GlobalSector findSubElement(GeoPoint point) {
        return null;
    }

    public boolean isStatic() {
        return true;
    }

//    public Acre findAcreByPoint(GeoPoint geoPoint) {
//        acres.intersect(new BoundingSphere(geoPoint.toPoint(1000.0), ), )
//    }

    @Override
    public BoundingVolume<?> getBounds(double radius) {
        return new BoundingSphere(Point.ORIGIN, radius * 1.05);
    }

    @Override
    protected Acre findAcreByID(long globalSectorId1, long sectorId1, long globalSectorId2, long sectorId2, long id) {
        if (sectorId1 == 0 && sectorId2 == 0) {
            // pentagonal sector, there are only 12 of these in the globe
            int seq = (int)(0xF & id);
            return null; // todo
        }
        if (sectorId1 != 0) {
            Sector sector = findSectorById(globalSectorId1, sectorId1);
            if (sector != null) {
                return sector.findAcreByID(globalSectorId1, sectorId1, globalSectorId2, sectorId2, id);
            }
        }
        return null;
    }

    @Override
    protected Sector findSectorById(long globalSectorId, long id) {
        GlobalSector globalSector = findGlobalSectorById(globalSectorId);
        return globalSector != null ? globalSector.findSectorById(globalSectorId, id) : null;
    }

    @Override
    protected GlobalSector findGlobalSectorById(long id) {
        int seq = (int)(id >> 43);
        if (seq == 0 || seq > sectors.length) {
            return null;
        }
        return sectors[seq - 1];
    }
}
