package net.venaglia.realms.common.map.things;

import net.venaglia.common.util.serializer.ArraySerializerStrategy;
import net.venaglia.common.util.serializer.PrimitiveSerializerStrategy;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.common.util.serializer.StringSerializerStrategy;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.realms.common.map.data.AbstractDataStore;
import net.venaglia.realms.common.map.data.CubeImpl;
import net.venaglia.realms.common.map.data.ThingRefImpl;
import net.venaglia.realms.common.map.data.rdbms.JDBCDataStore;
import net.venaglia.realms.common.map.serializers.ColorSerializerStrategy;
import net.venaglia.realms.common.map.things.annotations.AnnotationDrivenThingProcessor;
import net.venaglia.realms.common.map.things.annotations.Property;
import net.venaglia.realms.common.map.things.annotations.ThingType;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * User: ed
 * Date: 4/17/14
 * Time: 5:48 PM
 */
@ThingType("test")
public class TestThing extends AbstractThing {

    static {
        AnnotationDrivenThingProcessor.process(new TestThing());
    }

    @Property(Property.Flags.REQUIRED)
    String name;

    @Property
    Color color;

    @Property(Property.Flags.REQUIRED)
    int size;

    @Property
    Point[] mesh;

    @Property
    Vector[] normals;

    @Override
    public String toString() {
        return "TestThing{" +
                "name='" + name + '\'' +
                ", color=" + color +
                ", size=" + size +
                ", mesh=" + Arrays.toString(mesh) +
                ", normals=" + Arrays.toString(normals) +
                '}';
    }

    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        addRequired("name", "test", StringSerializerStrategy.INSTANCE, buffer);
        add("color", new Color(0.5f,0.5f,0.5f,1.0f), ColorSerializerStrategy.INSTANCE, buffer);
        addRequired("size", 3, PrimitiveSerializerStrategy.INTEGER, buffer);
        Point[] mesh = new Point[] { new Point(1,2,3), new Point(4,5,6), new Point(7,8,9) };
        Vector[] norm = new Vector[] { new Vector(1,2,3), new Vector(4,5,6), new Vector(7,8,9) };
        add("mesh", mesh, ArraySerializerStrategy.getHomogenousArraySerializer(Point.class, true), buffer);
        add("normals", norm, ArraySerializerStrategy.getHomogenousArraySerializer(Vector.class, true), buffer);

        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);

        System.out.println(Arrays.toString(data));
        System.out.println(data.length + " bytes");

        AbstractDataStore dataStore = new JDBCDataStore() {
            @Override
            protected void populateCube(Long id, CubeImpl cube) {
                // no-op
            }
        };
        ThingRefImpl<TestThing> ref = ThingRefImpl.getUnused();
        CubeImpl cube = dataStore.getCommonDataSources().getCubeCache().get(0L);
        ThingFactory<? extends TestThing> factory = ThingFactory.getFor(TestThing.class);
        ThingMetadata<? extends TestThing> metadata = factory.getMetadata();
        ref.load(1234L, 0, 0, 0, metadata, cube, new ThingProperties(metadata, data));
        TestThing testThing = ref.get();

        System.out.println(testThing);
    }

    private static <T> void add(String name, T value, SerializerStrategy<T> strategy, ByteBuffer out) {
        StringSerializerStrategy.INSTANCE.serialize(name, out);
        if (value == null) {
            PrimitiveSerializerStrategy.BOOLEAN.serialize(true, out);
        } else {
            PrimitiveSerializerStrategy.BOOLEAN.serialize(false, out);
            strategy.serialize(value, out);
        }
    }

    private static <T> void addRequired(String name, T value, SerializerStrategy<T> strategy, ByteBuffer out) {
        StringSerializerStrategy.INSTANCE.serialize(name, out);
        strategy.serialize(value, out);
    }
}
