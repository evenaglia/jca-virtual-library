package net.venaglia.gloo.util;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.projection.ColorBuffer;
import net.venaglia.gloo.util.debug.OutputGraph;

import java.awt.*;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * User: ed
 * Date: 1/12/15
 * Time: 8:40 AM
 */
public class ColorGradient {

    private static final Float ZERO = 0.0f;
    private static final Float ONE = 1.0f;

    private final NavigableMap<Float,Color> colorStops;

    private boolean mutable = true;
    private Color nanColor = Color.BLACK;

    public ColorGradient(Color left, Color right) {
        if (left == null) throw new NullPointerException("left");
        if (right == null) throw new NullPointerException("right");
        colorStops = new TreeMap<>();
        colorStops.put(ZERO, left);
        colorStops.put(ONE, right);
    }

    private ColorGradient(ColorGradient clone) {
        this(new TreeMap<>(clone.colorStops));
    }

    public ColorGradient(NavigableMap<Float, Color> colorStops) {
        this.colorStops = colorStops;
    }

    private void ensureMutable() {
        if (!mutable) {
            throw new UnsupportedOperationException("ColorGradient is immutable");
        }
    }

    public ColorGradient addStop(float stop, Color color) {
        ensureMutable();
        if (color == null) {
            throw new NullPointerException("color");
        }
        if (Float.isNaN(stop)) {
            nanColor = color;
        } else {
            colorStops.put(stop, color);
        }
        return this;
    }

    public boolean hasStop(float stop) {
        if (Float.isNaN(stop)) {
            return true;
        }
        return colorStops.containsKey(stop);
    }

    public Color removeStop(float stop) {
        ensureMutable();
        if (Float.isNaN(stop)) {
            return nanColor;
        } else if (stop <= 0.0f || stop >= 1.0f) {
            throw new IllegalArgumentException("Cannot remove a stop that is out of range, value must be between 0 and 1: " + stop);
        }
        return colorStops.remove(stop);
    }

    public Color getStop(float stop) {
        if (Float.isNaN(stop)) {
            return nanColor;
        } else if (stop < 0.0f || stop > 1.0f) {
            throw new IllegalArgumentException("Cannot get a stop that is out of range, value must be between 0 and 1: " + stop);
        }
        return colorStops.get(stop);
    }

    public void markImmutable() {
        mutable = false;
    }

    public ColorGradient getMutableCopy() {
        return new ColorGradient(this);
    }

    public ColorGradient getReverseCopy() {
        NavigableMap<Float,Color> reverse = new TreeMap<>();
        reverse.put(ZERO, colorStops.get(ONE));
        reverse.put(ONE, colorStops.get(ZERO));
        for (Map.Entry<Float,Color> entry : colorStops.subMap(ZERO, false, ONE, false).entrySet()) {
            reverse.put(1.0f - entry.getKey(), entry.getValue());
        }
        return new ColorGradient(reverse).addStop(Float.NaN, nanColor);
    }

    public Color get(float where) {
        if (Float.isNaN(where)) {
            return nanColor;
        } else if (where <= 0.0f) {
            return colorStops.get(ZERO);
        } else if (where >= 1.0f) {
            return colorStops.get(ONE);
        } else {
            Float stop = where;
            Color c = colorStops.get(stop);
            if (c != null) return c;
            Map.Entry<Float,Color> leftEntry = colorStops.floorEntry(stop);
            Map.Entry<Float,Color> rightEntry = colorStops.ceilingEntry(stop);
            float midpoint = calculateRelativeMidpoint(leftEntry.getKey(), rightEntry.getKey(), where);
            Color left = leftEntry.getValue();
            Color right = rightEntry.getValue();
            return new Color(
                    calculateMidpoint(left.r, right.r, midpoint),
                    calculateMidpoint(left.g, right.g, midpoint),
                    calculateMidpoint(left.b, right.b, midpoint),
                    calculateMidpoint(left.a, right.a, midpoint)
            );
        }
    }

    public <T> T get(float where, XForm.View<T> view) {
        if (Float.isNaN(where)) {
            return convert(view, nanColor);
        } else if (where <= 0.0f) {
            return convert(view, colorStops.get(ZERO));
        } else if (where >= 1.0f) {
            return convert(view, colorStops.get(ONE));
        } else {
            Float stop = where;
            Color c = colorStops.get(stop);
            if (c != null) {
                return convert(view, c);
            }
            Map.Entry<Float,Color> leftEntry = colorStops.floorEntry(stop);
            Map.Entry<Float,Color> rightEntry = colorStops.ceilingEntry(stop);
            float midpoint = calculateRelativeMidpoint(leftEntry.getKey(), rightEntry.getKey(), where);
            Color left = leftEntry.getValue();
            Color right = rightEntry.getValue();
            return view.convert(
                    calculateMidpoint(left.r, right.r, midpoint),
                    calculateMidpoint(left.g, right.g, midpoint),
                    calculateMidpoint(left.b, right.b, midpoint),
                    calculateMidpoint(left.a, right.a, midpoint)
            );
        }
    }

