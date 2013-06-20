package net.venaglia.gloo.physical.decorators;

import org.lwjgl.opengl.GL11;

/**
 * User: ed
 * Date: 5/20/13
 * Time: 8:24 AM
 */
public class AlphaRule {

    public static final AlphaRule ALPHA_TRANSPARENCY = new AlphaRule(Mode.ALPHA_TRANSPARENCY);
    public static final AlphaRule THRESHOLD_TRANSPARENCY_100 = new AlphaRule(Compare.GREATER_OR_EQUAL, 1.0f);
    public static final AlphaRule THRESHOLD_TRANSPARENCY_50 = new AlphaRule(Compare.GREATER_OR_EQUAL, 0.5f);

    public enum Mode {
        THRESHOLD_TRANSPARENCY, ALPHA_TRANSPARENCY
    }

    public enum Compare {
        GREATER(GL11.GL_GREATER), GREATER_OR_EQUAL(GL11.GL_GEQUAL);

        private final int glCode;

        Compare(int glCode) {
            this.glCode = glCode;
        }

        public int getGlCode() {
            return glCode;
        }

        @Override
        public String toString() {
            switch (this) {
                case GREATER:
                    return ">";
                case GREATER_OR_EQUAL:
                    return ">=";
            }
            return null;
        }
    }

    private final float thresholdValue;
    private final Compare compare;
    private final Mode transparencyMode;

    public AlphaRule(float thresholdValue) {
        this(validateThresholdValue(thresholdValue), Compare.GREATER, Mode.THRESHOLD_TRANSPARENCY);
    }

    public AlphaRule(Compare compare, float thresholdValue) {
        this(validateThresholdValue(thresholdValue), validateCompare(compare), Mode.THRESHOLD_TRANSPARENCY);
    }

    private AlphaRule(Mode transparencyMode) {
        this(0, null, transparencyMode);
    }

    private AlphaRule(float thresholdValue, Compare compare, Mode transparencyMode) {
        this.thresholdValue = thresholdValue;
        this.compare = compare;
        this.transparencyMode = transparencyMode;
    }

    private static float validateThresholdValue(float thresholdValue) {
        if (Float.isNaN(thresholdValue) || Float.isInfinite(thresholdValue)) {
            throw new IllegalArgumentException("Threshold value must be a real number: " + thresholdValue);
        }
        if (thresholdValue < 0 || thresholdValue > 1) {
            throw new IllegalArgumentException("Threshold values must be between 0 and 1: " + thresholdValue);
        }
        return thresholdValue;
    }

    private static Compare validateCompare(Compare compare) {
        if (compare == null) {
            throw new IllegalArgumentException("Value for compare cannot be null");
        }
        return compare;
    }

    public float getThresholdValue() {
        return thresholdValue;
    }

    public Compare getCompare() {
        return compare;
    }

    public Mode getTransparencyMode() {
        return transparencyMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlphaRule that = (AlphaRule)o;

        if (Float.compare(that.thresholdValue, thresholdValue) != 0) return false;
        if (transparencyMode != that.transparencyMode) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (thresholdValue != +0.0f ? Float.floatToIntBits(thresholdValue) : 0);
        result = 31 * result + transparencyMode.hashCode();
        return result;
    }

    @Override
    public String toString() {
        switch (transparencyMode) {
            case THRESHOLD_TRANSPARENCY:
                return String.format("AlphaBlending[%s%4.2f]", compare, thresholdValue);
            case ALPHA_TRANSPARENCY:
                return "AlphaBlending[alpha]";
        }
        return null;
    }
}
