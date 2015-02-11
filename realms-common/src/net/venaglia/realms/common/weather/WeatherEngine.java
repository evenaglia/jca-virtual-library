package net.venaglia.realms.common.weather;

import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.MutableSimpleBounds;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.complex.GeodesicSphere;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.impl.CapturingConsumer;
import net.venaglia.gloo.util.impl.OctreeMap;
import net.venaglia.realms.common.map.world.AcreDetail;
import net.venaglia.realms.spec.GeoSpec;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 2/3/15
 * Time: 8:37 PM
 */
public class WeatherEngine {

    private final ThreadPoolExecutor executor;

    {
        final ThreadFactory threadFactory = new ThreadFactory() {

            private final ThreadGroup tg = new ThreadGroup("work");
            private final AtomicInteger seq = new AtomicInteger();

            @SuppressWarnings("NullableProblems")
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(tg, runnable, String.format("WeatherWorker-%02d", seq.incrementAndGet()));
                thread.setDaemon(true);
                return thread;
            }
        };
        final BlockingQueue<Runnable> queue = new LinkedBlockingDeque<Runnable>();
        executor = new ThreadPoolExecutor(16, 16, 5, TimeUnit.SECONDS,
                                          queue,
                                          threadFactory);
    }

    private AtomicInteger processCounter = new AtomicInteger();

    private final double radius;
    private final String fragmentFormat;
    private final int count = 10000;

    private double fps;
    private SpatialMap<AirSampleImpl> map;
    private Advance advance;
    private AirSampleImpl[] samples;
    private SpatialMap.Entry<AirSampleImpl>[] entries;
    private SpatialMap<AcreDetail> acreMap;

    private AtomicBoolean observerQueryLock = new AtomicBoolean(false);
    private AtomicInteger pendingWorkCount = new AtomicInteger();

    public WeatherEngine() {
        radius = GeoSpec.APPROX_RADIUS_METERS.get();
        fragmentFormat = "Fragment[%0" + String.valueOf(count).length() + "d]";
        map = new OctreeMap<AirSampleImpl>(new BoundingSphere(Point.ORIGIN, radius * 1.2));
        acreMap = new OctreeMap<AcreDetail>(new BoundingSphere(Point.ORIGIN, radius * 1.2));
        GeodesicSphere sphere = new GeodesicSphere(30); // 9002 points
        CapturingConsumer<AirSampleImpl> capture = new CapturingConsumer<>();
        MutableSimpleBounds bounds = new MutableSimpleBounds();
        for (Point point : sphere) {
            bounds.load(point);
            map.intersect(bounds, capture.use());
            if (!capture.found()) {
                map.add(new AirSampleImpl(point), point);
            }
        }
        System.out.println();
        // todo: air sample points based on points of a geodesic sphere
    }

    private void queueIsEmpty() {
        switch (advance.runState.get()) {
            case RUNNING:
                processCounter.set(0);
                advance.addToQueue();
                break;
            case STOPPING:
                if (advance.runState.compareAndSet(RunState.STOPPING, RunState.STOPPED)) {
                    synchronized (this) {
                        notifyAll();
                    }
                } else {
                    queueIsEmpty(); // something changed mid execution, run it again
                }
                return;
        }
    }

    enum RunState {
        STOPPED, RUNNING, STOPPING, DESTROYED
    }

    protected abstract class AbstractWorker implements Runnable {

        private boolean inQueue = false;

        public synchronized final void addToQueue() {
            if (!inQueue) {
                inQueue = true;
                processCounter.incrementAndGet();
                executor.execute(this);
            }
        }

        public final void run() {
            try {
                doWork();
            } finally {
                synchronized (this) {
                    inQueue = false;
                }
                if (processCounter.decrementAndGet() <= 0) {
                    queueIsEmpty();
                }
            }
        }

        protected abstract void doWork();
    }

    protected class Advance extends AbstractWorker {

        private final AtomicReference<RunState> runState = new AtomicReference<RunState>(RunState.STOPPED);

        private long nextRun;
        private double frameDither = 0;

        private void waitForFrameSync() {
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
        }

        @Override
        protected void doWork() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    static class AirSampleImpl implements AirSample {

        double x, y, z;
        double i, j, k;
        float temperature;
        float moisture;
        float dewPoint;
        float cloudOpacity;
        float turbulance;
        float size;

        AirSampleImpl(Point start) {
            x = start.x;
            y = start.y;
            z = start.z;
            temperature = 50.0f;
            moisture = 10.0f;
            dewPoint = calcDewPoint();
        }

        @Override
        public <T> T getCenterXYZ(XForm.View<T> view) {
            return view.convert(x, y, z, 1);
        }

        @Override
        public void setCenterXYZ(double x, double y, double z) {
            this.i = x - this.x;
            this.j = y - this.y;
            this.k = z - this.z;
            // todo: coriolis effect
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public <T> T getVectorXYZ(XForm.View<T> view) {
            return view.convert(i, j, k, 1);
        }

        public float getTemperature() {
            return temperature;
        }

        public float getMoisture() {
            return moisture;
        }

        public float getDewPoint() {
            return dewPoint;
        }

        public float getCloudOpacity() {
            return cloudOpacity;
        }

        public float getTurbulance() {
            return turbulance;
        }

        public float getSize() {
            return size;
        }

        @Override
        public void update(float addMoisture, float addHeat) {
            // todo
        }

        private float calcDewPoint() {
            // todo
            return temperature;
        }
    }

    public static void main(String[] args) {
        new WeatherEngine();
    }
}
