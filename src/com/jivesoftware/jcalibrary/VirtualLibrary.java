package com.jivesoftware.jcalibrary;

import com.jivesoftware.jcalibrary.objects.Library;
import com.jivesoftware.jcalibrary.objects.LibraryBanner;
import com.jivesoftware.jcalibrary.objects.Objects;
import com.jivesoftware.jcalibrary.scheduler.InstanceDataFetcher;
import com.jivesoftware.jcalibrary.scheduler.WorkScheduler;
import com.jivesoftware.jcalibrary.structures.JiveInstance;
import com.jivesoftware.jcalibrary.structures.ServerRack;
import com.jivesoftware.jcalibrary.structures.ServerSlot;
import com.jivesoftware.jcalibrary.urgency.StandardUrgencyFilter;
import com.jivesoftware.jcalibrary.urgency.UrgencyFilter;
import com.jivesoftware.jcalibrary.util.Browser;
import net.venaglia.realms.common.navigation.Position;
import net.venaglia.realms.common.navigation.UserNavigation;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.Brush;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Shape;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.complex.Origin;
import net.venaglia.realms.common.physical.geom.detail.DynamicDetail;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.Camera;
import net.venaglia.realms.common.projection.GeometryBuffer;
import net.venaglia.realms.common.projection.ProjectionBuffer;
import net.venaglia.realms.common.view.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Dimension;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 4/12/13
 * Time: 3:52 PM
 */
public class VirtualLibrary {

    private Library library;
//    private DetailBanner detailBanner = new DetailBanner(null).scale(2).translate(new Vector(0,-0.5,5));
    private ServerRack[] serverRacks;
    private MouseTargets mouseTargets;
    private List<DynamicDetail<?>> demoObjects;
    private Origin origin = null; // new Origin(1.0);
    private AtomicReference<ServerSlot> hoverSlot = new AtomicReference<ServerSlot>();
    private AtomicReference<ServerSlot> selectedSlot = new AtomicReference<ServerSlot>();
    private View3D view;
    private LibraryHUD hud;

