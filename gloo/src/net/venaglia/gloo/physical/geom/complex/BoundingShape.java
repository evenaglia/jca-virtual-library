package net.venaglia.gloo.physical.geom.complex;

import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;

/**
 * User: ed
 * Date: 10/12/12
 * Time: 4:52 PM
 */
public class BoundingShape implements Projectable {

    private final BoundingVolume<?> bounds;

    public BoundingShape(BoundingVolume<?> bounds) {
        this.bounds = bounds.asBestFit();
    }

    public boolean isStatic() {
        return true;
    }

    public void project(long nowMS, GeometryBuffer buffer) {
        buffer.pushTransform();
        bounds.translate(toVector(bounds.center()));
        switch (bounds.getBestFit()) {
            case SPHERE:
                bounds.scale(bounds.asSphere().radius);
                sphereDisplayList.project(nowMS, buffer);
                break;
            case BOX:
                BoundingBox box = bounds.asBox();
                bounds.scale(new Vector(box.corner2.x - box.corner1.x,
                                        box.corner2.y - box.corner1.y,
                                        box.corner2.z - box.corner1.z));
                boxDisplayList.project(nowMS, buffer);
                break;
        }
        buffer.popTransform();
    }

    private static Vector toVector(Point a) {
        return new Vector(a.x, a.y, a.z);
    }

    private static DisplayList sphereDisplayList = getSphere();
    private static DisplayList boxDisplayList = getBox();

    private static DisplayList getSphere() {
        DisplayList dl = new DisplayListBuffer("BoundingSphere");
        dl.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                drawSphere(buffer, 1.0);
            }
        });
        return dl;
    }

    public static void drawSphere(GeometryBuffer buffer, double r) {
        double d = Math.PI * 8.0;
        buffer.start(GeometryBuffer.GeometrySequence.LINE_LOOP);
        for (int i = 0; i < 16; i++) {
            double a = Math.sin(i / d) * r;
            double b = Math.cos(i / d) * r;
            buffer.vertex(new Point(a, b, 0.0));
        }
        buffer.end();
        buffer.start(GeometryBuffer.GeometrySequence.LINE_LOOP);
        for (int i = 0; i < 16; i++) {
            double a = Math.sin(i / d) * r;
            double b = Math.cos(i / d) * r;
            buffer.vertex(new Point(a, 0.0, b));
        }
        buffer.end();
        buffer.start(GeometryBuffer.GeometrySequence.LINE_LOOP);
        for (int i = 0; i < 16; i++) {
            double a = Math.sin(i / d) * r;
            double b = Math.cos(i / d) * r;
            buffer.vertex(new Point(0.0, a, b));
        }
        buffer.end();
    }

    private static DisplayList getBox() {
        DisplayList dl = new DisplayListBuffer("BoundingBox");
        dl.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                drawBox(buffer, new Point(-0.5, -0.5, -0.5), new Point(0.5, 0.5, 0.5));
            }
        });
        return dl;
    }

    public static void drawBox(GeometryBuffer buffer, Point a, Point b) {
        Point[] p = new Point[8];
        for (int i = 0; i < 8; i++) {
            p[i] = new Point((i & 1) == 1 ? a.x : b.x,
                             (i & 2) == 2 ? a.y : b.y,
                             (i & 4) == 4 ? a.z : b.z);
        }
        buffer.start(GeometryBuffer.GeometrySequence.LINE_STRIP);
        for (int i : new int[]{ 4, 5, 6, 7, 4, 6, 0, 2, 4, 0, 5, 1, 4 }) {
            buffer.vertex(p[i]);
        }
        buffer.end();
        buffer.start(GeometryBuffer.GeometrySequence.LINE_STRIP);
        for (int i : new int[]{ 3, 6, 2, 7, 3, 0, 1, 2, 3, 1, 7, 5, 3 }) {
            buffer.vertex(p[i]);
        }
        buffer.end();
    }
}
