package net.venaglia.realms.builder.terraform.flow;

import com.apple.eawt.Application;
import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.decorators.*;
import net.venaglia.gloo.physical.geom.*;
import net.venaglia.gloo.physical.geom.complex.GeodesicSphere;
import net.venaglia.gloo.physical.geom.detail.DetailLevel;
import net.venaglia.gloo.physical.geom.primitives.Sphere;
import net.venaglia.gloo.physical.lights.FixedPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.camera.PerspectiveCamera;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import net.venaglia.gloo.view.ViewEventHandler;

import javax.swing.*;
import java.awt.Dimension;
import java.awt.Image;
import java.net.URL;
import java.util.List;

/**
 * User: ed
 * Date: 3/1/13
 * Time: 8:05 PM
 */
public class FlowViewer implements View3DMainLoop, ViewEventHandler {

    private final FlowSimulator flowSimulator;
    private final double radius;
    private final boolean onlyPoints;
    private final List<TectonicVectorArrow> tectonicArrows;

    private DisplayList fragmentDisplayList = new DisplayListBuffer("Fragment");
    private DisplayList sphereDisplayList = new DisplayListBuffer("Sphere");
    private DisplayList tectonicArrowsDisplayList = new DisplayListBuffer("Tectonic Arrows");
    private Light[] lights;
    private Camera camera;
    private double t = 0.0;

    public FlowViewer(FlowSimulator flowSimulator, boolean onlyPoints) {
        this.flowSimulator = flowSimulator;
        this.onlyPoints = onlyPoints;
        this.radius = flowSimulator.getRadius();
        this.tectonicArrows = flowSimulator.getTectonicArrows(radius);
    }

    public void handleInit() {
        fragmentDisplayList.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                Sphere sphere = new Sphere(radius < 35.0 ? DetailLevel.MEDIUM_LOW : DetailLevel.LOW).scale(0.66667);
                sphere.project(buffer);
            }
        });
        sphereDisplayList.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                GeodesicSphere sphere = new GeodesicSphere(30).scale(radius * 2.0);
                sphere.project(buffer);
            }
        });
        tectonicArrowsDisplayList.record(new GeometryRecorder() {
            public void record(RecordingBuffer buffer) {
                for (TectonicVectorArrow arrow : tectonicArrows) {
                    arrow.project(buffer);
                }
            }
        });
        lights = new Light[]{
                new FixedPointSourceLight(new Point(radius * -0.5 ,radius * 3.0, radius * 0.25)),
                new FixedPointSourceLight(new Point(radius * 1.1f, radius * 5.0f, radius * 3.5f)),
                new FixedPointSourceLight(new Point(radius * -2.1f, radius * 0.0f, radius * 1.5f)),
                new FixedPointSourceLight(new Point(radius * -0.1f, radius * -4.0f, radius * -2.5f))
        };
        camera = new PerspectiveCamera();
    }

    public void handleClose() {
        flowSimulator.stop();
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            // don't care
        }
        System.exit(0);
    }

    public void handleNewFrame(long now) {
        // no-op
    }

    public boolean beforeFrame(long nowMS) {
        t += 0.002;
        double cx = Math.sin(t) * radius * 5.0;
        double cy = Math.cos(t) * radius * -5.0;
        Point cameraPosition = new Point(cx, cy, radius * 2.0);
        camera.setPosition(cameraPosition);
        camera.setDirection(Vector.betweenPoints(cameraPosition, Point.ORIGIN));
        camera.setRight(Vector.cross(cameraPosition, Point.ORIGIN, new Point(0.0, 0.0, radius)).normalize(radius * 2.2));
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 1.15 + 2.0));
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
            buffer.color(Color.GRAY_50);
            sphereDisplayList.project(nowMS, buffer);
            buffer.applyBrush(Brush.POINTS);
        } else {
            if (!flowSimulator.areAllPointsIn()) {
                buffer.applyBrush(Brush.POINTS);
                buffer.color(Color.GRAY_50);
                sphereDisplayList.project(nowMS, buffer);
            }
            tectonicArrows.get(0).getMaterial().apply(nowMS, buffer);
            tectonicArrowsDisplayList.project(nowMS, buffer);
            buffer.applyBrush(Brush.FRONT_SHADED);
        }
        Fragment[] fragments = flowSimulator.getFragments();
        for (int i = 0, l = flowSimulator.activeFragments(); i < l; i++) {
            Fragment fragment = fragments[i];
            buffer.pushTransform();
            buffer.translate(fragment.getCenterXYZ(Vector.VECTOR_XFORM_VIEW));
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

    private static float colorSine(double a, double part) {
        float v = (float)(Math.sin((a + Math.PI / 2.0) + Math.PI * part * 0.6666666667) * 1.5 + 0.5);
        return Math.max(Math.min(v,1.0f),0.0f);
    }

    public static void main(String[] args) {
        Application application = Application.getApplication();
        application.setDockIconImage(loadAppIcon());
        final FlowSimulator flowSimulator =
//                new FlowSimulator(10,280,500,10.0);
//                new FlowSimulator(25,1000,240,12.0);
                new FlowSimulator(25,8000,60,10.0);
//                new FlowSimulator(25,5000,12.5,10.0); // slow
//                new FlowSimulator(40,16000,15,10.0);
//                new FlowSimulator(60,50000,8,10.0);
//                new FlowSimulator(240,1000000,1,1.0);
        flowSimulator.setObserver(new FlowObserver() {
            public void frame(FlowQueryInterface queryInterface) {
                queryInterface.changeSettings(60, 3.0);
            }
        });
        Fragment[] fragments = flowSimulator.getFragments();
        int endPadding = 0; //numTectonicPoints * 4;
        for (int i = 0, count = fragments.length; i < count; i++) {
            Fragment fragment = fragments[i];
            double a = Math.PI * -2.0 * (((double)i) / count);
            Color color = i < endPadding || (i + endPadding) > count ? Color.WHITE : new Color(colorSine(a, 0.0), colorSine(a, 1.0), colorSine(a, 2.0), 1.0f);
            fragment.setColor(color);
        }

        Dimension dimension =
//                new Dimension(640, 408);
//                new Dimension(1024, 768);
                new Dimension(1280, 1024);
//                new Dimension(1920, 1150);
        FlowViewer flowViewer = new FlowViewer(flowSimulator, false);
        View3D view = new View3D((int)dimension.getWidth(), (int)dimension.getHeight());
        view.setTitle("Global Flow");
        view.setMainLoop(flowViewer);
        view.addViewEventHandler(flowViewer);
        view.start();

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            // don't care
        }
        flowSimulator.start();
    }
}
