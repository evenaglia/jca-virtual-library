package net.venaglia.gloo.physical.texture;

import static net.venaglia.gloo.util.CallLogger.logCall;
import static net.venaglia.gloo.util.CallLogger.logCalls;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import net.venaglia.common.util.Consumer;
import org.lwjgl.util.glu.GLU;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
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
    private int scaleWidth = -1;
    private int scaleHeight = -1;
    private boolean mipMapped;
    private Consumer<BufferedImage> imageConsumer;

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

    public TextureFactory setScale(int scaleWidth, int scaleHeight) {
        if (scaleWidth < 8) {
            throw new IllegalArgumentException("scaleWidth cannot be less than 8: " + scaleWidth);
        }
        if (scaleHeight < 8) {
            throw new IllegalArgumentException("scaleHeight cannot be less than 8: " + scaleHeight);
        }
        if (scaleWidth > 4096) {
            throw new IllegalArgumentException("scaleWidth cannot exceed 4096: " + scaleWidth);
        }
        if (scaleHeight > 4096) {
            throw new IllegalArgumentException("scaleHeight cannot exceed 4096: " + scaleHeight);
        }
        if (!powerOfTwo(scaleWidth)) {
            throw new IllegalArgumentException("scaleWidth must be a power fo two: " + scaleWidth);
        }
        if (!powerOfTwo(scaleHeight)) {
            throw new IllegalArgumentException("scaleHeight must be a power fo two: " + scaleHeight);
        }
        this.scaleWidth = scaleWidth;
        this.scaleHeight = scaleHeight;
        return this;
    }

    private static boolean powerOfTwo(int n) {
        switch (n) {
            case 8:
            case 16:
            case 32:
            case 64:
            case 128:
            case 256:
            case 512:
            case 1024:
            case 2048:
            case 4096:
                return true;
            default:
                return false;
        }
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

    public TextureFactory captureImage(Consumer<BufferedImage> imageConsumer) {
        this.imageConsumer = imageConsumer;
        return this;
    }

    public Texture build() {
        return new TextureImpl(imageSource, scaleWidth, scaleHeight, forceAlpha, flipVertical, mipMapped, imageConsumer);
    }

    private static class TextureImpl extends AbstractTexture {

        private final Callable<BufferedImage> imageSource;
        private final int scaleWidth;
        private final int scaleHeight;
        private final boolean forceAlpha;
        private final boolean flipVertical;
        private final boolean mipMapped;

        private Consumer<BufferedImage> imageConsumer;

        private TextureImpl(Callable<BufferedImage> imageSource,
                            int scaleWidth,
                            int scaleHeight,
                            boolean forceAlpha,
                            boolean flipVertical,
                            boolean mipMapped,
                            Consumer<BufferedImage> imageConsumer)
                throws TooManyTexturesException {
            super(imageSource.toString());
            this.imageSource = imageSource;
            this.scaleWidth = scaleWidth;
            this.scaleHeight = scaleHeight;
            this.forceAlpha = forceAlpha;
            this.flipVertical = flipVertical;
            this.mipMapped = mipMapped;
            this.imageConsumer = imageConsumer;
        }

        @Override
        protected BufferedImage loadImage() throws Exception {
            BufferedImage img = imageSource.call();
            int w = img.getWidth();
            int h = img.getHeight();
            int tw = scaleWidth;
            int th = scaleHeight;
            if (tw < 0 || th < 0) {
                tw = 8;
                th = 8;
                while (tw < w && tw < 4096) tw <<= 1;
                while (th < h && tw < 4096) th <<= 1;
            }
            for (int sw = scale(w, tw), sh = scale(h, th); w != tw || h != th; w = sw, h = sh, sw = scale(w, tw), sh = scale(h, th)) {
                ColorModel colorModel = img.getColorModel();
                BufferedImage smallerImage;
                if (colorModel instanceof IndexColorModel) {
                    smallerImage = new BufferedImage(sw, sh, img.getType(), (IndexColorModel)colorModel);
                } else {
                    smallerImage = new BufferedImage(sw, sh, img.getType());
                }
                Graphics2D graphics = (Graphics2D)smallerImage.getGraphics();
                graphics.drawImage(img, 0, 0, sw, sh, 0, 0, w, h, null);
                img = smallerImage;
            }
            if (imageConsumer != null) {
                imageConsumer.consume(img);
                imageConsumer = null;
            }
            return img;
        }

        private int scale(int current, int target) {
            int result = current;
            if (result < target) {
                result <<= 1;
                if (result > target) {
                    result = target;
                }
            } else if (result > target) {
                result >>= 1;
                if (result < target) {
                    result = target;
                }
            }
            return result;
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
            super.loadImpl();
            if (mipMapped) {
                int srcFormat = alpha ? GL_RGBA : GL_RGB;
                GLU.gluBuild2DMipmaps(GL_TEXTURE_2D, 3, texWidth, texHeight, srcFormat, GL_UNSIGNED_BYTE, buffer);
                if (logCalls) logCall("gluBuild2DMipmaps", GL_TEXTURE_2D, (long)3, (long)texWidth, (long)texHeight, srcFormat, GL_UNSIGNED_BYTE, buffer);
            }
        }

        @Override
        protected int getMinFilterCode() {
            return mipMapped ? scalingFilter.mipMapedCode : super.getMinFilterCode();
        }
    }
}
