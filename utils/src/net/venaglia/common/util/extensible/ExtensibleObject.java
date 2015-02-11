package net.venaglia.common.util.extensible;

import java.util.Collection;

/**
 * User: ed
 * Date: 1/25/15
 * Time: 7:14 PM
 */
public interface ExtensibleObject {

    boolean hasExtendedProperty(ExtendedPropertyKey<?> key);

    boolean canSetExtendedProperty(ExtendedPropertyKey<?> key);

    <T> T getExtendedProperty(ExtendedPropertyKey<T> key);

    int getExtendedPrimitive(ExtendedPropertyKey.IntKey key);

    long getExtendedPrimitive(ExtendedPropertyKey.LongKey key);

    float getExtendedPrimitive(ExtendedPropertyKey.FloatKey key);

    double getExtendedPrimitive(ExtendedPropertyKey.DoubleKey key);

    boolean getExtendedPrimitive(ExtendedPropertyKey.BooleanKey key);

    <T> T getExtendedProperty(ExtendedPropertyKey<T> key, T defaultValue);

    int getExtendedPrimitive(ExtendedPropertyKey.IntKey key, int defaultValue);

    long getExtendedPrimitive(ExtendedPropertyKey.LongKey key, long defaultValue);

    float getExtendedPrimitive(ExtendedPropertyKey.FloatKey key, float defaultValue);

    double getExtendedPrimitive(ExtendedPropertyKey.DoubleKey key, double defaultValue);

    boolean getExtendedPrimitive(ExtendedPropertyKey.BooleanKey key, boolean defaultValue);

    <T> void setExtendedProperty(ExtendedPropertyKey<T> key, T value) throws UnsupportedOperationException;

    void setExtendedPrimitive(ExtendedPropertyKey.IntKey key, int value) throws UnsupportedOperationException;

    void setExtendedPrimitive(ExtendedPropertyKey.LongKey key, long value) throws UnsupportedOperationException;

    void setExtendedPrimitive(ExtendedPropertyKey.FloatKey key, float value) throws UnsupportedOperationException;

    void setExtendedPrimitive(ExtendedPropertyKey.DoubleKey key, double value) throws UnsupportedOperationException;

    void setExtendedPrimitive(ExtendedPropertyKey.BooleanKey key, boolean value) throws UnsupportedOperationException;

    Collection<ExtendedPropertyKey<?>> getRegisteredExtendedProperties();
}
