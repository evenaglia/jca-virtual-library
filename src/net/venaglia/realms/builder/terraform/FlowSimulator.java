package net.venaglia.realms.builder.terraform;

import net.venaglia.realms.common.physical.bounds.BoundingBox;
import net.venaglia.realms.common.physical.bounds.BoundingSphere;
import net.venaglia.realms.common.physical.decorators.Color;
import net.venaglia.realms.common.physical.geom.Facet;
import net.venaglia.realms.common.physical.geom.Point;
import net.venaglia.realms.common.physical.geom.Vector;
import net.venaglia.realms.common.physical.geom.primitives.Dodecahedron;
import net.venaglia.realms.common.util.SpatialMap;
import net.venaglia.realms.common.util.impl.OctreeMap;
import net.venaglia.realms.common.util.work.Results;
import net.venaglia.realms.common.util.work.WorkManager;
import net.venaglia.realms.common.util.work.WorkQueue;
import net.venaglia.realms.common.util.work.WorkSourceAdapter;
import net.venaglia.realms.common.util.work.WorkSourceKey;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Observable;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 9/16/12
 * Time: 3:42 PM
 */
public class FlowSimulator extends Observable {

    private final ThreadPoolExecutor executor;

    {
        final ThreadFactory threadFactory = new ThreadFactory() {

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
        final BlockingQueue<Runnable> queue = new LinkedBlockingDeque<Runnable>();
        executor = new ThreadPoolExecutor(16, 16, 5, TimeUnit.SECONDS,
                                          queue,
                                          threadFactory);
    }

    private AtomicInteger processCounter = new AtomicInteger();

    private final double radius;
    private final double nominalRadius;
    private final String fragmentFormat;
    private final int numTectonicPoints = 32;

    private double fps;
    private double timeScale;
    private SpatialMap<Fragment> map;
    private TectonicPoint[] tectonicPoints;
    private Advance advance;
    private Fragment[] fragments;
    private SpatialMap.Entry<Fragment>[] entries;

    private FlowObserver observer;

    private final QueryInterface queryInterface = new QueryInterface();
    private final AtomicBoolean observerQueryLock = new AtomicBoolean(false);
    private final AtomicInteger activeQueryCount = new AtomicInteger();

    public FlowSimulator(double radius, int count, double fps, double timeScale) {
        this.radius = radius;
        double surfaceAreaPerFragment = (radius * radius * 1.333333333333333) / count;
        this.nominalRadius = Math.sqrt(surfaceAreaPerFragment);
        this.fps = fps;
        this.timeScale = timeScale;
        double min = -2.0 - radius;
        double max =  2.0 + radius;
        Random random = new Random();
        BoundingBox bounds = new BoundingBox(new Point(min, min, min), new Point(max, max, max));
        map = new OctreeMap<Fragment>(bounds, 12, 5) {
            @Override
            protected AbstractEntry<Fragment> createEntry(Fragment fragment, double x, double y, double z) {
                AbstractEntry<Fragment> entry = super.createEntry(fragment, x, y, z);
                entries[fragment.seq] = entry;
                return entry;
            }
        };
        Point[] startingPoints = new Point[numTectonicPoints];
        tectonicPoints = new TectonicPoint[numTectonicPoints];
        int index = 0;
        Dodecahedron dodecahedron = new Dodecahedron().scale(radius);
        for (Point p : dodecahedron) {
            startingPoints[index] = p;
            tectonicPoints[index++] = new TectonicPoint(p, randomTectonicVector(random, p));
        }
        for (int i = 0, l = dodecahedron.facetCount(); i < l; i++) {
            Facet f = dodecahedron.getFacet(i);
            double x = 0.0, y = 0.0, z = 0.0, d = 0.0;
            for (Point p : f) {
                x += p.x;
                y += p.y;
                z += p.z;
                d += 1.0;
            }
            x /= d;
            y /= d;
            z /= d;
            Point p = new Point(x, y, z);
            startingPoints[index] = p;
            tectonicPoints[index++] = new TectonicPoint(p, randomTectonicVector(random, p));
        }
        for (int i = 0, l = startingPoints.length; i < l; i++) {
            startingPoints[i] = Point.ORIGIN.translate(Vector.betweenPoints(Point.ORIGIN, startingPoints[i]).normalize(radius));
        }
        fragmentFormat = "Fragment[%0" + String.valueOf(count).length() + "d]";

        advance = new Advance(startingPoints);

        Color[] fragmentColors = new Color[15];
        for (int i = 0, l = fragmentColors.length; i < l; i++) {
            double a = Math.PI * -2.0 * (((double)i) / l);
            fragmentColors[i] = new Color(colorSine(a, 0.0), colorSine(a, 1.0), colorSine(a, 2.0), 1.0f);
        }
        fragments = new Fragment[count];
        int endPadding = 0; //numTectonicPoints * 4;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * -2.0 * (((double)i) / count);
            Color color = i < endPadding || (i + endPadding) > count ? Color.WHITE : new Color(colorSine(a, 0.0), colorSine(a, 1.0), colorSine(a, 2.0), 1.0f);
//            fragments[i] = new Fragment(i, fragmentColors[random.nextInt(fragmentColors.length)]);
            fragments[i] = new Fragment(i, color);
        }
        //noinspection unchecked
        entries = (SpatialMap.Entry<Fragment>[])new SpatialMap.Entry[count];

    }

