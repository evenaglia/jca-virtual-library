package net.venaglia.gloo.demo;

import static net.venaglia.gloo.util.CallLogger.logCalls;

import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.*;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.complex.GeodesicSphere;
import net.venaglia.gloo.physical.lights.DynamicPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.shaders.ShaderProgram;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.physical.texture.mapping.SphericalMapping;

import java.awt.Image;
import java.io.IOException;

/**
 * User: ed
 * Date: 3/8/13
 * Time: 5:19 PM
 */
public class MoonDemo extends AbstractDemo {

    private Texture surface;
    private GeodesicSphere sphere = new GeodesicSphere(24);
//    private GeodesicSphere sphere = new GeodesicSphere(4);
    private BoundingVolume<?> bounds = sphere.getBounds();
    private DisplayListBuffer moon = new DisplayListBuffer("moon");
    private ShaderProgram shader;

    public MoonDemo() {
        surface = new TextureFactory().loadClasspathResource("images/MoonMap_2500x1250.jpg").build();
    }

    protected String getTitle() {
        return "La Luna";
    }

    @Override
    protected Image loadAppIcon() {
        return null;
    }

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
            "  vec4 clr = texture2D(texture1, gl_TexCoord[0].st);\n" +
            "  gl_FragColor = vec4(clr.r * gl_Color.r, clr.g * gl_Color.g, clr.b * gl_Color.b, clr***.a * gl_Color.a);\n" +
//            "  gl_FragColor = texture2D(texture1, gl_TexCoord[0].st) * 0.75 + vec4(gl_TexCoord[0].s,0,gl_TexCoord[0].t,1) * 0.25;\n" +
//            "  gl_FragColor = vec4(gl_TexCoord[0].s,0,gl_TexCoord[0].t,1);\n" +
            "}";

    protected double getCameraDistance() {
        return 4;
    }

    protected BoundingVolume<?> getRenderingBounds() {
        return bounds;
    }

    @Override
    protected Light[] getLights(double cameraDistance) {
        return new Light[]{
                    new DynamicPointSourceLight(new Point(-2.1f, 0.0f, 1.5f).scale(4)),
                    new DynamicPointSourceLight(new Point(-2.1f, 0.0f, 1.5f).scale(4)),
                    new DynamicPointSourceLight(new Point(-2.1f, 0.0f, 1.5f).scale(4))
            };
    }

//    @Override
//    protected double getStepA() {
//        return 0.0;
//    }

//    @Override
//    protected double getStepB() {
//        return 0.0;
//    }

    @Override
    protected void init() {
//        shader = new ShaderCompiler()
//                .setVertexShaderSource(vertexShaderSource)
//                .setFragmentShaderSource(fragmentShaderSource)
//                .setTextureNames("texture1")
//                .compile();
//        view.setDefaultShader(shader);
        view.setDefaultShader(ShaderProgram.DEFAULT_SHADER);
        Color offWhite = new Color(1.0f, 0.9f, 0.9f);
        sphere.setMaterial(Material.INHERIT_TEXTURE);
//        sphere.setMaterial(Material.makeFrontShaded(offWhite));
//        sphere.setMaterial(Material.makeWireFrame(offWhite));
//        moon.record(new DisplayList.DisplayListRecorder() {
//            public void record(GeometryBuffer buffer) {
//                sphere.project(buffer);
//            }
//        });
        a = 0;
    }

//    @Override
//    public boolean beforeFrame(long nowMS) {
//        double a = this.a, b = this.b;
//        this.a = Math.PI;
//        this.b = 0.2;
//        boolean render = super.beforeFrame(nowMS);
//        this.a = a + this.a - Math.PI;
//        this.b = b + this.b - 0.2;
//        return render;
//    }

    boolean cameraPositionHasBeenSet = false;

    @Override
    protected void setCameraPosition(Point c) {
        if (!cameraPositionHasBeenSet) {
            super.setCameraPosition(c);
            cameraPositionHasBeenSet = true;
        }
        for (Light light : lights) {
            ((DynamicPointSourceLight)light).setSource(c);
        }
    }

    @Override
    public void afterFrame(long nowMS) {
        if (logCalls) {
            System.exit(0);
        }
    }

    @Override
    protected void project(long nowMS, ProjectionBuffer buffer) {
//        buffer.pushTransform();
//        buffer.rotate(a * (180.0 / Math.PI), Vector.Z);
        buffer.useTexture(surface, new SphericalMapping());
//        buffer.useTexture(surface, new MatrixMapping(new Matrix_4x4().load(1,0,0,0,
//                                                                           0,1,0,0,
//                                                                           0,0,1,0,
//                                                                           0,0,0,1
//                                                                           )));
//        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
//        moon.project(nowMS, buffer);
//        buffer.useShader(shader);
        sphere.project(nowMS, buffer);
        buffer.clearTexture();
//        buffer.popTransform();
    }

    public static void main(String[] args) throws IOException {
        new MoonDemo().start();
    }
}
