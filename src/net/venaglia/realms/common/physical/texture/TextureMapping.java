package net.venaglia.realms.common.physical.texture;

import net.venaglia.realms.common.physical.geom.Point;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 12:55 AM
 */
public interface TextureMapping {

    void newSequence();

    TextureCoordinate unwrap(Point p);

    void unwrap(double x, double y, double z, float[] out);

    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    TextureMapping clone();
}
