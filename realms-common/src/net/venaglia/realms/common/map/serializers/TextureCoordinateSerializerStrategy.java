package net.venaglia.realms.common.map.serializers;

import net.venaglia.common.util.serializer.AbstractSerializerStrategy;
import net.venaglia.gloo.physical.texture.TextureCoordinate;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/6/14
 * Time: 9:36 PM
 */
public class TextureCoordinateSerializerStrategy extends AbstractSerializerStrategy<TextureCoordinate> {

    public static final TextureCoordinateSerializerStrategy INSTANCE = new TextureCoordinateSerializerStrategy();

    private TextureCoordinateSerializerStrategy() {
        super(TextureCoordinate.class, 'T');
    }

    public void serialize(TextureCoordinate value, ByteBuffer out) {
        out.putFloat(value.s);
        out.putFloat(value.t);
    }

    public TextureCoordinate deserialize(ByteBuffer in) {
        return new TextureCoordinate(in.getFloat(), in.getFloat());
    }

    public static void init() {
        // no-op, just here to force this class to initialize
    }
}
