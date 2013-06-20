package net.venaglia.gloo.demo;

import net.venaglia.common.util.Consumer;
import net.venaglia.gloo.navigation.Position;
import net.venaglia.gloo.navigation.UserNavigation;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.decorators.*;
import net.venaglia.gloo.physical.geom.Axis;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Shape;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.ZMap;
import net.venaglia.gloo.physical.geom.complex.BlenderObject;
import net.venaglia.gloo.physical.geom.complex.Mesh;
//import net.venaglia.gloo.physical.geom.complex.Origin;
import net.venaglia.gloo.physical.lights.FixedPointSourceLight;
import net.venaglia.gloo.physical.lights.Light;
import net.venaglia.gloo.physical.texture.Texture;
import net.venaglia.gloo.physical.texture.TextureFactory;
import net.venaglia.gloo.physical.texture.TextureMapping;
import net.venaglia.gloo.physical.texture.mapping.MatrixMapping;
import net.venaglia.gloo.physical.texture.mapping.SequenceMapping;
import net.venaglia.gloo.projection.Camera;
import net.venaglia.gloo.projection.DisplayList;
import net.venaglia.gloo.projection.GeometryRecorder;
import net.venaglia.gloo.projection.RecordingBuffer;
import net.venaglia.gloo.projection.GeometryBuffer;
import net.venaglia.gloo.projection.ProjectionBuffer;
import net.venaglia.gloo.projection.Transformable;
import net.venaglia.gloo.projection.camera.IsometricCamera;
import net.venaglia.gloo.projection.impl.DisplayListBuffer;
import net.venaglia.gloo.util.debug.OutputGraph;
import net.venaglia.gloo.util.matrix.Matrix_4x4;
import net.venaglia.gloo.view.KeyboardManager;
import net.venaglia.gloo.view.MouseTargets;
import net.venaglia.gloo.view.View3D;
import net.venaglia.gloo.view.View3DMainLoop;
import net.venaglia.gloo.view.ViewEventHandler;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * User: ed
 * Date: 6/6/13
 * Time: 5:54 PM
 */
public class IsometricMazeDemo {

    public static class Maze {

        private Map<Coord,MazeObject> map;

        private final int maxX;
        private final int maxY;
        private final Random rnd = new Random();
        private final OutputGraph debug;

        private boolean displayListsInitialized;
        private Map<MazeElementType,DisplayList> displayLists;

        public Maze(int maxX, int maxY, OutputGraph debug) {
            this.maxX = maxX;
            this.maxY = maxY;
            this.debug = debug;
            if (debug != null) {
                map = new HashMap<Coord,MazeObject>(1024) {
                    @Override
                    public void clear() {
                        Maze.this.debug.clear();
                        super.clear();
                    }

                    @Override
                    public MazeObject put(Coord key, MazeObject value) {
                        Maze.this.debug.addLine(Color.WHITE, key.getDebugPoints());
                        return super.put(key, value);
                    }
                };
            } else {
                map = new HashMap<Coord,MazeObject>(1024);
            }
        }

        public void generate() {
            map.clear();
            perimeter();
            for (int consecutiveFailCount = 0; consecutiveFailCount < 100; consecutiveFailCount++) {
                Coord coord = new Coord(rnd.nextInt(maxX) << 1, rnd.nextInt(maxY) << 1);
                if (!map.containsKey(coord)) {
                    consecutiveFailCount = 0; // reset
                    randomWall(coord);
                }
            }
            for (int j = 1; j < maxY; j++) {
                for (int i = 1; i < maxX; i++) {
                    Coord coord = new Coord(i << 1, j << 1);
                    if (!map.containsKey(coord)) {
                        Dir dir = Dir.values()[rnd.nextInt(4)];
                        map.put(coord, new MazeObject(coord));
                        coord = coord.go(dir);
                        map.put(coord, new MazeObject(coord));
                        coord = coord.go(dir);
                        if (!map.containsKey(coord)) {
                            map.put(coord, new MazeObject(coord));
                        }
                    }
                }
            }
        }

        public Collection<Coord> getPostCoords() {
            Collection<Coord> buffer = new ArrayList<Coord>();
            for (Map.Entry<Coord,MazeObject> entry : map.entrySet()) {
                MazeObject mazeObject = entry.getValue();
                if (entry.getKey().isPost() && mazeObject != null) {
                    mazeObject.init();
                    if (mazeObject.obj != null) {
                        buffer.add(mazeObject.coord);
                    }
                }
            }
            return buffer;
        }

