package net.venaglia.realms.demo;

import com.apple.eawt.Application;
import net.venaglia.realms.common.physical.bounds.BoundingVolume;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.view.KeyHandler;
import net.venaglia.realms.common.view.MouseTargets;
import net.venaglia.realms.common.view.View3D;
import net.venaglia.realms.common.view.View3DMainLoop;
import net.venaglia.realms.common.view.ViewEventHandler;
import org.lwjgl.util.Dimension;

import java.awt.*;

/**
 * User: ed
 * Date: 3/13/13
 * Time: 10:37 AM
 */
public abstract class AbstractDemo implements View3DMainLoop, ViewEventHandler {

    protected View3D view;

    protected Camera camera;
    protected Light[] lights;
    protected double a = Math.PI, b = 0.2;

    public void start() {
        double cameraDistance = getCameraDistance();
        view = new View3D(new Dimension(1280, 1024));
        view.setTitle(getTitle());
        view.registerKeyHandlers(KeyHandler.EXIT_JVM_ON_ESCAPE);

        Image image = loadAppIcon();
        if (image != null) {
            Application application = Application.getApplication();
            application.setDockIconImage(image);
        }

        camera = new Camera();
        camera.setPosition(new Point(0, 0, 1).scale(cameraDistance));
        camera.setDirection(new Vector(0, 0, -1).scale(cameraDistance));
        camera.setRight(new Vector(-0.5,0,0).scale(cameraDistance));
        BoundingVolume<?> bounds = getRenderingBounds();
        if (bounds != null) {
            camera.computeClippingDistances(bounds);
        }

        Brush brush = new Brush();

        lights = getLights(cameraDistance);

        view.setCamera(camera);
        view.setDefaultBrush(brush);
        view.setMainLoop(this);
        view.addViewEventHandler(this);
        view.start();
    }

    public void handleInit() {
        init();
    }

    public void handleClose() {
        System.exit(0);
    }

    public void handleNewFrame(long now) {
        // no-op
    }

    public void renderFrame(long nowMS, ProjectionBuffer buffer) {
        buffer.useLights(lights);
        project(nowMS, buffer);
    }

    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
    }

    public boolean beforeFrame(long nowMS) {
        double z = Math.sin(b);
        double x = Math.sin(a) * (1.0 - z);
        double y = Math.cos(a) * (1.0 - z);
        Point c = new Point(x,y,z).scale(getCameraDistance());
        setCameraPosition(c);
        a += getStepA();
        b += getStepB();
        return true;
    }

    protected void setCameraPosition(Point c) {
        camera.setPosition(c);
        Vector d = Vector.betweenPoints(c, Point.ORIGIN).normalize();
        camera.setDirection(d);
        camera.setRight(new Vector(0, 0, 1.0).cross(d).normalize(0.4));
        camera.computeClippingDistances(getRenderingBounds());
    }

    public MouseTargets getMouseTargets(long nowMS) {
        return null;
    }

    public void afterFrame(long nowMS) {
    }

    protected abstract String getTitle();

    protected Image loadAppIcon() {
        return null;
    }

    protected abstract double getCameraDistance();

    protected abstract BoundingVolume<?> getRenderingBounds();

    protected Light[] getLights(double cameraDistance) {
        return new Light[]{
                new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f).scale(cameraDistance)),
                new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f).scale(cameraDistance)),
                new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f).scale(cameraDistance))
        };
    }

    protected double getStepA() {
        return -0.01;
    }

    protected double getStepB() {
        return 0.0002;
    }

    protected abstract void init();

    protected abstract void project(long nowMS, ProjectionBuffer buffer);
}
