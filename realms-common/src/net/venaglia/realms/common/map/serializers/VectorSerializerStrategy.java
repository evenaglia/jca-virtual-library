package net.venaglia.realms.common.map.serializers;

import net.venaglia.common.util.serializer.AbstractSerializerStrategy;
import net.venaglia.gloo.physical.geom.Vector;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/6/14
 * Time: 9:36 PM
 */
public class VectorSerializerStrategy extends AbstractSerializerStrategy<Vector> {

    public static final VectorSerializerStrategy INSTANCE = new VectorSerializerStrategy();

    private VectorSerializerStrategy() {
        super(Vector.class, 'V');
    }

    public void serialize(Vector value, ByteBuffer out) {
        out.putDouble(value.i);
        out.putDouble(value.j);
        out.putDouble(value.k);
    }

    public Vector deserialize(ByteBuffer in) {
        return new Vector(in.getDouble(), in.getDouble(), in.getDouble());
    }

    public static void init() {
        // no-op, just here to force this class to initialize
    }
}
