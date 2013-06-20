package net.venaglia.gloo.physical.text;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.physical.texture.mapping.SequenceMapping;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.common.util.Pair;
import net.venaglia.gloo.demo.SingleShapeDemo;

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
    private final Point center;
    private final Texture fontTexture;
    private final TextureMapping textureMapping;

    public TextRibbon(FontBuilder fontBuilder, String text) {
        this(build(fontBuilder, text), Point.ORIGIN, fontBuilder.getTexture());
    }

    private TextRibbon(Pair<Point[],TextureMapping> ribbon, Point center, Texture fontTexture) {
        this(ribbon.getA(), ribbon.getB(), center, fontTexture);
    }

    private TextRibbon(Point[] points, TextureMapping textureMapping, Point center, Texture fontTexture) {
        super(points);
        this.normal = Vector.cross(points[2], points[1], points[0]);
        this.center = center;
        this.fontTexture = fontTexture;
        this.textureMapping = textureMapping;
    }

    private static Pair<Point[],TextureMapping> build(FontBuilder fontBuilder, String text) {
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
        return new Pair<Point[],TextureMapping>(points, new SequenceMapping(coords));
    }

    @Override
    public TextRibbon setMaterial(Material material) {
        throw new UnsupportedOperationException("TextRibbon does not support dynamic materials");
    }

    @Override
    protected TextRibbon build(Point[] points, XForm xForm) {
        return new TextRibbon(points, textureMapping, xForm.apply(center), fontTexture);
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

        buffer.useTexture(fontTexture, textureMapping);
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        buffer.normal(normal);
        int l = (points.length - 2) >> 1;
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

    public static void main(String[] args) {
        Random random = new Random();
        String[] months = "January February March April May June July August September October November December".split(" ");
        FontBuilder fontBuilder = new FontBuilder("fonts/DroidSansMono.ttf", DetailLevel.MEDIUM_HIGH);
        String text = months[random.nextInt(months.length)];
        System.out.println(text);
        new SingleShapeDemo(new TextRibbon(fontBuilder, text)).start();
    }
}
