package net.venaglia.realms.common.physical.decorators;

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
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color(java.awt.Color color) {
        float[] components = color.getRGBComponents(null);
        this.r = components[0];
        this.g = components[1];
        this.b = components[2];
        this.a = components[3];
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
}
