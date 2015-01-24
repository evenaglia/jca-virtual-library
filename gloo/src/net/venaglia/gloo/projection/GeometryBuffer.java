package net.venaglia.gloo.projection;

import static org.lwjgl.opengl.GL11.*;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;

import java.awt.geom.Rectangle2D;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * User: ed
 * Date: 9/28/12
 * Time: 7:20 AM
 */
public interface GeometryBuffer extends CoordinateBuffer {

    public enum GeometrySequence {
        POINTS(GL_POINTS),
        LINES(GL_LINES),
        LINE_LOOP(GL_LINE_LOOP),
        LINE_STRIP(GL_LINE_STRIP),
        TRIANGLES(GL_TRIANGLES),
        TRIANGLE_STRIP(GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL_TRIANGLE_FAN),
        QUADS(GL_QUADS),
        QUAD_STRIP(GL_QUAD_STRIP),
        POLYGON(GL_POLYGON);

        private final int code;

        GeometrySequence(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    void applyBrush(Brush brush);

    void pushBrush();

    void popBrush();

    void useTexture(Texture texture, TextureMapping mapping);

    void clearTexture();

    void start(GeometrySequence seq);

    void end();

    void coordinates(CoordinateList coordinateList, GeometrySequence seq);

    void coordinates(CoordinateList coordinateList, GeometrySequence seq, ShortBuffer order);

    void coordinates(CoordinateList coordinateList, GeometrySequence seq, IntBuffer order);

    void coordinates(CoordinateList coordinateList, Drawable drawable);

    void pushTransform();

    void popTransform();

    void identity();

    void rotate(Axis axis, double angle);

    void rotate(Vector axis, double angle);

    void translate(Vector magnitude);

    void scale(double magnitude);

    void scale(Vector magnitude);

    void callDisplayList(int glDisplayListId);

    Point whereIs(Point point);

    Rectangle2D whereIs(BoundingBox bounds);

    double viewingAngle(BoundingBox bounds, Point observer);

    boolean isScreen();

    boolean isTarget();

    boolean isOverlay();

    boolean isVirtual();

    interface Drawable {
        void draw(CoordinateListGeometryBuffer buffer);
    }

    interface CoordinateListGeometryBuffer extends ColorBuffer {

        void draw(GeometrySequence seq, ShortBuffer offsets);

        void draw(GeometrySequence seq, IntBuffer offsets);

        // todo: implement multi-draw

        void applyBrush(Brush brush);

        void pushBrush();

        void popBrush();

        void normal(Vector normal);

        void normal(double i, double j, double k);

        void pushTransform();

        void popTransform();

        void identity();

        void rotate(Axis axis, double angle);

        void rotate(Vector axis, double angle);

        void translate(Vector magnitude);

        void scale(double magnitude);

        void scale(Vector magnitude);
    }
}
