package net.venaglia.realms.builder.terraform;

import com.apple.eawt.Application;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.*;
import net.venaglia.realms.common.physical.geom.*;
import net.venaglia.realms.common.physical.geom.complex.GeodesicSphere;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.primitives.Sphere;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.projection.DisplayList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.projection.impl.DisplayListBuffer;
import net.venaglia.realms.common.view.MouseTargets;
import net.venaglia.realms.common.view.View3D;
import net.venaglia.realms.common.view.View3DMainLoop;
import net.venaglia.realms.common.view.ViewEventHandler;

import javax.swing.*;
import java.awt.Image;
import java.net.URL;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:05 PM
 */
public class FlowViewer implements View3DMainLoop, ViewEventHandler {

    private final FlowSimulator flowSimulator;
    private final double radius;
    private final boolean onlyPoints;

    private DisplayList fragmentDisplayList = new DisplayListBuffer("Fragment");
    private DisplayList sphereDisplayList = new DisplayListBuffer("Sphere");
    private Light[] lights;
    private Camera camera;
    private double t = 0.0;

    public FlowViewer(FlowSimulator flowSimulator, boolean onlyPoints) {
        this.flowSimulator = flowSimulator;
        this.onlyPoints = onlyPoints;
        this.radius = flowSimulator.getRadius();
    }

    public void handleInit() {
        fragmentDisplayList.record(new DisplayList.DisplayListRecorder() {
            public void record(GeometryBuffer buffer) {
                Sphere sphere = new Sphere(radius < 35.0 ? DetailLevel.MEDIUM_LOW : DetailLevel.LOW).scale(0.66667);
                sphere.project(buffer);
            }
        });
        sphereDisplayList.record(new DisplayList.DisplayListRecorder() {
            public void record(GeometryBuffer buffer) {
                GeodesicSphere sphere = new GeodesicSphere(30).scale(radius * 2.0);
                sphere.project(buffer);
            }
        });
        lights = new Light[]{
                new FixedPointSourceLight(new Point(radius * -0.5 ,radius * 3.0, radius * 0.25)),
                new FixedPointSourceLight(new Point(radius * 1.1f, radius * 5.0f, radius * 3.5f)),
                new FixedPointSourceLight(new Point(radius * -2.1f, radius * 0.0f, radius * 1.5f)),
                new FixedPointSourceLight(new Point(radius * -0.1f, radius * -4.0f, radius * -2.5f))
        };
        camera = new Camera();
    }

    public void handleClose() {
        flowSimulator.stop();
    }

    public void handleNewFrame(long now) {
        // no-op
    }

    public boolean beforeFrame(long nowMS) {
        t += 0.002;
        double cx = Math.sin(t) * radius * 5.0;
        double cy = Math.cos(t) * radius * -5.0;
        Point
                cameraPosition = new Point(cx, cy, radius * 2.0);
        camera.setPosition(cameraPosition);
        camera.setDirection(Vector.betweenPoints(cameraPosition, Point.ORIGIN));
        camera.setRight(Vector.cross(cameraPosition, Point.ORIGIN, new Point(0.0, 0.0, radius)).normalize(
                radius * 2.2));
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 1.1 + 2.0));
        return true;
    }

    public MouseTargets getMouseTargets(long nowMS) {
        return null;
    }

    public void renderFrame(long nowMS, ProjectionBuffer buffer) {
        buffer.useLights(lights);
        buffer.useCamera(camera);
        if (onlyPoints) {
            buffer.applyBrush(Brush.POINTS);
            buffer.color(net.venaglia.realms.common.physical.decorators.Color.GRAY_50);
            sphereDisplayList.project(nowMS, buffer);
            buffer.applyBrush(Brush.POINTS);
        } else {
            if (!flowSimulator.areAllPointsIn()) {
                buffer.applyBrush(Brush.POINTS);
                buffer.color(net.venaglia.realms.common.physical.decorators.Color.GRAY_50);
                sphereDisplayList.project(nowMS, buffer);
            }
            buffer.applyBrush(Brush.FRONT_SHADED);
        }
        FlowSimulator.Fragment[] fragments = flowSimulator.getFragments();
        for (int i = 0, l = flowSimulator.activeFragments(); i < l; i++) {
            FlowSimulator.Fragment fragment = fragments[i];
            buffer.pushTransform();
            buffer.translate(fragment.getVectorXYZ());
            buffer.color(fragment.getColor());
            fragmentDisplayList.project(nowMS, buffer);
            buffer.popTransform();
        }
//            ((OctreeMap)map).project(nowMS, buffer);
    }

    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
    }

    public void afterFrame(long nowMS) {
        // no-op
    }

    private static Image loadAppIcon() {
        URL url = FlowViewer.class.getClassLoader().getResource("images/icon-128.png");
        return new ImageIcon(url).getImage();
    }

    public static void main(String[] args) {
        Application application = Application.getApplication();
        application.setDockIconImage(loadAppIcon());
        FlowSimulator flowSimulator =
//                new FlowSimulator(10,280,500,10.0);
                new FlowSimulator(25,1000,240,12.0);
//                new FlowSimulator(25,8000,60,10.0);
//                new FlowSimulator(25,5000,12.5,10.0); // slow
//                new FlowSimulator(40,16000,15,10.0);
//                new FlowSimulator(60,50000,8,1.0);
//                new FlowSimulator(240,1000000,1,1.0);
        flowSimulator.setObserver(new FlowObserver() {
            public void frame(FlowQueryInterface queryInterface) {
                queryInterface.changeSettings(60, 3.0);
            }
        });

        org.lwjgl.util.Dimension dimension =
//                new Dimension(640, 408);
                new org.lwjgl.util.Dimension(1024, 768);
//                new Dimension(1920, 1150);
        FlowViewer flowViewer = new FlowViewer(flowSimulator, false);
        View3D view = new View3D(dimension);
        view.setTitle("Global Flow");
        view.setMainLoop(flowViewer);
        view.addViewEventHandler(flowViewer);
        view.start();

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            // don't care
        }
        flowSimulator.run();
    }
}
