package net.venaglia.gloo.physical.texture.mapping;

import net.venaglia.common.util.Series;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.TextureMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * User: ed
 * Date: 5/16/13
 * Time: 8:04 AM
 */
public class SequenceMapping implements TextureMapping, Series<TextureCoordinate> {

    protected int seq = 0;
    protected TextureCoordinate[] coords;

    public SequenceMapping(TextureCoordinate[] coords) {
        this.coords = coords;
    }

    public void newSequence() {
        seq = 0;
    }

    public TextureCoordinate unwrap(Point p) {
        return getNextTextureCoordinate();
    }

    public void unwrap(double x, double y, double z, float[] out) {
        TextureCoordinate coord = getNextTextureCoordinate();
        out[0] = coord.s;
        out[1] = coord.t;
    }

    protected TextureCoordinate getNextTextureCoordinate() {
        TextureCoordinate coord = coords[seq % coords.length];
        seq++;
        return coord;
    }

    public TextureMapping copy() {
        return new SequenceMapping(coords.clone());
    }

    public int size() {
        return coords.length;
    }

    public Iterator<TextureCoordinate> iterator() {
        return Arrays.asList(coords).iterator();
    }

    public static Recorder record() {
        return new Recorder();
    }

    public static class Recorder {

        private List<TextureCoordinate> coordinates = new ArrayList<TextureCoordinate>();

        private Recorder() {
            // private constructor
        }

        public Recorder add(TextureCoordinate coordinate) {
            if (coordinates == null) {
                throw new IllegalStateException("Can't add coordinates to a Recorder that's already done.");
            }
            if (coordinate == null) {
                throw new NullPointerException("coordinate");
            }
            coordinates.add(coordinate);
            return this;
        }

        public SequenceMapping done() {
            TextureCoordinate[] coords = coordinates.toArray(new TextureCoordinate[coordinates.size()]);
            this.coordinates = null;
            return new SequenceMapping(coords);
        }
    }
}
