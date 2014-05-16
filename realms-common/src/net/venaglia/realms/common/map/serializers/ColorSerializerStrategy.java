package net.venaglia.realms.common.map.serializers;

import net.venaglia.common.util.Secondary;
import net.venaglia.common.util.serializer.AbstractSerializerStrategy;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.gloo.physical.decorators.Color;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/6/14
 * Time: 9:36 PM
 */
public class ColorSerializerStrategy extends AbstractSerializerStrategy<Color> {

    public static final SerializerStrategy<Color> INSTANCE = new ColorSerializerStrategy();
    public static final SerializerStrategy<Color> OPAQUE_INSTANCE = new OpaqueColorSerializerStrategy();

    private ColorSerializerStrategy() {
        super(Color.class, '#');
    }

    public void serialize(Color value, ByteBuffer out) {
        out.putFloat(value.r);
        out.putFloat(value.g);
        out.putFloat(value.b);
        out.putFloat(value.a);
    }

    public Color deserialize(ByteBuffer in) {
        return new Color(in.getFloat(), in.getFloat(), in.getFloat(), in.getFloat());
    }

    public static void init() {
        // no-op, just here to force this class to initialize
    }

    @Secondary
    private static class OpaqueColorSerializerStrategy extends AbstractSerializerStrategy<Color> {

        private OpaqueColorSerializerStrategy() {
            super(Color.class, 'α');
        }

        public void serialize(Color value, ByteBuffer out) {
            out.putFloat(value.r);
            out.putFloat(value.g);
            out.putFloat(value.b);
        }

        public Color deserialize(ByteBuffer in) {
            return new Color(in.getFloat(), in.getFloat(), in.getFloat(), 1.0f);
        }
    }
}
