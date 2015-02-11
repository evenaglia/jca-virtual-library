package net.venaglia.common.util.extensible;

import net.venaglia.common.util.StronglyTypedKey;

/**
* User: ed
* Date: 1/25/15
* Time: 7:30 PM
*/
public class ExtendedPropertyKey<T> extends StronglyTypedKey<T> {

    public ExtendedPropertyKey(String name, Class<T> type) {
        super(name, type);
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtendedPropertyKey<T> create(String name, Class<?> type) {
        return new ExtendedPropertyKey<T>(name, (Class<T>)type);
    }

    public static class IntKey extends ExtendedPropertyKey<Integer> {

        private final int defaultValue;

        public IntKey(String name) {
            this(name, 0);

        }

        public IntKey(String name, int defaultValue) {
            super(name, Integer.class);
            this.defaultValue = defaultValue;
        }

        public int getDefaultValue() {
            return defaultValue;
        }
    }

    public static class LongKey extends ExtendedPropertyKey<Long> {

        private final long defaultValue;

        public LongKey(String name) {
            this(name, 0L);

        }

        public LongKey(String name, long defaultValue) {
            super(name, Long.class);
            this.defaultValue = defaultValue;
        }

        public long getDefaultValue() {
            return defaultValue;
        }
    }

    public static class FloatKey extends ExtendedPropertyKey<Float> {

        private final float defaultValue;

        public FloatKey(String name) {
            this(name, 0.0f);

        }

        public FloatKey(String name, float defaultValue) {
            super(name, Float.class);
            this.defaultValue = defaultValue;
        }

        public float getDefaultValue() {
            return defaultValue;
        }
    }

    public static class DoubleKey extends ExtendedPropertyKey<Double> {

        private final double defaultValue;

        public DoubleKey(String name) {
            this(name, 0.0);

        }

        public DoubleKey(String name, double defaultValue) {
            super(name, Double.class);
            this.defaultValue = defaultValue;
        }

        public double getDefaultValue() {
            return defaultValue;
        }
    }

    public static class BooleanKey extends ExtendedPropertyKey<Boolean> {

        private final boolean defaultValue;

        public BooleanKey(String name) {
            this(name, false);

        }

        public BooleanKey(String name, boolean defaultValue) {
            super(name, Boolean.class);
            this.defaultValue = defaultValue;
        }

        public boolean getDefaultValue() {
            return defaultValue;
        }
    }
}
