package net.venaglia.realms.builder.map;

import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;

/**
 * User: ed
 * Date: 7/15/12
 * Time: 11:09 AM
 */
public final class GeoPoint {

    private static final double PI = Math.PI;
    private static final double PI_NEGATIVE = 0.0 - PI;
    private static final double HALF_PI = PI * 0.5;
    private static final double HALF_PI_NEGATIVE = 0.0 - HALF_PI;

    public static final GeoPoint NORTH_POLE = new GeoPoint(0, HALF_PI_NEGATIVE);
    public static final GeoPoint SOUTH_POLE = new GeoPoint(0, HALF_PI);

    public final double longitude;
    public final double latitude;

    public GeoPoint(double longitude, double latitude) {
        assert longitude >= PI_NEGATIVE && longitude <= PI;
        assert latitude >= HALF_PI_NEGATIVE && latitude <= HALF_PI;
        this.longitude = longitude == PI_NEGATIVE ? PI : longitude;
        this.latitude = latitude;
    }

    public Point toPoint(double radius) {
        double c = Math.cos(latitude);
        double x = Math.sin(longitude) * c;
        double y = Math.cos(longitude) * c;
        double z = Math.sin(latitude);
        return new Point(x * radius, y * radius, z * radius);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoPoint geoPoint = (GeoPoint)o;

        return Double.compare(geoPoint.latitude, latitude) == 0 &&
               Double.compare(geoPoint.longitude, longitude) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = longitude != +0.0d ? Double.doubleToLongBits(longitude) : 0L;
        result = (int)(temp ^ (temp >>> 32));
        temp = latitude != +0.0d ? Double.doubleToLongBits(latitude) : 0L;
        result = 31 * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    private Object[] dms(double degrees) {
        boolean negative = degrees < 0.0;
        if (negative) {
            degrees = 0.0 - degrees;
        }
        double d = Math.floor(degrees);
        double m_ = (degrees - d) * 60.0;
        double m = Math.floor(m_);
        double s = (m_ - m) * 60;
        return new Object[]{
                (int)d,
                (int)m,
                s,
                negative ? 'S' : 'N',
                negative ? 'W' : 'E'
        };
    }

    @Override
    public String toString() {
        Object[] east = dms(longitude * (180.0 / Math.PI));
        Object[] north = dms(latitude * (180.0 / Math.PI));
        //noinspection MalformedFormatString
        return String.format("%d\u00BA%dm%.2fs %s x %d\u00BA%dm%.2fs %s",
                             north[0], north[1], north[2], north[3],
                             east[0], east[1], east[2], east[4]);
    }

    public static GeoPoint fromPoint(Point p) {
        if (p.x == 0 && p.y == 0) {
            if (p.z < 0) return NORTH_POLE;
            if (p.z > 0) return SOUTH_POLE;
            throw new IllegalArgumentException("Cannot synthesize a GeoPoint for the origin: " + p);
        }
        double longitude = Math.atan2(p.x, p.y);
        if (longitude > Math.PI) longitude -= 2.0 * Math.PI;
        double latitude = Math.atan2(p.z, Math.sqrt(p.x * p.x + p.y * p.y));
        if (latitude > HALF_PI) latitude -= Math.PI;
        return new GeoPoint(Math.max(longitude, PI_NEGATIVE), Math.max(latitude, HALF_PI_NEGATIVE));
    }

    public static void main(String[] args) {
        Point north = new Point(0, 0, -1);
        Point south = new Point(0, 0, 1);
        Point equator30mE = new Point(0.5, Math.sqrt(3) / 2, 0);
        assert NORTH_POLE.equals(fromPoint(north));
        assert SOUTH_POLE.equals(fromPoint(south));
        GeoPoint gp = fromPoint(equator30mE);
        assert Math.abs(gp.latitude) < 0.000001;
        assert Math.abs(gp.longitude - Math.PI / 6) < 0.000001;

        assert Vector.betweenPoints(north, fromPoint(north).toPoint(1.0)).l < 0.000001;
        assert Vector.betweenPoints(south, fromPoint(south).toPoint(1.0)).l < 0.000001;
        assert Vector.betweenPoints(equator30mE, fromPoint(equator30mE).toPoint(1.0)).l < 0.000001;

        double[] values = {0.89724, 0.16523, 0.64682, -0.89724, -0.16523, -0.64682};
        for (double i : values) {
            for (double j : values) {
                for (double k : values) {
                    Point p = Point.ORIGIN.translate(new Vector(j, j, k).normalize());
                    assert Vector.betweenPoints(p, fromPoint(p).toPoint(1.0)).l < 0.000001;
                }
            }
        }
    }
}
