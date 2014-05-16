package net.venaglia.realms.common.map.serializers;

import net.venaglia.common.util.serializer.AbstractSerializerStrategy;
import net.venaglia.gloo.physical.geom.Point;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/6/14
 * Time: 9:36 PM
 */
public class PointSerializerStrategy extends AbstractSerializerStrategy<Point> {

    public static final PointSerializerStrategy INSTANCE = new PointSerializerStrategy();

    private PointSerializerStrategy() {
        super(Point.class, '.');
    }

    public void serialize(Point value, ByteBuffer out) {
        out.putDouble(value.x);
        out.putDouble(value.y);
        out.putDouble(value.z);
    }

    public Point deserialize(ByteBuffer in) {
        return new Point(in.getDouble(), in.getDouble(), in.getDouble());
    }

    public static void init() {
        // no-op, just here to force this class to initialize
    }
}
