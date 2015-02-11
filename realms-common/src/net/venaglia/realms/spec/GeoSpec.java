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
    ACRE_SEAMS,
    PENTAGONAL_ACRES,
    HEXAGONAL_ACRES,
    SIX_SECTOR_ACRES,
    TWO_GLOBAL_SECTOR_ACRES,
    TWO_SECTOR_ACRES,
    ONE_SECTOR_ACRES,
    ZONES,
    POINTS,
    POINTS_SHARED_DUAL_ZONE,
    POINTS_SHARED_MANY_ZONE,
    POINTS_NOT_SHARED,
    POINTS_NOT_SHARED_PER_ZONE,
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
            buffer.append(String.format("%,20d  Sectors          (%,d per GlobalSector)\n", sectors, sectors / globalSectors));
            buffer.append(String.format("%,20d  Inner triangles  (%,d per Sector)\n", innerTriangles, innerTriangles / sectors));
            buffer.append(String.format("%,20d  GeoPoints\n", cf.get(GEO_POINTS)));
            buffer.append(String.format("                  %,12d  joining 3 acres\n", cf.get(ACRE_INTERSECTION_POINTS)));
            buffer.append(String.format("%,20d  Acres\n", cf.get(ACRES)));
            buffer.append(String.format("                  %,12d  joining 5 global sectors\n", cf.get(PENTAGONAL_ACRES)));
            buffer.append(String.format("                  %,12d  joining 6 sectors\n", cf.get(SIX_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  joining 2 global sectors\n", cf.get(TWO_GLOBAL_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  joining 2 sectors\n", cf.get(TWO_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  completely within a single sector\n", cf.get(ONE_SECTOR_ACRES)));
            buffer.append(String.format("                  %,12d  seams between two acres\n", cf.get(ACRE_SEAMS)));
            buffer.append(String.format("%,20d  Zones\n", cf.get(ZONES)));
            buffer.append(String.format("%,20d  Surface Vertices (%,d per zone)\n", cf.get(POINTS), 65 * 66 / 2));
            buffer.append(String.format("                  %,12d  joining 5 zones\n", cf.get(PENTAGONAL_ACRES)));
            buffer.append(String.format("                  %,12d  joining 6 zones\n", cf.get(POINTS_SHARED_MANY_ZONE) - cf.get(PENTAGONAL_ACRES)));
            buffer.append(String.format("          %,20d  joining 2 zones\n", cf.get(POINTS_SHARED_DUAL_ZONE)));
            buffer.append(String.format("          %,20d  completely within a single zone (%,d per zone)\n", cf.get(POINTS_NOT_SHARED), cf.get(POINTS_NOT_SHARED_PER_ZONE)));
            buffer.append("\n");
            buffer.append("Globe size:\n");
            buffer.append(String.format("       Surface area:  %,12.3fkm\u00B2\n", cf.get(SURFACE_AREA_SQ_METERS) * 0.000001));
            buffer.append(String.format("           Diameter:  %,12.3fkm\n", cf.get(APPROX_RADIUS_METERS) * 0.002));
            return buffer.toString();
        }
    };

    public enum Preset {
        SMALL(4,9), MEDIUM(9,15), LARGE(30,30);

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

    public int iGet() {
        return (int)get();
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
        String geospec = Configuration.GEOSPEC.getString();
        if (geospec.matches("SMALL|MEDIUM|LARGE")) {
            Preset.valueOf(geospec).use();
        } else if (geospec.matches("[1-9][0-9]?x[1-9][0-9]?")) {
            String[] split = geospec.split("x");
            setDivisions(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
        } else {
            throw new IllegalArgumentException("geospec is invalid: " + geospec);
        }
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
        long acreSeams = (acres * 6 - 12) / 2;
        long hexagonalAcres = acres - globalVertices;
        long sixSectorAcres = (sectors * 3 + globalVertices) / 6 - globalVertices;
        long twoSectorAcres = (sectorDivisions - 3) * sectors / 2;
        long zones = hexagonalAcres * 24 + globalVertices * 20;
        long seams = zones * 3 / 2;
        long sharedPointsDual = seams * 63; // 65 points on each leg, minus the ends
        long sharedPointsMany = (zones * 3 + globalVertices) / 6;
        long nonSharedPointsPerZone = 65 * 66 / 2 - 192;
        long points = zones * nonSharedPointsPerZone + sharedPointsDual + sharedPointsMany;
        double surfaceArea = sqmPerAcre * acres;
        double approximateRadius = Math.sqrt(surfaceArea/(Math.PI*1.3333333333333333333));
        assert points - sharedPointsDual - sharedPointsMany == nonSharedPointsPerZone * zones;

        fields.put(GLOBAL_SECTORS, globalSectors);
        fields.put(GLOBAL_VERTICES, globalVertices);
        fields.put(SECTORS, sectors);
        fields.put(INNER_TRIANGLES, innerTriangles);
        fields.put(GEO_POINTS, (innerTriangles + globalVertices) / 2);
        fields.put(ACRE_INTERSECTION_POINTS, acres * 2 - 12);
        fields.put(ACRES, acres);
        fields.put(ACRE_SEAMS, acreSeams);
        fields.put(PENTAGONAL_ACRES, globalVertices);
        fields.put(HEXAGONAL_ACRES, hexagonalAcres);
        fields.put(SIX_SECTOR_ACRES, sixSectorAcres);
        fields.put(TWO_GLOBAL_SECTOR_ACRES, globalSectorDivisions * sectorDivisions * globalSectors / 6);
        fields.put(TWO_SECTOR_ACRES, twoSectorAcres);
        fields.put(ONE_SECTOR_ACRES, hexagonalAcres - twoSectorAcres - sixSectorAcres);
        fields.put(SURFACE_AREA_SQ_METERS, Math.round(surfaceArea));
        fields.put(APPROX_RADIUS_METERS, Math.round(approximateRadius));
        fields.put(ZONES, zones);
        fields.put(POINTS, points);
        fields.put(POINTS_SHARED_DUAL_ZONE, sharedPointsDual);
        fields.put(POINTS_SHARED_MANY_ZONE, sharedPointsMany);
        fields.put(POINTS_NOT_SHARED, points - sharedPointsDual - sharedPointsMany);
        fields.put(POINTS_NOT_SHARED_PER_ZONE, nonSharedPointsPerZone);
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