        public Collection<Coord> getWallCoords(boolean includeNS, boolean includeEW) {
            Collection<Coord> buffer = new ArrayList<Coord>();
            for (Map.Entry<Coord,MazeObject> entry : map.entrySet()) {
                Coord coord = entry.getKey();
                if (includeNS && coord.isNorthSouth() || includeEW && coord.isEastWest()) {
                    entry.getValue().init();
                    buffer.add(entry.getValue().coord);
                }
            }
            return buffer;
        }

        public void initDisplayLists(MazeElementType which, GeometryRecorder recorder) {
            if (!displayListsInitialized) {
                displayLists = new EnumMap<MazeElementType,DisplayList>(MazeElementType.class);
                for (MazeElementType mazeElementType : MazeElementType.values()) {
                    displayLists.put(mazeElementType, new DisplayListBuffer(mazeElementType.modelName));
                }
                displayListsInitialized = true;
            }
            displayLists.get(which).record(recorder);
        }

        private void randomWall(Coord coord) {
            Coord start = coord;
            while (Math.abs(rnd.nextGaussian()) < 2.75) {
                Dir dir = Dir.values()[rnd.nextInt(4)];
                Coord wall = coord.go(dir);
                Coord post = wall.go(dir);
                if (map.containsKey(post)) {
                    continue;
                }
                if (start != null && !map.containsKey(start)) {
                    map.put(start, new MazeObject(start));
                    start = null;
                }
                map.put(wall, new MazeObject(wall));
                map.put(post, new MazeObject(post));
                coord = post;
                if (Math.abs(rnd.nextGaussian()) > 2.0) {
                    randomWall(coord);
                }
            }
        }

        private void line(Coord coord, Dir dir, int count) {
            if (!map.containsKey(coord)) {
                map.put(coord, new MazeObject(coord));
            }
            for (int i = 0; i < count; i++) {
                coord = coord.go(dir);
                if (!map.containsKey(coord)) {
                    map.put(coord, new MazeObject(coord));
                }
                coord = coord.go(dir);
                if (!map.containsKey(coord)) {
                    map.put(coord, new MazeObject(coord));
                }
            }
        }

        private void perimeter() {
            Coord topLeft = new Coord(0, 0);
            Coord bottomRight = new Coord(maxX << 1, maxY << 1);
            line(topLeft, Dir.E, maxX);
            line(topLeft, Dir.S, maxY);
            line(bottomRight, Dir.W, maxX);
            line(bottomRight, Dir.N, maxY);
        }

        private class MazeObject {
            private final Coord coord;

            private boolean initialized;
            private Transformable obj;
            private Transformable cap;

            private MazeObject(Coord coord) {
                this.coord = coord;
            }

            public void init() {
                if (initialized) {
                    return;
                }
                initialized = true;
                // rotate the standard E/W wall to be N/S
                boolean rotate90;
                MazeElementType objType;
                MazeElementType capType;
                if (coord.isPost()) {
                    int neighborWalls = (map.containsKey(coord.go(Dir.N)) ? 8 : 0) +
                                        (map.containsKey(coord.go(Dir.E)) ? 4 : 0) +
                                        (map.containsKey(coord.go(Dir.S)) ? 2 : 0) +
                                        (map.containsKey(coord.go(Dir.W)) ? 1 : 0);
                    switch (neighborWalls) {
                        case 0:
                        case 5:
                        case 10:
                            return; // no post
                    }
                    rotate90 = false;
                    objType = MazeElementType.POST;
                    capType = MazeElementType.POST_CAP;
                } else {
                    rotate90 = coord.isNorthSouth();
                    objType = MazeElementType.WALL;
                    capType = MazeElementType.WALL_CAP;
                }
                obj = displayLists.get(objType).transformable();
                cap = displayLists.get(capType).transformable();
                if (rotate90) {
                    obj.getTransformation().rotate(Axis.Z, Math.PI * 0.5);
                    cap.getTransformation().rotate(Axis.Z, Math.PI * 0.5);
                }
                obj.getTransformation().translate(coord.getTranslateVector());
                cap.getTransformation().translate(coord.getTranslateVector());
            }
        }

    }

    private enum MazeElementType {
        WALL("wall"), POST("post"), WALL_CAP("wall-cap"), POST_CAP("post-cap");

        private final String modelName;

        private MazeElementType(String modelName) {
            this.modelName = modelName;
        }
    }

    private enum Dir {
        N(0, -1), S(0, 1), E(1, 0), W(-1, 0);

        private final int dx, dy;

