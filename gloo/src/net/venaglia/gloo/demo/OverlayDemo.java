package net.venaglia.gloo.demo;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.BoundingVolume;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Transformation;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.view.KeyHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 4/26/13
 * Time: 6:08 PM
 */
public class OverlayDemo extends AbstractDemo {

    private Transformation transform;
    private Point[] points;
    private Color[] colors = {
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };
    private int[] order = { 0,1,0,2,0,3,1,2,1,3,2,3 };
    private Map<Integer,Point> screenCoords = new HashMap<Integer,Point>(16);
    private double pointRadius = 3.0;
    private double cameraDistance = 48;


    @Override
    protected String getTitle() {
        return "Overlay demo";
    }

    @Override
    protected double getCameraDistance() {
        return cameraDistance;
    }

    @Override
    protected BoundingVolume<?> getRenderingBounds() {
        return new BoundingSphere(Point.ORIGIN, 3);
    }

    @Override
    protected void init() {
        Shape<?> shape = new DemoObjects(0.025, 1, DemoObjects.ObjectCategory.PLATONIC_SOLIDS, true).getShape(0);
        Set<Point> points = new HashSet<Point>();
        for (Point p : shape) {
            points.add(p);
        }
        this.points = points.toArray(new Point[4]);
        transform = shape.getTransformation();
        view.registerKeyHandlers(new KeyHandler(Keyboard.KEY_EQUALS) {
            @Override
            protected void handleKeyDown(int keyCode) {
                cameraDistance = Math.max(cameraDistance * 0.5, 6.0);
            }
        }, new KeyHandler(Keyboard.KEY_MINUS) {
            @Override
            protected void handleKeyDown(int keyCode) {
                cameraDistance = Math.min(cameraDistance * 2.0, 192.0);
            }
        });
    }

    @Override
    protected void project(long nowMS, ProjectionBuffer buffer) {
        buffer.pushTransform();
        transform.apply(nowMS, buffer);
        buffer.applyBrush(Brush.NO_LIGHTING);
        buffer.start(GeometryBuffer.GeometrySequence.LINES);
        for (int index : order) {
            buffer.color(colors[index]);
            buffer.vertex(points[index]);
        }
        buffer.end();
        for (int i = 0; i < 4; i++) {
            screenCoords.put(i, buffer.whereIs(points[i]));
        }
        buffer.popTransform();
    }

    @Override
    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
        buffer.color(Color.GRAY_75);
        boolean started = false;
        for (Map.Entry<Integer,Point> entry : screenCoords.entrySet()) {
            Point point = entry.getValue();
            if (point == null) {
                continue;
            }
            if (!started) {
                started = true;
                buffer.applyBrush(Brush.WIRE_FRAME);
                buffer.pushTransform();
                buffer.translate(new Vector(Display.getWidth() >> 1, Display.getHeight() >> 1, 0));
                buffer.start(GeometryBuffer.GeometrySequence.QUADS);
            }
            point = point.scale(new Vector(Display.getWidth() * 0.0625, Display.getHeight() * 0.0625, 0));
//            point = point.scale(new Vector(128, 128, 0));
            buffer.color(colors[entry.getKey()]);
            buffer.vertex(point.x - pointRadius, point.y - pointRadius, 0);
            buffer.vertex(point.x - pointRadius, point.y + pointRadius, 0);
            buffer.vertex(point.x + pointRadius, point.y + pointRadius, 0);
            buffer.vertex(point.x + pointRadius, point.y - pointRadius, 0);
        }
        if (started) {
            buffer.end();
            buffer.popTransform();
        }
    }

    public static void main(String[] args) {
        new OverlayDemo().start();
    }
}
