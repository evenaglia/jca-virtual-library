package net.venaglia.realms.common.physical.decorators;

import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.projection.Decorator;
import net.venaglia.realms.common.projection.GeometryBuffer;

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
        return paint(color, Brush.NO_LIGHTING);
    }

    public static Material makeWireFrame(final Color color) {
        return paint(color, Brush.WIRE_FRAME);
    }

    public static Material makePoints(final Color color) {
        return paint(color, Brush.POINTS);
    }

    public static Material makeFrontShaded(final Color color) {
        return paint(color, Brush.FRONT_SHADED);
    }

    public static Material makeTexture(final Texture texture, final TextureMapping mapping) {
        return new Material() {
            public boolean isStatic() {
                return true;
            }

            public void apply(long nowMS, GeometryBuffer buffer) {
                buffer.applyBrush(Brush.TEXTURED);
                buffer.useTexture(texture, mapping);
                buffer.color(Color.WHITE);
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
}
