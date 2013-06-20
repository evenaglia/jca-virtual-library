package net.venaglia.gloo.physical.lights;

import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;

/**
 * User: ed
 * Date: 9/27/12
 * Time: 10:12 AM
 */
public class DynamicPointSourceLight extends AbstractLight {

    private Color color;
    private Point source;

    public DynamicPointSourceLight(Point source) {
        this(Color.WHITE, source);
    }

    public DynamicPointSourceLight(Color color, Point source) {
        if (color == null) throw new NullPointerException("color");
        if (source == null) throw new NullPointerException("source");
        this.color = color;
        this.source = source;
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    public Point getSource() {
        return source;
    }

    public void setSource(Point source) {
        this.source = source;
    }

    @Override
    protected Color getSimpleColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
