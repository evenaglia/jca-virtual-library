package net.venaglia.realms.spec;

import net.venaglia.realms.common.Configuration;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 12/9/12
 * Time: 10:28 AM
 */
public enum GeoSpec {
    GLOBAL_SECTOR_DIVISIONS,
    SECTOR_DIVISIONS,
    GLOBAL_SECTORS,
    GLOBAL_VERTICES,
    SECTORS,
    INNER_TRIANGLES,
    GEO_POINTS,
    ACRE_INTERSECTION_POINTS,
    ACRES,
    PENTAGONAL_ACRES,
    HEXAGONAL_ACRES,
    SIX_SECTOR_ACRES,
    TWO_GLOBAL_SECTOR_ACRES,
    TWO_SECTOR_ACRES,
    ONE_SECTOR_ACRES,
    SURFACE_AREA_SQ_METERS,
    APPROX_RADIUS_METERS,
    SUMMARY {
        @Override
        public String toString() {
            final Map<GeoSpec,Long> cf = computedFields;

            long globalSectors = cf.get(GLOBAL_SECTORS);
            long sectors = cf.get(SECTORS);
            long innerTriangles = cf.get(INNER_TRIANGLES);
            Preset preset = usingPreset.get();

            StringBuilder buffer = new StringBuilder(1024);
            buffer.append("The world consists of:\n");
            if (preset != null) {
                buffer.append(String.format("%20s  Active Preset\n", preset));
            }
            buffer.append(String.format("%,20d  GlobalSectors\n", globalSectors));
            buffer.append(String.format("%,20d  Sectors         (%,6d per GlobalSector)\n", sectors, sectors / globalSectors));
            buffer.append(String.format("%,20d  Inner triangles (%,6d per Sector)\n", innerTriangles, innerTriangles / sectors));
            buffer.append(String.format("%,20d  GeoPoints\n", cf.get(GEO_POINTS)));
            buffer.append(String.format("                  %,12d  joining 3 acres\n", cf.get(ACRE_INTERSECTION_POINTS)));
            buffer.append(String.format("%,20d  Acres\n", cf.get(ACRES)));
            buffer.append(String.format("                  %,12d  joining 5 global sectors\n", cf.get(PENTAGONAL_ACRES)));
            buffer.append(String.format("                  %,12d  joining 6 sectors\n", cf.get(SIX_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  joining 2 global sectors\n", cf.get(TWO_GLOBAL_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  joining 2 sectors\n", cf.get(TWO_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  completely within a single sector\n", cf.get(ONE_SECTOR_ACRES)));
            buffer.append("\n");
            buffer.append("Globe size:\n");
            buffer.append(String.format("       Surface area:  %,10.3fkm\u00B2\n", cf.get(SURFACE_AREA_SQ_METERS) * 0.000001));
            buffer.append(String.format("           Diameter:  %,10.3fkm\n", cf.get(APPROX_RADIUS_METERS) * 0.002));
            return buffer.toString();
        }
    };

    public enum Preset {
        SMALL(4,9), MEDIUM(9,15), LARGE(35,30);

        private final int globalSectorDivisions;
        private final int sectorDivisions;

        private Preset(int globalSectorDivisions, int sectorDivisions) {
            this.globalSectorDivisions = globalSectorDivisions;
            this.sectorDivisions = sectorDivisions;
        }

        public void use() {
            setDivisions(globalSectorDivisions, sectorDivisions);
            usingPreset.set(this);
        }
    }

    public long get() {
        return computedFields.get(this);
    }

    public void set(long value) {
        switch (this) {
            case GLOBAL_SECTOR_DIVISIONS:
            case SECTOR_DIVISIONS:
                computedFields.put(this, value);
                usingPreset.set(null);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (computedFields.containsKey(GLOBAL_SECTOR_DIVISIONS) &&
            computedFields.containsKey(SECTOR_DIVISIONS)) {
            int globalSectorDivisions = (int)GLOBAL_SECTOR_DIVISIONS.get();
            int sectorDivisions = (int)SECTOR_DIVISIONS.get();
            computedFields.putAll(compute(globalSectorDivisions, sectorDivisions));
        }
    }

    @Override
    public String toString() {
        return String.format("%s=%d", name(), get());
    }

    private static final Map<GeoSpec,Long> computedFields = new EnumMap<GeoSpec,Long>(GeoSpec.class);
    private static final AtomicReference<Preset> usingPreset = new AtomicReference<Preset>();

    static {
        String geospec = Configuration.GEOSPEC.getString("LARGE");
        Preset.valueOf(geospec).use();
    }

    private static void setDivisions(int globalSectorDivisions, int sectorDivisions) {
        GLOBAL_SECTOR_DIVISIONS.set(globalSectorDivisions);
        SECTOR_DIVISIONS.set(sectorDivisions);
        usingPreset.set(null);
    }

    private static Map<GeoSpec,Long> compute(int globalSectorDivisions, int sectorDivisions) {
        assert globalSectorDivisions >= 4;
        assert sectorDivisions >= 9;
        assert sectorDivisions % 3 == 0;

        final double sqmPerAcre = 4046.85642570257d;
        Map<GeoSpec,Long> fields = new EnumMap<GeoSpec,Long>(GeoSpec.class);
        long globalSectors = 20; // icosahedron
        long globalVertices = 12; // icosahedron
        long sectors = globalSectorDivisions * globalSectorDivisions * globalSectors;
        long innerTriangles = sectorDivisions * sectorDivisions * sectors;
        long acres = (innerTriangles + globalVertices) / 6;
        long hexagonalAcres = acres - globalVertices;
        long sixSectorAcres = (sectors + globalVertices) / 6;
        long twoSectorAcres = sectorDivisions * sectors / 6;
        double surfaceArea = sqmPerAcre * acres;
        double approximateRadius = Math.sqrt(surfaceArea/(Math.PI*1.3333333333333333333));

        fields.put(GLOBAL_SECTORS, globalSectors);
        fields.put(GLOBAL_VERTICES, globalVertices);
        fields.put(SECTORS, sectors);
        fields.put(INNER_TRIANGLES, innerTriangles);
        fields.put(GEO_POINTS, (innerTriangles + globalVertices) / 2);
        fields.put(ACRE_INTERSECTION_POINTS, acres * 2 - 12);
        fields.put(ACRES, acres);
        fields.put(PENTAGONAL_ACRES, globalVertices);
        fields.put(HEXAGONAL_ACRES, hexagonalAcres);
        fields.put(SIX_SECTOR_ACRES, sixSectorAcres);
        fields.put(TWO_GLOBAL_SECTOR_ACRES, globalSectorDivisions * sectorDivisions * globalSectors / 6);
        fields.put(TWO_SECTOR_ACRES, twoSectorAcres);
        fields.put(ONE_SECTOR_ACRES, hexagonalAcres - twoSectorAcres - sixSectorAcres);
        fields.put(SURFACE_AREA_SQ_METERS, Math.round(surfaceArea));
        fields.put(APPROX_RADIUS_METERS, Math.round(approximateRadius));
        fields.put(SUMMARY, 0L);

        Configuration.DATABASE_DIRECTORY.override("world." + getGeoIdentity());

        return fields;
    }

    public static double getRadiusForElevation(double meanRadius, double elevation) {
        double baseRadius = computedFields.get(APPROX_RADIUS_METERS);
        return ((baseRadius + elevation) / baseRadius) * meanRadius;
    }

    public static String getGeoIdentity() {
        return String.format("%02dx%02d", GLOBAL_SECTOR_DIVISIONS.get(), SECTOR_DIVISIONS.get());
    }

    public static void main(String[] args) {
        System.out.println(SUMMARY);
    }
}
