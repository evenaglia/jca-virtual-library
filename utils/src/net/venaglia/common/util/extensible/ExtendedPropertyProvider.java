package net.venaglia.common.util.extensible;

/**
 * User: ed
 * Date: 1/25/15
 * Time: 8:39 PM
 */
public interface ExtendedPropertyProvider<O extends ExtensibleObject,V> {

    ExtendedPropertyKey<V> getKey();

    Class<O> getType();

    boolean hasValue(O object);

    boolean writable(O object);

    V getValue(O object, V defaultValue);

    void setValue(O object, V value);

    interface IntProvider<O extends ExtensibleObject> extends ExtendedPropertyProvider<O,Integer> {
        ExtendedPropertyKey.IntKey getKey();
        int getInt(O object, int defaultValue);
        void setInt(O object, int value);
    }

    interface LongProvider<O extends ExtensibleObject> extends ExtendedPropertyProvider<O,Long> {
        ExtendedPropertyKey.LongKey getKey();
        long getLong(O object, long defaultValue);
        void setLong(O object, long value);
    }

    interface FloatProvider<O extends ExtensibleObject> extends ExtendedPropertyProvider<O,Float> {
        ExtendedPropertyKey.FloatKey getKey();
        float getFloat(O object, float defaultValue);
        void setFloat(O object, float value);
    }

    interface DoubleProvider<O extends ExtensibleObject> extends ExtendedPropertyProvider<O,Double> {
        ExtendedPropertyKey.DoubleKey getKey();
        double getDouble(O object, double defaultValue);
        void setDouble(O object, double value);
    }

    interface BooleanProvider<O extends ExtensibleObject> extends ExtendedPropertyProvider<O,Boolean> {
        ExtendedPropertyKey.BooleanKey getKey();
        boolean getBoolean(O object, boolean defaultValue);
        void setBoolean(O object, boolean value);
    }
}
