package net.venaglia.common.util.serializer;

import java.nio.ByteBuffer;

/**
 * User: ed
 * Date: 4/3/14
 * Time: 10:14 PM
 */
public class StringSerializerStrategy extends AbstractSerializerStrategy<String> {

    public static final StringSerializerStrategy INSTANCE = new StringSerializerStrategy();

    private StringSerializerStrategy() {
        super(String.class, '$');
    }

    public void serialize(String value, ByteBuffer out) {
        serializeString("str", value, out);
    }

    public String deserialize(ByteBuffer in) {
        return deserializeString("str", in);
    }

    static void init() {
        // no-op, just here to force this class to initialize
    }
}
