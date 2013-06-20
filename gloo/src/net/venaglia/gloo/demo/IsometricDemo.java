package net.venaglia.gloo.demo;

import net.venaglia.gloo.navigation.Position;
import net.venaglia.gloo.navigation.UserNavigation;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.*;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.ZMap;
import net.venaglia.gloo.physical.geom.complex.BlenderObject;
import net.venaglia.gloo.physical.geom.complex.Mesh;
import net.venaglia.gloo.physical.geom.complex.Origin;
import net.venaglia.gloo.physical.lights.FixedPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.physical.texture.mapping.MatrixMapping;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.camera.IsometricCamera;
import net.venaglia.gloo.util.matrix.Matrix_4x4;
import net.venaglia.gloo.view.KeyboardManager;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.NativeBinariesInstaller;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import org.lwjgl.input.Keyboard;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Set;

/**
 * User: ed
 * Date: 5/27/13
 * Time: 5:56 PM
 */
public class IsometricDemo {

    static {
        System.setProperty("org.lwjgl.util.Debug", "true");
        new NativeBinariesInstaller().installIfRunningFromJar();
    }

    private Camera camera;
    private UserNavigation navigation;
    private View3D view;

    private final Collection<Shape<?>> shapesToDraw;

    public IsometricDemo(Collection<Shape<?>> shapesToDraw) {
        this.shapesToDraw = shapesToDraw;
    }

