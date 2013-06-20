package net.venaglia.gloo.demo;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.primitives.QuadSequence;
import net.venaglia.gloo.physical.texture.mapping.MatrixMapping;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.util.debug.OutputGraph;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;

import static net.venaglia.gloo.util.CallLogger.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

import javax.imageio.ImageIO;

/**
 * User: ed
 * Date: 3/14/13
 * Time: 2:18 PM
 */
public class TextureDemo extends AbstractDemo {

    private String vertexShaderSource =
            "void main() {\n" +
//            "  gl_Normal = gl_NormalMatrix * gl_Normal;\n" +
            "  gl_Position = ftransform();\n" +
            "  gl_FrontColor = gl_Color;\n" +
            "  gl_BackColor = gl_Color;\n" +
            "  gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "}";
    private String fragmentShaderSource =
            "uniform sampler2D texture1;\n" +
            "\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(texture1, gl_TexCoord[0].st);\n" +
            "  gl_FragColor = texture2D(texture1, gl_TexCoord[0].st) * 0.75 + vec4(gl_TexCoord[0].s,0,gl_TexCoord[0].t,1) * 0.25;\n" +
//            "  gl_FragColor = vec4(gl_TexCoord[0].s,0,gl_TexCoord[0].t,1);\n" +
            "}";

    private int glTextureId;
    private int glProgramId;
    private int glVertexShaderId;
    private int glFragmentShaderId;

    @Override
    protected String getTitle() {
        return "texture demo";
    }

    @Override
    protected double getCameraDistance() {
        return 8;
    }

    @Override
    protected BoundingVolume<?> getRenderingBounds() {
        return new BoundingSphere(Point.ORIGIN, 4);
    }

    private QuadSequence quad;
    private MatrixMapping mapping;

    @Override
    protected void init() {
        view.setDefaultBrush(Brush.TEXTURED);
        glTextureId = glGenTextures();
        if (logCalls) logCall(glTextureId, "glGenTextures");
        assert noError();
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("images/profile-image-square.png");
        BufferedImage texture = null;
        try {
            texture = ImageIO.read(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        new OutputGraph("texture", 256, 0, 0, 1).addImage(null, texture, "profile-image-square.png", -128, -128);
        int srcFormat = texture.getColorModel().hasAlpha() ? GL_RGBA : GL_RGB;
        int intFormat = GL_RGBA8;
        int width = texture.getWidth();
        int height = texture.getHeight();
        glActiveTexture(GL_TEXTURE0);
        if (logCalls) logCall("glActiveTexture", GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, glTextureId);
        if (logCalls) logCall("glBindTexture", GL_TEXTURE_2D, (long)glTextureId);
        glTexImage2D(GL_TEXTURE_2D,
                     0,
                     intFormat,
                     get2Fold(width),
                     get2Fold(height),
                     0,
                     srcFormat,
                     GL_UNSIGNED_BYTE,
                     imageToByteBuffer(texture, true, false, true, null));
        if (logCalls) logCall("glTexImage2D",
                              GL_TEXTURE_2D,
                              0,
                              intFormat,
                              get2Fold(width),
                              get2Fold(height),
                              0,
                              srcFormat,
                              GL_UNSIGNED_BYTE,
                              imageToByteBuffer(texture, true, false, true, null));
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        if (logCalls) logCall("glTexParameteri", GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        if (logCalls) logCall("glTexParameteri", GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glProgramId = glCreateProgram();
        if (logCalls) logCall(glProgramId, "glCreateProgram");
        glVertexShaderId = glCreateShader(GL_VERTEX_SHADER);
        if (logCalls) logCall(glVertexShaderId, "glCreateShader", GL_VERTEX_SHADER);
        glShaderSource(glVertexShaderId, vertexShaderSource);
        if (logCalls) logCall("glShaderSource", (long)glVertexShaderId, vertexShaderSource);
        glCompileShader(glVertexShaderId);
        if (logCalls) logCall("glCompileShader", (long)glVertexShaderId);
        {
            int compileStatus = glGetShaderi(glVertexShaderId, GL_COMPILE_STATUS);
            if (logCalls) logCall(compileStatus, "glGetShaderi", (long)glVertexShaderId, GL_COMPILE_STATUS);
            assert compileStatus != GL_FALSE;
        }
        glAttachShader(glProgramId, glVertexShaderId);
        if (logCalls) logCall("glAttachShader", (long)glProgramId, (long)glVertexShaderId);
        glFragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
        if (logCalls) logCall(glFragmentShaderId, "glCreateShader", GL_FRAGMENT_SHADER);
        glShaderSource(glFragmentShaderId, fragmentShaderSource);
        if (logCalls) logCall("glShaderSource", (long)glFragmentShaderId, fragmentShaderSource);
        glCompileShader(glFragmentShaderId);
        if (logCalls) logCall("glCompileShader", (long)glFragmentShaderId);
        {
            int compileStatus = glGetShaderi(glFragmentShaderId, GL_COMPILE_STATUS);
            if (logCalls) logCall(compileStatus, "glGetShaderi", (long)glFragmentShaderId, GL_COMPILE_STATUS);
            assert compileStatus != GL_FALSE;
        }
        glAttachShader(glProgramId, glFragmentShaderId);
        if (logCalls) logCall("glAttachShader", (long)glProgramId, (long)glFragmentShaderId);
        glLinkProgram(glProgramId);
        if (logCalls) logCall("glLinkProgram", (long)glProgramId);
        glValidateProgram(glProgramId);
        if (logCalls) logCall("glValidateProgram", (long)glProgramId);
        glUseProgram(glProgramId);
        if (logCalls) logCall("glUseProgram", (long)glProgramId);
        int loc = glGetUniformLocation(glProgramId, "texture1");
        if (logCalls) logCall(loc, "glGetUniformLocation", (long)glProgramId, "texture1");
        glUniform1i(loc, GL_TEXTURE0);
        if (logCalls) logCall("glUniform1i", (long)loc, GL_TEXTURE0);

        quad = new QuadSequence(new Point(-1, -1, 0), new Point(1, -1, 0), new Point(1, 1, 0), new Point(-1, 1, 0));
        quad.setMaterial(Material.INHERIT);
    }

    private boolean noError() {
        int err = glGetError();
        String type;
        switch (err) {
            case GL_NO_ERROR:
                return true;
            case GL_INVALID_ENUM:
                type = "GL_INVALID_ENUM";
                break;
            case GL_INVALID_VALUE:
                type = "GL_INVALID_VALUE";
                break;
            case GL_INVALID_OPERATION:
                type = "GL_INVALID_OPERATION";
                break;
            case GL_STACK_OVERFLOW:
                type = "GL_STACK_OVERFLOW";
                break;
            case GL_STACK_UNDERFLOW:
                type = "GL_STACK_UNDERFLOW";
                break;
            case GL_OUT_OF_MEMORY:
                type = "GL_OUT_OF_MEMORY";
                break;
            default:
                type = "GL_ERROR_" + err;
        }
        StackTraceElement stackTraceElement = new Exception().getStackTrace()[1];
        System.err.println("OpenGL error: " + type + " @ " + stackTraceElement);
        return false;
    }

    @Override
    protected void project(long nowMS, ProjectionBuffer buffer) {
//        glEnable(GL_TEXTURE_2D);
//        if (logCalls) logCall("glEnable", GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, glTextureId);
        if (logCalls) logCall("glBindTexture", GL_TEXTURE_2D, (long)glTextureId);
        glUseProgram(glProgramId);
        if (logCalls) logCall("glUseProgram", (long)glProgramId);
        int loc = glGetUniformLocation(glProgramId, "texture1");
        if (logCalls) logCall(loc, "glGetUniformLocation", (long)glProgramId, "texture1");
        glUniform1i(loc, 0);
        if (logCalls) logCall("glUniform1i", (long)loc, 0);
//        glActiveTexture(GL_TEXTURE0);
//        if (logCalls) logCall("glUniform1i", GL_TEXTURE0);

//        quad.project(nowMS, buffer);
        glBegin(GL_QUADS);
        if (logCalls) logCall("glBegin", GL_QUADS);
        glTexCoord2f(0, 0);
        if (logCalls) logCall("glTexCoord2f", 0.0f, 0.0f);
        glVertex3d(-1, -1, 0);
        if (logCalls) logCall("glVertex3d", -1.0, -1.0, 0.0);
        glTexCoord2f(1, 0);
        if (logCalls) logCall("glTexCoord2f", 1.0f, 0.0f);
        glVertex3d(1, -1, 0);
        if (logCalls) logCall("glVertex3d", 1.0, -1.0, 0.0);
        glTexCoord2f(1, 1);
        if (logCalls) logCall("glTexCoord2f", 1.0f, 1.0f);
        glVertex3d(1, 1, 0);
        if (logCalls) logCall("glVertex3d", 1.0, 1.0, 0.0);
        glTexCoord2f(0, 1);
        if (logCalls) logCall("glTexCoord2f", 0.0f, 1.0f);
        glVertex3d(-1, 1, 0);
        if (logCalls) logCall("glVertex3d", -1.0, 1.0, 0.0);
        glEnd();
        if (logCalls) logCall("glEnd");
    }

    @Override
    public void afterFrame(long nowMS) {
        if (logCalls) {
            System.exit(0);
        }
    }

    /**
     * Get the closest greater power of 2 to the fold number
     *
     * @param fold The target number
     * @return The power of 2
     */
    public static int get2Fold(int fold) {
        int ret = 2;
        while (ret < fold) {
            ret *= 2;
        }
        return ret;
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

    public ByteBuffer imageToByteBuffer(BufferedImage image, boolean flipped, boolean forceAlpha, boolean edging, int[] transparent) {
        ByteBuffer imageBuffer = null;
        WritableRaster raster;
        BufferedImage texImage;

        int texWidth = 2;
        int texHeight = 2;

        // find the closest power of 2 for the width and height
        // of the produced texture

        while (texWidth < image.getWidth()) {
            texWidth *= 2;
        }
        while (texHeight < image.getHeight()) {
            texHeight *= 2;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        int depth;

        // create a raster that can be used by OpenGL as a source
        // for a texture
        boolean useAlpha = image.getColorModel().hasAlpha() || forceAlpha;

        if (useAlpha) {
            depth = 32;
            raster = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, texWidth, texHeight, 4, null);
            texImage = new BufferedImage(glAlphaColorModel, raster, false, new Hashtable());
        } else {
            depth = 24;
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

        if (flipped) {
            g.scale(1, -1);
            g.drawImage(image, 0, -height, null);
        } else {
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

    private void copyArea(BufferedImage image, int x, int y, int width, int height, int dx, int dy) {
        Graphics2D g = (Graphics2D)image.getGraphics();
        g.drawImage(image.getSubimage(x, y, width, height), x + dx, y + dy, null);
    }

    public static void main(String[] args) {
        new TextureDemo().start();
    }
}