        private Dir(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    private static class Coord {
        final int x;
        final int y;

        private Vector translateVector;

        private Coord(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Coord go(Dir dir) {
            return new Coord(x + dir.dx, y + dir.dy);
        }

        public Vector getTranslateVector() {
            if (translateVector == null) {
                translateVector = new Vector(x, y, 0);
            }
            return translateVector;
        }

        public boolean isPost() {
            return x % 2 == 0 && y % 2 == 0;
        }

        public double[] getDebugPoints() {
            if (isPost()) {
                // post
                return box(0.15, 0.15);
            } else if (isEastWest()) {
                // e-w wall
                return box(2, 0.05);
            } else {
                // n-s wall
                return box(0.05, 2);
            }
        }

        public boolean isEastWest() {
            return x % 2 == 1 && y % 2 == 0;
        }

        public boolean isNorthSouth() {
            return x % 2 == 0 && y % 2 == 1;
        }

        private double[] box(double w, double h) {
            w *= 0.5;
            h *= 0.5;
            double x1 = x - w, y1 = y - h;
            double x2 = x + w, y2 = y + h;
            return new double[]{
                    Math.min(x1,x2), Math.min(y1,y2),
                    Math.max(x1,x2), Math.min(y1,y2),
                    Math.max(x1,x2), Math.max(y1,y2),
                    Math.min(x1,x2), Math.max(y1,y2),
                    Math.min(x1,x2), Math.min(y1,y2)
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Coord coord = (Coord)o;
            return x == coord.x && y == coord.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

        @Override
        public String toString() {
            return String.format("(%03d,%03d)", x, y);
        }
    }

    private static class Recorder implements GeometryRecorder {

        private final BlenderObject obj;
        private final TextureMapping mapping;
        private final Texture texture;

        private Recorder(MazeElementType mazeElementType, Texture texture) {
            try {
                this.texture = texture;
                String resource = "models/" + mazeElementType.modelName + ".model";
                InputStream objIn = Thread.currentThread()
                                          .getContextClassLoader()
                                          .getResourceAsStream(resource);
                SequenceMapping.Recorder recorder = SequenceMapping.record();
                BlenderObject obj = new BlenderObject(new InputStreamReader(objIn), BlenderObject.NormalSource.FROM_FILE, recorder);
                if (mazeElementType == MazeElementType.WALL || mazeElementType == MazeElementType.WALL_CAP) {
                    obj = obj.scale(new Vector(1, 2, 2));
                } else {
                    obj = obj.scale(2);
                }
                this.obj = obj;
                mapping = recorder.done();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void record(RecordingBuffer buffer) {
//            buffer.setRecordTextureBindings(false);
            buffer.setBrush(Brush.TEXTURED);
            buffer.useTexture(texture, mapping);
            obj.project(0, buffer);
        }
    }

    public static class Demo {

        private final Maze maze;
        private final OutputGraph debug;

        private Camera camera;
        private View3D view;
        private UserNavigation navigation;
        private Texture brickTexture;
        private Texture creteTexture;

        public Demo(Maze maze, OutputGraph debug) {
            this.maze = maze;
            this.debug = debug;
        }

        public void start() {
            camera = new IsometricCamera(new Point(0,0,15), IsometricCamera.HEADING_POS_X_POS_Y, Math.PI * -0.1666, 6);
    //        camera = new OrthagonalCamera();
    //        camera = new PerspectiveCamera();
            view = new View3D(1024,768);
            view.setTitle("Isometric Demo" + 3);
    //        final Shape<?> floor = new QuadStrip(new Point(-100, 100, 0), new Point(-100, -100, 0), new Point(100, -100, 0), new Point(100, 100, 0));
            ZMap zMap = new ZMap(new Rectangle(128, 128));
    //        Random rnd = new Random();
    //        for (int j = 0; j < 64; j++) {
    //            for (int i = 0; i < 64; i++) {
    //                zMap.setZ(i, j, rnd.nextGaussian() * 0.1);
    //            }
    //        }
            final Shape<?> floor = new Mesh(zMap).translate(new Vector(-63.5, -63.5, 0)).flip();
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
                                           Set<Move> allActiveMoves) {
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
//            final Origin origin = new Origin(3);
            final Light[] lights = {
                    new FixedPointSourceLight(new Point(1.1f, 5.0f, 3.5f).scale(50)),
                    new FixedPointSourceLight(new Point(-2.1f, 0.0f, 1.5f).scale(50)),
                    new FixedPointSourceLight(new Point(-0.1f, -4.0f, -2.5f).scale(50))
            };
            final Map<String,Collection<Coord>> projectables = new HashMap<String,Collection<Coord>>();
            final Map<MazeElementType,TextureMapping> textureMappings = new EnumMap<MazeElementType,TextureMapping>(MazeElementType.class);
            final Map<MazeElementType,BlenderObject> objects = new EnumMap<MazeElementType,BlenderObject>(MazeElementType.class);
            final Map<MazeElementType,Texture> textures = new EnumMap<MazeElementType,Texture>(MazeElementType.class);
            view.addViewEventHandler(new ViewEventHandler() {

                public void handleInit() {
                    Consumer<BufferedImage> imageConsumer = null;
                    if (debug != null) {
                        imageConsumer = new Consumer<BufferedImage>() {

                            int x = 0;

                            public void consume(BufferedImage value) {
                                debug.addImage(null, value, null, x, 0);
                                x += 160;
                            }
                        };
                    }
                    brickTexture = new TextureFactory().loadClasspathResource("images/brick_texture.jpg").setMipMapped(true).setScale(128,128).captureImage(imageConsumer).build();
                    creteTexture = new TextureFactory().loadClasspathResource("images/granite_texture.jpg").setMipMapped(true).setScale(128,128).captureImage(imageConsumer).build();
                    brickTexture.load(); // ensure this is loaded before recording a display list
                    creteTexture.load(); // ensure this is loaded before recording a display list
                    maze.generate();
                    maze.initDisplayLists(MazeElementType.WALL, newRecorder(MazeElementType.WALL, brickTexture));
                    maze.initDisplayLists(MazeElementType.POST, newRecorder(MazeElementType.POST, brickTexture));
                    maze.initDisplayLists(MazeElementType.WALL_CAP, newRecorder(MazeElementType.WALL_CAP, creteTexture));
                    maze.initDisplayLists(MazeElementType.POST_CAP, newRecorder(MazeElementType.POST_CAP, creteTexture));
                    projectables.put("wall-ns", maze.getWallCoords(true, false));
                    projectables.put("wall-ew", maze.getWallCoords(false, true));
                    projectables.put("post", maze.getPostCoords());
                }

                private Recorder newRecorder(MazeElementType mazeElementType, Texture texture) {
                    Recorder recorder = new Recorder(mazeElementType, texture);
                    textureMappings.put(mazeElementType, recorder.mapping);
                    textures.put(mazeElementType, texture);
                    objects.put(mazeElementType, recorder.obj);
                    return recorder;
                }

                public void handleClose() {
                    System.exit(0);
                }

                public void handleNewFrame(long now) {
                    // no-op
                }
            });
            view.setMainLoop(new View3DMainLoop() {

                private final Map<MazeElementType, String[]> projectableKeys;

                {
                    projectableKeys = new EnumMap<MazeElementType, String[]>(MazeElementType.class);
                    String[] wallProjectables = { "wall-ns", "wall-ew" };
                    String[] postProjectables = { "post" };
                    projectableKeys.put(MazeElementType.WALL, wallProjectables);
                    projectableKeys.put(MazeElementType.WALL_CAP, wallProjectables);
                    projectableKeys.put(MazeElementType.POST, postProjectables);
                    projectableKeys.put(MazeElementType.POST_CAP, postProjectables);
                }

                public boolean beforeFrame(long nowMS) {
                    return true;
                }

                public MouseTargets getMouseTargets(long nowMS) {
                    return null;
                }

                public void renderFrame(long nowMS, ProjectionBuffer buffer) {
//                    origin.project(nowMS, buffer);
                    floor.project(nowMS, buffer);
                    BlenderObject obj;
                    buffer.applyBrush(Brush.TEXTURED);
//                    buffer.applyBrush(Brush.WIRE_FRAME);
                    buffer.useLights(lights);
                    for (MazeElementType mazeElementType : MazeElementType.values()) {
                        buffer.useTexture(textures.get(mazeElementType), textureMappings.get(mazeElementType));
                        obj = objects.get(mazeElementType);
                        for (String key : projectableKeys.get(mazeElementType)) {
                            boolean rotate90 = key.endsWith("-ns");
                            for (Coord c : projectables.get(key)) {
                                buffer.pushTransform();
                                buffer.translate(c.getTranslateVector());
                                if (rotate90) {
                                    buffer.rotate(Axis.Z, Math.PI * 0.5);
                                }
                                obj.project(nowMS, buffer);
                                buffer.popTransform();
                            }
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

    }

    public static void main(String[] args) {
//        final int size = 1;
//        final int size = 12;
        final int size = 32;

//        OutputGraph debug = new OutputGraph("maze", 1150, size, size, 480 / size);
        OutputGraph debug = null;

//        OutputGraph debug2 = new OutputGraph("maze", 384, 144, 64, 1);
        OutputGraph debug2 = null;

        Maze maze = new Maze(size, size, debug);
        Demo demo = new Demo(maze, debug2);
        demo.start();
    }
}
