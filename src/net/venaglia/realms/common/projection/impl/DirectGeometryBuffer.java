package net.venaglia.realms.common.projection.impl;

import static net.venaglia.realms.common.util.CallLogger.*;
import static net.venaglia.realms.common.util.CallLogger.logCall;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.projection.Coordinate;
import net.venaglia.realms.common.projection.CoordinateList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.physical.texture.Texture;
import net.venaglia.realms.common.physical.texture.TextureCoordinate;
import net.venaglia.realms.common.physical.texture.TextureMapping;
import net.venaglia.realms.common.util.Tuple2;

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

    protected GeometrySequence begin = null;
    protected Brush activeBrush = new ActiveBrush();
    protected Stack<Brush> brushStack = new Stack<Brush>();
    protected TextureMapping mapping = null;
    protected float[] textureCoords = {0,0};
    protected int activeTextureId = 0;
    protected boolean textureSet;

    public void applyBrush(Brush brush) {
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
        glActiveTexture(GL_TEXTURE0);
        if (logCalls) logCall("glActiveTexture", GL_TEXTURE0);
        bindTextureImpl(glTextureId);
        texture.load();
        mapping.newSequence();
        this.mapping = mapping;
    }

    protected void bindTextureImpl(int glTextureId) {
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
        load(coordinateList);
        if (logCalls) logCall("glDrawArrays", seq.getCode(), 0, (long)coordinateList.size());
        glDrawArrays(seq.getCode(), 0, coordinateList.size());
        unload(coordinateList);
    }

    public void coordinates(CoordinateList coordinateList, GeometrySequence seq, int[] order) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        load(coordinateList);
        coordinatesImpl(coordinateList.size(), seq, order);
        unload(coordinateList);
    }

    public void coordinates(CoordinateList coordinateList,
                            Iterable<Tuple2<GeometrySequence, int[]>> sequences) {
        if (begin != null) {
            throw new IllegalStateException();
        }
        load(coordinateList);
        for (Tuple2<GeometrySequence,int[]> sequence : sequences) {
            coordinatesImpl(coordinateList.size(), sequence.getA(), sequence.getB());
        }
        unload(coordinateList);
    }

    protected void coordinatesImpl(int size, GeometrySequence seq, int[] order) {
        if (size < 256) {
            ByteBuffer indices = ByteBuffer.allocateDirect(order.length);
            for (int i : order) {
                indices.put((byte)i);
            }
            glDrawElements(seq.getCode(), indices);
            if (logCalls) logCall("glDrawElements", seq.getCode(), indices);
        } else if (size < 65536) {
            ShortBuffer indices = ByteBuffer.allocateDirect(order.length * (Short.SIZE >> 3)).asShortBuffer();
            for (int i : order) {
                indices.put((short)i);
            }
            glDrawElements(seq.getCode(), indices);
            if (logCalls) logCall("glDrawElements", seq.getCode(), indices);
        } else {
            IntBuffer indices = ByteBuffer.allocateDirect(order.length * (Integer.SIZE >> 3)).asIntBuffer();
            indices.put(order);
            glDrawElements(seq.getCode(), indices);
            if (logCalls) logCall("glDrawElements", seq.getCode(), indices);
        }
    }

    protected void load(CoordinateList coordinateList) {
        ByteBuffer data = coordinateList.data();
        if (coordinateList.has(CoordinateList.Field.VERTEX)) {
            glEnableClientState(GL_VERTEX_ARRAY);
            if (logCalls) logCall("glEnableClientState", GL_VERTEX_ARRAY);
            data.position(coordinateList.offset(CoordinateList.Field.VERTEX));
            glVertexPointer(3, coordinateList.stride(CoordinateList.Field.VERTEX), data.slice().asDoubleBuffer());
            if (logCalls) logCall("glVertexPointer",
                                  3,
                                  (long)coordinateList.stride(CoordinateList.Field.VERTEX),
                                  data.slice().asDoubleBuffer());
        }
        if (coordinateList.has(CoordinateList.Field.NORMAL)) {
            glEnableClientState(GL_NORMAL_ARRAY);
            if (logCalls) logCall("glEnableClientState", GL_NORMAL_ARRAY);
            data.position(coordinateList.offset(CoordinateList.Field.NORMAL));
            glVertexPointer(3, coordinateList.stride(CoordinateList.Field.NORMAL), data.slice().asDoubleBuffer());
            if (logCalls) logCall("glVertexPointer",
                                  3,
                                  (long)coordinateList.stride(CoordinateList.Field.NORMAL),
                                  data.slice().asDoubleBuffer());
        }
        if (coordinateList.has(CoordinateList.Field.COLOR)) {
            glEnableClientState(GL_COLOR_ARRAY);
            if (logCalls) logCall("glEnableClientState", GL_COLOR_ARRAY);
            data.position(coordinateList.offset(CoordinateList.Field.COLOR));
            glVertexPointer(3, coordinateList.stride(CoordinateList.Field.COLOR), data.slice().asFloatBuffer());
            if (logCalls) logCall("glVertexPointer",
                                  3,
                                  (long)coordinateList.stride(CoordinateList.Field.COLOR),
                                  data.slice().asFloatBuffer());
        }
        if (coordinateList.has(CoordinateList.Field.TEXTURE_COORDINATE)) {
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
            if (logCalls) logCall("glEnableClientState", GL_TEXTURE_COORD_ARRAY);
            data.position(coordinateList.offset(CoordinateList.Field.TEXTURE_COORDINATE));
            if (logCalls) logCall("glVertexPointer",
                                  3,
                                  (long)coordinateList.stride(CoordinateList.Field.TEXTURE_COORDINATE),
                                  data.slice().asFloatBuffer());
            glVertexPointer(3,
                            coordinateList.stride(CoordinateList.Field.TEXTURE_COORDINATE),
                            data.slice().asFloatBuffer());
        }
    }

    private void unload(CoordinateList coordinateList) {
        if (coordinateList.has(CoordinateList.Field.VERTEX)) {
            glDisableClientState(GL_VERTEX_ARRAY);
            if (logCalls) logCall("glDisableClientState", GL_VERTEX_ARRAY);
        }
        if (coordinateList.has(CoordinateList.Field.NORMAL)) {
            glDisableClientState(GL_NORMAL_ARRAY);
            if (logCalls) logCall("glDisableClientState", GL_NORMAL_ARRAY);
        }
        if (coordinateList.has(CoordinateList.Field.COLOR)) {
            glDisableClientState(GL_COLOR_ARRAY);
            if (logCalls) logCall("glDisableClientState", GL_COLOR_ARRAY);
        }
        if (coordinateList.has(CoordinateList.Field.TEXTURE_COORDINATE)) {
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
            if (logCalls) logCall("glDisableClientState", GL_TEXTURE_COORD_ARRAY);
        }
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
            super(GL_DEFAULTS);
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
                    if (logCalls) logCall("glDepthFunc", depth.glCode);
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
}
