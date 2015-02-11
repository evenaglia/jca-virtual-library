package net.venaglia.realms.builder.view;

import static org.lwjgl.input.Keyboard.*;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.complex.GeodesicSphere;
import net.venaglia.gloo.physical.lights.FixedPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.gloo.projection.camera.PerspectiveCamera;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;
import net.venaglia.gloo.view.KeyHandler;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import net.venaglia.gloo.view.ViewEventHandler;

import java.awt.Dimension;

/**
* User: ed
* Date: 2/28/13
* Time: 6:02 PM
*/
public class AcreViewer implements ViewEventHandler, View3DMainLoop {

    private final AcreViews switchableViews;
    private final AcreViews persistentViews;
    private final double radius;
    private final Camera camera;
    private final Light[] lights;
    private final String windowTitle;

    private DisplayList sphere;
    private double t = 0.0;

    private AcreViewer(AcreViews switchableViews, AcreViews persistentViews, double radius, Camera camera, String windowTitle) {
        assert switchableViews != null;
        assert camera != null;
        this.switchableViews = switchableViews;
        this.persistentViews = persistentViews;
        this.radius = radius;
        this.camera = camera;
        this.windowTitle = windowTitle;
        Point point = new Point(0, 0, 3);
        Point source1 = point.scale(radius);
        Point source2 = point.rotate(Axis.Y, Math.PI * 0.66667).scale(radius);
        Point source3 = point.rotate(Axis.Y, Math.PI * 0.66667).rotate(Axis.Z, Math.PI * 0.66667).scale(radius);
        Point source4 = point.rotate(Axis.Y, Math.PI * 0.66667).rotate(Axis.Z, Math.PI * -0.66667).scale(radius);
        lights = new Light[]{
                new FixedPointSourceLight(source1),
                new FixedPointSourceLight(source2),
                new FixedPointSourceLight(source3),
                new FixedPointSourceLight(source4)
        };
    }

    public void handleInit() {
        this.sphere = new DisplayListBuffer("Sphere");
        this.sphere.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                GeodesicSphere sphere = new GeodesicSphere(30).scale(radius * 2.0);
                sphere.project(buffer);
            }
        });
    }

    public void handleClose() {
        System.exit(0);
    }

    public void handleNewFrame(long now) {
        // no-op
    }

    private void registerKeyHandlers(View3D view) {
        view.registerKeyHandlers(
                KeyHandler.EXIT_JVM_ON_ESCAPE,
                new KeyHandler(KEY_Q) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        System.exit(0);
                    }
                },
                new KeyHandler(KEY_MINUS) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        scale = Math.min(1.0, scale + 0.015625);
                    }
                },
                new KeyHandler(KEY_EQUALS) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        scale = Math.max(0.015625, scale - 0.015625);
                    }
                },
                new KeyHandler(KEY_LBRACKET) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        switchableViews.previousView();
                        view.setTitle(getWindowTitle());
                    }
                },
                new KeyHandler(KEY_RBRACKET) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        switchableViews.nextView();
                        view.setTitle(getWindowTitle());
                    }
                }
        );
    }

    double scale = 1.0;

    public boolean beforeFrame(long nowMS) {
        t += 0.005 * scale;
        double cx = Math.sin(t) * radius * 5.0;
        double cy = Math.cos(t) * radius * -5.0;
        Point cameraPosition = new Point(cx, cy, radius);
        camera.setPosition(cameraPosition);
        camera.setDirection(Vector.betweenPoints(cameraPosition, Point.ORIGIN));
        camera.setRight(Vector.cross(cameraPosition, Point.ORIGIN, new Point(0.0, 0.0, radius))
                              .normalize(radius * 2.2 * scale));
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 1.1 + 2.0));
        return true;
    }

    public MouseTargets getMouseTargets(long nowMS) {
        return null;
    }

    public void renderFrame(long nowMS, ProjectionBuffer buffer) {
        buffer.useLights(lights);
//        buffer.applyBrush(Brush.POINTS);
//        buffer.color(Color.WHITE);
//        sphere.project(nowMS, buffer);

        buffer.applyBrush(Brush.FRONT_SHADED);
        buffer.color(Color.GRAY_25);
//        buffer.applyBrush(Brush.WIRE_FRAME);
        Projectable activeView = switchableViews.getActiveView();
        if (activeView != null) {
            activeView.project(nowMS, buffer);
        } else {
            sphere.project(nowMS, buffer);
        }

        if (persistentViews != null) {
            for (Projectable projectable : persistentViews.getAllViews()) {
                projectable.project(nowMS, buffer);
            }
        }
    }

    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
    }

    public void afterFrame(long nowMS) {
    }

    String getWindowTitle() {
        String activeView = switchableViews.getActiveViewName();
        return windowTitle + " - " + (activeView == null ? "(none)" : activeView);
    }

    public static View3D view(AcreViews switchableViews,
                              AcreViews persistentViews,
                              double radius,
                              String windowTitle,
                              Dimension windowSize) {
        Camera camera = new PerspectiveCamera();
        camera.setPosition(new Point(0.0, radius * -2.0, 0.0));
        camera.setDirection(new Vector(0.0, radius * 2.0, 0.0));
        camera.setRight(new Vector(radius * 1.25, 0.0, 0.0));
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 1.1));
        AcreViewer view = new AcreViewer(switchableViews, persistentViews, radius, camera, windowTitle);
        View3D view3D = new View3D((int)windowSize.getWidth(), (int)windowSize.getHeight());
        view.registerKeyHandlers(view3D);
        view3D.setTitle(view.getWindowTitle());
        view3D.setCamera(camera);
        view3D.addViewEventHandler(view);
        view3D.setMainLoop(view);
        view3D.start();
        return view3D;
    }
}