    public void applyColor(float where, ColorBuffer buffer) {
        if (Float.isNaN(where)) {
            buffer.color(nanColor);
        } else if (where <= 0.0f) {
            buffer.color(colorStops.get(ZERO));
        } else if (where >= 1.0f) {
            buffer.color(colorStops.get(ONE));
        } else {
            Float stop = where;
            Color c = colorStops.get(stop);
            if (c != null) {
                buffer.color(c);
                return;
            }
            Map.Entry<Float,Color> leftEntry = colorStops.floorEntry(stop);
            Map.Entry<Float,Color> rightEntry = colorStops.ceilingEntry(stop);
            float midpoint = calculateRelativeMidpoint(leftEntry.getKey(), rightEntry.getKey(), where);
            Color left = leftEntry.getValue();
            Color right = rightEntry.getValue();
            buffer.color(
                    calculateMidpoint(left.r, right.r, midpoint),
                    calculateMidpoint(left.g, right.g, midpoint),
                    calculateMidpoint(left.b, right.b, midpoint)
            );
        }
    }

    public void applyColorAndAlpha(float where, ColorBuffer buffer) {
        if (Float.isNaN(where)) {
            buffer.colorAndAlpha(nanColor);
        } else if (Float.isNaN(where)) {
            buffer.colorAndAlpha(colorStops.get(ZERO));
        } else if (where <= 0.0f) {
            buffer.colorAndAlpha(colorStops.get(ZERO));
        } else if (where >= 1.0f) {
            buffer.colorAndAlpha(colorStops.get(ONE));
        } else {
            Float stop = where;
            Color c = colorStops.get(stop);
            if (c != null) {
                buffer.colorAndAlpha(c);
                return;
            }
            Map.Entry<Float,Color> leftEntry = colorStops.floorEntry(stop);
            Map.Entry<Float,Color> rightEntry = colorStops.ceilingEntry(stop);
            float midpoint = calculateRelativeMidpoint(leftEntry.getKey(), rightEntry.getKey(), where);
            Color left = leftEntry.getValue();
            Color right = rightEntry.getValue();
            buffer.colorAndAlpha(
                    calculateMidpoint(left.r, right.r, midpoint),
                    calculateMidpoint(left.g, right.g, midpoint),
                    calculateMidpoint(left.b, right.b, midpoint),
                    calculateMidpoint(left.a, right.a, midpoint)
            );
        }
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder(32);
        buffer.append("ColorGradient(");
        boolean first = true;
        for (Map.Entry<Float,Color> entry : colorStops.entrySet()) {
            if (first) {
                first = false;
            } else {
                buffer.append(",");
            }
            buffer.append(String.format("%.2f", entry.getKey()));
            buffer.append("->");
            buffer.append(entry.getValue());
        }
        buffer.append(")(");
        return buffer.toString();
    }

    private static <T> T convert(XForm.View<T> view, Color c) {
        return view.convert(c.r, c.g, c.b, c.a);
    }

    private static float calculateRelativeMidpoint(float left, float right, float where) {
        assert left < where;
        assert right > where;
        return (where - left) / (right - left);
    }

    private static float calculateMidpoint(float left, float right, float p) {
        if (p <= 0.0f) {
            return left;
        } else if (p >= 1.0f) {
            return right;
        } else {
            return right * p + left * (1.0f - p);
        }
    }

    public static void main(String[] args) {
        ColorGradient colorGradient = new ColorGradient(Color.BLACK, Color.WHITE);
        colorGradient.addStop(0.2f, Color.BLUE);
        colorGradient.addStop(0.4f, Color.GREEN);
        colorGradient.addStop(0.6f, Color.YELLOW);
        colorGradient.addStop(0.8f, Color.RED);
        colorGradient = new ColorGradient(new Color(0.0f,0.0f,0.25f), new Color(1.0f,1.0f,1.0f))
                .addStop(0.10f, new Color(0.0f, 0.0f, 0.4f))
                .addStop(0.35f, new Color(0.0f, 0.2f, 1.0f))
                .addStop(0.49f, new Color(0.6f, 0.9f, 1.0f))
                .addStop(0.50f, new Color(1.0f, 0.9f, 0.8f))
                .addStop(0.51f, new Color(0.0f, 0.8f, 0.1f))
                .addStop(0.65f, new Color(0.4f, 0.1f, 0.0f))
                .addStop(0.90f, new Color(0.8f, 0.8f, 0.9f));

//        colorGradient = colorGradient.getReverseCopy();
        OutputGraph debug = new OutputGraph("Gradient", new Dimension(1024, 768), 0, 0, 600);
        float lastX = Float.NaN;
        for (int i = -128; i <= 640; i++) {
            float stop = i / 512.0f;
            float x = stop - 0.5f;
            float y = i < 0 || i > 512 ? 0.1f : 0.25f;
            if (!Float.isNaN(lastX)) {
                Color color = colorGradient.get(stop);
                java.awt.Color javaColor = new java.awt.Color(color.r, color.g, color.b, color.a);
                debug.addPoly(javaColor, lastX, -y, x, -y, x, y, lastX, y);
            }
            lastX = x;
        }
    }
}
