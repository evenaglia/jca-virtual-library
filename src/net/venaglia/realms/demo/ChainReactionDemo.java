package net.venaglia.realms.demo;

import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.decorators.Material;
import net.venaglia.realms.common.physical.geom.Axis;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.complex.Origin;
import net.venaglia.realms.common.physical.geom.detail.DetailLevel;
import net.venaglia.realms.common.physical.geom.primitives.Box;
import net.venaglia.realms.common.physical.geom.primitives.Sphere;
import net.venaglia.realms.common.physical.lights.FixedPointSourceLight;
import net.venaglia.realms.common.physical.lights.Light;
import net.venaglia.realms.common.projection.*;
import net.venaglia.realms.common.projection.impl.DisplayListBuffer;
import net.venaglia.realms.common.util.SpatialMap;
import net.venaglia.realms.common.util.impl.OctreeMap;
import net.venaglia.realms.common.view.MouseTargets;
import net.venaglia.realms.common.view.View3D;
import net.venaglia.realms.common.view.View3DMainLoop;
import net.venaglia.realms.common.view.ViewEventHandler;
import org.lwjgl.util.Dimension;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: ed
 * Date: 9/17/12
 * Time: 9:12 PM
 */
public class ChainReactionDemo implements View3DMainLoop, ViewEventHandler {

    private final ThreadFactory threadFactory = new ThreadFactory() {

        private final ThreadGroup tg = new ThreadGroup("work");
        private final AtomicInteger seq = new AtomicInteger();

        {
            tg.setDaemon(true);
        }

        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(tg, runnable, String.format("Worker-%02d", seq.incrementAndGet()));
            thread.setDaemon(true);
            return thread;
        }
    };
    private final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(1500);
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(16, 16, 5, TimeUnit.SECONDS, queue, threadFactory);
    private final double fps;
    private final boolean showVoxels;

