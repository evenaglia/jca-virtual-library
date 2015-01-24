package net.venaglia.realms.spec.map;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;

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
    private static final double TWO_PI = PI * 2.0;

    public static final GeoPoint NORTH_POLE = new GeoPoint(0, HALF_PI_NEGATIVE);
    public static final GeoPoint SOUTH_POLE = new GeoPoint(0, HALF_PI);

    public static final XForm.View<GeoPoint> GEO_POINT_FROM_XYZ = new XForm.View<GeoPoint>() {
        public GeoPoint convert(double x, double y, double z, double w) {
            if (x == 0 && y == 0) {
                if (z < 0) return NORTH_POLE;
                if (z > 0) return SOUTH_POLE;
                throw new IllegalArgumentException("Cannot synthesize a GeoPoint for the origin: (0,0,0)");
            }
            @SuppressWarnings("SuspiciousNameCombination")
            double longitude = Math.atan2(x, y);
            if (longitude > Math.PI) longitude -= 2.0 * Math.PI;
            double latitude = Math.atan2(z, Math.sqrt(x * x + y * y));
            if (latitude > HALF_PI) latitude -= Math.PI;
            return new GeoPoint(Math.max(longitude, PI_NEGATIVE), Math.max(latitude, HALF_PI_NEGATIVE));
        }
    };

    public final double longitude;
    public final double latitude;

    public GeoPoint(double longitude, double latitude) {
        assert longitude >= PI_NEGATIVE;
        assert longitude <= PI;
        assert latitude >= HALF_PI_NEGATIVE;
        assert latitude <= HALF_PI;
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

    public boolean nearLatitude(double latitude, double threshold) {
        return threshold >= Math.abs(latitude - this.latitude);
    }

    public boolean nearLongitude(double longitude, double threshold) {
        double diff = Math.abs(longitude - this.longitude);
        return threshold >= diff || threshold >= Math.abs(TWO_PI - diff);
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

    public String toSourceLiteral() {
        return String.format("new GeoPoint(longBitsToDouble(%dL), longBitsToDouble(%dL))",
                             Double.doubleToLongBits(longitude),
                             Double.doubleToLongBits(latitude));
    }

    public static GeoPoint fromPoint(Point p) {
        return GEO_POINT_FROM_XYZ.convert(p.x, p.y, p.z, 1);
    }

    public static GeoPoint fromPoint(double x, double y, double z) {
        return GEO_POINT_FROM_XYZ.convert(x, y, z, 1);
    }

    public static GeoPoint midPoint(GeoPoint a, GeoPoint b, double n) {
        if (n == 0.0) {
            return a;
        }
        if (n == 1.0) {
            return b;
        }
        double m = 1.0 - n;

        double ac = Math.cos(a.latitude);
        double ax = Math.sin(a.longitude) * ac;
        double ay = Math.cos(a.longitude) * ac;
        double az = Math.sin(a.latitude);
        double bc = Math.cos(b.latitude);
        double bx = Math.sin(b.longitude) * bc;
        double by = Math.cos(b.longitude) * bc;
        double bz = Math.sin(b.latitude);

        double x = (ax * m + bx * n);
        double y = (ay * m + by * n);
        double z = (az * m + bz * n);
        return fromPoint(x, y, z);
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
                    Point p = Point.ORIGIN.translate(new Vector(i, j, k).normalize());
                    assert Vector.betweenPoints(p, fromPoint(p).toPoint(1.0)).l < 0.000001;
                }
            }
        }
    }
}