    public VirtualLibrary() {
        final KeyboardManager keyboardManager = new KeyboardManager() {

            @Override
            public void keyDown(int keyCode) {
                super.keyDown(keyCode);
                switch (keyCode) {
                    case Keyboard.KEY_F1:
                        applyUrgencyFilter(StandardUrgencyFilter.LOAD_AVERAGE);
                        break;
                    case Keyboard.KEY_F2:
                        applyUrgencyFilter(StandardUrgencyFilter.ACTIVE_CONNECTIONS);
                        break;
                    case Keyboard.KEY_F3:
                        applyUrgencyFilter(StandardUrgencyFilter.ACTIVE_SESSIONS);
                        break;
                    case Keyboard.KEY_F10:
                        ServerSlot slot = selectedSlot.get();
                        JiveInstance inst = slot == null ? null : slot.getJiveInstance();
                        if (inst != null) {
                            String url = LibraryProps.INSTANCE.getProperty(LibraryProps.JCA_CLIENT_VIEW_URL);
                            Browser.openURL(url.replace("{id}", inst.getCustomerInstallationId().toString()));
                        }
                        break;
                    case Keyboard.KEY_ESCAPE:
                        applyUrgencyFilter(StandardUrgencyFilter.NONE);
                        LibraryBanner.INSTANCE.setText("");
                        break;
                    case Keyboard.KEY_0: search('0'); break;
                    case Keyboard.KEY_1: search('1'); break;
                    case Keyboard.KEY_2: search('2'); break;
                    case Keyboard.KEY_3: search('3'); break;
                    case Keyboard.KEY_4: search('4'); break;
                    case Keyboard.KEY_5: search('5'); break;
                    case Keyboard.KEY_6: search('6'); break;
                    case Keyboard.KEY_7: search('7'); break;
                    case Keyboard.KEY_8: search('8'); break;
                    case Keyboard.KEY_9: search('9'); break;
                    case Keyboard.KEY_A: search('a'); break;
                    case Keyboard.KEY_B: search('b'); break;
                    case Keyboard.KEY_C: search('c'); break;
                    case Keyboard.KEY_D: search('d'); break;
                    case Keyboard.KEY_E: search('e'); break;
                    case Keyboard.KEY_F: search('f'); break;
                    case Keyboard.KEY_G: search('g'); break;
                    case Keyboard.KEY_H: search('h'); break;
                    case Keyboard.KEY_I: search('i'); break;
                    case Keyboard.KEY_J: search('j'); break;
                    case Keyboard.KEY_K: search('k'); break;
                    case Keyboard.KEY_L: search('l'); break;
                    case Keyboard.KEY_M: search('m'); break;
                    case Keyboard.KEY_N: search('n'); break;
                    case Keyboard.KEY_O: search('o'); break;
                    case Keyboard.KEY_P: search('p'); break;
                    case Keyboard.KEY_Q: search('q'); break;
                    case Keyboard.KEY_R: search('r'); break;
                    case Keyboard.KEY_S: search('s'); break;
                    case Keyboard.KEY_T: search('t'); break;
                    case Keyboard.KEY_U: search('u'); break;
                    case Keyboard.KEY_V: search('v'); break;
                    case Keyboard.KEY_W: search('w'); break;
                    case Keyboard.KEY_X: search('x'); break;
                    case Keyboard.KEY_Y: search('y'); break;
                    case Keyboard.KEY_Z: search('z'); break;
                    case Keyboard.KEY_BACK: search('\b'); break;
                    default:
                        StandardUrgencyFilter.SEARCH_FOR.set("");
                        break;
                }
            }

            private void search(char c) {
                if (c == '\0') {
                    StandardUrgencyFilter.SEARCH_FOR.set("");
                    LibraryBanner.INSTANCE.clear();
                } else if (c == '\b') {
                    String newValue = StandardUrgencyFilter.SEARCH_FOR.get();
                    if (newValue.length() > 1) {
                        newValue = newValue.substring(0, newValue.length() - 1);
                        LibraryBanner.INSTANCE.setText(newValue);
                        StandardUrgencyFilter.SEARCH_FOR.set(newValue);
                        applyUrgencyFilter(StandardUrgencyFilter.SEARCH_STRING);
                    }
                } else {
                    String newValue = StandardUrgencyFilter.SEARCH_FOR.get() + c;
                    LibraryBanner.INSTANCE.setText(newValue);
                    StandardUrgencyFilter.SEARCH_FOR.set(newValue);
                    applyUrgencyFilter(StandardUrgencyFilter.SEARCH_STRING);
                }
            }
        };
        final Camera camera = new Camera();
        final MouseTargetEventListener<ServerSlot> eventListener = new MouseTargetEventListener<ServerSlot>() {
            public void mouseOver(MouseTarget<? extends ServerSlot> target) {
                ServerSlot value = target.getValue();
                hoverSlot.set(value);
                JiveInstance jiveInstance = value.getJiveInstance();
                if (jiveInstance != null) {
                    Display.setTitle("JCA Virtual Library - " + jiveInstance.getCustomer().getName());
                } else {
                    Display.setTitle("JCA Virtual Library - empty slot");
                }
            }

            public void mouseOut(MouseTarget<? extends ServerSlot> target) {
                hoverSlot.compareAndSet(target.getValue(), null);
                Display.setTitle("JCA Virtual Library");
            }

            @Override
            public void mouseDown(MouseTarget<? extends ServerSlot> target, MouseButton button) {
                ServerSlot value = target.getValue();
                ServerSlot previousSlot = selectedSlot.getAndSet(value);
                if (previousSlot != null) {
                    previousSlot.getSlotTransformation().setTarget(1, 0);
                }
                JiveInstance jiveInstance = value.getJiveInstance();
                if (jiveInstance != null) {
                    LibraryBanner.INSTANCE.setText(jiveInstance.getCustomer().getDomain());
                    value.getSlotTransformation().setTarget(4, 8);
                } else {
                    LibraryBanner.INSTANCE.setText("");
                }
                hud.showJiveInstanceData(jiveInstance);
                long instanceID = jiveInstance != null ? jiveInstance.getCustomerInstallationId() : -1;
                System.out.printf("mouseDown on Rack[%d], Shelf[%d], Slot[%d], JiveInstance[%d]\n", value.getServerRack().getSeq(), value.getSeq() / 9 + 1, value.getSeq() % 9 + 1, instanceID);
            }

            @Override
            public void mouseUp(MouseTarget<? extends ServerSlot> target, MouseButton button) {
                ServerSlot value = target.getValue();
                JiveInstance jiveInstance = value.getJiveInstance();
                long instanceID = jiveInstance != null ? jiveInstance.getCustomerInstallationId() : -1;
                System.out.printf("mouseUp on Rack[%d], Shelf[%d], Slot[%d], JiveInstance[%d]\n", value.getServerRack().getSeq(), value.getSeq() / 9 + 1, value.getSeq() % 9 + 1, instanceID);
            }
        };
        this.serverRacks = buildServerRacks(eventListener);
        final Light[] lights = { null, null, null, null };
        final UserNavigation userNavigation = new UserNavigation(keyboardManager, camera, new BoundingSphere(Point.ORIGIN, Math.sqrt(425))) {
            @Override
            public void handleInit() {
                super.handleInit();
                library = new Library();
                LibraryBanner.INSTANCE.setLibrary(library);
                lights[0] = new FixedPointSourceLight(new Point(0,0,library.getCeilingHeight() + 1.5));
                for (int i = 1, l = lights.length; i < l; i++) {
                    lights[i] = new FixedPointSourceLight(Point.ORIGIN.translate(library.rotatedVector(i, l)));
                }
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
                double desiredZ = 0.5 - (r <= 4.2 ? -0.5 : r < 13.8 ? 0.5 : 0);
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
                hud.updateCamera(nextPosition);
            }
        };
        final List<MouseTargets> testedMouseTargets = new ArrayList<MouseTargets>(4);
        final AtomicReference<MouseTarget<?>> activeMouseTarget = new AtomicReference<MouseTarget<?>>();
        Dimension windowSize = new Dimension(1600, 1024);
//        Dimension windowSize = new Dimension(1024, 600);
        view = new View3D(windowSize) {
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
                if (event == 1) {
                    activeMouseTarget.set(target);
                } else if (event == 2) {
                    activeMouseTarget.compareAndSet(target, null);
                }
            }
        };

