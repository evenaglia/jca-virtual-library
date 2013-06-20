package net.venaglia.gloo.physical.texture;

import static org.lwjgl.opengl.GL11.*;

import net.venaglia.gloo.physical.decorators.Material;

import java.awt.image.BufferedImage;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 1:16 AM
 */
public interface Texture {

    Texture NO_TEXTURE = new AbstractTexture("") {
        public int getGlTextureId() {
            return 0;
        }

        public void allocate() {
            throw new UnsupportedOperationException();
        }

        public void deallocate() {
            throw new UnsupportedOperationException();
        }

        public void load() {
            // no-op
        }

        public Material asMaterial(TextureMapping mapping) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected BufferedImage loadImage() throws Exception {
            return null;
        }
    };

    enum ScalingFilter {
        NEAREST(GL_NEAREST, GL_NEAREST_MIPMAP_NEAREST), LINEAR(GL_LINEAR, GL_LINEAR_MIPMAP_LINEAR);

        public final int code;
        public final int mipMapedCode;

        private ScalingFilter(int code, int mipMapedCode) {
            this.code = code;
            this.mipMapedCode = mipMapedCode;
        }
    }

    int getGlTextureId();

    int getWidth();

    int getHeight();

    void allocate() throws TooManyTexturesException, IllegalStateException, TextureLoadException;

    void load();

    void deallocate() throws IllegalStateException;

    Material asMaterial(TextureMapping mapping);
}
