package net.venaglia.realms.common.map.things;

import net.venaglia.common.util.serializer.ArraySerializerStrategy;
import net.venaglia.common.util.serializer.PrimitiveSerializerStrategy;
import net.venaglia.common.util.serializer.SerializerStrategy;
import net.venaglia.common.util.serializer.StringSerializerStrategy;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.map.serializers.ColorSerializerStrategy;
import net.venaglia.realms.common.map.serializers.GeoPointSerializerStrategy;
import net.venaglia.realms.common.map.serializers.PointSerializerStrategy;
import net.venaglia.realms.common.map.serializers.TextureCoordinateSerializerStrategy;
import net.venaglia.realms.common.map.serializers.VectorSerializerStrategy;
import net.venaglia.realms.common.util.Identifiable;
import net.venaglia.realms.spec.map.GeoPoint;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ed
 * Date: 3/18/14
 * Time: 8:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class PropertyType<P> {

    private static final Map<Class<?>,PropertyType<?>> BY_TYPE = new HashMap<Class<?>,PropertyType<?>>();
    private static final Map<Class<?>,Class<?>> NATIVE_WRAPPERS = new HashMap<Class<?>,Class<?>>(16);

    static {
        NATIVE_WRAPPERS.put(boolean.class, Boolean.class);
        NATIVE_WRAPPERS.put(char.class, Character.class);
        NATIVE_WRAPPERS.put(byte.class, Byte.class);
        NATIVE_WRAPPERS.put(short.class, Short.class);
        NATIVE_WRAPPERS.put(int.class, Integer.class);
        NATIVE_WRAPPERS.put(long.class, Long.class);
        NATIVE_WRAPPERS.put(float.class, Float.class);
        NATIVE_WRAPPERS.put(double.class, Double.class);
    }

    public static final PropertyType<Long> ID =
            new PropertyType<Long>(Long.class, "id", PrimitiveSerializerStrategy.LONG_OBJ, null);
    public static final PropertyType<Boolean> FLAG =
            new PropertyType<Boolean>(Boolean.class, "flag", PrimitiveSerializerStrategy.BOOLEAN, false);
    public static final PropertyType<String> STRING =
            new PropertyType<String>(String.class, "string", StringSerializerStrategy.INSTANCE, null);
    public static final PropertyType<byte[]> BYTES =
            new PropertyType<byte[]>(byte[].class, "bytes", ArraySerializerStrategy.BYTES, new byte[0]);
    public static final PropertyType<Integer> INTEGER =
            new PropertyType<Integer>(int.class, "integer", PrimitiveSerializerStrategy.INTEGER, 0);
    public static final PropertyType<int[]> INTEGERS =
            new PropertyType<int[]>(int[].class, "integers", ArraySerializerStrategy.INTS, null);
    public static final PropertyType<Long> LONG =
            new PropertyType<Long>(Long.class, "long", PrimitiveSerializerStrategy.LONG, 0L);
    public static final PropertyType<Float> FLOAT =
            new PropertyType<Float>(float.class, "float", PrimitiveSerializerStrategy.FLOAT, 0.0f);
    public static final PropertyType<float[]> FLOATS =
            new PropertyType<float[]>(float[].class, "floats", ArraySerializerStrategy.FLOATS, new float[0]);
    public static final PropertyType<Double> DOUBLE =
            new PropertyType<Double>(double.class, "double", PrimitiveSerializerStrategy.DOUBLE, 0.0);
    public static final PropertyType<double[]> DOUBLES =
            new PropertyType<double[]>(double[].class, "doubles", ArraySerializerStrategy.DOUBLES, new double[0]);
    public static final PropertyType<Point> POINT =
            new PropertyType<Point>(Point.class, "point", PointSerializerStrategy.INSTANCE, null);
    public static final PropertyType<Point[]> POINTS =
            new PropertyType<Point[]>(Point[].class, "points", ArraySerializerStrategy.getHomogenousArraySerializer(Point.class, true), new Point[0]);
    public static final PropertyType<Vector> VECTOR =
            new PropertyType<Vector>(Vector.class, "vector", VectorSerializerStrategy.INSTANCE, null);
    public static final PropertyType<Vector[]> VECTORS =
            new PropertyType<Vector[]>(Vector[].class, "vectors", ArraySerializerStrategy.getHomogenousArraySerializer(Vector.class, true), new Vector[0]);
    public static final PropertyType<Color> COLOR =
            new PropertyType<Color>(Color.class, "color", ColorSerializerStrategy.INSTANCE, null);
    public static final PropertyType<GeoPoint> GEO_POINT =
            new PropertyType<GeoPoint>(GeoPoint.class, "geopoint", GeoPointSerializerStrategy.INSTANCE, null);
    public static final PropertyType<GeoPoint[]> GEO_POINTS =
            new PropertyType<GeoPoint[]>(GeoPoint[].class, "geopoints", ArraySerializerStrategy.getHomogenousArraySerializer(GeoPoint.class, true), new GeoPoint[0]);
    public static final PropertyType<TextureCoordinate> TEXTURE_COORD =
            new PropertyType<TextureCoordinate>(TextureCoordinate.class, "texture_coord", TextureCoordinateSerializerStrategy.INSTANCE, null);
    public static final PropertyType<TextureCoordinate[]> TEXTURE_COORDS =
            new PropertyType<TextureCoordinate[]>(TextureCoordinate[].class, "texture_coords", ArraySerializerStrategy.getHomogenousArraySerializer(TextureCoordinate.class, true), new TextureCoordinate[0]);

    private final Class<P> nativeType;
    private final Class<P> nativeWrapperType;
    private final String typeName;
    private final SerializerStrategy<P> serializer;
    private final P defaultValue;

    private PropertyType(Class<P> nativeType, String typeName, SerializerStrategy<P> serializer, P defaultValue) {
        if (nativeType == null) throw new NullPointerException("nativeType");
        if (typeName == null) throw new NullPointerException("typeName");
        if (serializer == null) throw new NullPointerException("serializer");
        this.nativeType = nativeType;
        this.typeName = typeName;
        this.serializer = serializer;
        this.defaultValue = defaultValue;
        //noinspection unchecked
        this.nativeWrapperType = NATIVE_WRAPPERS.containsKey(nativeType)
                                 ? (Class<P>)NATIVE_WRAPPERS.get(nativeType)
                                 : nativeType;
        BY_TYPE.put(nativeType, this);
    }

    public Class<P> getNativeType() {
        return nativeType;
    }

    public String getTypeName() {
        return typeName;
    }

    public SerializerStrategy<P> getSerializer() {
        return serializer;
    }

    public boolean isA(Object value) {
        return value != null && nativeWrapperType.isAssignableFrom(value.getClass());
    }

    public P getDefaultValue() {
        return defaultValue;
    }

    public P cast(Object obj) {
        return nativeWrapperType.cast(obj);
    }

    public P read(ByteBuffer in) {
        return serializer.deserialize(in);
    }

    public void write(P value, ByteBuffer out) {
        serializer.serialize(value, out);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PropertyType that = (PropertyType)o;

        if (!nativeType.equals(that.nativeType)) return false;
        if (!typeName.equals(that.typeName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = nativeType.hashCode();
        result = 31 * result + typeName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return typeName;
    }

    public static PropertyType<?> resolveForType(Class<?> type) {
        if (Identifiable.class.isAssignableFrom(type)) {
            return ID;
        }
        return BY_TYPE.get(type);
    }
}