    public void setObserver(FlowObserver observer) {
        this.observer = observer;
    }

    public void stop() {
        advance.running.set(false);
    }

    private Vector randomTectonicVector(Random random, Point p) {
        double x = p.x + random.nextGaussian();
        double y = p.y + random.nextGaussian();
        double z = p.z + random.nextGaussian();
        double l = Vector.computeDistance(x, y, z);
        double m = radius / l;
        x *= m;
        y *= m;
        z *= m;
        return new Vector(x - p.x, y - p.y, z - p.z).normalize().scale(0.005);
    }

    public double getRadius() {
        return radius;
    }

    public Fragment[] getFragments() {
        return fragments;
    }

    private float colorSine(double a, double part) {
        float v = (float)(Math.sin((a + Math.PI / 2.0) + Math.PI * part * 0.6666666667) * 1.5 + 0.5);
        return Math.max(Math.min(v,1.0f),0.0f);
    }

    public void run() {
        queueIsEmpty();
        waitUntilAllIn();
    }

    private void queueIsEmpty() {
        if (advance.running.get()) {
            processCounter.set(0);
            advance.addToQueue();
        }
        if (advance.allPointsIn.get()) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public synchronized void waitUntilAllIn() {
        while (!advance.allPointsIn.get()) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilDone() {
        while (advance.running.get()) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public boolean areAllPointsIn() {
        return advance.allPointsIn.get();
    }

    public int activeFragments() {
        return map.size();
    }

    private void observeFrame() {
        if (!advance.allPointsIn.get()) {
            return;
        }
        queryInterface.frameCount++;
        if (queryInterface.frameCount >= 0 && observer != null) {
            observerQueryLock.set(true);
            try {
                observer.frame(queryInterface);
            } finally {
                while (activeQueryCount.get() != 0) {
                    synchronized (activeQueryCount) {
                        try {
                            activeQueryCount.wait(250L);
                        } catch (InterruptedException e) {
                            // don't care
                        }
                    }
                }
                observerQueryLock.set(false);
            }
        }
    }

    private class TectonicPoint {

        private final Vector vector;
        private final Point point;

        public TectonicPoint(Point point, Vector vector) {
            this.point = point;
            this.vector = vector;
        }
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

    protected class Fragment extends AbstractWorker implements SpatialMap.Consumer<Fragment>, Comparator<Integer> {

        private final int seq;
        private final Color color;
        private final Integer[] tectonicPointIndices = genIntSeq();

        private Integer[] genIntSeq() {
            Integer[] integers = new Integer[numTectonicPoints];
            for (int i = 0; i < numTectonicPoints; i++) {
                integers[i] = i;
            }
            return integers;
        }

        private final double[] tectonicPointDistances = new double[numTectonicPoints];

        private double x, y, z;
        private double i, j, k;

        public Fragment(int seq, Color color) {
            this.seq = seq;
            this.color = color;
        }

        public Vector getVectorXYZ() {
            return new Vector(x, y, z);
        }

        public Color getColor() {
            return color;
        }

        @Override
        protected void doWork() {
            computeTectonicVector();
            map.intersect(new BoundingSphere(new Point(x, y, z), 2.5), this);
        }

        public int compare(Integer a, Integer b) {
            double cmp = tectonicPointDistances[b] - tectonicPointDistances[a];
            return cmp < 0.0 ? -1 : cmp > 0.0 ? 1 : 0;
        }

        private void computeTectonicVector() {
            for (int i = 0; i < 20; i++) {
                Point p = tectonicPoints[i].point;
                tectonicPointDistances[i] = d2(p.x - x, p.y - y, p.z - z);
            }
            Arrays.sort(tectonicPointIndices, this);

            i = 0.0;
            j = 0.0;
            k = 0.0;
            double sumWeight = 0.0;
            double runOut = Math.sqrt(tectonicPointDistances[tectonicPointIndices[6]]);
            for (int a = 0; a < 6; a++) {
                double weight = runOut - Math.sqrt(tectonicPointDistances[tectonicPointIndices[a]]);
                sumWeight += weight;
                Vector v = tectonicPoints[tectonicPointIndices[a]].vector;
                i += v.i * weight;
                j += v.j * weight;
                k += v.k * weight;
            }
            if (sumWeight > 0.0) {
                sumWeight = 1.0 / sumWeight;
                i *= sumWeight;
                j *= sumWeight;
                k *= sumWeight;
            }
        }

        private double d2 (double x, double y, double z) {
            return x * x + y * y + z * z;
        }

//        private double a1, a2, b1, b2, c1, c2;

        public void move(SpatialMap.Entry<Fragment> entry) {
            double v = Vector.computeDistance(i, j, k);
            if (v > 0.0125) {
                i = i * 0.0125 / v;
                j = j * 0.0125 / v;
                k = k * 0.0125 / v;
            }
            double s = x + i * timeScale;
            double t = y + j * timeScale;
            double u = z + k * timeScale;
//            double s = x + (i - a1 * 0.5 + a2 * 0.125) * timeScale;
//            double t = y + (j - b1 * 0.5 + a2 * 0.125) * timeScale;
//            double u = z + (k - c1 * 0.5 + a2 * 0.125) * timeScale;
            double d = radius / Vector.computeDistance(s, t, u);
//            a2 = a1;
//            b2 = b1;
//            c2 = c1;
//            a1 = i;
//            b1 = j;
//            c1 = k;
            x = s * d;
            y = t * d;
            z = u * d;
            entry.move(x, y, z);
        }

        public void found(SpatialMap.Entry<Fragment> entry, double x, double y, double z) {
            Fragment fragment = entry.get();
            if (fragment != this) {
                Vector vector = new Vector(this.x - x, this.y - y, this.z - z);
                if (vector.l > 0) {
                    double d = (2.5 / vector.l) - 1.0;
                    vector = vector.scale(d * 0.05);
                    i += vector.i;
                    j += vector.j;
                    k += vector.k;
                }
            }
        }

        @Override
        public String toString() {
            return String.format(fragmentFormat, seq);
        }
    }

    protected class Advance extends AbstractWorker implements SpatialMap.Consumer<Fragment> {

        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicBoolean allPointsIn = new AtomicBoolean(false);
        private final BoundingSphere[] startingPoints;
        private final AtomicInteger counter = new AtomicInteger();

        private long nextRun;
        private double frameDither = 0;

        public Advance(Point[] startingPoints) {
            this.startingPoints = new BoundingSphere[startingPoints.length];
            for (int i = 0; i < startingPoints.length; i++) {
                this.startingPoints[i] = new BoundingSphere(startingPoints[i], 0.15);
            }
        }

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
            observeFrame();
            waitForFrameSync();
            int l = map.size();
            int j = 0;
            if (!allPointsIn.get()) {
                while (l < fragments.length && j < startingPoints.length) {
                    if (insert(fragments[l], startingPoints[j])) {
                        l++;
                    }
                    j++;
                }
                if (l >= fragments.length) {
                    allPointsIn.set(true);
                }
            }
//            try {
//                Thread.sleep(90000L);
//            } catch (InterruptedException e) { }
            for (int i = 0; i < l; i++) {
                SpatialMap.Entry<Fragment> entry = entries[i];
                Fragment fragment = entry.get();
                fragment.move(entry);
                fragment.addToQueue();
            }
        }

        private boolean canInsert(BoundingSphere startingPoint) {
            return true;
//            counter.set(0);
//            map.intersect(startingPoint, this);
//            return counter.get() == 0;
        }

        private boolean insert(Fragment fragment, BoundingSphere startingPoint) {
            if (canInsert(startingPoint)) {
                fragment.x = startingPoint.center.x;
                fragment.y = startingPoint.center.y;
                fragment.z = startingPoint.center.z;
                if (map.add(fragment, startingPoint.center)) {
                    return true;
                }
            }
            return false;
        }

        public void found(SpatialMap.Entry<Fragment> entry, double x, double y, double z) {
            counter.incrementAndGet();
        }
    }

    private class QueryInterface implements FlowQueryInterface {

        private final WorkManager workManager = new WorkManager("Query");
        private final AtomicInteger seq = new AtomicInteger(1);

        int frameCount = (int)Math.round(-100.0 / timeScale);

        private void ensureInsideQueryCall(final String methodName) {
            activeQueryCount.incrementAndGet();
            if (!observerQueryLock.get()) {
                oneLess();
                throw new IllegalStateException("Cannot call " + methodName + "() outside a call to LiquidGlobeObserver.frame()");
            }
        }

        public int getFrameCount() {
            return frameCount;
        }

        public void changeSettings(double fps, double timeScale) {
            ensureInsideQueryCall("changeSettings");
            try {
                FlowSimulator.this.fps = fps;
                FlowSimulator.this.timeScale = timeScale;
            } finally {
                oneLess();
            }
        }

        private void oneLess() {
            if (activeQueryCount.decrementAndGet() == 0) {
                synchronized (activeQueryCount) {
                    activeQueryCount.notifyAll();
                }
            }
        }

        public void query(final Iterable<? extends FlowQuery> queries) {
            ensureInsideQueryCall("query");
            try {
                WorkSourceKey<Void> key = WorkSourceKey.create("query-" + seq.get(), Void.class);
                workManager.addWorkSource(new WorkSourceAdapter<Void>(key) {
                    public void addWork(WorkQueue workQueue, Results dependencies) {
                        activeQueryCount.incrementAndGet();
                        for (final FlowQuery query : queries) {
                            workQueue.addWorkUnit(new QueryRunner(query));
                        }
                    }
                });
                workManager.getResults().getResult(key);
            } finally {
                activeQueryCount.decrementAndGet();
            }
        }
    }

    private class QueryRunner extends FlowPointData implements Runnable, SpatialMap.Consumer<Fragment> {

        private final FlowQuery query;

        private double i = 0.0, j = 0.0, k = 0.0, l = 0.0;
        private double contributorSum = 0.0;

        public QueryRunner(FlowQuery query) {
            this.query = query;
        }

        public void run() {
            try {
                geoPoint = query.getPoint();
                point = geoPoint.toPoint(radius);
                direction = null;
                velocity = Double.NaN;
                map.intersect(new BoundingSphere(point, nominalRadius * 3), this);
                if (l > 0) {
                    double avg = 1.0 / contributorSum;
                    magnitude = new Vector(i * avg, j * avg, k * avg);
                    pressure = Math.log(l / nominalRadius);
                    query.processDataForPoint(this);
                } else {
                    magnitude = Vector.ZERO;
                    pressure = Double.NEGATIVE_INFINITY;
                }
            } finally {
                queryInterface.oneLess();
            }
        }

        private double calcContribution(double distance) {
            double exp = distance / (nominalRadius * nominalRadius);
            exp = exp * exp * -0.5;
            return Math.exp(exp);
        }

        public void found(SpatialMap.Entry<Fragment> entry, double x, double y, double z) {
            double i = point.x - x;
            double j = point.y - y;
            double k = point.z - z;
            double l = Vector.computeDistance(i, j, k);
            double contribution = calcContribution(l);
            this.i += i * contribution;
            this.j += j * contribution;
            this.k += k * contribution;
            this.l += l * contribution;
            this.contributorSum += contribution;
        }

        @Override
        public Vector getDirection() {
            if (direction == null) {
                direction = magnitude.normalize();
            }
            return direction;
        }

        @Override
        public double getVelocity() {
            if (Double.isNaN(velocity)) {
                velocity = magnitude.l;
            }
            return velocity;
        }

    }

}