    public void start() {
        camera = new IsometricCamera(new Point(0,0,15), IsometricCamera.HEADING_POS_X_POS_Y, Math.PI * -0.1666, 6);
//        camera = new OrthagonalCamera();
//        camera = new PerspectiveCamera();
        view = new View3D(1024,768);
        view.setTitle("Isometric Demo");
//        final Shape<?> floor = new QuadStrip(new Point(-100, 100, 0), new Point(-100, -100, 0), new Point(100, -100, 0), new Point(100, 100, 0));
        ZMap zMap = new ZMap(new Rectangle(64, 64));
//        Random rnd = new Random();
//        for (int j = 0; j < 64; j++) {
//            for (int i = 0; i < 64; i++) {
//                zMap.setZ(i, j, rnd.nextGaussian() * 0.1);
//            }
//        }
        final Shape<?> floor = new Mesh(zMap).translate(new Vector(-31.5, -31.5, 0)).flip();
//        Texture texture = new TextureFactory().loadClasspathResource("images/hex-grid-256.png").build();
//        TextureMapping mapping = new MatrixMapping(Matrix_4x4.scale(new Vector(7.0/16.0, 1, 1)));
        Texture texture = new TextureFactory().loadClasspathResource("images/grid-256.png").setMipMapped(true).build();
        TextureMapping mapping = new MatrixMapping(Matrix_4x4.scale(new Vector(1, 1, 1)));
        floor.setMaterial(Material.makeTexture(texture, mapping));
//        floor.setMaterial(Material.makeWireFrame(Color.WHITE));
//        floor.setMaterial(Material.makeFrontShaded(new Color(0.1f,0.3f,0.5f)));
//        floor.setMaterial(Material.makeFrontShaded(Color.WHITE));
        final KeyboardManager keyboardManager = new KeyboardManager();
        final BoundingBox bounds = new BoundingBox(new Point(-100, -100, -1), new Point(100, 100, 1));
//        final Shape<?> extraShape = new Teapot().scale(2);
        final Shape<?> extraShape = loadBunny();
//        extraShape.setMaterial(Material.makeWireFrame(Color.CYAN));
        extraShape.setMaterial(Material.makeFrontShaded(new Color(1.0f, 0.95f, 0.85f)));
//        camera.setPosition(new Point(-10, -10, -5));
        camera.setPosition(new Point(-15, -15, 15));
//        camera.setDirection(new Vector(0, 0, -5).scale(50));
//        camera.setRight(Vector.X.scale(100));
        camera.computeClippingDistances(bounds);
        view.setCamera(camera);
        navigation = new UserNavigation(keyboardManager, camera, bounds, UserNavigation.KeyboardPreset.OVERHEAD) {

            {
                Point cameraPosition = camera.getPosition();
                position.cameraX = cameraPosition.x;
                position.cameraY = cameraPosition.y;
                position.cameraZ = cameraPosition.z;
                position.moveX = 1;
                position.moveY = 1;
                position.heading = Math.PI * -0.25; // -45 deg
                position.pitch = Math.PI * -1.6666; // -30 deg
                position.fov = camera.getRight().l;
                bindMoveToKey(new Move() {
                    public void update(double elapsedSeconds,
                                       Position position,
                                       Set<UserNavigation.Move> allActiveMoves) {
                        double fovExp = Math.log(position.fov);
                        fovExp -= pitchPerSecond * 0.0009765625;
                        position.fov = Math.exp(fovExp);

                    }
                }, Keyboard.KEY_LBRACKET);
                bindMoveToKey(new Move() {
                    public void update(double elapsedSeconds,
                                       Position position,
                                       Set<UserNavigation.Move> allActiveMoves) {
                        double fovExp = Math.log(position.fov);
                        fovExp += pitchPerSecond * 0.0009765625;
                        position.fov = Math.exp(fovExp);

                    }
                }, Keyboard.KEY_RBRACKET);
            }

            @Override
            protected void update(double elapsedSeconds, Position nextPosition) {
                super.update(elapsedSeconds, nextPosition);
                nextPosition.cameraZ = position.cameraZ;
                nextPosition.heading = position.heading;
                nextPosition.pitch = position.pitch;
                nextPosition.roll = position.roll;
                nextPosition.moveX = position.moveX;
                nextPosition.moveY = position.moveY;
                nextPosition.moveZ = 0;
            }
        };
        view.addViewEventHandler(navigation);
        view.addKeyboardEventHandler(keyboardManager);
        final Origin origin = new Origin(3);
        final Light[] lights = {
                new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f).scale(50)),
                new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f).scale(50)),
                new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f).scale(50))
        };

        view.setMainLoop(new View3DMainLoop() {

            public boolean beforeFrame(long nowMS) {
                int slice = (int)(nowMS % 20000);
                if (slice > 10000) {
                    slice = 20000 - slice;
                }
                double p = 0;//slice / 10000.0;
//                ((IsometricCamera)camera).setHeading(p * IsometricCamera.HEADING_POS_X_POS_Y);
//                ((IsometricCamera)camera).setPitch(Math.PI * 0.5 - IsometricCamera.PITCH_DOWN_60 * p);
//                ((IsometricCamera)camera).setScale(75.0 * p + 25.0);
//                camera.computeClippingDistances(bounds);
                return true;
            }

            public MouseTargets getMouseTargets(long nowMS) {
                return null;
            }

            public void renderFrame(long nowMS, ProjectionBuffer buffer) {
                buffer.useLights(lights);
                floor.project(nowMS, buffer);
                if (shapesToDraw == null || shapesToDraw.isEmpty()) {
                    origin.project(nowMS, buffer);
                    extraShape.project(nowMS, buffer);
                } else {
                    for (Shape<?> shape : shapesToDraw) {
                        shape.project(nowMS, buffer);
                    }
                }
            }

            public void renderOverlay(long nowMS, GeometryBuffer buffer) {
            }

            public void afterFrame(long nowMS) {
            }
        });
        view.start();
    }

    private Shape<?> loadBunny() {
        try {
            InputStream objIn = Thread.currentThread().getContextClassLoader().getResourceAsStream("models/bunny.model");
            return new BlenderObject(new InputStreamReader(objIn)).scale(0.5).rotate(Axis.X, Math.PI * 0.5).translate(new Vector(6, 6, 0));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new IsometricDemo(null).start();
    }
}
