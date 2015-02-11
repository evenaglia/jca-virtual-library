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

    protected final NavigableMap<Float,Color> colorStops;

    protected boolean mutable = true;
    protected Color nanColor = Color.BLACK;

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

    private ColorGradient(NavigableMap<Float, Color> colorStops) {
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

    public ColorGradient tint(Color tintColor, float tintAmount) {
        NavigableMap<Float,Color> tint = new TreeMap<>();
        for (Map.Entry<Float,Color> entry : colorStops.subMap(ZERO, false, ONE, false).entrySet()) {
            Color color = entry.getValue();
            tint.put(entry.getKey(), new Color(
                    calculateMidpoint(color.r, tintColor.r, tintAmount),
                    calculateMidpoint(color.g, tintColor.g, tintAmount),
                    calculateMidpoint(color.b, tintColor.b, tintAmount),
                    calculateMidpoint(color.a, tintColor.a, tintAmount)
            ));
        }
        Color nanColor = new Color(
                calculateMidpoint(this.nanColor.r, tintColor.r, tintAmount),
                calculateMidpoint(this.nanColor.g, tintColor.g, tintAmount),
                calculateMidpoint(this.nanColor.b, tintColor.b, tintAmount),
                calculateMidpoint(this.nanColor.a, tintColor.a, tintAmount)
        );
        return new ColorGradient(tint).addStop(Float.NaN, nanColor);
    }

    public ColorGradient highPerformance() {
        return new HighPerformance(this);
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

    private static class HighPerformance extends ColorGradient {

        private final float[] color;

        private HighPerformance(ColorGradient base) {
            super((NavigableMap<Float,Color>)null);
            this.nanColor = base.nanColor;
            this.mutable = false;
            this.color = new float[8196];
            for (int i = 0, j = 0; i <= 2048; i++) {
                float p = i / 2048.0f;
                Color c = base.get(p);
                color[j++] = c.r;
                color[j++] = c.g;
                color[j++] = c.b;
                color[j++] = c.a;
            }
        }

        private HighPerformance(float[] color, Color nanColor) {
            super((NavigableMap<Float,Color>)null);
            this.color = color;
            this.nanColor = nanColor;
        }

        @Override
        public boolean hasStop(float stop) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Color getStop(float stop) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ColorGradient getMutableCopy() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ColorGradient highPerformance() {
            return this;
        }

        @Override
        public ColorGradient getReverseCopy() {
            float[] reverse = new float[8196];
            for (int i = 0, j = 0, k = 8192; i <= 2048; i++, k = 8192 - j) {
                reverse[j++] = color[k++];
                reverse[j++] = color[k++];
                reverse[j++] = color[k++];
                reverse[j++] = color[k];
            }
            return new HighPerformance(reverse, nanColor);
        }

        @Override
        public ColorGradient tint(Color tintColor, float tintAmount) {
            float[] color = this.color.clone();
            for (int i = 0, j = 0; i <= 2048; i++) {
                color[j] = calculateMidpoint(color[j], tintColor.r, tintAmount);
                j++;
                color[j] = calculateMidpoint(color[j], tintColor.g, tintAmount);
                j++;
                color[j] = calculateMidpoint(color[j], tintColor.b, tintAmount);
                j++;
                color[j] = calculateMidpoint(color[j], tintColor.a, tintAmount);
                j++;
            }
            Color nanColor = new Color(
                    calculateMidpoint(this.nanColor.r, tintColor.r, tintAmount),
                    calculateMidpoint(this.nanColor.g, tintColor.g, tintAmount),
                    calculateMidpoint(this.nanColor.b, tintColor.b, tintAmount),
                    calculateMidpoint(this.nanColor.a, tintColor.a, tintAmount)
            );
            return new HighPerformance(color, nanColor);
        }

        @Override
        public Color get(float where) {
            if (Float.isNaN(where)) {
                return nanColor;
            } else {
                int i = getIndex(where);
                return new Color(color[i++], color[i++], color[i++], color[i]);
            }
        }

        @Override
        public <T> T get(float where, XForm.View<T> view) {
            if (Float.isNaN(where)) {
                return view.convert(nanColor.r, nanColor.g, nanColor.b, nanColor.a);
            } else {
                int i = getIndex(where);
                return view.convert(color[i++], color[i++], color[i++], color[i]);
            }
        }

        @Override
        public void applyColor(float where, ColorBuffer buffer) {
            if (Float.isNaN(where)) {
                buffer.color(nanColor);
            } else {
                int i = getIndex(where);
                buffer.color(color[i++], color[i++], color[i]);
            }
        }

        @Override
        public void applyColorAndAlpha(float where, ColorBuffer buffer) {
            if (Float.isNaN(where)) {
                buffer.colorAndAlpha(nanColor);
            } else {
                int i = getIndex(where);
                buffer.colorAndAlpha(color[i++], color[i++], color[i++], color[i]);
            }
        }

        private int getIndex(float where) {
            if (where <= 0.0f) {
                return 0;
            } else if (where >= 1.0f) {
                return 8192;
            } else {
                return Math.round(where * 2048.0f) << 2;
            }
        }

        @Override
        public String toString() {
            return "ColorGradient(high-performance)";
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
