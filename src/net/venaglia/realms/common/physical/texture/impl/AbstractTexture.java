package net.venaglia.realms.common.physical.texture.impl;

import static net.venaglia.realms.common.util.CallLogger.*;
import static org.lwjgl.opengl.GL11.*;

import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureLoadException;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.physical.texture.TextureSizeException;
import net.venaglia.realms.common.physical.texture.TooManyTexturesException;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.util.debug.OutputGraph;
import org.lwjgl.BufferUtils;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Hashtable;

/**
 * User: ed
 * Date: 3/6/13
 * Time: 1:17 AM
 */
public abstract class AbstractTexture implements Texture {

    protected enum State {
        NEW, ALLOCATED, LOADED
    }

    protected final String name;
    protected final boolean logging;

    protected int glTextureId;
    protected int width = -1;
    protected int height = -1;
    protected int texWidth = -1;
    protected int texHeight = -1;
    protected boolean alpha;
    protected ScalingFilter scalingFilter = ScalingFilter.LINEAR;
    protected ByteBuffer buffer;
    protected State state;

    public AbstractTexture(String name) throws TooManyTexturesException {
        this(name, false);
    }

    protected AbstractTexture(String name, boolean logging) {
        if (name == null) throw new NullPointerException("name");
        this.name = name;
        this.logging = logging;
        this.state = State.NEW;
    }

    public int getGlTextureId() {
        if (state == State.NEW) {
            allocate();
        }
        return glTextureId;
    }

    public int getWidth() {
        if (state == State.NEW) {
            allocate();
        }
        return width;
    }

    public int getHeight() {
        if (state == State.NEW) {
            allocate();
        }
        return height;
    }

    public void load() {
        if (state == State.NEW) {
            allocate();
        }
        if (state == State.ALLOCATED) {
            loadImpl();
            state = State.LOADED;
        }
    }

