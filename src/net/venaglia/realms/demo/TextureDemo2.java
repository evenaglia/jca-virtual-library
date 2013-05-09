package net.venaglia.realms.demo;

import static net.venaglia.realms.common.util.CallLogger.*;

import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.primitives.QuadSequence;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.impl.TextureFactory;
import net.venaglia.realms.common.physical.texture.mapping.MatrixMapping;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.projection.shaders.ShaderProgram;
import net.venaglia.realms.common.projection.shaders.ShaderCompiler;
import net.venaglia.realms.common.util.matrix.Matrix_4x4;

/**
 * User: ed
 * Date: 3/18/13
 * Time: 9:47 PM
 */
public class TextureDemo2 extends AbstractDemo {

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
        return new BoundingSphere(net.venaglia.realms.common.physical.geom.Point.ORIGIN, 4);
    }

    private Texture texture;
    private ShaderProgram shader;
    private Shape<?> quad;
    private MatrixMapping mapping;

//    private int glProgramId;
//    private int glVertexShaderId;
//    private int glFragmentShaderId;

    @Override
    protected void init() {
        texture = new TextureFactory().loadURL(getClass().getResource("profile-image-square.png")).build();
        shader = new ShaderCompiler()
                .setVertexShaderSource(vertexShaderSource)
                .setFragmentShaderSource(fragmentShaderSource)
                .setTextureNames("texture1")
                .compile();
        quad = new QuadSequence(new Point(-1, -1, 0), new Point(1, -1, 0), new Point(1, 1, 0), new Point(-1, 1, 0));
        quad.setMaterial(Material.INHERIT);
        mapping = new MatrixMapping(new Matrix_4x4().load(.5, 0, 0,.5,
                                                           0,.5, 0,.5,
                                                           0, 0,.5,.5,
                                                           0, 0, 0, 1));

//        glProgramId = glCreateProgram();
//        glVertexShaderId = glCreateShader(GL_VERTEX_SHADER);
//        glFragmentShaderId = glCreateShader(GL_FRAGMENT_SHADER);
//        glShaderSource(glVertexShaderId, vertexShaderSource);
//        glCompileShader(glVertexShaderId);
//        assert glGetShaderi(glVertexShaderId, GL_COMPILE_STATUS) != GL_FALSE;
//        glShaderSource(glFragmentShaderId, fragmentShaderSource);
//        glCompileShader(glFragmentShaderId);
//        assert glGetShaderi(glFragmentShaderId, GL_COMPILE_STATUS) != GL_FALSE;
//        glAttachShader(glProgramId, glVertexShaderId);
//        glAttachShader(glProgramId, glFragmentShaderId);
//        glLinkProgram(glProgramId);
//        glValidateProgram(glProgramId);
//        glUseProgram(glProgramId);
//        int loc = glGetUniformLocation(glProgramId, "texture1");
//        glUniform1i(loc, GL_TEXTURE0);
    }

    @Override
    protected void project(long nowMS, ProjectionBuffer buffer) {
//        glEnable(GL_TEXTURE_2D);
//        glUseProgram(glProgramId);
//        int loc = glGetUniformLocation(glProgramId, "texture1");
//        glUniform1i(loc, 0);

        buffer.useTexture(texture, mapping);
        buffer.useShader(shader);
        quad.project(nowMS, buffer);
    }

    public static void main(String[] args) {
        new TextureDemo2().start();
    }

    @Override
    public void afterFrame(long nowMS) {
        if (logCalls) {
            System.exit(0);
        }
    }
}
