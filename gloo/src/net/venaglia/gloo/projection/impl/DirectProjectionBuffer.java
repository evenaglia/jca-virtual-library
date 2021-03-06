package net.venaglia.gloo.projection.impl;

import static net.venaglia.gloo.util.CallLogger.*;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.AlphaRule;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.SelectObserver;
import net.venaglia.gloo.projection.shaders.ShaderProgram;
import net.venaglia.common.util.ChangeCounter;
import net.venaglia.gloo.util.matrix.Matrix_4x4;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.glu.Project;

import static net.venaglia.gloo.util.CallLogger.logCall;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.util.glu.GLU.*;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Stack;

/**
 * User: ed
 * Date: 8/5/12
 * Time: 11:24 AM
 */
public class DirectProjectionBuffer extends DelegatingGeometryBuffer implements ProjectionBuffer {

    protected DirectGeometryBuffer.ActiveBrush activeBrush = new DirectGeometryBuffer.ActiveBrush() {

        private AlphaRule.Mode alphaBlendingMode = null;

        @Override
        public void setLighting(boolean lighting) {
            if (this.lighting != lighting) {
                enableOrDisable(lighting, GL_LIGHTING);
                this.lighting = lighting;
//                if (lighting) {
//                    glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL, GL_SEPARATE_SPECULAR_COLOR);
//                    if (logCalls) logCall("glLightModeli", GL_LIGHT_MODEL_COLOR_CONTROL, GL_SEPARATE_SPECULAR_COLOR);
//                }
            }
        }

        @Override
        public void setTexturing(boolean texturing) {
            if (this.texturing != texturing) {
                enableOrDisable(texturing, GL_TEXTURE_2D);
                this.texturing = texturing;
                applyShaderImpl(texturing, alphaRule != null);
                if (!texturing) {
                    alphaBlendingMode = null;
                }
            }
        }

        @Override
        public void setAlphaRule(AlphaRule alphaRule) {
            if (!eq(this.alphaRule, alphaRule)) {
                if (alphaRule == null) {
                    if (this.alphaBlendingMode == AlphaRule.Mode.ALPHA_TRANSPARENCY) {
                        enableOrDisable(false, GL_BLEND);
                    } else if (this.alphaBlendingMode == AlphaRule.Mode.THRESHOLD_TRANSPARENCY) {
                        enableOrDisable(false, GL_ALPHA_TEST);
                    }
                } else {
                    AlphaRule.Mode transparencyMode = alphaRule.getTransparencyMode();
                    if (transparencyMode == AlphaRule.Mode.THRESHOLD_TRANSPARENCY) {
                        glAlphaFunc(alphaRule.getCompare().getGlCode(), alphaRule.getThresholdValue());
                        if (logCalls) logCall("glAlphaFunc", alphaRule.getCompare().getGlCode(), alphaRule.getThresholdValue());
                        enableOrDisable(true, GL_ALPHA_TEST);
                        this.alphaBlendingMode = AlphaRule.Mode.THRESHOLD_TRANSPARENCY;
                    } else if (transparencyMode == AlphaRule.Mode.ALPHA_TRANSPARENCY) {
                        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
                        if (logCalls) logCall("glTexEnvi", GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
//                        glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_ALPHA, );
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        if (logCalls) logCall("glBlendFunc", GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        enableOrDisable(true, GL_BLEND);
                        this.alphaBlendingMode = AlphaRule.Mode.ALPHA_TRANSPARENCY;
                    }
                }
                this.alphaRule = alphaRule;
                applyShaderImpl(texturing, alphaRule != null);
            }
        }

        private boolean eq(AlphaRule a, AlphaRule b) {
            return a == null ? b == null : a.equals(b);
        }

        private void applyShaderImpl(boolean texturing, boolean blending) {
            if (activeShader != ShaderProgram.DEFAULT_SHADER) {
                if (texturing || blending) {
                    applyShader(activeShader);
                } else {
                    applyShader(ShaderProgram.DEFAULT_SHADER);
                }
            }
            if (normal.activeTextureId != 0) {
                if (texturing) {
                    normal.bindTextureImpl(-1, normal.activeTextureId);
                } else {
                    normal.unbindTextureImpl();
                }
            }
        }
    };
    protected DirectGeometryBuffer normal = new DirectGeometryBuffer() {
        {
            this.activeBrush = DirectProjectionBuffer.this.activeBrush;
        }

        @Override
        public Point whereIs(Point point) {
            return DirectProjectionBuffer.this.whereIs(point);
        }

        @Override
        public Rectangle2D whereIs(BoundingBox bounds) {
            return DirectProjectionBuffer.this.whereIs(bounds);
        }

        public double viewingAngle(BoundingBox bounds, Point observer) {
            return DirectProjectionBuffer.this.viewingAngle(bounds, observer);
        }
    };

    protected DisabledGeometryBuffer fail = new DisabledGeometryBuffer() {
        @Override
        protected void fail() {
            throw new IllegalStateException();
        }
    };

    protected int cameraId = Integer.MIN_VALUE;
    protected int cameraModCount = Integer.MIN_VALUE;
    protected int cameraChangeCount = 0;
    protected float cameraViewAngle;
    protected float cameraAspect;
    protected float cameraNearClippingDistance;
    protected float cameraFarClippingDistance;
    protected float[] cameraLookAt = new float[9];
    protected ChangeCounter projectionMatrixChangeCounter = new ChangeCounter();

    protected ShaderProgram activeShader = ShaderProgram.DEFAULT_SHADER;
    protected Stack<Brush> brushStack = normal.brushStack;
    protected Stack<ShaderProgram> shaderStack = new Stack<ShaderProgram>();
    protected GlLightManager glLightManager = new GlLightManager();

    // for handling mouse targets and overlays
    protected Mode mode = Mode.NORMAL;
    protected IntBuffer viewport;
    protected FloatBuffer projectionMatrix;
    protected ChangeCounter modelMatrixChangeCounter = new ChangeCounter();
    protected FloatBuffer modelViewMatrix;
    protected IntBuffer targetHits;
    protected DelegatingGeometryBuffer targetGeometryBuffer;
    protected DelegatingGeometryBuffer overlayGeometryBuffer;
    protected Matrix_4x4 viewMatrix = new Matrix_4x4();
    protected DoubleView whereIsView = new DoubleView();
    protected ScreenExtentsView whereIsScreenExtentsView = new ScreenExtentsView();

    public DirectProjectionBuffer() {
        super(null);
        this.delegate = normal;
    }

    public Mode getMode() {
        return mode;
    }

    public void useCamera(Camera camera) {
        endSpecialMode();
        if (camera.getId() != cameraId || camera.getModCount() != cameraModCount) {
            DisplayMode displayMode = Display.getDisplayMode();
            cameraViewAngle = (float)camera.getViewAngle();
            cameraAspect = ((float)displayMode.getWidth()) / ((float)displayMode.getHeight());
            cameraId = camera.getId();
            cameraModCount = camera.getModCount();
            Point eye = camera.getPosition();
            Point at = eye.translate(camera.getDirection());
            Vector up = camera.getUp().normalize();
            cameraLookAt[0] = (float)eye.x;
            cameraLookAt[1] = (float)eye.y;
            cameraLookAt[2] = (float)eye.z;
            cameraLookAt[3] = (float)at.x;
            cameraLookAt[4] = (float)at.y;
            cameraLookAt[5] = (float)at.z;
            cameraLookAt[6] = (float)up.i;
            cameraLookAt[7] = (float)up.j;
            cameraLookAt[8] = (float)up.k;
            cameraNearClippingDistance = camera.getNearClippingDistance();
            cameraFarClippingDistance = camera.getFarClippingDistance();

            glMatrixMode(GL_PROJECTION);
            if (logCalls) logCall("glMatrixMode", GL_PROJECTION);
            glLoadIdentity();
            if (logCalls) logCall("glLoadIdentity");
            projectCameraMatrix(camera);
            gluLookAt(cameraLookAt[0], cameraLookAt[1], cameraLookAt[2],
                      cameraLookAt[3], cameraLookAt[4], cameraLookAt[5],
                      cameraLookAt[6], cameraLookAt[7], cameraLookAt[8]);
            if (logCalls) logCall("gluLookAt",
                                  cameraLookAt[0], cameraLookAt[1], cameraLookAt[2],
                                  cameraLookAt[3], cameraLookAt[4], cameraLookAt[5],
                                  cameraLookAt[6], cameraLookAt[7], cameraLookAt[8]);
            glMatrixMode(GL_MODELVIEW);
            if (logCalls) logCall("glMatrixMode", GL_MODELVIEW);
            glLoadIdentity();
            if (logCalls) logCall("glLoadIdentity");
            cameraChangeCount++;
            projectionMatrixChangeCounter.increment();
            modelMatrixChangeCounter.increment();
        }
    }

    public Point getCameraViewPoint() {
        return new Point(cameraLookAt[0], cameraLookAt[1], cameraLookAt[2]);
    }

    public float getCameraFOV() {
        return cameraViewAngle;
    }

    private void initSelectVars() {
        viewport = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        projectionMatrix = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        modelViewMatrix = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        targetHits = ByteBuffer.allocateDirect(512 * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
        targetGeometryBuffer = new TargetProjectionBuffer(fail);
        overlayGeometryBuffer = new OverlayGeometryBuffer(fail);
    }

    public GeometryBuffer beginCameraSelect(Camera camera, float x, float y) {
        endSpecialMode();
        if (targetGeometryBuffer == null) initSelectVars();
        glGetInteger(GL_VIEWPORT, viewport);
        if (logCalls) logCall(viewport, "glGetInteger", GL_VIEWPORT);
        targetHits.clear();
        glSelectBuffer(targetHits);
        if (logCalls) logCall("glSelectBuffer", targetHits);
        glRenderMode(GL_SELECT);
        if (logCalls) logCall("glRenderMode", GL_SELECT);
        glInitNames();
        if (logCalls) logCall("glInitNames");
        glPushName(0);
        if (logCalls) logCall("glPushName", 0);
        useCamera(camera);
        glMatrixMode(GL_PROJECTION);
        if (logCalls) logCall("glMatrixMode", GL_PROJECTION);
        glPushMatrix();
        if (logCalls) logCall("glPushMatrix");
        glLoadIdentity();
        if (logCalls) logCall("glLoadIdentity");
        Project.gluPickMatrix(x, y, 1.0f, 1.0f, viewport);
        if (logCalls) logCall("gluPickMatrix", x, y, 1.0f, 1.0f, viewport);
        projectCameraMatrix(camera);
        gluLookAt(cameraLookAt[0], cameraLookAt[1], cameraLookAt[2],
                  cameraLookAt[3], cameraLookAt[4], cameraLookAt[5],
                  cameraLookAt[6], cameraLookAt[7], cameraLookAt[8]);
        if (logCalls) logCall("gluLookAt",
                              cameraLookAt[0], cameraLookAt[1], cameraLookAt[2],
                              cameraLookAt[3], cameraLookAt[4], cameraLookAt[5],
                              cameraLookAt[6], cameraLookAt[7], cameraLookAt[8]);
        glMatrixMode(GL_MODELVIEW);
        if (logCalls) logCall("glMatrixMode", GL_MODELVIEW);
        mode = Mode.CAMERA_SELECT;

        projectionMatrixChangeCounter.push();
        projectionMatrixChangeCounter.increment();
        modelMatrixChangeCounter.increment();

        targetGeometryBuffer.delegate = normal;
        delegate = fail;
        return targetGeometryBuffer;
    }

    private void projectCameraMatrix(Camera camera) {
        if (camera.isOrthogonal()) {
            Vector direction = camera.getDirection();
            boolean flipVertical = direction.i < 0 ^ direction.j < 0 ^ direction.k < 0;
            double height = camera.getUp().l;
            double width = height * Display.getWidth() / Display.getHeight();
            if (flipVertical) {
                height = -height;
            }
            glOrtho(-width, width, height, -height,
                    cameraNearClippingDistance, cameraFarClippingDistance);
            if (logCalls) logCall("glOrtho",
                                  -width, width, height, -height,
                                  cameraNearClippingDistance, cameraFarClippingDistance);
        } else {
            gluPerspective(cameraViewAngle, cameraAspect,
                           cameraNearClippingDistance, cameraFarClippingDistance);
            if (logCalls) logCall("gluPerspective",
                                  cameraViewAngle, cameraAspect,
                                  cameraNearClippingDistance, cameraFarClippingDistance);
        }
    }

    protected void endSpecialMode() {
        switch (mode) {
            case CAMERA_SELECT:
                endCameraSelect(null);
                break;
            case OVERLAY_2D:
                endOverlay();
                break;
        }
    }

    public void endCameraSelect(SelectObserver selectObserver) {
        if (mode == Mode.CAMERA_SELECT) {
            endCameraSelectImpl(selectObserver);
        }
    }

    protected void endCameraSelectImpl(SelectObserver selectObserver) {
        glMatrixMode(GL_PROJECTION);
        if (logCalls) logCall("glMatrixMode", GL_PROJECTION);
        glPopMatrix();
        if (logCalls) logCall("glPopMatrix");
        glMatrixMode(GL_MODELVIEW);
        if (logCalls) logCall("glMatrixMode", GL_MODELVIEW);
        int hits = glRenderMode(GL_RENDER);
        if (logCalls) logCall(hits, "glRenderMode", GL_RENDER);
        mode = Mode.NORMAL;
        targetGeometryBuffer.delegate = fail;
        delegate = normal;
        if (hits > 0 && selectObserver != null) {
            for (int i = 0, j = 3, k = 1; i < hits; i++, j += 4, k += 4) {
                selectObserver.selected(targetHits.get(j), targetHits.get(k));
            }
        }
        projectionMatrixChangeCounter.pop();
    }

    public GeometryBuffer beginOverlay() {
        endSpecialMode();
        if (overlayGeometryBuffer == null) initSelectVars();
        loadViewMatrix();
        glMatrixMode(GL_PROJECTION);
        if (logCalls) logCall("glMatrixMode", GL_PROJECTION);
        glPushMatrix();
        if (logCalls) logCall("glPushMatrix");
        glLoadIdentity();
        if (logCalls) logCall("glLoadIdentity");
        glOrtho(0,Display.getWidth(),0,Display.getHeight(),0,1);
        if (logCalls) logCall("glOrtho", 0, (long)Display.getWidth(), 0, (long)Display.getHeight(), 0, 1L);
        glMatrixMode(GL_MODELVIEW);
        if (logCalls) logCall("glMatrixMode", GL_MODELVIEW);
        glPushMatrix();
        if (logCalls) logCall("glPushMatrix");
        glLoadIdentity();
        if (logCalls) logCall("glLoadIdentity");
        mode = Mode.OVERLAY_2D;
        delegate = fail;
        overlayGeometryBuffer.delegate = normal;
        projectionMatrixChangeCounter.push();
        projectionMatrixChangeCounter.increment();
        modelMatrixChangeCounter.push();
        modelMatrixChangeCounter.increment();
        return overlayGeometryBuffer;
    }

    public void endOverlay() {
        if (mode == Mode.OVERLAY_2D) {
            glPopMatrix();
            if (logCalls) logCall("glPopMatrix");
            glMatrixMode(GL_PROJECTION);
            if (logCalls) logCall("glMatrixMode", GL_PROJECTION);
            glPopMatrix();
            if (logCalls) logCall("glPopMatrix");
            glMatrixMode(GL_MODELVIEW);
            if (logCalls) logCall("glMatrixMode", GL_MODELVIEW);
            mode = Mode.NORMAL;
            delegate = normal;
            overlayGeometryBuffer.delegate = fail;
            projectionMatrixChangeCounter.pop();
            modelMatrixChangeCounter.pop();
        }
    }

    public void loadName(int glName) {
        if (mode == Mode.CAMERA_SELECT) {
            glLoadName(glName);
            if (logCalls) logCall("glLoadName", (long)glName);
        }
    }

    public void useLights(Light[] lights) {
        if (mode != Mode.CAMERA_SELECT) {
            glLightManager.use(lights);
        }
    }

    public void useShader(ShaderProgram shaderProgram) {
        if (activeShader.getGlProgramId() != shaderProgram.getGlProgramId()) {
            if (activeBrush.isTexturing() || activeBrush.getAlphaRule() != null) {
                applyShader(shaderProgram);
            }
            activeShader = shaderProgram;
        }
    }

    private int activeShaderProgramId = ShaderProgram.DEFAULT_SHADER.getGlProgramId();

    private void applyShader(ShaderProgram shaderProgram) {
        int glProgramId = shaderProgram.getGlProgramId();
        if (glProgramId != activeShaderProgramId) {
            glUseProgram(glProgramId);
            if (logCalls) logCall("glUseProgram", (long)glProgramId);
            activeShaderProgramId = glProgramId;
            int unit = 0;
            for (String name : shaderProgram.getTextureNames()) {
                int loc = glGetUniformLocation(glProgramId, name);
                if (logCalls) logCall(loc, "glGetUniformLocation", (long)glProgramId, name);
                glUniform1i(loc, unit++);
                if (logCalls) logCall("glUniform1i", (long)loc, unit - 1);
            }
        }
    }

    public void pushShader() {
        shaderStack.push(activeShader);
    }

    public void popShader() {
        useShader(shaderStack.pop());
    }

    @Override
    public void popTransform() {
        super.popTransform();
        modelMatrixChangeCounter.increment();
    }

    @Override
    public void identity() {
        super.identity();
        modelMatrixChangeCounter.increment();
    }

    @Override
    public void rotate(Axis axis, double angle) {
        super.rotate(axis, angle);
        modelMatrixChangeCounter.increment();
    }

    @Override
    public void rotate(Vector axis, double angle) {
        super.rotate(axis, angle);
        modelMatrixChangeCounter.increment();
    }

    @Override
    public void translate(Vector magnitude) {
        super.translate(magnitude);
        modelMatrixChangeCounter.increment();
    }

    @Override
    public void scale(double magnitude) {
        super.scale(magnitude);
        modelMatrixChangeCounter.increment();
    }

    @Override
    public void scale(Vector magnitude) {
        super.scale(magnitude);
        modelMatrixChangeCounter.increment();
    }

    public Point whereIs(Point point) {
        if (targetGeometryBuffer == null) initSelectVars();
        loadViewMatrix();
        double[] result = viewMatrix.product(point.x, point.y, point.z, whereIsView);
        double x = result[0];
        double y = result[1];
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return null;
        }
        return new Point(x, y, 0);
    }

    @Override
    public Rectangle2D whereIs(BoundingBox bounds) {
        if (targetGeometryBuffer == null) initSelectVars();
        Matrix_4x4 viewMatrix = loadViewMatrix();
        whereIsScreenExtentsView.reset();
        viewMatrix.product(bounds.min(Axis.X), bounds.min(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.min(Axis.X), bounds.min(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.min(Axis.X), bounds.max(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.min(Axis.X), bounds.max(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.min(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.min(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.max(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.max(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        return whereIsScreenExtentsView.getLimits();
    }

    @Override
    public double viewingAngle(BoundingBox bounds, Point observer) {
        if (targetGeometryBuffer == null) initSelectVars();
        Matrix_4x4 viewMatrix = loadModelMatrix();
        whereIsScreenExtentsView.reset();
        viewMatrix.product(bounds.min(Axis.X), bounds.min(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.min(Axis.X), bounds.min(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.min(Axis.X), bounds.max(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.min(Axis.X), bounds.max(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.min(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.min(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.max(Axis.Y), bounds.min(Axis.Z), whereIsScreenExtentsView);
        viewMatrix.product(bounds.max(Axis.X), bounds.max(Axis.Y), bounds.max(Axis.Z), whereIsScreenExtentsView);
        return whereIsScreenExtentsView.getWidestAngle(viewMatrix.product(observer.x, observer.y, observer.z, Point.POINT_XFORM_VIEW));
    }

    protected Matrix_4x4 loadViewMatrix() {
        if (!projectionMatrixChangeCounter.isCurrent()) {
            glGetFloat(GL_PROJECTION_MATRIX, projectionMatrix);
            if (logCalls) logCall(logMatrix("result", projectionMatrix, 4, 4), "glGetFloat", GL_PROJECTION_MATRIX);
            projectionMatrixChangeCounter.setCurrent();
        }
        if (!modelMatrixChangeCounter.isCurrent()) {
            glGetFloat(GL_MODELVIEW_MATRIX, modelViewMatrix);
            if (logCalls) logCall(logMatrix("result", modelViewMatrix, 4, 4), "glGetFloat", GL_MODELVIEW_MATRIX);
            modelMatrixChangeCounter.setCurrent();
        }
        Matrix_4x4.product(
                projectionMatrix.get( 0), projectionMatrix.get( 4), projectionMatrix.get( 8), projectionMatrix.get(12),
                projectionMatrix.get( 1), projectionMatrix.get( 5), projectionMatrix.get( 9), projectionMatrix.get(13),
                projectionMatrix.get( 2), projectionMatrix.get( 6), projectionMatrix.get(10), projectionMatrix.get(14),
                projectionMatrix.get( 3), projectionMatrix.get( 7), projectionMatrix.get(11), projectionMatrix.get(15),
                modelViewMatrix.get( 0), modelViewMatrix.get( 4), modelViewMatrix.get( 8), modelViewMatrix.get(12),
                modelViewMatrix.get( 1), modelViewMatrix.get( 5), modelViewMatrix.get( 9), modelViewMatrix.get(13),
                modelViewMatrix.get( 2), modelViewMatrix.get( 6), modelViewMatrix.get(10), modelViewMatrix.get(14),
                modelViewMatrix.get( 3), modelViewMatrix.get( 7), modelViewMatrix.get(11), modelViewMatrix.get(15),
                viewMatrix);
//        Matrix_4x4.product(
//                projectionMatrix.get( 0), projectionMatrix.get( 1), projectionMatrix.get( 2), projectionMatrix.get( 3),
//                projectionMatrix.get( 4), projectionMatrix.get( 5), projectionMatrix.get( 6), projectionMatrix.get( 7),
//                projectionMatrix.get( 8), projectionMatrix.get( 9), projectionMatrix.get(10), projectionMatrix.get(11),
//                projectionMatrix.get(12), projectionMatrix.get(13), projectionMatrix.get(14), projectionMatrix.get(15),
//                modelViewMatrix.get( 0), modelViewMatrix.get( 1), modelViewMatrix.get( 2), modelViewMatrix.get( 3),
//                modelViewMatrix.get( 4), modelViewMatrix.get( 5), modelViewMatrix.get( 6), modelViewMatrix.get( 7),
//                modelViewMatrix.get( 8), modelViewMatrix.get( 9), modelViewMatrix.get(10), modelViewMatrix.get(11),
//                modelViewMatrix.get(12), modelViewMatrix.get(13), modelViewMatrix.get(14), modelViewMatrix.get(15),
//                viewMatrix);
        return viewMatrix;
    }

    protected Matrix_4x4 loadModelMatrix() {
        if (!modelMatrixChangeCounter.isCurrent()) {
            glGetFloat(GL_MODELVIEW_MATRIX, modelViewMatrix);
            if (logCalls) logCall(modelViewMatrix, "glGetFloat", GL_MODELVIEW_MATRIX);
            modelMatrixChangeCounter.setCurrent();
        }
        viewMatrix.load(
                modelViewMatrix.get( 0), modelViewMatrix.get( 4), modelViewMatrix.get( 8), modelViewMatrix.get(12),
                modelViewMatrix.get( 1), modelViewMatrix.get( 5), modelViewMatrix.get( 9), modelViewMatrix.get(13),
                modelViewMatrix.get( 2), modelViewMatrix.get( 6), modelViewMatrix.get(10), modelViewMatrix.get(14),
                modelViewMatrix.get( 3), modelViewMatrix.get( 7), modelViewMatrix.get(11), modelViewMatrix.get(15)
        );
        return viewMatrix;
    }

    public void resetAllStacks() {
        brushStack.clear();
        shaderStack.clear();
    }

    private static class DoubleView implements XForm.View<double[]> {

        private final double[] buffer = {0,0,0};

        private DoubleView() {
        }

        public double[] convert(double x, double y, double z, double w) {
            buffer[0] = x;
            buffer[1] = y;
            buffer[2] = z;
            return buffer;
        }
    }

    private static class ScreenExtentsView implements XForm.View<Void> {

        private static final int[][] CORNER_PAIR_PERMUTATIONS;

        static {
            CORNER_PAIR_PERMUTATIONS = new int[28][];
            int k = 0;
            for (int i = 0; i < 7; i++) {
                for (int j = i + 1; j < 8; j++) {
                    CORNER_PAIR_PERMUTATIONS[k++] = new int[]{ i, j, i ^ j | 8 };
                }
            }
            assert k == 28;
        }

        private double minX, minY, minZ;
        private double maxX, maxY, maxZ;

        public Void convert(double x, double y, double z, double w) {
            minX = Math.min(x, minX);
            maxX = Math.max(x, maxX);
            minY = Math.min(y, minY);
            maxY = Math.max(y, maxY);
            minZ = Math.min(z, minZ);
            maxZ = Math.max(z, maxZ);
            return null;
        }

        public ScreenExtentsView reset() {
            minX = Double.MAX_VALUE; minY = Double.MAX_VALUE; minZ = Double.MAX_VALUE;
            maxX = Double.MIN_VALUE; maxY = Double.MIN_VALUE; maxZ = Double.MIN_VALUE;
            return this;
        }

        public Rectangle2D getLimits() {
            if (Double.isNaN(minX) || Double.isNaN(minY) ||
                Double.isNaN(maxX) || Double.isNaN(maxY)) {
                return null;
            }
            int width = Display.getWidth() + 192;
            int height = Display.getHeight() + 192;
            int x1 = Math.max(-96, Math.min((int)Math.round(minX), width));
            int x2 = Math.max(-96, Math.min((int)Math.round(maxX), width));
            int y1 = Math.max(-96, Math.min((int)Math.round(minY), height));
            int y2 = Math.max(-96, Math.min((int)Math.round(maxY), height));
            return new Rectangle(x1, y1, x2 - x1, y2 - y1);
        }

        public double getBoundingBoxVolume() {
            if (Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) ||
                Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ)) {
                return Double.NaN;
            }
            return (maxX - minX) * (maxY - minY) * (maxZ - minZ);
        }

        public double getWidestAngle(Point observer) {
            if (Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) ||
                Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ)) {
                return Double.NaN;
            }

            if (observer.x >= minX && observer.x <= maxX &&
                observer.y >= minY && observer.y <= maxY &&
                observer.z >= minZ && observer.z <= maxZ) {
                return Math.PI;
            }

            double dx = maxX - minX;
            double dy = maxY - minY;
            double dz = maxZ - minZ;
            double[] d = {
                    Vector.computeDistance(observer.x - minX, observer.y - minY, observer.z - minZ), // 0
                    Vector.computeDistance(observer.x - minX, observer.y - minY, observer.z - maxZ), // 1
                    Vector.computeDistance(observer.x - minX, observer.y - maxY, observer.z - minZ), // 2
                    Vector.computeDistance(observer.x - minX, observer.y - maxY, observer.z - maxZ), // 3
                    Vector.computeDistance(observer.x - maxX, observer.y - minY, observer.z - minZ), // 4
                    Vector.computeDistance(observer.x - maxX, observer.y - minY, observer.z - maxZ), // 5
                    Vector.computeDistance(observer.x - maxX, observer.y - maxY, observer.z - minZ), // 6
                    Vector.computeDistance(observer.x - maxX, observer.y - maxY, observer.z - maxZ), // 7
                    0,                                      // ---
                    dz,                                     // --z
                    dy,                                     // -y-
                    Math.sqrt(dy * dy + dz * dz),           // -yz
                    dx,                                     // x--
                    Math.sqrt(dx * dx + dz * dz),           // x-z
                    Math.sqrt(dx * dx + dy * dy),           // xy-
                    Math.sqrt(dx * dx + dy * dy + dz * dz)  // xyz
            };

            double cosine = 1.0;
            for (int[] p : CORNER_PAIR_PERMUTATIONS) {
                cosine = Math.min(cosine, calculateAngleCosine(d[p[0]], d[p[1]], d[p[2]]));
            }
            return Math.cos(cosine);
        }

        private double calculateAngleCosine(double a, double b, double c) {
            return (a * a + b * b - c * c) / (2.0 * a * b);
        }
    }
}
