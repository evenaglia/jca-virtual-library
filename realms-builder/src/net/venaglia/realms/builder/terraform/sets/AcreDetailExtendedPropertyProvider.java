package net.venaglia.realms.builder.terraform.sets;

import net.venaglia.common.util.extensible.ExtendedPropertyKey;
import net.venaglia.common.util.extensible.ExtendedPropertyProvider;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.spec.GeoSpec;

import java.util.Arrays;

/**
 * User: ed
 * Date: 1/26/15
 * Time: 8:17 AM
 */
public abstract class AcreDetailExtendedPropertyProvider<V> implements ExtendedPropertyProvider<AcreDetail,V> {

    private final ExtendedPropertyKey<V> key;

    protected AcreDetailExtendedPropertyProvider(ExtendedPropertyKey<V> key) {
        this.key = key;
    }

    @Override
    public ExtendedPropertyKey<V> getKey() {
        return key;
    }

    @Override
    public Class<AcreDetail> getType() {
        return AcreDetail.class;
    }

    @Override
    public boolean hasValue(AcreDetail object) {
        return getValue(object, null) != null;
    }

    @Override
    public boolean writable(AcreDetail object) {
        return false;
    }

    @Override
    public void setValue(AcreDetail object, V value) {
        throw new UnsupportedOperationException();
    }

    public static class IntProvider extends AcreDetailExtendedPropertyProvider<Integer>
                                    implements ExtendedPropertyProvider.IntProvider<AcreDetail> {

        protected final int[] data = new int[GeoSpec.ACRES.iGet()];

        public IntProvider(ExtendedPropertyKey.IntKey key) {
            super(key);
            Arrays.fill(data, key.getDefaultValue());
        }

        @Override
        public boolean writable(AcreDetail object) {
            return true;
        }

        @Override
        public boolean hasValue(AcreDetail object) {
            return true;
        }

        @Override
        public ExtendedPropertyKey.IntKey getKey() {
            return (ExtendedPropertyKey.IntKey)super.getKey();
        }

        @Override
        public int getInt(AcreDetail object, int defaultValue) {
            return data[object.getId()];
        }

        @Override
        public void setInt(AcreDetail object, int value) {
            data[object.getId()] = value;
        }

        @Override
        public Integer getValue(AcreDetail object, Integer defaultValue) {
            return getInt(object, defaultValue == null ? getKey().getDefaultValue() : defaultValue);
        }

        @Override
        public void setValue(AcreDetail object, Integer value) {
            setInt(object, value);
        }
    }

    public static class FloatProvider extends AcreDetailExtendedPropertyProvider<Float>
                                      implements ExtendedPropertyProvider.FloatProvider<AcreDetail> {

        protected final float[] data = new float[GeoSpec.ACRES.iGet()];

        public FloatProvider(ExtendedPropertyKey.FloatKey key) {
            super(key);
            Arrays.fill(data, key.getDefaultValue());
        }

        @Override
        public boolean writable(AcreDetail object) {
            return true;
        }

        @Override
        public boolean hasValue(AcreDetail object) {
            return true;
        }

        @Override
        public ExtendedPropertyKey.FloatKey getKey() {
            return (ExtendedPropertyKey.FloatKey)super.getKey();
        }

        @Override
        public float getFloat(AcreDetail object, float defaultValue) {
            return data[object.getId()];
        }

        @Override
        public void setFloat(AcreDetail object, float value) {
            data[object.getId()] = value;
        }

        @Override
        public Float getValue(AcreDetail object, Float defaultValue) {
            return getFloat(object, defaultValue == null ? getKey().getDefaultValue() : defaultValue);
        }

        @Override
        public void setValue(AcreDetail object, Float value) {
            setFloat(object, value);
        }
    }
}
