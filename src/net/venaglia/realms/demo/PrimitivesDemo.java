package net.venaglia.realms.demo;

import com.apple.eawt.Application;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.util.CallLogger;
import net.venaglia.realms.common.view.KeyHandler;
import net.venaglia.realms.common.view.MouseTarget;
import net.venaglia.realms.common.view.MouseTargetEventListener;
import net.venaglia.realms.common.view.MouseTargets;
import net.venaglia.realms.common.view.View3D;
import net.venaglia.realms.common.view.View3DMainLoop;
import net.venaglia.realms.common.view.ViewEventHandler;
import org.lwjgl.util.Dimension;

import javax.swing.*;
import java.awt.Image;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 7/13/12
 * Time: 10:00 PM
 */
public class PrimitivesDemo {

    public void start(boolean wireFrame, float divisionCount, DemoObjects.ObjectCategory objectCategory) {
//        final double timescale = 0.0667;
        final double timescale = 0.667;
//        final double timescale = 6.67;
        final View3D view3D = new View3D(new Dimension(1024,768));
        final AtomicReference<Shape<?>> highlightObject = new AtomicReference<Shape<?>>();
        view3D.setTitle("Primitives Demo");
        view3D.registerKeyHandlers(KeyHandler.EXIT_JVM_ON_ESCAPE);

        Application application = Application.getApplication();
        application.setDockIconImage(loadAppIcon());

        final DemoObjects objects = new DemoObjects(timescale, divisionCount, objectCategory, wireFrame);

        final Camera camera = new Camera();
        camera.setPosition(new Point(0,0,4));
        camera.setDirection(new Vector(0,0,-4));
        camera.setRight(new Vector(-2,0,0));
        camera.setClippingDistance(0.1f,40.0f);

        Brush brush = new Brush();
        brush.setLighting(!wireFrame);
        brush.setColor(true);
        brush.setPolygonFrontFace(wireFrame ? Brush.PolygonMode.LINE : Brush.PolygonMode.FILL);
        brush.setPolygonBackFace(wireFrame ? Brush.PolygonMode.LINE : Brush.PolygonMode.FILL);
        brush.setCulling(wireFrame ? null : Brush.PolygonSide.BACK);

        final Light[] lights = {
                new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f)),
                new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f)),
                new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f))
        };
        final MouseTargets mouseTargets = new MouseTargets();
        for (int shapeNum = 0; shapeNum < 3; shapeNum++) {
            Shape<?> shape = objects.getShape(shapeNum);
            String name = objects.getShapeName(shapeNum);
            ColorChangeListener colorChangeListener =
                    new ColorChangeListener(shapeNum, objects, wireFrame, highlightObject);
            mouseTargets.add(new MouseTarget<Shape<?>>(shape, colorChangeListener, shape, name));
        }

        view3D.setCamera(camera);
        view3D.setDefaultBrush(brush);
        view3D.setMainLoop(new View3DMainLoop() {

            private double a = Math.PI, b = 0.0;
            private Point[] centers = new Point[objects.size()];

            public boolean beforeFrame(long nowMS) {
                double z = Math.sin(b);
                double x = Math.sin(a) * (1.0 - z);
                double y = Math.cos(a) * (1.0 - z);
                Point c = new Point(x,y,z).scale(4);
                camera.setPosition(c);
                Vector d = DemoObjects.toVector(c, Point.ORIGIN).normalize();
                camera.setDirection(d);
                camera.setRight(new Vector(0,0,1.0).cross(d).normalize(0.4));
                a -= 0.015 * timescale;
                b += 0.0003 * timescale;
                return true;
            }

            public MouseTargets getMouseTargets(long nowMS) {
                return mouseTargets;
            }

            public void renderFrame(long nowMS, ProjectionBuffer buffer) {
                if (CallLogger.logCalls) {
                    System.exit(0);
                }
                buffer.useLights(lights);
                int i = 0;
                for (Shape<?> shape : objects) {
                    shape.project(nowMS, buffer);
                    centers[i++] = computeCenter(nowMS, buffer, shape);
                }
                objects.getOrigin().project(nowMS, buffer);
                Shape<?> highlightShape = highlightObject.get();
                if (highlightShape != null) {
                    highlightShape.project(nowMS, buffer);
                }
            }

            private Point computeCenter(long nowMS, ProjectionBuffer buffer, Shape<?> shape) {
                buffer.pushTransform();
                try {
                    shape.getTransformation().apply(nowMS, buffer);
                    return buffer.whereIs(Point.ORIGIN);
                } finally {
                    buffer.popTransform();
                }
            }

            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            public void afterFrame(long nowMS) {
            }

        });
        view3D.addViewEventHandler(new ViewEventHandler() {
            public void handleInit() {
            }

            public void handleClose() {
                System.exit(0);
            }

            public void handleNewFrame(long now) {
                // no-op
            }
        });
        view3D.start();
    }

    private Image loadAppIcon() {
        URL url = getClass().getClassLoader().getResource("images/icon-128.png");
        return new ImageIcon(url).getImage();
    }

    private class ColorChangeListener implements MouseTargetEventListener<Shape<?>> {

        private final String name;
        private final AtomicReference<Shape<?>> highlightObject;

        private Material baseMaterial;
        private Material highlightMaterial;
        private Shape<?> wireFrameShape;

        private ColorChangeListener(int shapeNum,
                                    DemoObjects objects,
                                    boolean wireFrame,
                                    AtomicReference<Shape<?>> highlightObject) {
            this(shapeNum, objects, wireFrame, highlightObject, objects.getShapeName(shapeNum));
        }

        private ColorChangeListener(int shapeNum,
                                    DemoObjects objects,
                                    boolean wireFrame,
                                    AtomicReference<Shape<?>> highlightObject,
                                    String name) {
            Shape<?> baseShape = objects.getShape(shapeNum);
            this.highlightObject = highlightObject;
            this.baseMaterial = baseShape.getMaterial();
            Color color = objects.getMaterialColor(shapeNum);
            color = new Color(color.r * 1.333f, color.g * 1.333f, color.b * 1.333f);
            this.highlightMaterial = Material.paint(color, objects.getBrush());
            if (!wireFrame) {
                this.wireFrameShape = baseShape.copy();
                Brush brush = new Brush(Brush.WIRE_FRAME);
                brush.setDepth(Brush.DepthMode.LESS_OR_EQUAL);
                this.wireFrameShape.setMaterial(Material.paint(Color.BLACK, brush));
            }
            this.name = name;
        }

        public void mouseOver(MouseTarget<? extends Shape<?>> target) {
            target.getValue().setMaterial(highlightMaterial);
            highlightObject.compareAndSet(null, wireFrameShape);
            System.out.println(name + ".mouseOver()");
        }

        public void mouseOut(MouseTarget<? extends Shape<?>> target) {
            target.getValue().setMaterial(baseMaterial);
            highlightObject.compareAndSet(wireFrameShape, null);
            System.out.println(name + ".mouseOut()");
        }

        public void mouseClick(MouseTarget<? extends Shape<?>> target, MouseButton button) {
            // no-op
            System.out.println(name + ".mouseClick(" + button + ")");
        }
    }

    public static void main(String[] argv) {
//        new PrimitivesDemo().start(false, null, 0, false, false);
//        new PrimitivesDemo().start(true, null, 1, false, false);
//        new PrimitivesDemo().start(false, null, 12, true, false);
//        new PrimitivesDemo().start(true, Sphere.Detail.MEDIUM, 8, false, false);
//        new PrimitivesDemo().start(true, Sphere.Detail.VERY_FINE, 30, false, false);
//        new PrimitivesDemo().start(false, null, 30, true, false);
//        new PrimitivesDemo().start(false, Sphere.Detail.VERY_FINE, 30, true, false);
//        new PrimitivesDemo().start(false, null, 1, false, false);
//        new PrimitivesDemo().start(false, 30, DemoObjects.ObjectCategory.ROUND_SOLIDS);
        new PrimitivesDemo().start(false, 30, DemoObjects.ObjectCategory.PLATONIC_SOLIDS);
    }


}
