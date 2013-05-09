package net.venaglia.realms.builder.map;

import static net.venaglia.realms.builder.geoform.GeoSpec.*;

import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.util.work.WorkQueue;

/**
 * User: ed
 * Date: 7/15/12
 * Time: 11:04 AM
 *
 * A global sector represents one side of an icosahedron. Each sector is
 * geometrically identical and covers 1/20th of the total surface area of the
 * globe.
 */
public class GlobalSector extends AbstractCartographicElement {


    private Sector[] sectors; // the 1024 sectors defining this global sector

    public GlobalSector(int seq, Globe globe, GeoPoint... points) {
        super(seq, 5, 43, globe, 3, points);
    }

    @Override
    public Globe getParent() {
        return (Globe)super.getParent();
    }

    @Override
    protected Acre findAcreByID(long globalSectorId1, long sectorId1, long globalSectorId2, long sectorId2, long id) {
        if (id != globalSectorId1) {
            return null;
        }
        Sector sector = findSectorById(globalSectorId1, id);
        return sector != null ? sector.findAcreByID(globalSectorId1, sectorId1, globalSectorId2, sectorId2, id) : null;
    }

    @Override
    protected Sector findSectorById(long globalSectorId, long id) {
        int seq = (int)(id >> 32) & 0x7FF;
        if (seq == 0 || seq > sectors.length) {
            return null;
        }
        return sectors[seq - 1];
    }

    public Sector[] getSectors() {
        return sectors;
    }

    public class Initializer extends AbstractSectorInitializer<Sector> {

        private final boolean inverted;

        public Initializer(Point a,
                           Point b,
                           Point c,
                           boolean inverted,
                           WorkQueue workQueue) {
            this((int)GLOBAL_SECTOR_DIVISIONS.get(), a, b, c, inverted, workQueue);
        }

        public Initializer(int divisions,
                           Point a,
                           Point b,
                           Point c,
                           boolean inverted,
                           WorkQueue workQueue) {
            super(divisions, a, b, c, workQueue);
            this.inverted = inverted;
        }

        @Override
        protected boolean isInverted() {
            return inverted;
        }

        @Override
        protected Sector[] getChildren(int length) {
            sectors = new Sector[length];
            return sectors;

        }

        @Override
        protected Sector buildChild(int index,
                                    GeoPoint a,
                                    GeoPoint b,
                                    GeoPoint c,
                                    Point i,
                                    Point j,
                                    Point k,
                                    boolean inverted) {
            Sector sector = new Sector(index, inverted, GlobalSector.this, a, b, c);
            sector.setInit(sector.new Initializer(i, j, k, workQueue));
            sectors[index] = sector;
            return sector;
        }
    }
}