//    private SpatialMap<Ball> map = new SweepAndPrune<Ball>();
    private SpatialMap<Ball> map = new OctreeMap<Ball>(new BoundingBox(new Point(-20,-20,-20), new Point(20,20,20)));
    @SuppressWarnings("unchecked")
    private SpatialMap.Entry<Ball>[] ballEntries = (SpatialMap.Entry<Ball>[])new SpatialMap.Entry[201];
    private Origin origin = new Origin(2.0);
    private Sphere sphere = new Sphere(DetailLevel.MEDIUM_LOW);
    private DisplayList sphereDisplayList = new DisplayListBuffer("Sphere");
    private Box box = new net.venaglia.realms.common.physical.geom.primitives.Box().scale(40);
    private Light[] lights = { new FixedPointSourceLight(new Point(0,0,75)) };
    private Simulator[] simulators = new Simulator[201];
    private AdvanceFrame advanceFrame;
    private Vector gravity = new Vector(0.0,0.0,-0.0098);
    private AtomicInteger processCounter = new AtomicInteger();
    private AtomicBoolean pendingFrame = new AtomicBoolean();
    private AtomicLong moveCount = new AtomicLong();
    private long lastMoveCount = -1;
    private final Set<Collision> collisions = Collections.synchronizedSet(new HashSet<Collision>());
    private Camera camera;
    private double a = Math.PI, b = 0.1;

    public boolean beforeFrame(long nowMS) {
//        for (Runnable simulator : simulators) {
//            simulator.run();
//        }
//        pendingFrame.set(false);
//        processCounter.addAndGet(map.size());
//        for (SpatialMap.Entry<Ball> entry : map) {
//            entry.get().advanceNext();
//        }
        double z = Math.sin(b);
        double x = Math.sin(a) * (1.0 - z);
        double y = Math.cos(a) * (1.0 - z);
        Point c = new Point(x,y,z).scale(107);
        camera.setPosition(c);
        Vector d = DemoObjects.toVector(c, Point.ORIGIN).normalize();
        camera.setDirection(d);
        camera.setRight(new Vector(0, 0, 1.0).cross(d).normalize(1.0));
        camera.computeClippingDistances(box.getBounds());
        a -= 0.0025;
        b += 0.00005;
        return true;
//
//        long currentMoveCount = moveCount.get();
//        if (lastMoveCount == currentMoveCount) {
//            return false;
//        }
//        lastMoveCount = currentMoveCount;
//        return true;
    }

    public MouseTargets getMouseTargets(long nowMS) {
        return null;
    }

    public synchronized void renderFrame(long nowMS, ProjectionBuffer buffer) {
//        long start = System.currentTimeMillis();
        buffer.useLights(lights);
        origin.project(nowMS, buffer);
        box.project(nowMS, buffer);
//        for (SpatialMap.Entry<Ball> entry : java.util.Collections.singleton(ballEntries[0])) {
        for (SpatialMap.Entry<Ball> entry : ballEntries) {
            Ball ball = entry.get();
            buffer.pushTransform();
            buffer.translate(new Vector(ball.x, ball.y, ball.z));
            ball.color.apply(nowMS, buffer);
            sphereDisplayList.project(nowMS, buffer);
            buffer.popTransform();
        }
        if (showVoxels && map instanceof Projectable) {
            ((Projectable)map).project(nowMS, buffer);
        }
//        if (pendingFrame.compareAndSet(true, false)) {
//            executor.execute(advanceFrame);
//        }
//        showElapsed("renderFrame", start);
    }

    public void renderOverlay(long nowMS, GeometryBuffer buffer) {
    }

    public void afterFrame(long nowMS) {
    }

    public void handleInit() {
        sphereDisplayList.record(new DisplayList.DisplayListRecorder() {
            public void record(GeometryBuffer buffer) {
                sphere.project(buffer);
            }
        });
    }

    public void handleClose() {
        advanceFrame.running.set(false);
    }

    public void handleNewFrame(long now) {
        // no-op
    }

    public ChainReactionDemo(double fps, boolean showVoxels) {
        this.fps = fps;
        this.showVoxels = showVoxels;
        final int expectedSize = 201;
        int i = 0;
        Random rand = new Random();
        Ball[] balls = new Ball[expectedSize];
        Ball b = new Ball(i++, Material.makeFrontShaded(Color.WHITE), 0.0, 0.0, 19.0);
        b.stopped = false;
        Vector initial = randomLaunchVector(rand);
        b.s = initial.i;
        b.t = initial.j;
        b.u = initial.k - 0.5;
        balls[b.index] = b;
        Material color1 = Material.makeFrontShaded(new Color(1.0f, 1.0f, 0.5f));
        Material color2 = Material.makeFrontShaded(new Color(0.5f, 0.5f, 1.0f));
        Vector launchVector1 = new Vector(-0.2, 0.2, 0.4);
        Vector launchVector2 = new Vector(0.2, -0.2, 0.4);
        for (double x = -18; x <= 18; x += 4) {
            for (double y = -18; y <= 18; y += 4) {
                b = new Ball(i++, color1, x - 0.75, y + 0.75, -19.0);
                b.launchVector = launchVector1.sum(randomLaunchVector(rand)).normalize(2.0);
                balls[b.index] = b;
                b = new Ball(i++, color2, x + 0.75, y - 0.75, -19.0);
                b.launchVector = launchVector2.sum(randomLaunchVector(rand)).normalize(2.0);
                balls[b.index] = b;
            }
        }
        assert i == expectedSize;
        for (Ball ball : balls) {
            map.add(ball, ball.x, ball.y, ball.z);
        }
        for (SpatialMap.Entry<Ball> entry : map) {
            int index = entry.get().index;
            ballEntries[index] = entry;
            simulators[index] = buildSimulator(entry);
        }
        sphere.setMaterial(Material.INHERIT);
        box.setMaterial(Material.makeWireFrame(Color.WHITE));
        advanceFrame = new AdvanceFrame(expectedSize);
    }

    private Vector randomLaunchVector(Random rand) {
        double i = Math.min(0.05, Math.max(-0.05, rand.nextGaussian() * 0.025));
        double j = Math.min(0.05, Math.max(-0.05, rand.nextGaussian() * 0.025));
        double k = Math.min(0.02, Math.max(-0.02, rand.nextGaussian() * 0.01));
        return new Vector(i,j,k);
    }

    private Simulator buildSimulator(final SpatialMap.Entry<Ball> entry) {
        return new Simulator(entry);
    }

    private synchronized void lastPendingProcessHasRun() {
        pendingFrame.set(true);
        queueAdvanceFrame();
    }

    private void queueAdvanceFrame() {
        if (advanceFrame.running.get()) {
            processCounter.incrementAndGet();
            executor.execute(advanceFrame);
        }
    }

    public void start() {
        View3D view3D = new View3D(new Dimension(1024,768));
        camera = new Camera();
        camera.setPosition(new Point(15.0, -100.0, 35.0));
        camera.setDirection(new Vector(-15.0, 100, -35.0));
        camera.setRight(new Vector(-100.0, 0.0, 0.0).scale(0.75));
        camera.computeClippingDistances(box.getBounds());
        view3D.setCamera(camera);
        view3D.setTitle("Simulation");
        view3D.setMainLoop(this);
        view3D.addViewEventHandler(this);
        view3D.start();
        try {
            Thread.sleep(2500L);
        } catch (InterruptedException e) {
            // don't care
        }
        queueAdvanceFrame();
        while (advanceFrame.running.get()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // don't care
            }
        }
        try {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // don't care
        }
