package net.venaglia.realms.common.map.serializers;

import net.venaglia.common.util.serializer.AbstractSerializerStrategy;
import net.venaglia.realms.spec.map.GeoPoint;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/6/14
 * Time: 9:36 PM
 */
public class GeoPointSerializerStrategy extends AbstractSerializerStrategy<GeoPoint> {

    public static final GeoPointSerializerStrategy INSTANCE = new GeoPointSerializerStrategy();

    private GeoPointSerializerStrategy() {
        super(GeoPoint.class, 'G');
    }

    public void serialize(GeoPoint value, ByteBuffer out) {
        out.putDouble(value.longitude);
        out.putDouble(value.latitude);
    }

    public GeoPoint deserialize(ByteBuffer in) {
        return new GeoPoint(in.getDouble(), in.getDouble());
    }

    public static void init() {
        // no-op, just here to force this class to initialize
    }
}
