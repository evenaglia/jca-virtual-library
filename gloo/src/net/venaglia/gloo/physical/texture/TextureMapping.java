package net.venaglia.gloo.physical.texture;

import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.texture.mapping.SequenceMapping;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 12:55 AM
 */
public interface TextureMapping {

    TextureMapping DUMMY = new SequenceMapping(new TextureCoordinate[]{new TextureCoordinate(0,0)});

    void newSequence();

    TextureCoordinate unwrap(Point p);

    void unwrap(double x, double y, double z, float[] out);

    TextureMapping copy();
}
