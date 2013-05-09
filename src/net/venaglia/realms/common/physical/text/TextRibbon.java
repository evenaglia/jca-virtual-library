package net.venaglia.realms.common.physical.text;

import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.Pair;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.Random;

/**
 * User: ed
 * Date: 5/8/13
 * Time: 1:57 PM
 */
public class TextRibbon extends AbstractShape<TextRibbon> {

    private static final Brush BRUSH;

    static {
        Brush brush = new Brush();
        brush.setTexturing(true);
        BRUSH = brush.immutable();
    }

    private final Vector normal;
    private final TextureCoordinate[] coords;
    private final Point center;
    private final Texture fontTexture;

    private TextureMapping myTextureMapping;

    public TextRibbon(FontBuilder fontBuilder, String text) {
        this(build(fontBuilder, text), Vector.Y.reverse(), Point.ORIGIN, fontBuilder.getTexture());
    }

    private TextRibbon(Pair<Point[],TextureCoordinate[]> ribbon, Vector normal, Point center, Texture fontTexture) {
        this(ribbon.getA(), ribbon.getB(), normal, center, fontTexture);
    }

    private TextRibbon(Point[] points, TextureCoordinate[] coords, Vector normal, Point center, Texture fontTexture) {
        super(points);
        this.coords = coords;
        this.normal = normal;
        this.center = center;
        this.fontTexture = fontTexture;
        this.myTextureMapping = new MyTextureMapping();
    }

    private static Pair<Point[],TextureCoordinate[]> build(FontBuilder fontBuilder, String text) {
        float[] xCoords = new float[text.length() + 1];
        char[] characters = new char[text.length()];
        float xAccum = 0;
        int k = 0;
        for (int i = 0, l = text.length(); i < l; i++) {
            char c = text.charAt(i);
            float w = fontBuilder.getWidth(c);
            if (c > 0) {
                xAccum += w;
                xCoords[k] = xAccum;
                characters[k] = c;
                k++;
            }
        }
        if (k == 0) {
            throw new IllegalArgumentException("No printable characters in passed string");
        }

        float xCenter = xAccum * 0.5f;
        Point[] points = new Point[k * 2 + 2];
        TextureCoordinate[] coords = new TextureCoordinate[k * 4];
        float[] box = { 0.0f, 0.0f, 0.0f, 0.0f };
        points[0] = new Point(-xCenter, 0, 0.5);
        points[1] = new Point(-xCenter, 0, -0.5);
        int l = k, j = 2;
        k = 0;
        for (int i = 0; i < l; i++) {
            points[j++] = new Point(xCoords[i] - xCenter, 0, 0.5);
            points[j++] = new Point(xCoords[i] - xCenter, 0, -0.5);
            fontBuilder.getCharcaterBox(characters[i], box);
            coords[k++] = new TextureCoordinate(box[0],box[1]);
            coords[k++] = new TextureCoordinate(box[0],box[3]);
            coords[k++] = new TextureCoordinate(box[2],box[3]);
            coords[k++] = new TextureCoordinate(box[2],box[1]);
        }
        return new Pair<Point[],TextureCoordinate[]>(points, coords);
    }

    @Override
    public TextRibbon setMaterial(Material material) {
        throw new UnsupportedOperationException("TextRibbon does not support dynamic materials");
    }

    @Override
    protected TextRibbon build(Point[] points, XForm xForm) {
        return new TextRibbon(points, coords, xForm.apply(normal), xForm.apply(center), fontTexture);
    }

    public Vector getNormal(int index) {
        if (index < 0 || index > points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return normal;
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.pushBrush();
        buffer.applyBrush(BRUSH);

        buffer.useTexture(fontTexture, myTextureMapping);
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        buffer.normal(normal);
        int l = coords.length >> 2;
        for (int i = 0, j = 0; i < l; i++, j += 2) {
            buffer.vertex(points[j]);
            buffer.vertex(points[j + 1]);
            buffer.vertex(points[j + 3]);
            buffer.vertex(points[j + 2]);
        }
        buffer.end();

//        projectOutlines(buffer, l);

        buffer.popBrush();
    }

    private void projectOutlines(GeometryBuffer buffer, int l) {
        buffer.applyBrush(Brush.WIRE_FRAME);
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        buffer.normal(normal);
        for (int i = 0, j = 0; i < l; i++, j += 2) {
            buffer.vertex(points[j]);
            buffer.vertex(points[j + 1]);
            buffer.vertex(points[j + 3]);
            buffer.vertex(points[j + 2]);
        }
        buffer.end();
    }

    private class MyTextureMapping implements TextureMapping {

        private int seq = 0;

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

        private TextureCoordinate getNextTextureCoordinate() {
            TextureCoordinate coord = coords[seq % coords.length];
            seq++;
            return coord;
        }

        @SuppressWarnings({ "CloneDoesntDeclareCloneNotSupportedException", "CloneDoesntCallSuperClone" })
        public TextureMapping clone() {
            return new MyTextureMapping();
        }
    }

    public static void main(String[] args) {
        Random random = new Random();
        String[] months = "January February March April May June July August September October November December".split(" ");
        FontBuilder fontBuilder = new FontBuilder("fonts/DroidSansMono.ttf", DetailLevel.MEDIUM_HIGH);
        String text = months[random.nextInt(months.length)];
        System.out.println(text);
        new SingleShapeDemo(new TextRibbon(fontBuilder, text)).start();
    }
}