        view.setTitle("JCA Virtual Library");
        view.addViewEventHandler(new ViewEventHandler() {
            public void handleInit() {
                GL11.glEnable(GL11.GL_BLEND);
//                Objects.SERVER_RACK.getDynamicDetail().setDetailListener(detailBanner);
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
//                if (detailBanner != null) {
//                    buffer.pushTransform();
//                    detailBanner.project(nowMS, buffer);
//                    buffer.rotate(Axis.Z, Math.PI * 0.666667);
//                    detailBanner.project(nowMS, buffer);
//                    buffer.rotate(Axis.Z, Math.PI * 0.666667);
//                    detailBanner.project(nowMS, buffer);
//                    buffer.popTransform();
//                }
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
                LibraryBanner.INSTANCE.project(nowMS, buffer);
            }

            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            public void afterFrame(long nowMS) {
            }
        });
        view.setCamera(camera);
        view.setDefaultBrush(Brush.TEXTURED);
        JiveInstancesRegistry.getInstance().init(this);
        hud = new LibraryHUD(serverRacks);
    }

    public void start() {
        hud.show();
        view.start();
    }

    public ServerRack[] getServerRacks() {
        return serverRacks;
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
        Collections.reverse(Arrays.asList(serverRacks));
        return serverRacks;
    }

    private MouseTargets buildMouseTargets(ServerRack[] serverRacks) {
        MouseTargets mouseTargets = new MouseTargets();
        for (ServerRack serverRack : serverRacks) {
            mouseTargets.add(serverRack.getMouseTarget());
        }
        return mouseTargets;
    }

    private <BASELINE> void applyUrgencyFilter(UrgencyFilter<BASELINE> filter) {
        if (filter != StandardUrgencyFilter.SEARCH_STRING) {
            StandardUrgencyFilter.SEARCH_FOR.set("");
        }
        final List<ServerSlot> allServerSlots = new ArrayList<ServerSlot>(4096);
        for (ServerRack rack : serverRacks) {
            for (ServerSlot slot : rack.getSlots()) {
                JiveInstance instance = slot.getJiveInstance();
                if (instance == null) {
                    slot.getSlotTransformation().setTarget(1,0);
                } else {
                    allServerSlots.add(slot);
                }
            }
        }
        BASELINE baseLine = filter.buildBaseLine(new AbstractList<JiveInstance>() {
            @Override
            public JiveInstance get(int index) {
                return allServerSlots.get(index).getJiveInstance();
            }

            @Override
            public int size() {
                return allServerSlots.size();
            }
        });
        for (ServerSlot slot : allServerSlots) {
            filter.apply(slot.getJiveInstance(), slot.getSlotTransformation(), baseLine);
        }
    }

    public static void main(String[] args) throws Exception {
        LibraryProps.INSTANCE.getJCACredentials();
        VirtualLibrary virtualLibrary = new VirtualLibrary();
        WorkScheduler.once(new Runnable() {
            public void run() {
                WorkScheduler.interval(new InstanceDataFetcher(), 60, TimeUnit.SECONDS);
            }
        }, 3, TimeUnit.SECONDS);
        virtualLibrary.start();

//        Thread.sleep(18000000);
    }
}