//        System.out.println("total moves computed: " + moveCount.get());
    }

    private Image loadAppIcon() {
        URL url = getClass().getClassLoader().getResource("images/icon-128.png");
        return new ImageIcon(url).getImage();
    }

    private static void showElapsed(String name, long start) {
        double elapsed = (System.currentTimeMillis() - start) / 1000.0;
        System.out.printf("Operation '%s' took %.3fs\n", name, elapsed);
    }

    private Collision newCollision() {
        return new Collision(0,0);
    }

    static class Ball {

        private final int index;
        private final Material color;

        private double x, y, z; // position
        private double i, j, k; // velocity
        private double a, b, c; // next position
        private double s, t, u; // next velocity
        private Vector launchVector = null;
        private boolean launch = false;
        private boolean stopped = true;

        Ball(int index, Material color, double x, double y, double z) {
            this.index = index;
            this.color = color;
            this.a = this.x = x;
            this.b = this.y = y;
            this.c = this.z = z;
        }

        void advanceNext() {
            x = a;
            y = b;
            z = c;
            i = s;
            j = t;
            k = u;
        }

        @Override
        public String toString() {
            if (index == 0) return "white ball";
            int i = index - 1, j = i / 2 + 1;
            return ((i & 1) == 0) ? "yellow ball " + j : "blue ball " + j;
        }
    }

    private abstract class AbstractProcess implements Runnable {
        public final void run() {
            try {
                runImpl();
            } finally {
                if (processCounter.decrementAndGet() == 0) {
                    lastPendingProcessHasRun();
                }
            }
        }

        protected abstract void runImpl();
    }

    private class Simulator extends AbstractProcess implements SpatialMap.Consumer<Ball> {

        private final SpatialMap.Entry<Ball> entry;
        private final int ballIndex;

        public Simulator(SpatialMap.Entry<Ball> entry) {
            this.entry = entry;
            this.ballIndex = entry.get().index;
        }

        public boolean isStopped() {
            return entry.get().stopped;
        }

        protected void runImpl() {
            Ball ball = entry.get();
            moveCount.incrementAndGet();
//                System.out.println("Ball simulation for " + ball + ", velocity is " + Vector.computeDistance(ball.i, ball.j, ball.k));
            if (ball.launch && ball.launchVector != null) {
                ball.s = ball.launchVector.i;
                ball.t = ball.launchVector.j;
                ball.u = ball.launchVector.k;
                ball.launchVector = null;
            } else {
                ball.s = ball.i * 0.999;
                ball.t = ball.j * 0.999;
                ball.u = ball.k * 0.999;
            }
            ball.launch = false;
            ball.a = checkForBounce(ball, ball.x, ball.s, gravity.i, Axis.X);
            ball.b = checkForBounce(ball, ball.y, ball.t, gravity.j, Axis.Y);
            ball.c = checkForBounce(ball, ball.z, ball.u, gravity.k, Axis.Z);
            double velocity = Vector.computeDistance(ball.s, ball.t, ball.u);
            map.intersect(new BoundingSphere(new Point(ball.a, ball.b, ball.c), 2.0 + velocity * 1.1), this);
            if (Math.abs(ball.s) <= 0.0001 && Math.abs(ball.t) <= 0.0001 && Math.abs(ball.u) <= gravity.l + 0.0001 && ball.z < -18.99) {
                ball.stopped = true;
            }
        }

        private double checkForBounce(Ball ball, double value, double velocity, double gravity, Axis axis) {
            double v = value + velocity;
            if (Math.abs(v) > 19.0) {
                double a = 19.0 - Math.abs(value);
                double b = 19.0 - Math.abs(v);
                double g = Math.abs(gravity);
                double w = Math.abs(velocity);
                if (w < g) {
                    adjustVector(ball, axis, 0.0, 0.0);
                } else if (w < g * 18.0) {
                    adjustVector(ball, axis, -Math.sqrt((w - g) / (g * 18)), 0.0);
                } else if (a < -b) {
                    adjustVector(ball, axis, -0.90, 0.0);
                } else {
                    double n = gravity * (a + b) / w;
                    adjustVector(ball, axis, -0.95, n * 0.95);
                }
                v = v > 0 ? 19.0 + b * 0.95 : -19.0 - b * 0.95;
            } else {
                adjustVector(ball, axis, 1.0, gravity);
            }
            return v;
        }

        private double adjustVector(Ball ball, Axis axis, double magnitude, double absolute) {
            switch (axis) {
                case X:
                    return ball.s = ball.s * magnitude + absolute;
                case Y:
                    return ball.t = ball.t * magnitude + absolute;
                case Z:
                    return ball.u = ball.u * magnitude + absolute;
            }
            return Double.NaN;
        }

        public void found(SpatialMap.Entry<Ball> entry, double x, double y, double z) {
            Ball ball = entry.get();
            if (ball.index != ballIndex) {
                Ball t = ballEntries[ballIndex].get();
                double d = Vector.computeDistance(t.x - x, t.y - y, t.z - z);
//                System.out.println(t + " intersection with " + ball + " d=" + d);
                if (d < 2.0) {
                    if (ball.launchVector != null) {
                        ball.launch = true;
                        ball.stopped = false;
                    } else {
                        collisions.add(new Collision(ball,t));
                    }
                    if (ballIndex > 0) {
                        Ball other = ballEntries[ballIndex % 2 == 1 ? ballIndex + 1 : ballIndex - 1].get();
                        if (other.launchVector != null && !other.launch) {
                            other.launch = true;
                            other.stopped = false;
                        }
                    }
                }
            }
        }
    }

    private class Collision extends AbstractProcess {
        private final Integer a;
        private final Integer b;

        private Collision(Ball a, Ball b) {
            this(a.index,b.index);
        }

        private Collision(Integer a, Integer b) {
            if (a < b) {
                this.a = a;
                this.b = b;
            } else {
                this.a = b;
                this.b = a;
            }
        }

        protected void runImpl() {
            Ball a = ballEntries[this.a].get();
            Ball b = ballEntries[this.b].get();
            double points[] = computeCollision(a.x, a.y, a.z, a.a, a.b, a.c, b.x, b.y, b.z, b.a, b.b, b.c);
            double x = points.length > 0 ? points[0] : -1.0;
            double y = points.length > 1 ? points[1] : -1.0;
            x = (x < 0 || x >= 1.0) ? 1.0 : x;
            y = (y < 0 || y >= 1.0) ? 1.0 : y;
            if (x < 1.0 && y < 1.0) {
                x = Math.min(x,y);
            }
            if (x < 1.0) {
                processCollision(a,b,x);
            }
        }

        private void processCollision(Ball a, Ball b, double t) {
            double T = 1.0 - t;
            Point A = new Point(a.x*T+a.a*t, a.y*T+a.b*t, a.z*T+a.c*t);
            Point B = new Point(b.x*T+b.a*t, b.y*T+b.b*t, b.z*T+b.c*t);

        }

        private double[] computeCollision(double a, double b, double c, double d, double e, double f, double i, double j, double k, double l, double m, double n) {
            double A = computeA(a, b, c, d, e, f, i, j, k, l, m, n);
            double B = computeB(a, b, c, d, e, f, i, j, k, l, m, n);
            double C = computeC(a, b, c, d, e, f, i, j, k, l, m, n);
            return quadratic(A, B, C);
        }

        private double computeA(double a, double b, double c, double d, double e, double f, double i, double j, double k, double l, double m, double n) {
            return 0.0
                    + a * a
                    + d * d
                    + i * i
                    + l * l
                    + b * b
                    + e * e
                    + j * j
                    + m * m
                    + c * c
                    + f * f
                    + k * k
                    + n * n
                    + 2.0 * (
                          a * l
                        + b * m
                        + c * n
                        - a * d
                        - a * i
                        - d * l
                        - i * l
                        - b * e
                        - b * j
                        - e * m
                        - j * m
                        - c * f
                        - c * k
                        - f * n
                        - k * n
                    )
                    ;
        }

        private double computeB(double a, double b, double c, double d, double e, double f, double i, double j, double k, double l, double m, double n) {
            return 2.0 * (
                    + a * d
                    + a * i
                    + a * i
                    + d * i
                    + i * l
                    + b * e
                    + b * j
                    + b * j
                    + e * j
                    + j * m
                    + c * f
                    + c * k
                    + c * k
                    + f * k
                    + k * n
                    - a * a
                    - a * l
                    - i * i
                    - b * b
                    - b * m
                    - j * j
                    - c * c
                    - c * n
                    - k * k
                    )
                    ;
        }

        private double computeC(double a, double b, double c, double d, double e, double f, double i, double j, double k, double l, double m, double n) {
            return 0.0
                    + a * a
                    + i * i
                    + b * b
                    + j * j
                    + c * c
                    + k * k
                    - 2.0 * (
                        - a * i
                        - d * i
                        - b * j
                        - e * j
                        - c * k
                        - f * k
                    )
                    ;
        }

        private double[] quadratic(double a, double b, double c) {
            if (a == 0) {
                return new double[]{};
            }
            double n = b * b - 4.0 * a * c;
            if (n < 0) {
                // x is undefined, curve does not cross 0
                return new double[]{};
            } else if (n == 0) {
                // curve touches 0 at a single point
                double d = 2 * a;
                return new double[]{-b / d};
            } else {
                // curve crosses 0 at two points
                n = Math.sqrt(n);
                double d = 2 * a;
                return new double[]{
                    (-b - n) / d,
                    (-b + n) / d
                };
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Collision collision = (Collision)o;
            return a.equals(collision.a) && b.equals(collision.b);

        }

        @Override
        public int hashCode() {
            int result = a.hashCode();
            result = 31 * result + b.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return String.format("Collision[\"%s\"<-->\"%s\"]", ballEntries[a].get(), ballEntries[b].get());
        }
    }

    private class AdvanceFrame extends AbstractProcess {

        private long nextRun;
        private final int expectedSize;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private double frameDither = 0;

        public AdvanceFrame(int expectedSize) {
            this.expectedSize = expectedSize;
            nextRun = 0L;
        }

        @Override
        protected void runImpl() {
            if (!collisions.isEmpty()) {
                synchronized (collisions) {
//                        System.out.println("Scheduling " + collisions.size() + " collision calculations");
                    processCounter.addAndGet(collisions.size());
                    for (Collision collision : collisions) {
                        executor.execute(collision);
                    }
                    collisions.clear();
                    return;
                }
            }
            long now = System.currentTimeMillis();
            if (now < nextRun) {
                try {
                    Thread.sleep(nextRun - now);
                } catch (InterruptedException e) {
                    // don't care
                }
                now = nextRun;
            } else if (nextRun == 0L) {
                nextRun = now;
            }
            double ms = (1000.0 / fps) + frameDither;
            long msWait = Math.round(ms);
            frameDither = ms - msWait;

            nextRun = now + msWait;
            int count = expectedSize;
            for (SpatialMap.Entry<Ball> entry : ballEntries) {
                Ball ball = entry.get();
                ball.advanceNext();
                entry.move(ball.x, ball.y, ball.z);
                count--;
            }
            assert count == 0;
            boolean allStopped = true;
            count = expectedSize;
            for (Simulator simulator : simulators) {
                count--;
//                for (Runnable simulator : java.util.Collections.singleton(simulators[0])) {
                if (!simulator.isStopped() && running.get()) {
                    processCounter.incrementAndGet();
                    allStopped = false;
                    executor.execute(simulator);
                }
            }
            assert count == 0;
            if (allStopped) {
                running.set(false);
            }
        }
    }

    public static void main(String[] args) {
        ChainReactionDemo demo = new ChainReactionDemo(60, false);
        demo.start();
        System.out.println("Simulation complete!");
//        Collision collision = demo.newCollision();
//        double[] points = collision.computeCollision(-8,-8,-8,0,0,0,8,8,8,0,0,0);
//        System.out.printf("Intersections=%d : [%s,%s]\n",
//                          points.length,
//                          points.length > 0 ? points[0] : Double.NaN,
//                          points.length > 1 ? points[1] : Double.NaN);
    }
}
