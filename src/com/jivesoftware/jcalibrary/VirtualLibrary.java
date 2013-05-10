package com.jivesoftware.jcalibrary;

import com.jivesoftware.jcalibrary.objects.Library;
import com.jivesoftware.jcalibrary.objects.Objects;
import com.jivesoftware.jcalibrary.structures.ServerRack;
import com.jivesoftware.jcalibrary.structures.ServerSlot;
import net.venaglia.realms.common.navigation.Position;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.complex.Origin;
import net.venaglia.realms.common.physical.geom.detail.DetailBanner;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetail;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.view.KeyboardManager;
import net.venaglia.realms.common.view.MouseTarget;
import net.venaglia.realms.common.view.MouseTargetEventListener;
import net.venaglia.realms.common.view.MouseTargets;
import net.venaglia.realms.common.navigation.UserNavigation;
import net.venaglia.realms.common.view.View3D;
import net.venaglia.realms.common.view.View3DMainLoop;
import net.venaglia.realms.common.view.ViewEventHandler;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 3:52 PM
 */
public class VirtualLibrary {

    private Library library;
    private DetailBanner detailBanner = new DetailBanner(null).scale(2).translate(new Vector(0,-0.5,5));
    private ServerRack[] serverRacks;
    private MouseTargets mouseTargets;
    private List<DynamicDetail<?>> demoObjects;
    private Origin origin = null; // new Origin(1.0);
    private AtomicReference<ServerSlot> hoverSlot = new AtomicReference<ServerSlot>();

