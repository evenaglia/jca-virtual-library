package net.venaglia.realms.common.physical.texture.impl;

import static net.venaglia.realms.common.util.CallLogger.logCall;
import static net.venaglia.realms.common.util.CallLogger.logCalls;
import static org.lwjgl.opengl.GL11.*;

import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureLoadException;
import net.venaglia.realms.common.physical.texture.TooManyTexturesException;
import org.lwjgl.util.glu.GLU;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * User: ed
 * Date: 3/9/13
 * Time: 7:40 AM
 */
public class TextureFactory {

    private boolean closed = false;
    private Callable<BufferedImage> imageSource;
    private boolean forceAlpha;
    private boolean flipVertical;
    private boolean mipMapped;

    public TextureFactory loadFile(final File file) {
        if (closed) {
            throw new IllegalStateException("Builder is closed");
        }
        if (imageSource != null) {
            throw new IllegalStateException("Image source has already been set");
        }
        if (file == null) {
            throw new TextureLoadException("file cannot be null");
        }
        imageSource = new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                return ImageIO.read(file);
            }

            @Override
            public String toString() {
                return file.toString();
            }
        };
        return this;
    }

    public TextureFactory loadURL(final URL url) {
        if (closed) {
            throw new IllegalStateException("Builder is closed");
        }
        if (imageSource != null) {
            throw new IllegalStateException("Image source has already been set");
        }
        if (url == null) {
            throw new TextureLoadException("url cannot be null");
        }
        imageSource = new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                return ImageIO.read(url);
            }

            @Override
            public String toString() {
                return url.toString();
            }
        };
        return this;
    }

    public TextureFactory loadStream(final InputStream stream) {
        if (closed) {
            throw new IllegalStateException("Builder is closed");
        }
        if (imageSource != null) {
            throw new IllegalStateException("Image source has already been set");
        }
        if (stream == null) {
            throw new TextureLoadException("stream cannot be null");
        }
        imageSource = new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                return ImageIO.read(stream);
            }

            @Override
            public String toString() {
                return stream.toString();
            }
        };
        return this;
    }

    public TextureFactory loadClasspathResource(final String resource) {
        if (closed) {
            throw new IllegalStateException("Builder is closed");
        }
        if (imageSource != null) {
            throw new IllegalStateException("Image source has already been set");
        }
        if (resource == null) {
            throw new TextureLoadException("resource cannot be null");
        }
        final URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
        if (url == null) {
            throw new TextureLoadException("unable to resolve classpath resource: " + resource);
        }
        imageSource = new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                return ImageIO.read(url);
            }

            @Override
            public String toString() {
                return resource;
            }
        };
        return this;
    }

    public TextureFactory loadImage(final String name, final BufferedImage image) {
        if (closed) {
            throw new IllegalStateException("Builder is closed");
        }
        if (imageSource != null) {
            throw new IllegalStateException("Image source has already been set");
        }
        if (image == null) {
            throw new TextureLoadException("image cannot be null");
        }
        imageSource = new Callable<BufferedImage>() {
            public BufferedImage call() throws Exception {
                return image;
            }

            @Override
            public String toString() {
                return name;
            }
        };
        return this;
    }

    public TextureFactory loadImageSource(final String name, final Callable<BufferedImage> imageSource) {
        if (closed) {
            throw new IllegalStateException("Builder is closed");
        }
        if (this.imageSource != null) {
            throw new IllegalStateException("Image source has already been set");
        }
        this.imageSource = imageSource;
        return this;
    }

    public TextureFactory setForceAlpha(boolean forceAlpha) {
        this.forceAlpha = forceAlpha;
        return this;
    }

    public TextureFactory setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
        return this;
    }

    public TextureFactory setMipMapped(boolean mipMapped) {
        this.mipMapped = mipMapped;
        return this;
    }

    public Texture build() {
        return new TextureImpl(imageSource, forceAlpha, flipVertical, mipMapped);
    }

    private static class TextureImpl extends AbstractTexture {

        private final Callable<BufferedImage> imageSource;
        private final boolean forceAlpha;
        private final boolean flipVertical;
        private final boolean mipMapped;

        private TextureImpl(Callable<BufferedImage> imageSource, boolean forceAlpha, boolean flipVertical, boolean mipMapped)
                throws TooManyTexturesException {
            super(imageSource.toString());
            this.imageSource = imageSource;
            this.forceAlpha = forceAlpha;
            this.flipVertical = flipVertical;
            this.mipMapped = mipMapped;
        }

        @Override
        protected BufferedImage loadImage() throws Exception {
            return imageSource.call();
        }

        @Override
        protected boolean flipVertical() {
            return flipVertical;
        }

        @Override
        protected boolean forceAlpha() {
            return forceAlpha;
        }

        @Override
        protected void loadImpl() {
            if (mipMapped) {
                int srcFormat = alpha ? GL_RGBA : GL_RGB;
                int intFormat = alpha ? GL_RGBA8 : GL_RGB8;
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scalingFilter.mipMapedCode);
                if (logCalls) logCall("glTexParameteri", GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scalingFilter.mipMapedCode);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scalingFilter.code);
                if (logCalls) logCall("glTexParameteri", GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scalingFilter.code);
                GLU.gluBuild2DMipmaps(GL_TEXTURE_2D, 3, texWidth, texHeight, srcFormat, GL_UNSIGNED_BYTE, buffer);
                if (logCalls) logCall("gluBuild2DMipmaps", GL_TEXTURE_2D, (long)3, (long)texWidth, (long)texHeight, srcFormat, GL_UNSIGNED_BYTE, buffer);
            } else {
                super.loadImpl();
            }
        }
    }
}
