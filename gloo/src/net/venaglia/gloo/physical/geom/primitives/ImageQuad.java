package net.venaglia.gloo.physical.geom.primitives;

import net.venaglia.gloo.physical.decorators.AlphaRule;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.base.AbstractShape;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.physical.texture.mapping.SequenceMapping;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.demo.SingleShapeDemo;

/**
 * User: ed
 * Date: 5/15/13
 * Time: 6:10 PM
 */
public class ImageQuad extends AbstractShape<ImageQuad> {

    private static final Point[] BASE_POINTS = {
            new Point(-0.5, 0, 0.5),
            new Point(-0.5, 0, -0.5),
            new Point(0.5, 0, -0.5),
            new Point(0.5, 0, 0.5)
    };

    public enum Mode {
        SHADED(true, false),
        SHADED_WITH_ALPHA_TRANSPARENCY(true, true),
        SHADED_WITH_THRESHOLD_TRANSPARENCY_100(true, AlphaRule.Compare.GREATER_OR_EQUAL, 1),
        SHADED_WITH_THRESHOLD_TRANSPARENCY_50(true, AlphaRule.Compare.GREATER_OR_EQUAL, 0.5f),
        SHADED_WITH_THRESHOLD_TRANSPARENCY_0(true, AlphaRule.Compare.GREATER, 0.0f),
        SELF_ILLUMINATED(false, false),
        SELF_ILLUMINATED_WITH_THRESHOLD_TRANSPARENCY_100(false, AlphaRule.Compare.GREATER_OR_EQUAL, 1),
        SELF_ILLUMINATED_WITH_THRESHOLD_TRANSPARENCY_50(false, AlphaRule.Compare.GREATER_OR_EQUAL, 0.5f),
        SELF_ILLUMINATED_WITH_THRESHOLD_TRANSPARENCY_0(false, AlphaRule.Compare.GREATER, 0.0f),
        SELF_ILLUMINATED_WITH_ALPHA_TRANSPARENCY(false, true);

        private final Brush brush;

        private Mode(boolean shaded, boolean transparency) {
            Brush brush = new Brush();
            brush.setTexturing(true);
            brush.setLighting(!shaded);
            brush.setAlphaRule(transparency ? AlphaRule.ALPHA_TRANSPARENCY : null);
            this.brush = brush.immutable();
        }

        private Mode(boolean shaded, AlphaRule.Compare compare, float threshold) {
            Brush brush = new Brush();
            brush.setTexturing(true);
            brush.setLighting(!shaded);
            brush.setAlphaRule(new AlphaRule(compare, threshold));
            this.brush = brush.immutable();
        }

        public Brush getBrush() {
            return brush;
        }
    }

    private final Point center;
    private final Texture image;
    private final Mode mode;
    private final Vector frontNormal;
    private final Vector backNormal;
    private final TextureMapping textureMapping;

    private Color tint;

    public ImageQuad(Texture image) {
        this(image, Mode.SHADED, 0, 0, 1, 1);
    }

    public ImageQuad(Texture image, Mode mode) {
        this(image, mode, 0, 0, 1, 1);
    }

    public ImageQuad(Texture image, float left, float top, float right, float bottom) {
        this(image, Mode.SHADED, left, top, right, bottom);
    }

    public ImageQuad(Texture image, Mode mode, float left, float top, float right, float bottom) {
        this(BASE_POINTS.clone(), Point.ORIGIN, image, mode, buildTextureMapping(left, top, right, bottom));
    }

    private ImageQuad(Point[] points, Point center, Texture image, Mode mode, TextureMapping textureMapping) {
        super(assertLength(points, 4));
        this.center = center;
        this.image = image;
        this.mode = mode;
        this.frontNormal = Vector.cross(points[2], points[1], points[0]).normalize();
        this.backNormal = frontNormal.reverse();
        this.textureMapping = textureMapping;
        this.material = Material.INHERIT;
        this.tint = Color.WHITE;
    }

    private static TextureMapping buildTextureMapping(float left, float top, float right, float bottom) {
        TextureCoordinate[] coords = {
                new TextureCoordinate(left, top),
                new TextureCoordinate(left, bottom),
                new TextureCoordinate(right, bottom),
                new TextureCoordinate(right, top)
        };
        return new SequenceMapping(coords);
    }

    @Override
    public ImageQuad setMaterial(Material material) {
        Color color = Material.getColor(material);
        tint = color == null ? Color.WHITE : color;
        return this;
    }

    public Vector getNormal(int index) {
        if (index < 0 || index > 4) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return index < 2 ? frontNormal : backNormal;
    }

    @Override
    protected ImageQuad build(Point[] points, XForm xForm) {
        return new ImageQuad(points, xForm.apply(center), image, mode, textureMapping.copy());
    }

    @Override
    protected void project(GeometryBuffer buffer) {
        buffer.color(tint);
        buffer.applyBrush(mode.getBrush());
        buffer.useTexture(image, textureMapping);
        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
        buffer.vertex(points[0]);
        buffer.vertex(points[1]);
        buffer.vertex(points[2]);
        buffer.vertex(points[3]);
        buffer.end();
//        buffer.applyBrush(Brush.WIRE_FRAME);
//        buffer.color(Color.WHITE);
//        buffer.start(GeometryBuffer.GeometrySequence.QUADS);
//        buffer.vertex(points[0]);
//        buffer.vertex(points[1]);
//        buffer.vertex(points[2]);
//        buffer.vertex(points[3]);
//        buffer.end();
    }

    public static void main(String[] args) {
        Texture texture = new TextureFactory().loadClasspathResource("images/demo-texture.png")
                                              .setForceAlpha(true)
                                              .build();
        ImageQuad imageQuad = new ImageQuad(texture);
        new SingleShapeDemo(imageQuad).start();
    }
}
