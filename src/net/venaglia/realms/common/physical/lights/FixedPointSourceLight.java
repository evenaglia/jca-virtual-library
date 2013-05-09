package net.venaglia.realms.common.physical.lights;

import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Point;

/**
 * User: ed
 * Date: 9/27/12
 * Time: 10:12 AM
 */
public class FixedPointSourceLight extends AbstractLight {

    private final Color color;
    private final Point source;

    public FixedPointSourceLight(Point source) {
        this(Color.WHITE, source);
    }

    public FixedPointSourceLight(Color color, Point source) {
        if (color == null) throw new NullPointerException("color");
        if (source == null) throw new NullPointerException("source");
        this.color = color;
        this.source = source;
    }

    public Point getSource() {
        return source;
    }

    @Override
    protected Color getSimpleColor() {
        return color;
    }
}
