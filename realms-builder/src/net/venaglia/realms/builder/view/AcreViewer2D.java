package net.venaglia.realms.builder.view;

import static org.lwjgl.input.Keyboard.*;
import static org.lwjgl.input.Keyboard.KEY_RBRACKET;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.primitives.RoundedRectangle;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.CoordinateList;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.Projectable;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.camera.PerspectiveCamera;
import net.venaglia.gloo.view.KeyHandler;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import net.venaglia.gloo.view.ViewEventHandler;
import net.venaglia.realms.spec.map.GeoPoint;

import java.awt.Dimension;

/**
 * User: ed
 * Date: 2/4/15
 * Time: 5:33 PM
 */
public class AcreViewer2D implements ViewEventHandler, View3DMainLoop {

    private final AcreViews switchableViews;
    private final double radius;
    private final Camera camera;
    private final Light[] lights;
    private final Brush brush;
    private final String windowTitle;
    private final Shape<?> frame;

    private Projectable activeView;

    private AcreViewer2D(AcreViews switchableViews, double radius, Camera camera, String windowTitle) {
        this.switchableViews = switchableViews;
        this.radius = radius;
        this.camera = camera;
        this.lights = new Light[]{};
        this.brush = Brush.SELF_ILLUMINATED.copy();
        this.brush.setCulling(Brush.PolygonSide.BACK);
        this.windowTitle = windowTitle;
        Point corner = new Point(radius * Math.PI * RECIP_SQRT2,
                                 radius * SQRT2_OVER_2_PLUS_1 * Math.tan(Math.PI * 0.25),
                                 0.5);
        this.frame = new RoundedRectangle(corner.x * 2, corner.y * 2, corner.y * 0.0625, DetailLevel.LOW);
    }

    String getWindowTitle() {
        String activeView = switchableViews.getActiveViewName();
        return windowTitle + " - " + (activeView == null ? "(none)" : activeView);
    }

    @Override
    public boolean beforeFrame(long nowMS) {
        Point cameraPosition = new Point(0.0, 0.0, radius * 8);
        camera.setPosition(cameraPosition);
        camera.setDirection(Vector.betweenPoints(cameraPosition, Point.ORIGIN));
        camera.setRight(Vector.cross(cameraPosition, Point.ORIGIN, new Point(0.0, radius, 0.0))
                              .normalize(radius * 4.0));
//        camera.computeClippingDistances(box);
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 2.5 + 2.0));
        Projectable view = switchableViews.getActiveView();
        if (activeView != view) {
            activeView = view;
            return true;
        }
        return !activeView.isStatic();
    }

    @Override
    public MouseTargets getMouseTargets(long nowMS) {
        return null;
    }

    @Override
    public void renderFrame(long nowMS, ProjectionBuffer buffer) {
        buffer.useLights(lights);
        buffer.applyBrush(brush);
        if (activeView != null) {
            activeView.project(nowMS, buffer);
        } else {
            frame.project(nowMS, buffer);
        }
    }

    @Override
    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
        // no-op
    }

    @Override
    public void afterFrame(long nowMS) {
        // no-op
    }

    @Override
    public void handleInit() {
        // no-op
    }

    @Override
    public void handleClose() {
        System.exit(0);
    }

    @Override
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

    public static void transform(CoordinateList coordinates, double radius) {
        for (int i = 0, l = coordinates.size(); i < l; i++) {
            coordinates.set(i, projectPoint(coordinates.get(i).getVertex(), radius));
        }
    }

    private static final double RECIP_SQRT2 = 1.0 / Math.sqrt(2.0);
    private static final double SQRT2_OVER_2_PLUS_1 = Math.sqrt(2.0) / 2.0 + 1.0;

    private static Point projectPoint(Point point, double radius) {
        GeoPoint p = GeoPoint.fromPoint(point);
        return new Point(radius * -p.longitude * RECIP_SQRT2,
                         radius * SQRT2_OVER_2_PLUS_1 * Math.tan(p.latitude * 0.5),
                         0.0);
    }

    public static View3D view(AcreViews switchableViews,
                              double radius,
                              String windowTitle,
                              Dimension windowSize) {
        Camera camera = new PerspectiveCamera();
        camera.setPosition(new Point(0.0, radius * -2.0, 0.0));
        camera.setDirection(new Vector(0.0, radius * 2.0, 0.0));
        camera.setRight(new Vector(radius * 1.25, 0.0, 0.0));
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 1.1));
        AcreViewer2D view = new AcreViewer2D(switchableViews, radius, camera, windowTitle);
        View3D view3D = new View3D((int)windowSize.getWidth(), (int)windowSize.getHeight());
        view.registerKeyHandlers(view3D);
        view3D.setTitle(view.getWindowTitle());
        view3D.setCamera(camera);
        view3D.addViewEventHandler(view);
        view3D.setMainLoop(view);
        view3D.setDefaultBrush(Brush.SELF_ILLUMINATED);
        view3D.start();
        return view3D;
    }

    public static void main(String[] args) {
        view(new AcreViews(), 4000, "test", new Dimension(1280,1024));
    }
}
