package net.venaglia.gloo.projection.impl;

import static net.venaglia.gloo.util.CallLogger.*;
import static net.venaglia.gloo.util.CallLogger.logCall;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.Coordinate;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureCoordinate;
import net.venaglia.gloo.physical.texture.TextureMapping;

import java.awt.geom.Rectangle2D;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 8/5/12
 * Time: 11:24 AM
 */
public class DirectGeometryBuffer implements GeometryBuffer {

    protected final AtomicInteger transformDepth = new AtomicInteger();
    protected final MyCoordinateListGeometryBuffer clgb = new MyCoordinateListGeometryBuffer();

    protected GeometrySequence begin = null;
    protected Brush activeBrush = new ActiveBrush();
    protected Stack<Brush> brushStack = new Stack<Brush>();
    protected TextureMapping mapping = null;
    protected float[] textureCoords = {0,0};
    protected int activeTextureId = 0;
    protected boolean textureSet;
    protected ArrayBufferBindings arrayBufferBindings = new ActiveArrayBufferBindings();

    public void applyBrush(Brush brush) {
        if (logCalls) logCall("applyBrush", brush);
        activeBrush.copyFrom(brush);
    }

    public void pushBrush() {
        brushStack.push(new Brush(activeBrush));
    }

    public void popBrush() {
        applyBrush(brushStack.pop());
    }

    public void useTexture(Texture texture, TextureMapping mapping) {
        int glTextureId = texture.getGlTextureId();
        if (activeTextureId == glTextureId) {
            this.mapping = mapping;
            return;
        }
        activeTextureId = glTextureId;
        textureSet = glTextureId != 0;
        if (!textureSet) {
            if (activeBrush.isTexturing()) {
                clearTexture();
            }
            return;
        }
        bindTextureImpl(GL_TEXTURE0, glTextureId);
        texture.load();
        mapping.newSequence();
        this.mapping = mapping;
    }

    protected void bindTextureImpl(int glTextueNum, int glTextureId) {
        if (glTextueNum >= GL_TEXTURE0) {
            glActiveTexture(glTextueNum);
            if (logCalls) logCall("glActiveTexture", glTextueNum);
        }
        glBindTexture(GL_TEXTURE_2D, glTextureId);
        if (logCalls) logCall("glBindTexture", GL_TEXTURE_2D, (long)glTextureId);
    }

    public void clearTexture() {
        if (textureSet && activeBrush.isTexturing()) {
            unbindTextureImpl();
            activeTextureId = 0;
            textureSet = false;
        }
    }

    protected void unbindTextureImpl() {
        glBindTexture(GL_TEXTURE_2D, 0);
        if (logCalls) logCall("glBindTexture", GL_TEXTURE_2D, 0L);
    }

