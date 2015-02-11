package net.venaglia.common.util.extensible;

import java.util.Collection;

/**
 * User: ed
 * Date: 1/25/15
 * Time: 7:31 PM
 */
public abstract class AbstractExtensibleObject implements ExtensibleObject {

    protected <O extends ExtensibleObject,V> ExtendedPropertyProvider<O,V> getPropertyProvider(ExtendedPropertyKey<V> key) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)getClass();
        return ExtensibleObjectRegistry.getProvider(type, key);
    }

    protected <O extends ExtensibleObject> ExtendedPropertyProvider.IntProvider<O> getPropertyProvider(ExtendedPropertyKey.IntKey key) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)getClass();
        return ExtensibleObjectRegistry.getProvider(type, key);
    }

    protected <O extends ExtensibleObject> ExtendedPropertyProvider.LongProvider<O> getPropertyProvider(ExtendedPropertyKey.LongKey key) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)getClass();
        return ExtensibleObjectRegistry.getProvider(type, key);
    }

    protected <O extends ExtensibleObject> ExtendedPropertyProvider.FloatProvider<O> getPropertyProvider(ExtendedPropertyKey.FloatKey key) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)getClass();
        return ExtensibleObjectRegistry.getProvider(type, key);
    }

    protected <O extends ExtensibleObject> ExtendedPropertyProvider.DoubleProvider<O> getPropertyProvider(ExtendedPropertyKey.DoubleKey key) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)getClass();
        return ExtensibleObjectRegistry.getProvider(type, key);
    }

    protected <O extends ExtensibleObject> ExtendedPropertyProvider.BooleanProvider<O> getPropertyProvider(ExtendedPropertyKey.BooleanKey key) {
        @SuppressWarnings("unchecked")
        Class<O> type = (Class<O>)getClass();
        return ExtensibleObjectRegistry.getProvider(type, key);
    }

    protected Collection<ExtendedPropertyKey<?>> getKeys() {
        return ExtensibleObjectRegistry.getKeys(getClass());
    }

    @Override
    public boolean hasExtendedProperty(ExtendedPropertyKey<?> key) {
        return getPropertyProvider(key).hasValue(this);
    }

    @Override
    public boolean canSetExtendedProperty(ExtendedPropertyKey<?> key) {
        return getPropertyProvider(key).writable(this);
    }

    @Override
    public Collection<ExtendedPropertyKey<?>> getRegisteredExtendedProperties() {
        return getKeys();
    }

    @Override
    public <V> V getExtendedProperty(ExtendedPropertyKey<V> key) {
        return getPropertyProvider(key).getValue(this, null);
    }

    @Override
    public int getExtendedPrimitive(ExtendedPropertyKey.IntKey key) {
        return getPropertyProvider(key).getInt(this, key.getDefaultValue());
    }

    @Override
    public long getExtendedPrimitive(ExtendedPropertyKey.LongKey key) {
        return getPropertyProvider(key).getLong(this, key.getDefaultValue());
    }

    @Override
    public float getExtendedPrimitive(ExtendedPropertyKey.FloatKey key) {
        return getPropertyProvider(key).getFloat(this, key.getDefaultValue());
    }

    @Override
    public double getExtendedPrimitive(ExtendedPropertyKey.DoubleKey key) {
        return getPropertyProvider(key).getDouble(this, key.getDefaultValue());
    }

    @Override
    public boolean getExtendedPrimitive(ExtendedPropertyKey.BooleanKey key) {
        return getPropertyProvider(key).getBoolean(this, key.getDefaultValue());
    }

    @Override
    public <V> V getExtendedProperty(ExtendedPropertyKey<V> key, V defaultValue) {
        return getPropertyProvider(key).getValue(this, defaultValue);
    }

    @Override
    public int getExtendedPrimitive(ExtendedPropertyKey.IntKey key, int defaultValue) {
        return getPropertyProvider(key).getInt(this, defaultValue);
    }

    @Override
    public long getExtendedPrimitive(ExtendedPropertyKey.LongKey key, long defaultValue) {
        return getPropertyProvider(key).getLong(this, defaultValue);
    }

    @Override
    public float getExtendedPrimitive(ExtendedPropertyKey.FloatKey key, float defaultValue) {
        return getPropertyProvider(key).getFloat(this, defaultValue);
    }

    @Override
    public double getExtendedPrimitive(ExtendedPropertyKey.DoubleKey key, double defaultValue) {
        return getPropertyProvider(key).getDouble(this, defaultValue);
    }

    @Override
    public boolean getExtendedPrimitive(ExtendedPropertyKey.BooleanKey key, boolean defaultValue) {
        return getPropertyProvider(key).getBoolean(this, defaultValue);
    }

    @Override
    public <V> void setExtendedProperty(ExtendedPropertyKey<V> key, V value) {
        getPropertyProvider(key).setValue(this, value);
    }

    @Override
    public void setExtendedPrimitive(ExtendedPropertyKey.IntKey key, int value) throws UnsupportedOperationException {
        getPropertyProvider(key).setValue(this, value);
    }

    @Override
    public void setExtendedPrimitive(ExtendedPropertyKey.LongKey key, long value) throws UnsupportedOperationException {
        getPropertyProvider(key).setValue(this, value);
    }

    @Override
    public void setExtendedPrimitive(ExtendedPropertyKey.FloatKey key, float value) throws UnsupportedOperationException {
        getPropertyProvider(key).setValue(this, value);
    }

    @Override
    public void setExtendedPrimitive(ExtendedPropertyKey.DoubleKey key, double value) throws UnsupportedOperationException {
        getPropertyProvider(key).setValue(this, value);
    }

    @Override
    public void setExtendedPrimitive(ExtendedPropertyKey.BooleanKey key, boolean value) throws UnsupportedOperationException {
        getPropertyProvider(key).setValue(this, value);
    }
}
