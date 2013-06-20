package net.venaglia.gloo.demo;

import static net.venaglia.gloo.physical.geom.ZMap.Fn;

import net.venaglia.gloo.physical.decorators.Brush;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.ZMap;
import net.venaglia.gloo.physical.geom.complex.Mesh;
import net.venaglia.gloo.physical.geom.complex.PolyMesh;
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
import net.venaglia.gloo.util.surfaceFn.CompositeFn;
import net.venaglia.gloo.util.surfaceFn.FractalFn;
import net.venaglia.gloo.util.surfaceFn.NurbsFn;
import net.venaglia.gloo.view.KeyHandler;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import net.venaglia.gloo.view.ViewEventHandler;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * User: ed
 * Date: 2/12/13
 * Time: 10:10 PM
 */
public class PolyMeshDemo {

    public void start(final boolean wireFrame,
                      final PolyMesh polyMesh,
                      final Map<Point,Vector> normals,
                      Color color,
                      final Set<Mesh> controlPoints) {
        if (polyMesh != null) {
            if (wireFrame) {
                polyMesh.setMaterial(Material.paint(color, Brush.WIRE_FRAME));
            } else {
                polyMesh.setMaterial(Material.paint(color, Brush.FRONT_SHADED));
            }
        }
//        final PolyMesh wireframe = polyMesh.copy();
//        wireframe.setMaterial(Material.makeWireFrame(new Color(0.55f, 1.0f, 0.55f)));

//        final Origin origin = new Origin(0.5);
        final DisplayList meshDisplayList = new DisplayListBuffer("Mesh");
        final DisplayList normalsDisplayList = new DisplayListBuffer("Normals");

        final View3D view3D = new View3D(1024,768);
        view3D.setTitle("Poly Mesh Demo");
        view3D.registerKeyHandlers(KeyHandler.EXIT_JVM_ON_ESCAPE);

        final Camera camera = new PerspectiveCamera();
        camera.setPosition(new Point(0,0,4));
        camera.setDirection(new Vector(0,0,-4));
        camera.setRight(new Vector(-2,0,0));
        camera.setClippingDistance(0.1f,40.0f);

        Brush brush = new Brush();
        brush.setLighting(!wireFrame);
        brush.setColor(true);
        brush.setPolygonFrontFace(wireFrame ? Brush.PolygonMode.LINE : Brush.PolygonMode.FILL);
        brush.setPolygonBackFace(wireFrame ? Brush.PolygonMode.LINE : Brush.PolygonMode.FILL);
//        brush.setCulling(wireFrame ? null : Brush.PolygonSide.BACK);

        final Light[] lights = {
                new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f)),
                new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f)),
                new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f))
        };

        view3D.setCamera(camera);
        view3D.setDefaultBrush(brush);
        view3D.setMainLoop(new View3DMainLoop() {

            private double a = Math.PI, b = 0.5;

            public boolean beforeFrame(long nowMS) {
                double z = Math.sin(b);
                double x = Math.sin(a) * (1.0 - z);
                double y = Math.cos(a) * (1.0 - z);
                Point c = new Point(x,y,z).scale(4);
                camera.setPosition(c);
                Vector d = DemoObjects.toVector(c, Point.ORIGIN).normalize();
                camera.setDirection(d);
                camera.setRight(new Vector(0,0,1.0).cross(d).normalize(0.4));
                a -= 0.002;
                b += 0.0001;
                return true;
            }

            public MouseTargets getMouseTargets(long nowMS) {
                return null;
            }

            public void renderFrame(long nowMS, ProjectionBuffer buffer) {
                buffer.useLights(lights);
                if (polyMesh != null) {
                    polyMesh.project(nowMS, buffer);
//                    meshDisplayList.project(nowMS, buffer);
                }
                if (normals != null) {
                    Material.makeWireFrame(Color.CYAN).apply(nowMS, buffer);
                    normalsDisplayList.project(nowMS, buffer);
                }
                if (controlPoints != null) {
                    for (Mesh mesh : controlPoints) {
                        mesh.project(nowMS, buffer);
                    }
                }
//                origin.project(nowMS, buffer);
            }

            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            public void afterFrame(long nowMS) {
            }

        });
        view3D.addViewEventHandler(new ViewEventHandler() {
            public void handleInit() {
                if (polyMesh != null) {
                    meshDisplayList.record(new GeometryRecorder() {
                        public void record(RecordingBuffer buffer) {
                            polyMesh.project(0, buffer);
                        }
                    });
                }
                if (normals != null) {
                    normalsDisplayList.record(new GeometryRecorder() {
                        public void record(RecordingBuffer buffer) {
                            buffer.start(GeometryBuffer.GeometrySequence.LINES);
                            for (Map.Entry<Point, Vector> entry : normals.entrySet()) {
                                Point p = entry.getKey();
                                buffer.vertex(p);
                                buffer.vertex(p.translate(entry.getValue()));
                            }
                            buffer.end();
                        }
                    });
                }
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

    public static void main(String[] args) {
//        final double scale = 250;
        final double scale = 64;
//        final double scale = 8;

//        Color color = new Color(0.5f, 1.0f, 0.5f); // green
//        Fn fn = new SpiralSine(0.2, 3.2 / scale, 6);
//        Color color = new Color(0.4f, 0.6f, 1.0f); // blue
//        Fn fn = new SineWave(0, scale / 2.666, scale / 18.0).sum(new SineWave(2, scale / 2.0, scale / 48.0)).sum(new SineWave(4, scale / 1.456, scale / 64.0));
        Color color = new Color(0.3f, 0.2f, 0.05f); // brown
        Fn fn = new FractalFn((int)Math.ceil(scale * 4.8), 123456789, scale * 0.5, (int)Math.floor(scale * -2.4), (int)Math.floor(scale * -2.4));
        int divisions = (int)Math.round(scale / 4.0);
//        fn = divisions <= 0
//             ? fn
//             : fn.buildNurbs()
//               .setBounds(Math.floor(scale * -2.125),
//                          Math.floor(scale * -2.125),
//                          Math.ceil(scale * 2.125),
//                          Math.ceil(scale * 2.125))
//               .setComposite(true)
//               .setDivisions(divisions, divisions)
//               .build();

//        String mode = "nurbs";
//        String mode = "nurbs-shaded";
//        String mode = "wireframe";
//        String mode = "wireframe-with-normals";
        String mode = "shaded";

        final double sq3 = Math.sqrt(3);
        final Fn f = fn;
        double[] poly = {
                1, -sq3,
                2, 0,
                1, sq3,
                -1, sq3,
                -2, 0,
                -1, -sq3
        };
        for (int i = 0; i < poly.length; i++) {
            poly[i] *= scale;
        }
        ZMap.Source src = new ZMap.Source() {
            public ZMap getZMap(int x1, int y1, int x2, int y2) {
                ZMap zMap = new ZMap(new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1));
                zMap.load(f);
                return zMap;
            }
        };

        PolyMesh.Builder builder = new PolyMesh.Builder();
        PolyMesh polyMesh = builder.build(src, poly);
        boolean wireframe = mode.contains("wireframe");
        Set<Mesh> controlPoints = new LinkedHashSet<Mesh>();
        double invertScale = 0.32 / scale;
        if (mode.contains("nurbs")) {
            Vector translate = mode.contains("wireframe") || mode.contains("shaded") ? new Vector(0,0,8) : Vector.ZERO;
            if (f instanceof NurbsFn) {
                controlPoints.add(((NurbsFn)f).getControlPoints().translate(translate).scale(invertScale));
            } else if (f instanceof CompositeFn) {
                for (Fn delegateFn : ((CompositeFn)f)) {
                    if (delegateFn instanceof NurbsFn) {
                        controlPoints.add(((NurbsFn)delegateFn).getControlPoints().translate(translate).scale(invertScale));
                    }
                }
            }
            Material redWireFrameA = Material.makeWireFrame(new Color(1.0f, 0.5f, 0.0f));
            Material redWireFrameB = Material.makeWireFrame(new Color(1.0f, 0.0f, 0.5f));
            int c = 0;
            for (Mesh mesh : controlPoints) {
                int cx = c % divisions;
                int cy = c / divisions;
                Material m = (cx & 1 ^ cy & 1) == 0 ? redWireFrameA : redWireFrameB;
                mesh.setMaterial(m);
                c++;
            }
        }
        Map<Point,Vector> normals = null;
        if (mode.contains("normals") || mode.contains("shaded") || mode.contains("wireframe")) {
            normals = mode.contains("normals") ? new HashMap<Point,Vector>() : null;
            for (int i = 0, l = polyMesh.points.length; i < l; i++) {
                Point point = polyMesh.points[i];
                Point scaledPoint = point.scale(invertScale);
                if (normals != null) {
                    normals.put(scaledPoint, polyMesh.normals[i].scale(invertScale));
                }
                polyMesh.points[i] = scaledPoint;
            }
        }
        PolyMesh mesh = mode.contains("shaded") || mode.contains("wireframe") ? polyMesh : null;
        new PolyMeshDemo().start(wireframe, mesh, normals, color, controlPoints);
    }
}
