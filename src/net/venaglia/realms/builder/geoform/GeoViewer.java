package net.venaglia.realms.builder.geoform;

import static net.venaglia.realms.builder.geoform.GeoSpec.SECTORS;
import static org.lwjgl.input.Keyboard.*;

import net.venaglia.realms.builder.map.AbstractCartographicElement;
import net.venaglia.realms.builder.map.Acre;
import net.venaglia.realms.builder.map.GlobalSector;
import net.venaglia.realms.builder.map.Globe;
import net.venaglia.realms.builder.map.Sector;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.complex.GeodesicSphere;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.projection.DisplayList;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.projection.impl.DisplayListBuffer;
import net.venaglia.realms.common.util.SpatialMap;
import net.venaglia.realms.common.view.KeyHandler;
import net.venaglia.realms.common.view.MouseTargets;
import net.venaglia.realms.common.view.View3D;
import net.venaglia.realms.common.view.View3DMainLoop;
import net.venaglia.realms.common.view.ViewEventHandler;
import org.lwjgl.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
* User: ed
* Date: 2/28/13
* Time: 6:02 PM
*/
public class GeoViewer implements ViewEventHandler, View3DMainLoop {

    private final double radius;
    private final Map<Integer,Set<Acre>> fillAcresBySector;
    private final Set<Acre> edgeAcres;
    private final Camera camera;
    private final Light[] lights;

    private DisplayList[] fillAcres;
    private DisplayList acres;
    private DisplayList sphere;
    private double t = 0.0;
    private int show = 20;

    public GeoViewer(Globe globe, double radius, Camera camera) {
        this.radius = radius;
        this.fillAcresBySector = new TreeMap<Integer,Set<Acre>>();
        this.edgeAcres = new TreeSet<Acre>(AbstractCartographicElement.ELEMENT_ORDER);
        this.camera = camera;
        for (int i = 0; i < 20; i++) {
            Set<Acre> fillAcres = new TreeSet<Acre>(AbstractCartographicElement.ELEMENT_ORDER);
            this.fillAcresBySector.put(i, fillAcres);
            GlobalSector gs = globe.sectors[i];
            for (Sector s : gs.getSectors()) {
                for (SpatialMap.Entry<Acre> entry : s.acres) {
                    Acre a = entry.get();
                    if (a.flavor == Acre.Flavor.MULTI_SECTOR) {
                        this.edgeAcres.add(a);
                    } else {
                        fillAcres.add(a);
                    }
                }
            }
        }
        Point source = new Point(radius * -0.5, radius * -3.0, radius * 0.25);
        lights = new Light[]{ new FixedPointSourceLight(source) };
    }

    public void handleInit() {
        List<DisplayList> sectorDisplayLists = new ArrayList<DisplayList>((int)SECTORS.get());
        int i = 1;
        for (final Set<Acre> acres : fillAcresBySector.values()) {
//                for (int side = 0; side < 6; side++) {
//                    DisplayList list = new DisplayListBuffer("Fill Acres for Sector (" + ("abcdef".charAt(side)) + ") " + i++);
//                    list.record(new MyDisplayListRecorder(acres, side));
//                    sectorDisplayLists.add(list);
//                }
            DisplayList list = new DisplayListBuffer("Fill Acres for Sector " + i++);
            list.record(new MyDisplayListRecorder(acres));
            sectorDisplayLists.add(list);
        }
        this.fillAcres = sectorDisplayLists.toArray(new DisplayList[sectorDisplayLists.size()]);
        this.show = fillAcres.length;
        this.acres = new DisplayListBuffer("Edge Acres");
        this.acres.record(new MyDisplayListRecorder(edgeAcres));
        this.sphere = new DisplayListBuffer("Sphere");
        this.sphere.record(new DisplayList.DisplayListRecorder() {
            public void record(GeometryBuffer buffer) {
                GeodesicSphere sphere = new GeodesicSphere(30).scale(2000.0);
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
                        show = (show + fillAcres.length) % (fillAcres.length + 1);
                        System.out.println("Showing " + show);
                    }
                },
                new KeyHandler(KEY_RBRACKET) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        show = (show + 1) % (fillAcres.length + 1);
                        System.out.println("Showing " + show);
                    }
                },
                new KeyHandler(KEY_A) {
                    @Override
                    protected void handleKeyDown(int keyCode) {
                        show = -1;
                    }
                }
        );
    }

    double scale = 1.0;

    public boolean beforeFrame(long nowMS) {
        t += 0.002 * scale;
        double cx = Math.sin(t) * radius * 5.0;
        double cy = Math.cos(t) * radius * -5.0;
        Point cameraPosition = new Point(cx, cy, radius * 2.0);
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
        buffer.applyBrush(Brush.POINTS);
        buffer.color(Color.GRAY_50);
        sphere.project(nowMS, buffer);

        buffer.applyBrush(Brush.FRONT_SHADED);
        buffer.color(Color.RED);
        DisplayList list;
        if (show == -1) {
            acres.project(nowMS, buffer);
            for (DisplayList l : fillAcres) {
                l.project(nowMS, buffer);
            }
        } else {
            if (show >= fillAcres.length) {
                list = acres;
            } else {
                list = fillAcres[show];
            }
            list.project(nowMS, buffer);
        }
    }

    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
    }

    public void afterFrame(long nowMS) {
    }

    public static void view(Globe globe, double radius, String windowTitle, Dimension windowSize) {
        Camera camera = new Camera();
        camera.setPosition(new Point(0.0, radius * -2.0, 0.0));
        camera.setDirection(new Vector(0.0, radius * 2.0, 0.0));
        camera.setRight(new Vector(radius * 1.25, 0.0, 0.0));
        camera.computeClippingDistances(new BoundingSphere(Point.ORIGIN, radius * 1.1));
        GeoViewer view = new GeoViewer(globe, radius, camera);
        View3D view3D = new View3D(windowSize);
        view.registerKeyHandlers(view3D);
        view3D.setTitle(windowTitle);
        view3D.setCamera(camera);
        view3D.addViewEventHandler(view);
        view3D.setMainLoop(view);
        view3D.start();
    }

    private class MyDisplayListRecorder implements DisplayList.DisplayListRecorder {

        private final Set<Acre> acres;
        private final int side;

        public MyDisplayListRecorder(Set<Acre> acres, int side) {
            this.acres = acres;
            this.side = side;
        }

        public MyDisplayListRecorder(Set<Acre> acres) {
            this(acres, -1);
        }

        public void record(GeometryBuffer buffer) {
            for (Acre a : acres) {
                Shape shape = side == -1 ? a.get3DShape(radius) : a.get3DShape(radius, side);
                shape.project(0, buffer);
            }
        }
    }
}