    protected void loadImpl() {
        int srcFormat = alpha ? GL_RGBA : GL_RGB;
        int intFormat = alpha ? GL_RGBA8 : GL_RGB8;
        glTexImage2D(GL_TEXTURE_2D, 0, intFormat, texWidth, texHeight, 0, srcFormat, GL_UNSIGNED_BYTE, buffer);
        if (logCalls) logCall("glTexImage2D", GL_TEXTURE_2D, 0, intFormat, (long)texWidth, (long)texHeight, 0, srcFormat, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scalingFilter.code);
        if (logCalls) logCall("glTexParameteri", GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, scalingFilter.code);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scalingFilter.code);
        if (logCalls) logCall("glTexParameteri", GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, scalingFilter.code);
    }

    protected abstract BufferedImage loadImage() throws Exception;

    public void allocate() throws TooManyTexturesException, IllegalStateException, TextureLoadException {
        if (state != State.NEW) {
            throw new IllegalStateException();
        }
        try {
            allocateImpl(loadImage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new TextureLoadException(e);
        }
    }

    protected void allocateImpl(BufferedImage image) {
        width = image.getWidth();
        height = image.getHeight();
        if (width > GLConstants.MAX_TEXTURE_DIMENSION || height > GLConstants.MAX_TEXTURE_DIMENSION) {
            throw new TextureSizeException(GLConstants.MAX_TEXTURE_DIMENSION);
        }
        alpha = image.getColorModel().hasAlpha();
        buffer = imageToByteBuffer(image, null);
        glTextureId = glGenTextures();
        if (logCalls) logCall(glTextureId, "glGenTextures");
        state = State.ALLOCATED;
    }

    protected boolean flipVertical() {
        return true;
    }

    protected boolean forceAlpha() {
        return false;
    }

    protected boolean edging() {
        return false;
    }

    public void deallocate() {
        if (state != State.NEW) {
            glDeleteTextures(glTextureId);
            if (logCalls) logCall("glDeleteTextures", (long)glTextureId);
            glTextureId = 0;
            state = State.NEW;
            buffer = null;
            alpha = false;
            width = -1;
            height = -1;
            texWidth = -1;
            texHeight = -1;
        }
    }

    public Material asMaterial(final TextureMapping mapping) {
        return new Material() {
            public boolean isStatic() {
                return true;
            }

            public void apply(long nowMS, GeometryBuffer buffer) {
                buffer.applyBrush(Brush.TEXTURED);
                buffer.useTexture(AbstractTexture.this, mapping);
            }
        };
    }

    /**
     * The colour model including alpha for the GL image
     */
    private static final ColorModel glAlphaColorModel =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    new int[]{ 8, 8, 8, 8 },
                                    true,
                                    false,
                                    ComponentColorModel.TRANSLUCENT,
                                    DataBuffer.TYPE_BYTE);

    /**
     * The colour model for the GL image
     */
    private static final ColorModel glColorModel =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    new int[]{ 8, 8, 8, 0 },
                                    false,
                                    false,
                                    ComponentColorModel.OPAQUE,
                                    DataBuffer.TYPE_BYTE);

    private ByteBuffer imageToByteBuffer(BufferedImage image, int[] transparent) {

        ByteBuffer imageBuffer = null;
        WritableRaster raster;
        BufferedImage texImage;

        texWidth = 2;
        texHeight = 2;
        width = image.getWidth();
        height = image.getHeight();

        // find the closest power of 2 for the width and height
        // of the produced texture

        while (texWidth < width) {
            texWidth *= 2;
        }
        while (texHeight < height) {
            texHeight *= 2;
        }


        // create a raster that can be used by OpenGL as a source
        // for a texture
        boolean useAlpha = image.getColorModel().hasAlpha() || forceAlpha();

        if (useAlpha) {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
            texImage = new BufferedImage(glAlphaColorModel, raster, false, new Hashtable());
        } else {
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 3, null);
            texImage = new BufferedImage(glColorModel, raster, false, new Hashtable());
        }

        // copy the source image into the produced image
        Graphics2D g = (Graphics2D)texImage.getGraphics();

        // only need to blank the image for mac compatibility if we're using alpha
        if (useAlpha) {
            g.setColor(new Color(0f, 0f, 0f, 0f));
            g.fillRect(0, 0, texWidth, texHeight);
        }

        boolean edging = edging();
        double scaleX = edging ? 1.0 : ((double)texWidth) / width;
        double scaleY = edging ? 1.0 : ((double)texHeight) / height;

        if (flipVertical()) {
            g.scale(scaleX, -scaleY);
            g.drawImage(image, 0, -height, null);
        } else {
            g.scale(scaleX, scaleY);
            g.drawImage(image, 0, 0, null);
        }

        if (edging) {
            if (height < texHeight - 1) {
                copyArea(texImage, 0, 0, width, 1, 0, texHeight - 1);
                copyArea(texImage, 0, height - 1, width, 1, 0, 1);
            }
            if (width < texWidth - 1) {
                copyArea(texImage, 0, 0, 1, height, texWidth - 1, 0);
                copyArea(texImage, width - 1, 0, 1, height, 1, 0);
            }
        }

        // build a byte buffer from the temporary image
        // that be used by OpenGL to produce a texture.
//        new OutputGraph("texture", 1280, 0, 0, 1).addImage(null, texImage, null, -texWidth / 8, -texHeight / 8, 0.25);
        byte[] data = ((DataBufferByte)texImage.getRaster().getDataBuffer()).getData();

        if (transparent != null) {
            for (int i = 0; i < data.length; i += 4) {
                boolean match = true;
                for (int c = 0; c < 3; c++) {
                    int value = data[i + c] < 0 ? 256 + data[i + c] : data[i + c];
                    if (value != transparent[c]) {
                        match = false;
                    }
                }

                if (match) {
                    data[i + 3] = 0;
                }
            }
        }

        imageBuffer = ByteBuffer.allocateDirect(data.length);
        imageBuffer.order(ByteOrder.nativeOrder());
        imageBuffer.put(data, 0, data.length);
        imageBuffer.flip();
        g.dispose();

        return imageBuffer;
    }

    private static void copyArea(BufferedImage image, int x, int y, int width, int height, int dx, int dy) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.drawImage(image.getSubimage(x, y, width, height), x + dx, y + dy, null);
    }

    private static class GLConstants {

        private static final int MAX_TEXTURE_DIMENSION;

        static {
            IntBuffer temp = BufferUtils.createIntBuffer(16);
            glGetInteger(GL_MAX_TEXTURE_SIZE, temp);
            MAX_TEXTURE_DIMENSION = temp.get(0);
        }
    }
}
