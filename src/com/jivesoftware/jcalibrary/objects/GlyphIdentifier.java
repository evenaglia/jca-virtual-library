package com.jivesoftware.jcalibrary.objects;

import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.XForm;
import net.venaglia.realms.common.physical.geom.base.AbstractShape;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.physical.texture.impl.TextureFactory;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.demo.SingleShapeDemo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * User: ed
 * Date: 5/3/13
 * Time: 9:07 AM
 */
public class GlyphIdentifier extends AbstractShape<GlyphIdentifier> {

    private static ThreadLocal<Texture> GLYPH_TEXTURE = new ThreadLocal<Texture>() {
        @Override
        protected Texture initialValue() {
            return new TextureFactory().loadImageSource("Glyphs", new GlyphTextureImageSource(DetailLevel.HIGH)).build();
        }
    };

    public final Point center;
    public final Vector frontNormal;
    public final Vector backNormal;
    public final int[] glyphId;

    private TextureMapping myTextureMapping;

    public GlyphIdentifier(int... glyphId) {
        this(buildPoints(glyphId.length), Point.ORIGIN, Vector.Y.reverse(), Vector.Y, glyphId);
    }

    private static Point[] buildPoints(int length) {
        Point[] points = new Point[length * 2 + 2];
        int k = 0;
        double x = length * -0.5;
        for (int i = 0; i <= length; i++) {
            points[k++] = new Point(x, 0, 0.5);
            points[k++] = new Point(x, 0, -0.5);
            x += 1.0;
        }
        return points;
    }

    public GlyphIdentifier(Point[] points,
                           Point center,
                           Vector frontNormal,
                           Vector backNormal,
                           int[] glyphId) {
        super(points);
        if (glyphId.length == 0) {
            throw new IllegalArgumentException("No glyphIds specified");
        }
        if (glyphId.length > 16) {
            throw new IllegalArgumentException("Too many glyphIds specified: " + glyphId.length);
        }
        for (int i : glyphId) {
            if (i < 0 || i >= GlyphTextureImageSource.FONT_AWESOME_CHARACTERS.length) {
                throw new IllegalArgumentException("Invalid glyphId, must be between 0 and " +
                                                           (GlyphTextureImageSource.FONT_AWESOME_CHARACTERS.length - 1) +
                                                           ": " + i);
            }
        }
        this.center = center;
        this.frontNormal = frontNormal;
        this.backNormal = backNormal;
        this.glyphId = glyphId;
        this.myTextureMapping = new MyTextureMapping();
    }

    public Vector getNormal(int index) {
        if (index < 0 || index > points.length) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return (index & 1) == 0 ? frontNormal : backNormal;
    }

    @Override
    protected GlyphIdentifier build(Point[] points, XForm xForm) {
        Vector normal = Vector.cross(points[2], points[1], points[0]).normalize();
        return new GlyphIdentifier(points, xForm.apply(center), normal, normal.reverse(), glyphId);
    }

    @Override
    protected void project(GeometryBuffer buffer) {
//        buffer.applyBrush(Brush.WIRE_FRAME);
        buffer.applyBrush(Brush.TEXTURED);
//        buffer.applyBrush(Brush.FRONT_SHADED);
        buffer.useTexture(GLYPH_TEXTURE.get(), myTextureMapping);
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        buffer.color(Color.WHITE);
        buffer.normal(frontNormal);
        for (int i = 0, l = points.length - 2; i < l; i += 2) {
            buffer.vertex(points[i]);
            buffer.vertex(points[i + 1]);
            buffer.vertex(points[i + 3]);
            buffer.vertex(points[i + 2]);
        }
        buffer.normal(backNormal);
        for (int i = points.length - 4; i >= 0; i -= 2) {
            buffer.vertex(points[i + 2]);
            buffer.vertex(points[i + 3]);
            buffer.vertex(points[i + 1]);
            buffer.vertex(points[i]);
        }
        buffer.end();
    }

    private class MyTextureMapping implements TextureMapping {

        private TextureCoordinate[] coords;
        private int seq = 0;

        {
            float f = 0.0625f;
            int[] ids = GlyphIdentifier.this.glyphId;
            List<TextureCoordinate> coords = new ArrayList<TextureCoordinate>(ids.length * 4);
            for (int id : ids) {
                float x = (id & 0xF) * f;
                float y = (id >> 4) * f;
                coords.add(new TextureCoordinate(x, y));
                coords.add(new TextureCoordinate(x, y + f));
                coords.add(new TextureCoordinate(x + f, y + f));
                coords.add(new TextureCoordinate(x + f, y));
            }
            this.coords = coords.toArray(new TextureCoordinate[coords.size()]);
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
        int glyphId1 = random.nextInt(GlyphTextureImageSource.FONT_AWESOME_CHARACTERS.length);
        int glyphId2 = random.nextInt(GlyphTextureImageSource.FONT_AWESOME_CHARACTERS.length);
        int glyphId3 = random.nextInt(GlyphTextureImageSource.FONT_AWESOME_CHARACTERS.length);
        int glyphId4 = random.nextInt(GlyphTextureImageSource.FONT_AWESOME_CHARACTERS.length);
        new SingleShapeDemo(new GlyphIdentifier(glyphId1, glyphId2, glyphId3, glyphId4), Color.WHITE, SingleShapeDemo.Mode.SHADED).start();
    }
}
