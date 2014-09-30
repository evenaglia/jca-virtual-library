package net.venaglia.gloo.physical.decorators;

/**
 * User: ed
 * Date: 8/26/12
 * Time: 6:07 PM
 */
public final class Color {

    public static final Color WHITE   = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    public static final Color BLACK   = new Color(0.0f, 0.0f, 0.0f, 1.0f);
    public static final Color RED     = new Color(1.0f, 0.0f, 0.0f, 1.0f);
    public static final Color GREEN   = new Color(0.0f, 1.0f, 0.0f, 1.0f);
    public static final Color BLUE    = new Color(0.0f, 0.0f, 1.0f, 1.0f);
    public static final Color CYAN    = new Color(0.0f, 1.0f, 1.0f, 1.0f);
    public static final Color MAGENTA = new Color(1.0f, 0.0f, 1.0f, 1.0f);
    public static final Color YELLOW  = new Color(1.0f, 1.0f, 0.0f, 1.0f);
    public static final Color ORANGE  = new Color(1.0f, 0.9f, 0.0f, 1.0f);

    public static final Color GRAY_75 = new Color(0.75f, 0.75f, 0.75f, 1.0f);
    public static final Color GRAY_50 = new Color(0.5f, 0.5f, 0.5f, 1.0f);
    public static final Color GRAY_25 = new Color(0.25f, 0.25f, 0.25f, 1.0f);

    public static final Color DEFAULT = WHITE;

    public final float r;
    public final float g;
    public final float b;
    public final float a;

    public Color(float r, float g, float b) {
        this(r,g,b,1);
    }

    public Color(float r, float g, float b, float a) {
        this.r = rangeCheck(r, "red");
        this.g = rangeCheck(g, "green");
        this.b = rangeCheck(b, "blue");
        this.a = rangeCheck(a, "alpha");
    }

    public Color(java.awt.Color color) {
        this(color.getRGBComponents(null));
    }

    public Color(int rgba) {
        this(getRGBComponents(rgba));
    }

    private Color(float[] components) {
        this(components[0], components[1], components[2], components[3]);
    }

    private float rangeCheck(float value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be greater than 0: " + value);
        }
        return Math.min(value,1);
    }

    public boolean isOpaque() {
        return a < 1;
    }

    public int toRGBA() {
        int r = Math.round(this.r * 255.0f) & 0xFF;
        int g = Math.round(this.g * 255.0f) & 0xFF;
        int b = Math.round(this.b * 255.0f) & 0xFF;
        int a = Math.round(this.a * 255.0f) & 0xFF;
        return a << 24 | r << 16 | g << 8 | b;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Color color = (Color)o;

        if (Float.compare(color.a, a) != 0) return false;
        if (Float.compare(color.b, b) != 0) return false;
        if (Float.compare(color.g, g) != 0) return false;
        if (Float.compare(color.r, r) != 0) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (r != +0.0f ? Float.floatToIntBits(r) : 0);
        result = 31 * result + (g != +0.0f ? Float.floatToIntBits(g) : 0);
        result = 31 * result + (b != +0.0f ? Float.floatToIntBits(b) : 0);
        result = 31 * result + (a != +0.0f ? Float.floatToIntBits(a) : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("c[%02x%02x%02x,a=%4.2f]", Math.round(r * 255), Math.round(g * 255), Math.round(b * 255), a);
    }

    private static float[] getRGBComponents(int rgba) {
        int r = rgba >> 16 & 0xFF;
        int g = rgba >> 8 & 0xFF;
        int b = rgba & 0xFF;
        int a = rgba >> 24 & 0xFF;
        return new float[]{
                r / 255.0f,
                g / 255.0f,
                b / 255.0f,
                a / 255.0f
        };
    }
}
