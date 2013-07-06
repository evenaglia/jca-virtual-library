package net.venaglia.gloo.physical.decorators;

import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.projection.Decorator;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.impl.NoOpGeometryBuffer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 10:53 AM
 */
public abstract class Material implements Decorator {

    public static final Material DEFAULT = new Material() {
        public boolean isStatic() {
            return true;
        }

        public void apply(long nowMS, GeometryBuffer buffer) {
            buffer.color(Color.DEFAULT);
        }
    };

    public static final Material INHERIT = new Material() {

        public boolean isStatic() {
            return true;
        }

        public void apply(long nowMS, GeometryBuffer buffer) {
            // no-op
        }
    };

    public static final Material INHERIT_TEXTURE = new Material() {
        public boolean isStatic() {
            return true;
        }

        public void apply(long nowMS, GeometryBuffer buffer) {
            buffer.applyBrush(Brush.TEXTURED);
        }
    };

    public static Material makeSelfIlluminating(final Color color) {
        return paint(color, color.isOpaque() ? Brush.SELF_ILLUMINATED : Brush.SELF_ILLUMINATED_ALPHA);
    }

    public static Material makeWireFrame(final Color color) {
        return paint(color, Brush.WIRE_FRAME);
    }

    public static Material makePoints(final Color color) {
        return paint(color, Brush.POINTS);
    }

    public static Material makeFrontShaded(final Color color) {
        return paint(color, color.isOpaque() ? Brush.FRONT_SHADED : Brush.FRONT_SHADED_ALPHA);
    }

    public static Material makeTexture(final Texture texture, final TextureMapping mapping) {
        return makeTexture(Brush.TEXTURED, Color.WHITE, texture, mapping);
    }

    public static Material makeAlphaBlended(final Texture texture, final TextureMapping mapping) {
        return makeTexture(Brush.TEXTURED_ALPHA, Color.WHITE, texture, mapping);
    }

    public static Material makeTexture(final Brush brush, final Texture texture, final TextureMapping mapping) {
        return makeTexture(brush, Color.WHITE, texture, mapping);
    }

    public static Material makeTexture(final Color tint, final Texture texture, final TextureMapping mapping) {
        return makeTexture(Brush.TEXTURED, tint, texture, mapping);
    }

    public static Material makeAlphaBlended(final Color tint, final Texture texture, final TextureMapping mapping) {
        return makeTexture(Brush.TEXTURED_ALPHA, tint, texture, mapping);
    }

    public static Material makeTexture(final Brush brush, final Color tint, final Texture texture, final TextureMapping mapping) {
        if (!brush.isTexturing()) {
            throw new IllegalArgumentException("The passed brush does not enable texturing");
        }
        return new Material() {
            public boolean isStatic() {
                return true;
            }

            public void apply(long nowMS, GeometryBuffer buffer) {
                buffer.applyBrush(brush);
                buffer.useTexture(texture, mapping);
                buffer.color(tint);
            }
        };
    }

    public static Material paint(final Color color, final Brush brush) {
        if (color == null) throw new NullPointerException("color");
        if (brush == null) throw new NullPointerException("brush");
        return new Material() {
            public boolean isStatic() {
                return true;
            }

            public void apply(long nowMS, GeometryBuffer buffer) {
                buffer.applyBrush(brush);
                buffer.color(color);
            }
        };
    }

    public static Brush getBrush(Material material) {
        ValueBuffer valueBuffer = new ValueBuffer();
        material.apply(0, valueBuffer);
        return valueBuffer.brush.get();
    }

    public static Color getColor(Material material) {
        ValueBuffer valueBuffer = new ValueBuffer();
        material.apply(0, valueBuffer);
        return valueBuffer.color.get();
    }

    private static class ValueBuffer extends NoOpGeometryBuffer {

        final AtomicReference<Brush> brush = new AtomicReference<Brush>();
        final AtomicReference<Color> color = new AtomicReference<Color>();

        void reset() {
            brush.set(null);
            color.set(null);
        }

        @Override
        public void applyBrush(Brush brush) {
            this.brush.compareAndSet(null, brush);
        }

        @Override
        public void color(Color color) {
            this.colorAndAlpha(new Color(color.r, color.g, color.b, 1.0f));
        }

        @Override
        public void color(float r, float g, float b) {
            this.colorAndAlpha(new Color(r, g, b));
        }

        @Override
        public void colorAndAlpha(Color color) {
            this.color.compareAndSet(null, color);
        }

        @Override
        public void colorAndAlpha(float r, float g, float b, float a) {
            this.colorAndAlpha(new Color(r, g, b, a));
        }
    }
}