    public VirtualLibrary() {
        final KeyboardManager keyboardManager = new KeyboardManager();
        final Camera camera = new Camera();
        final MouseTargetEventListener<ServerSlot> eventListener = new MouseTargetEventListener<ServerSlot>() {
            public void mouseOver(MouseTarget<? extends ServerSlot> target) {
                hoverSlot.set(target.getValue());
            }

            public void mouseOut(MouseTarget<? extends ServerSlot> target) {
                hoverSlot.compareAndSet(target.getValue(), null);
            }

            public void mouseClick(MouseTarget<? extends ServerSlot> target, MouseButton button) {
                // todo
            }
        };
        final UserNavigation userNavigation = new UserNavigation(keyboardManager, camera, new BoundingSphere(Point.ORIGIN, Math.sqrt(425))) {
            @Override
            public void handleInit() {
                super.handleInit();
                library = new Library();
                serverRacks = buildServerRacks(eventListener);
                mouseTargets = buildMouseTargets(serverRacks);
                demoObjects = new ArrayList<DynamicDetail<?>>(15);
                for (Objects objects : Objects.values()) {
                    if (objects.name().startsWith("DEMO_OBJECT_")) {
                        demoObjects.add(objects.getDynamicDetail());
                    }
                }
//                unbindMove(LOOK_UP);
//                unbindMove(LOOK_DOWN);
            }

            private double deltaZ = 0;

            {
                double faster = 3.0; // bump this to make moving faster
                position.cameraX = 13.5;
                position.cameraY = -13.5;
                position.cameraZ = 0.5;
                position.heading = Math.PI * 0.25;
                position.normalize();
                movePerSecond = 3.0 * faster;
                turnPerSecond = 50.0 * faster;
                pitchPerSecond = 25.0 * faster;
            }

            @Override
            protected void update(double elapsedSeconds, Position nextPosition) {
                super.update(elapsedSeconds, nextPosition);
                double r = Math.sqrt(nextPosition.cameraX * nextPosition.cameraX + nextPosition.cameraY * nextPosition.cameraY);
                double desiredZ = 0.5 - (r <= 3.8 ? 2 : r < 13.8 ? 1 : 0);
                if (nextPosition.cameraZ > desiredZ) {
                    deltaZ += 5 * elapsedSeconds;
                } else {
                    deltaZ = 0;
                }
                nextPosition.cameraZ = Math.max(nextPosition.cameraZ - deltaZ * elapsedSeconds, desiredZ);
                if (r > 18.8) {
                    nextPosition.cameraX *= 18.8 / r;
                    nextPosition.cameraY *= 18.8 / r;
                }
//                if ()
            }
        };
        final Light[] lights = {
                new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f)),
                new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f)),
                new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f))
        };
        final List<MouseTargets> testedMouseTargets = new ArrayList<MouseTargets>(4);
        final AtomicReference<MouseTarget<?>> activeMouseTarget = new AtomicReference<MouseTarget<?>>();
        View3D view = new View3D(new Dimension(1600, 1024)) {
            @Override
            protected void userRenderTargets(ProjectionBuffer buffer,
                                             GeometryBuffer targetBuffer,
                                             long now,
                                             MouseTargets targets) {
                super.userRenderTargets(buffer, targetBuffer, now, targets);
//                testedMouseTargets.add(targets);
            }

            @Override
            protected <V> void fireMouseEvent(MouseTarget<V> target,
                                              MouseTargetEventListener.MouseButton button,
                                              int event) {
                super.fireMouseEvent(target, button, event);
                if (event == 2) {
                    activeMouseTarget.set(target);
                } else if (event == 1) {
                    activeMouseTarget.compareAndSet(target, null);
                }
            }
        };

        view.setTitle("JCA Virtual Library");
        view.addViewEventHandler(new ViewEventHandler() {
            public void handleInit() {
                GL11.glEnable(GL11.GL_BLEND);
                Objects.SERVER_RACK.getDynamicDetail().setDetailListener(detailBanner);
            }

            public void handleClose() {
                System.exit(0);
            }

            public void handleNewFrame(long now) {
            }
        });
        view.addViewEventHandler(userNavigation);
        view.addKeyboardEventHandler(keyboardManager);
        view.setMainLoop(new View3DMainLoop() {
            public boolean beforeFrame(long nowMS) {
                return true;
            }

            public MouseTargets getMouseTargets(long nowMS) {
                testedMouseTargets.clear();
                return mouseTargets;
            }

            public void renderFrame(long nowMS, ProjectionBuffer buffer) {
                buffer.useLights(lights);
                if (origin != null) {
                    origin.project(nowMS, buffer);
                }
                library.project(nowMS, buffer);
//                for (DynamicDetail<?> demoObject : demoObjects) {
//                    demoObject.project(nowMS, buffer);
//                }
                for (ServerRack serverRack : serverRacks) {
                    serverRack.project(nowMS, buffer);
                }
                if (detailBanner != null) {
                    buffer.pushTransform();
                    detailBanner.project(nowMS, buffer);
                    buffer.rotate(Axis.Z, Math.PI * 0.666667);
                    detailBanner.project(nowMS, buffer);
                    buffer.rotate(Axis.Z, Math.PI * 0.666667);
                    detailBanner.project(nowMS, buffer);
                    buffer.popTransform();
                }
                ServerSlot hoverSlot = VirtualLibrary.this.hoverSlot.get();
                if (hoverSlot != null) {
                    buffer.pushTransform();
                    hoverSlot.getServerRack().getTransformation().apply(nowMS, buffer);
                    hoverSlot.getTransformation().apply(nowMS, buffer);
                    Objects.BOX_CURSOR_SMALL.project(nowMS, buffer);
                    buffer.popTransform();
                }
                if (!testedMouseTargets.isEmpty()) {
                    buffer.pushBrush();
                    buffer.applyBrush(Brush.WIRE_FRAME);
                    MouseTarget<?> markMouseTarget = activeMouseTarget.get();
                    for (MouseTargets targets : testedMouseTargets) {
                        for (MouseTarget<?> target : targets) {
                            buffer.color(target == markMouseTarget ? Color.WHITE : Color.YELLOW);
                            target.getProjectableObject().project(nowMS, buffer);
                        }
                    }
                    buffer.popBrush();
                }
            }

            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            public void afterFrame(long nowMS) {
            }
        });
        view.setCamera(camera);
        view.setDefaultBrush(Brush.TEXTURED);
        view.start();
    }

    private ServerRack[] buildServerRacks(MouseTargetEventListener<ServerSlot> eventListener) {
        ServerRack[] serverRacks = new ServerRack[40];
        Vector translate = Vector.Y.scale(19.625);
        Shape<?> target = Objects.SERVER_RACK.getTarget().scale(0.3).translate(translate);
        for (int i = 0, l = serverRacks.length; i < l; i++) {
            double angle = Math.PI * 2.0 * i / l;
            Shape<?> thisTarget = target.rotate(Axis.Z, angle);
            thisTarget.setMaterial(Material.INHERIT);
            ServerRack serverRack = new ServerRack(thisTarget, eventListener, i);
            serverRack.getTransformation().scale(0.3);
            serverRack.getTransformation().translate(translate);
            serverRack.getTransformation().rotate(Axis.Z, angle);
            serverRacks[i] = serverRack;
        }
        return serverRacks;
    }

    private MouseTargets buildMouseTargets(ServerRack[] serverRacks) {
        MouseTargets mouseTargets = new MouseTargets();
        for (ServerRack serverRack : serverRacks) {
            mouseTargets.add(serverRack.getMouseTarget());
        }
        return mouseTargets;
    }

    public static void main(String[] args) {
        new VirtualLibrary();
    }
}