    public void start(GeometrySequence seq) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        if (mapping != null) {
            mapping.newSequence();
        }
        begin = seq;
        glBegin(seq.getCode());
        if (logCalls) logCall("glBegin", seq.getCode());
    }

    public void end() {
        if (begin != null) {
            glEnd();
            if (logCalls) logCall("glEnd");
            begin = null;
        }
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        loadCoordinateList(coordinateList);
        if (logCalls) logCall("glDrawArrays", seq.getCode(), 0, (long)coordinateList.size());
        glDrawArrays(seq.getCode(), 0, coordinateList.size());
        unloadCoordinateList();
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, ShortBuffer order) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        loadCoordinateList(coordinateList);
        coordinatesImpl(seq, order);
        unloadCoordinateList();
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, IntBuffer order) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        loadCoordinateList(coordinateList);
        coordinatesImpl(seq, order);
        unloadCoordinateList();
    }

    @Override
    public void coordinates(CoordinateList coordinateList, Drawable drawable) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        loadCoordinateList(coordinateList);
        clgb.setEnabled(true);
        try {
            drawable.draw(clgb);
        } finally {
            clgb.setEnabled(false);
        }
        unloadCoordinateList();
    }

    protected void coordinatesImpl(GeometrySequence seq, ShortBuffer order) {
        glDrawElements(seq.getCode(), order);
        if (logCalls) logCall("glDrawElements", seq.getCode(), order);
    }

    protected void coordinatesImpl(GeometrySequence seq, IntBuffer order) {
        glDrawElements(seq.getCode(), order);
        if (logCalls) logCall("glDrawElements", seq.getCode(), order);
    }

    protected void loadCoordinateList(CoordinateList coordinateList) {
        arrayBufferBindings.apply(new ArrayBufferBindings(coordinateList));
    }

    protected void unloadCoordinateList() {
        arrayBufferBindings.apply(ArrayBufferBindings.ALL_DISABLED);
    }

    public void vertex(Point point) {
        if (textureSet && mapping != null && activeBrush.isTexturing()) {
            mapping.unwrap(point.x, point.y, point.z, textureCoords);
            glTexCoord2f(textureCoords[0], textureCoords[1]);
            if (logCalls) logCall("glTexCoord2f", textureCoords[0], textureCoords[1]);
        }
        glVertex3d(point.x, point.y, point.z);
        if (logCalls) logCall("glVertex3d", point.x, point.y, point.z);
    }

    public void vertex(double x, double y, double z) {
        if (textureSet && mapping != null && activeBrush.isTexturing()) {
            mapping.unwrap(x, y, z, textureCoords);
            glTexCoord2f(textureCoords[0], textureCoords[1]);
            if (logCalls) logCall("glTexCoord2f", textureCoords[0], textureCoords[1]);
        }
        glVertex3d(x, y, z);
        if (logCalls) logCall("glVertex3d", x, y, z);
    }

    public void normal(Vector normal) {
        if (activeBrush.isLighting()) {
            glNormal3d(normal.i, normal.j, normal.k);
            if (logCalls) logCall("glNormal3d", normal.i, normal.j, normal.k);
        }
    }

    public void normal(double i, double j, double k) {
        if (activeBrush.isLighting()) {
            glNormal3d(i, j, k);
            if (logCalls) logCall("glNormal3d", i, j, k);
        }
    }

    public void color(Color color) {
        if (activeBrush.isColor()) {
            glColor3f(color.r, color.g, color.b);
            if (logCalls) logCall("glColor3f", color.r, color.g, color.b);
        }
    }

    public void color(float r, float g, float b) {
        if (activeBrush.isColor()) {
            glColor3f(r, g, b);
            if (logCalls) logCall("glColor3f", r, g, b);
        }
    }

    public void colorAndAlpha(Color color) {
        if (activeBrush.isColor()) {
            glColor4f(color.r, color.g, color.b, color.a);
            if (logCalls) logCall("glColor4f", color.r, color.g, color.b, color.a);
        }
    }

    public void colorAndAlpha(float r, float g, float b, float a) {
        if (activeBrush.isColor()) {
            glColor4f(r, g, b, a);
            if (logCalls) logCall("glColor4f", r, g, b, a);
        }
    }

    public void coordinate(Coordinate coordinate) {
        coordinateImpl(coordinate);
    }

    public void coordinates(Iterable<Coordinate> coordinates) {
        for (Coordinate coordinate : coordinates) {
            coordinateImpl(coordinate);
        }
    }

    private void coordinateImpl(Coordinate coordinate) {
        Point vertex = coordinate.getVertex();
        Vector normal = coordinate.getNormal();
        Color color = coordinate.getColor();
        TextureCoordinate textureCoordinate = coordinate.getTextureCoordinate();
        if (normal != null && activeBrush.isLighting()) {
            glNormal3d(normal.i, normal.j, normal.k);
            if (logCalls) logCall("glNormal3d", normal.i, normal.j, normal.k);
        }
        if (color != null && activeBrush.isColor()) {
            glColor4f(color.r, color.g, color.b, color.a);
            if (logCalls) logCall("glColor4f", color.r, color.g, color.b, color.a);
        }
        if (textureSet && textureCoordinate != null && activeBrush.isTexturing()) {
            glTexCoord2f(textureCoordinate.s, textureCoordinate.t);
            if (logCalls) logCall("glTexCoord2f", textureCoordinate.s, textureCoordinate.t);
        }
        glVertex3d(vertex.x, vertex.y, vertex.z);
        if (logCalls) logCall("glVertex3d", vertex.x, vertex.y, vertex.z);
    }

    public void pushTransform() {
        transformDepth.incrementAndGet();
        glPushMatrix();
        if (logCalls) logCall("glPushMatrix");
    }

    public void popTransform() {
        int depth;
        do {
            depth = transformDepth.get();
            if (depth <= 0) {
                throw new IllegalStateException();
            }
        } while (!transformDepth.compareAndSet(depth, depth - 1));
        glPopMatrix();
        if (logCalls) logCall("glPopMatrix");
    }

    public void identity() {
        glLoadIdentity();
        if (logCalls) logCall("glLoadIdentity");
    }

    public void rotate(Axis axis, double angle) {
        double degrees = angle * (180.0 / Math.PI);
        rotateImpl(axis.vector(), degrees);
    }

    public void rotate(Vector axis, double angle) {
        double degrees = angle * (180.0 / Math.PI);
        rotateImpl(axis, degrees);
    }

    private void rotateImpl(Vector axis, double degrees) {
        glRotated(degrees, axis.i, axis.j, axis.k);
        if (logCalls) logCall("glRotated", degrees, axis.i, axis.j, axis.k);
    }

    public void translate(Vector magnitude) {
        glTranslated(magnitude.i, magnitude.j, magnitude.k);
        if (logCalls) logCall("glTranslated", magnitude.i, magnitude.j, magnitude.k);
    }

    public void scale(double magnitude) {
        glScaled(magnitude, magnitude, magnitude);
        if (logCalls) logCall("glScaled", magnitude, magnitude, magnitude);
    }

    public void scale(Vector magnitude) {
        glScaled(magnitude.i, magnitude.j, magnitude.k);
        if (logCalls) logCall("glScaled", magnitude.i, magnitude.j, magnitude.k);
    }

    public void callDisplayList(int glDisplayListId) {
        glCallList(glDisplayListId);
        if (logCalls) logCall("glCallList", (long)glDisplayListId);
    }

    public Point whereIs(Point point) {
        throw new UnsupportedOperationException("whereIs() cannot be invoked in this context");
    }

    public Rectangle2D whereIs(BoundingBox bounds) {
        throw new UnsupportedOperationException("whereIs() cannot be invoked in this context");
    }

    public double viewingAngle(BoundingBox bounds, Point observer) {
        throw new UnsupportedOperationException("viewingAngle() cannot be invoked in this context");
    }

    public boolean isScreen() {
        return true;
    }

    public boolean isTarget() {
        return false;
    }

    public boolean isOverlay() {
        return false;
    }

    public boolean isVirtual() {
        return false;
    }

    protected static class ActiveBrush extends Brush {

        protected ActiveBrush() {
            this(GL_DEFAULTS);
        }

        protected ActiveBrush(Brush defaults) {
            super(defaults);
        }

        @Override
        public void setLighting(boolean lighting) {
            if (this.lighting != lighting) {
                enableOrDisable(lighting, GL_LIGHTING);
                this.lighting = lighting;
            }
        }

        @Override
        public void setColor(boolean color) {
            if (this.color != color) {
                enableOrDisable(color, GL_COLOR_MATERIAL);
                this.color = color;
            }
        }

        @Override
        public void setTexturing(boolean texturing) {
            if (this.texturing != texturing) {
                enableOrDisable(texturing, GL_TEXTURE_2D);
                this.texturing = texturing;
            }
        }

        @Override
        public void setDepth(DepthMode depth) {
            if (this.depth  != depth) {
                if (depth != null) {
                    glDepthMask(true);
                    if (logCalls) logCall("glDepthMask", true);
                    if (this.depth != depth) {
                        glDepthFunc(depth.glCode);
                        if (logCalls) logCall("glDepthFunc", depth.glCode);
                        this.depth = depth;
                    }
                } else {
                    glDepthMask(false);
                    if (logCalls) logCall("glDepthMask", false);
                    glDepthFunc(GL_ALWAYS);
                    if (logCalls) logCall("glDepthFunc", GL_ALWAYS);
                    this.depth = depth;
                }
            }
        }

        @Override
        public void setPolygonFrontFace(PolygonMode polygonFrontFace) {
            if (this.polygonFrontFace != polygonFrontFace) {
                glPolygonMode(GL_FRONT, polygonFrontFace.glCode);
                if (logCalls) logCall("glPolygonMode", GL_FRONT, polygonFrontFace.glCode);
                this.polygonFrontFace = polygonFrontFace;
            }
        }

        @Override
        public void setPolygonBackFace(PolygonMode polygonBackFace) {
            if (this.polygonBackFace != polygonBackFace) {
                glPolygonMode(GL_BACK, polygonBackFace.glCode);
                if (logCalls) logCall("glPolygonMode", GL_BACK, polygonBackFace.glCode);
                this.polygonBackFace = polygonBackFace;
            }
        }

        @Override
        public void setCulling(PolygonSide culling) {
            if (this.culling != culling) {
                enableOrDisable(culling != null, GL_CULL_FACE);
                if (culling != null) {
                    glCullFace(culling.glCode);
                    if (logCalls) logCall("glCullFace", culling.glCode);
                }
                this.culling = culling;
            }
        }

        protected void enableOrDisable(boolean enable, int feature) {
            if (enable) {
                glEnable(feature);
                if (logCalls) logCall("glEnable", feature);
            } else {
                glDisable(feature);
                if (logCalls) logCall("glDisable", feature);
            }
        }

        @Override
        public Brush immutable() {
            throw new UnsupportedOperationException("The active brush cannot be made immutable");
        }
    }

    private static class ActiveArrayBufferBindings extends ArrayBufferBindings {

        @Override
        protected void doEnable(Type type) {
            glEnableClientState(type.getCode());
            if (logCalls) logCall("glEnableClientState", type.getCode());
        }

        @Override
        protected void doDisable(Type type) {
            glDisableClientState(type.getCode());
            if (logCalls) logCall("glDisableClientState", type.getCode());
        }

        @Override
        protected void doBind(Type type, ByteBuffer buffer, int offset, int stride) {
            buffer.position(offset);
            switch (type) {
                case VERTEX:
                    glVertexPointer(3, GL_DOUBLE, stride, buffer);
                    if (logCalls) logCall("glVertexPointer",
                                          3,
                                          stride,
                                          buffer);
                    break;
                case NORMAL:
                    glNormalPointer(GL_DOUBLE, stride, buffer);
                    if (logCalls) logCall("glNormalPointer",
                                          stride,
                                          buffer);
                    break;
                case COLOR:
                    glColorPointer(3, GL_FLOAT, stride, buffer);
                    if (logCalls) logCall("glColorPointer",
                                          3,
                                          stride,
                                          buffer);
                    break;
                case TEXTURE_COORDINATE:
                    glTexCoordPointer(2, GL_FLOAT, stride, buffer);
                    if (logCalls) logCall("glTexCoordPointer",
                                          2,
                                          stride,
                                          buffer);
                    break;
            }
        }
    }

    private class MyCoordinateListGeometryBuffer implements CoordinateListGeometryBuffer {

        private boolean enabled = false;

        private void checkEnabled(String methodName) {
            if (!enabled) {
                throw new IllegalStateException("Cannot call CoordinateListGeometryBuffer." + methodName +
                                                " outside a call to GeometryBuffer.coordinates(CoordinateList, " +
                                                "GeometryBuffer.Drawable)");
            }
        }

        @Override
        public void draw(GeometrySequence seq, ShortBuffer offsets) {
            checkEnabled("draw(GeometrySequence,ShortBuffer)");
            DirectGeometryBuffer.this.coordinatesImpl(seq, offsets);
        }

        @Override
        public void draw(GeometrySequence seq, IntBuffer offsets) {
            checkEnabled("draw(GeometrySequence,IntBuffer)");
            DirectGeometryBuffer.this.coordinatesImpl(seq, offsets);
        }

        @Override
        public void applyBrush(Brush brush) {
            checkEnabled("applyBrush(Brush)");
            DirectGeometryBuffer.this.pushBrush();
        }

        @Override
        public void pushBrush() {
            checkEnabled("pushBrush()");
            DirectGeometryBuffer.this.pushBrush();
        }

        @Override
        public void popBrush() {
            checkEnabled("popBrush()");
            DirectGeometryBuffer.this.popBrush();
        }

        @Override
        public void normal(Vector normal) {
            checkEnabled("normal(Vector)");
            DirectGeometryBuffer.this.normal(normal);
        }

        @Override
        public void normal(double i, double j, double k) {
            checkEnabled("normal(double,double,double)");
            DirectGeometryBuffer.this.normal(i, j, k);
        }

        @Override
        public void color(Color color) {
            checkEnabled("color(Color)");
            DirectGeometryBuffer.this.color(color);
        }

        @Override
        public void color(float r, float g, float b) {
            checkEnabled("color(float,float,float)");
            DirectGeometryBuffer.this.color(r, g, b);
        }

        @Override
        public void colorAndAlpha(Color color) {
            checkEnabled("colorAndAlpha(Color)");
            DirectGeometryBuffer.this.colorAndAlpha(color);
        }

        @Override
        public void colorAndAlpha(float r, float g, float b, float a) {
            checkEnabled("colorAndAlpha(float,float,float,float)");
            DirectGeometryBuffer.this.colorAndAlpha(r, g, b, a);
        }

        @Override
        public void pushTransform() {
            checkEnabled("pushTransform()");
            DirectGeometryBuffer.this.pushTransform();
        }

        @Override
        public void popTransform() {
            checkEnabled("popTransform()");
            DirectGeometryBuffer.this.popTransform();
        }

        @Override
        public void identity() {
            checkEnabled("popTransform()");
            DirectGeometryBuffer.this.identity();
        }

        @Override
        public void rotate(Axis axis, double angle) {
            checkEnabled("rotate(Axis,double)");
            DirectGeometryBuffer.this.rotate(axis, angle);
        }

        @Override
        public void rotate(Vector axis, double angle) {
            checkEnabled("rotate(Vector,double)");
            DirectGeometryBuffer.this.rotate(axis, angle);
        }

        @Override
        public void translate(Vector magnitude) {
            checkEnabled("translate()");
            DirectGeometryBuffer.this.translate(magnitude);
        }

        @Override
        public void scale(double magnitude) {
            checkEnabled("scale(double)");
            DirectGeometryBuffer.this.scale(magnitude);
        }

        @Override
        public void scale(Vector magnitude) {
            checkEnabled("scale(Vector)");
            DirectGeometryBuffer.this.scale(magnitude);
        }

        void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
