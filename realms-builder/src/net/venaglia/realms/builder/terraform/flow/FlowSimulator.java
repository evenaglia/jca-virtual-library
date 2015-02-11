package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.common.util.CumulativeDeviation;
import net.venaglia.common.util.Factory;
import net.venaglia.common.util.recycle.Recyclable;
import net.venaglia.common.util.recycle.RecycleBin;
import net.venaglia.common.util.recycle.RecycleDeque;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.MutableSimpleBounds;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Facet;
import net.venaglia.gloo.physical.geom.PlatonicShape;
import net.venaglia.gloo.physical.geom.Point;
import net.venaglia.gloo.physical.geom.Vector;
import net.venaglia.gloo.physical.geom.XForm;
import net.venaglia.gloo.physical.geom.primitives.Dodecahedron;
import net.venaglia.gloo.util.SpatialMap;
import net.venaglia.gloo.util.impl.OctreeMap;
import net.venaglia.realms.common.util.work.Results;
import net.venaglia.realms.common.util.work.WorkManager;
import net.venaglia.realms.common.util.work.WorkQueue;
import net.venaglia.realms.common.util.work.WorkSourceAdapter;
import net.venaglia.realms.common.util.work.WorkSourceKey;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;
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
 * Date: 9/16/12
 * Time: 3:42 PM
 */
public class FlowSimulator {

    private final ThreadPoolExecutor executor;

    {
        final ThreadFactory threadFactory = new ThreadFactory() {

            private final ThreadGroup tg = new ThreadGroup("work");
            private final AtomicInteger seq = new AtomicInteger();

            @SuppressWarnings("NullableProblems")
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(tg, runnable, String.format("FlowWorker-%02d", seq.incrementAndGet()));
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
    private final int count;
    private final int numTectonicPoints;
    private final boolean captureFragmentContributions;

    private double fps;
    private double timeScale;
    private double pressureBias = 0.0;
    private double pressureScale = 1.0;
    private SpatialMap<FragmentImpl> map;
    private TectonicPoint[] tectonicPoints;
    private Advance advance;
    private FragmentImpl[] fragments;
    private SpatialMap.Entry<FragmentImpl>[] entries;
    private FlowObserver observer;
    private QueryInterface queryInterface = new QueryInterface();
    private AtomicBoolean observerQueryLock = new AtomicBoolean(false);
    private AtomicInteger pendingWorkCount = new AtomicInteger();

    public FlowSimulator(double radius, int count, double fps, double timeScale, TectonicDensity density) {
        this(radius, count, fps, timeScale, density, System.currentTimeMillis(), false);
    }

    public FlowSimulator(double radius, int count, double fps, double timeScale, TectonicDensity density, long seed, boolean captureContributions) {
        this.radius = radius;
        this.count = count;
        this.numTectonicPoints = density.getNumTectonicPoints();
        double surfaceAreaPerFragment = (radius * radius * 1.333333333333333) / count;
        this.nominalRadius = Math.sqrt(surfaceAreaPerFragment);
        this.fps = fps;
        this.timeScale = timeScale;
        double min = -2.0 - radius;
        double max =  2.0 + radius;
        Random random = new Random(seed);
        BoundingBox bounds = new BoundingBox(new Point(min, min, min), new Point(max, max, max));
        map = new OctreeMap<FragmentImpl>(bounds, 12, 5) {
            @Override
            protected AbstractEntry<FragmentImpl> createEntry(FragmentImpl fragment, double x, double y, double z) {
                AbstractEntry<FragmentImpl> entry = super.createEntry(fragment, x, y, z);
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
            tectonicPoints[index++] = new TectonicPoint(p,
                                                        randomTectonicVector(random, p),
                                                        random.nextGaussian() * 0.001,
                                                        TectonicPoint.PointClass.PLATONIC,
                                                        TectonicPoint.Source.RANDOM);
        }
        for (int i = 0, l = dodecahedron.facetCount(); i < l; i++) {
            Facet f = dodecahedron.getFacet(i);
            Point p = getFacetCenterPoint(f);
            startingPoints[index] = p;
            tectonicPoints[index++] = new TectonicPoint(p,
                                                        randomTectonicVector(random, p),
                                                        random.nextGaussian() * 0.001,
                                                        TectonicPoint.PointClass.MIDPOINT,
                                                        TectonicPoint.Source.RANDOM);
        }
        if (index < numTectonicPoints) {
            for (int i = 0, l = dodecahedron.edgeCount(); i < l; i++) {
                PlatonicShape.Edge e = dodecahedron.getEdge(i);
                Point p = getEdgeCenterPoint(e);
                startingPoints[index] = p;
                tectonicPoints[index++] = new TectonicPoint(p,
                                                            reverseTectonicVector(p),
                                                            random.nextGaussian() * 0.001,
                                                            TectonicPoint.PointClass.MIDPOINT,
                                                            TectonicPoint.Source.REVERSE);
            }
        }
        if (index < numTectonicPoints) {
            Point[] points = new Point[5];
            for (int i = 0, l = dodecahedron.facetCount(); i < l; i++) {
                double x = 0.0, y = 0.0, z = 0.0;
                Facet f = dodecahedron.getFacet(i);
                assert f.size() == 5;
                Iterator<Point> iterator = f.iterator();
                for (int j = 0; j < 5; j++) {
                    Point p = iterator.next();
                    x += p.x;
                    y += p.y;
                    z += p.z;
                    points[j] = p;
                }
                Point c = new Point(x * 0.2, y * 0.2, z * 0.2);
                for (int j = 0; j < 5; j++) {
                    Point p = Point.midPoint(points[j], c, 0.5);
                    startingPoints[index] = p;
                    tectonicPoints[index++] = new TectonicPoint(p,
                                                                reverseTectonicVector(p),
                                                                random.nextGaussian() * 0.001 - 0.0005,
                                                                TectonicPoint.PointClass.INTERPOLATED,
                                                                TectonicPoint.Source.REVERSE);
                }
            }
        }
        assert index == numTectonicPoints;
        for (int i = 0, l = startingPoints.length; i < l; i++) {
            startingPoints[i] = Point.ORIGIN.translate(Vector.betweenPoints(Point.ORIGIN, startingPoints[i]).normalize(radius));
        }
        fragmentFormat = "Fragment[%0" + String.valueOf(count).length() + "d]";
        this.captureFragmentContributions = captureContributions;

        advance = new Advance(startingPoints);

        fragments = new FragmentImpl[count];
        for (int i = 0; i < count; i++) {
            fragments[i] = new FragmentImpl(i);
        }
        //noinspection unchecked
        entries = (SpatialMap.Entry<FragmentImpl>[])new SpatialMap.Entry[count];
    }

    private Point getFacetCenterPoint(Facet f) {
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
        return new Point(x, y, z);
    }

    private Point getEdgeCenterPoint(PlatonicShape.Edge e) {
        return Point.midPoint(e.a, e.b, 0.5);
    }

    public int getCount() {
        return count;
    }

    public List<TectonicVectorArrow> getTectonicArrows(double radius) {
        ensureNotDestroyed();
        Material green = Material.makeSelfIlluminating(Color.GREEN);
        Material yellow = Material.makeSelfIlluminating(Color.YELLOW);
        Material gray = Material.makeSelfIlluminating(Color.GRAY_50);
        List<TectonicVectorArrow> arrows = new ArrayList<>(tectonicPoints.length);
        for (TectonicPoint tectonicPoint : tectonicPoints) {
            Material material;
            if (tectonicPoint.source == TectonicPoint.Source.RANDOM) {
                material = green;
            } else if (tectonicPoint.pointClass == TectonicPoint.PointClass.MIDPOINT) {
                material = yellow;
            } else {
                material = gray;
            }
            arrows.add(TectonicVectorArrow.createArrow(tectonicPoint)
                               .scale(radius)
                               .setMaterial(material));
        }
        return Collections.unmodifiableList(arrows);
    }

    public void setObserver(FlowObserver observer) {
        ensureNotDestroyed();
        this.observer = observer;
    }

    public synchronized void start() {
        ensureNotDestroyed();
        switch (advance.runState.getAndSet(RunState.RUNNING)) {
            case STOPPED:
                queueIsEmpty();
                waitUntilAllIn();
        }
    }

    public synchronized void stop() {
        ensureNotDestroyed();
        advance.runState.compareAndSet(RunState.RUNNING, RunState.STOPPING);
    }

    public void destroy() {
        while (true) {
            RunState runState = advance.runState.get();
            switch (runState) {
                case STOPPED:
                    if (advance.runState.compareAndSet(RunState.STOPPED, RunState.DESTROYED)) {
                        destroyImpl();
                        return;
                    }
                    continue;
                case RUNNING:
                case STOPPING:
                    throw new IllegalStateException("Cannot destroy a FlowSimulator that is not stopped");
                case DESTROYED:
                    // no-op
                    return;
            }
        }
    }

    private void destroyImpl() {
        queryInterface.destroy();
        queryInterface = null;
        map.clear();
        map = null;
        Arrays.fill(tectonicPoints, null);
        tectonicPoints = null;
        advance = null;
        Arrays.fill(fragments, null);
        fragments = null;
        Arrays.fill(entries, null);
        entries = null;
        observer = null;
        observerQueryLock = null;
        pendingWorkCount = null;
        //To change body of created methods use File | Settings | File Templates.
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
        return new Vector(x - p.x, y - p.y, z - p.z).normalize(0.005);
    }

    private Vector reverseTectonicVector(Point p) {
        NavigableMap<Double,TectonicPoint> closestPoints = new TreeMap<>();
        for (int i = 0; i < TectonicDensity.LOW.getNumTectonicPoints(); i++) {
            TectonicPoint point = tectonicPoints[i];
            closestPoints.put(Vector.computeDistance(p, point.point), point);
        }
        Iterator<TectonicPoint> iter = closestPoints.values().iterator();
        return iter.next().vector.sum(iter.next().vector).normalize(0.005);
    }

    public double getRadius() {
        return radius;
    }

    public double getTimeScale() {
        ensureNotDestroyed();
        return timeScale;
    }

    public Fragment[] getFragments() {
        ensureNotDestroyed();
        return fragments;
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
        if (advance.allPointsIn.get()) {
            synchronized (this) {
                notifyAll();
            }
        }
    }

    public synchronized void waitUntilAllIn() {
        ensureNotDestroyed();
        while (!advance.allPointsIn.get()) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilRunning() {
        ensureNotDestroyed();
        while (advance.runState.get() != RunState.RUNNING) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilDone() {
        ensureNotDestroyed();
        while (advance.runState.get() != RunState.STOPPED) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilStable() {
        ensureNotDestroyed();
        while (!queryInterface.isStable()) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void startThenStopOnceStable() {
        start();
        waitUntilStable();
        stop();
        waitUntilDone();
    }

    public synchronized void runOneQuery(FlowQuery query) {
        ensureNotDestroyed();
        if (advance.runState.get() != RunState.STOPPED) {
            throw new IllegalStateException("Flow simulator is not stopped");
        }
        if (observerQueryLock.get()) {
            throw new IllegalStateException("Cannot call runOneQuery() inside FlowObserver.frame()");
        }
        if (!pendingWorkCount.compareAndSet(0, 2)) {
            throw new IllegalStateException("Cannot call runOneQuery() while another query is running");
        }
        try {
            queryInterface.queryRunnerRecycleBin.get().init(query).run();
        } finally {
            assert pendingWorkCount.get() == 1;
            pendingWorkCount.set(0);
        }
    }

    public boolean areAllPointsIn() {
        ensureNotDestroyed();
        return advance.allPointsIn.get();
    }

    public int activeFragments() {
        ensureNotDestroyed();
        return map.size();
    }

    private void ensureNotDestroyed() {
        if (advance == null || advance.runState.get() == RunState.DESTROYED) {
            throw new IllegalStateException("Flow simulator has been destroyed");
        }
    }

    private void observeFrame() {
        if (!advance.allPointsIn.get()) {
            return;
        }
        queryInterface.frameCount++;
        if (queryInterface.frameCount >= 0 && observer != null) {
            observerQueryLock.set(true);
            queryInterface.advanceFrame();
            try {
                observer.frame(queryInterface);
            } finally {
                try {
                    while (pendingWorkCount.get() != 0) {
                        synchronized (pendingWorkCount) {
                            try {
                                pendingWorkCount.wait(250L);
                            } catch (InterruptedException e) {
                                // don't care
                            }
                        }
                    }
                } finally {
                    observerQueryLock.set(false);
                }
            }
        }
    }

    private void calculateNormalizedPressure() {
        CumulativeDeviation cumulativeDeviation = new CumulativeDeviation();
        pressureBias = 0.0;
        pressureScale = 1.0;
        assert queryInterface.isStable();
        for (FragmentImpl fragment : fragments) {
            double pressure = fragment.getPressure();
            cumulativeDeviation.add(pressure);
        }
        pressureBias = 0 - cumulativeDeviation.average();
        pressureScale = 1.0 / cumulativeDeviation.deviation();
    }

    public enum TectonicDensity {
        LOW, MEDIUM, HIGH;

        int getNumTectonicPoints() {
            switch (this) {
                case LOW:
                    return 32;
                case MEDIUM:
                    return 62;
                case HIGH:
                    return 122;
            }
            return -1;
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

    protected class FragmentImpl
            extends AbstractWorker
            implements SpatialMap.Consumer<FragmentImpl>, Comparator<Integer>, Fragment {

        private final int seq;
        private final Integer[] tectonicPointIndices = genIntSeq();
        private final MutableSimpleBounds bounds = new MutableSimpleBounds(true, 2.5);

        private Color color;

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
        private double a;
        private double p;

        public FragmentImpl(int seq) {
            this.seq = seq;
            this.color = Color.WHITE;
        }

        public <T> T getCenterXYZ(XForm.View<T> view) {
            return view.convert(x, y, z, 1);
        }

        public <T> T getVectorXYZ(XForm.View<T> view) {
            return view.convert(i, j, k, 1);
        }

        public double getPressure() {
            return (p + pressureBias) * pressureScale;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public Fragment immutableCopy() {
            return new ImmutableFragment(x, y, z, i, j, k, p, color);
        }

        @Override
        protected void doWork() {
            computeTectonicVector();
            map.intersect(bounds.load(x, y, z), this);
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

            a = 0.0;
            i = 0.0;
            j = 0.0;
            k = 0.0;
            double sumWeight = 0.0;
            double runOut = Math.sqrt(tectonicPointDistances[tectonicPointIndices[9]]);
            for (int a = 0; a < 9; a++) {
                double weight = runOut - Math.sqrt(tectonicPointDistances[tectonicPointIndices[a]]);
                sumWeight += weight;
                TectonicPoint t = tectonicPoints[tectonicPointIndices[a]];
                Vector v = t.vector;
                Point p = t.point;
                i += v.i * weight;
                j += v.j * weight;
                k += v.k * weight;
                this.a += t.attraction * weight;
            }
            if (sumWeight > 0.0) {
                sumWeight = 1.0 / sumWeight;
                i *= sumWeight;
                j *= sumWeight;
                k *= sumWeight;
                a *= sumWeight * sumWeight;
            }
        }

        private double d2 (double x, double y, double z) {
            return x * x + y * y + z * z;
        }

//        private double a1, a2, b1, b2, c1, c2;

        public void move(SpatialMap.Entry<FragmentImpl> entry) {
            double v = Vector.computeDistance(i, j, k);
            if (v > 0.0125) {
                i = i * 0.0125 / v;
                j = j * 0.0125 / v;
                k = k * 0.0125 / v;
            }
            double s = x + i * timeScale;
            double t = y + j * timeScale;
            double u = z + k * timeScale;
            double r = Vector.computeDistance(s, t, u);
            double d = radius / r;
            p = r - radius;// + a;
            x = s * d;
            y = t * d;
            z = u * d;
            entry.move(x, y, z);
        }

        public void found(SpatialMap.Entry<FragmentImpl> entry, double x, double y, double z) {
            FragmentImpl fragment = entry.get();
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

    private static class ImmutableFragment implements Fragment {

        private final double x;
        private final double y;
        private final double z;
        private final double i;
        private final double j;
        private final double k;
        private final double p;
        private final Color color;

        public ImmutableFragment(double x, double y, double z, double i, double j, double k, double p, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.i = i;
            this.j = j;
            this.k = k;
            this.p = p;
            this.color = color;
        }

        public <T> T getCenterXYZ(XForm.View<T> view) {
            return view.convert(x, y, z, 1);
        }

        public <T> T getVectorXYZ(XForm.View<T> view) {
            return view.convert(i, j, k, 1);
        }

        public double getPressure() {
            return p;
        }

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            throw new UnsupportedOperationException();
        }

        public Fragment immutableCopy() {
            return this;
        }
    }
    
    enum RunState {
        STOPPED, RUNNING, STOPPING, DESTROYED
    }

    protected class Advance extends AbstractWorker implements SpatialMap.Consumer<FragmentImpl> {

        private final AtomicReference<RunState> runState = new AtomicReference<RunState>(RunState.STOPPED);
        private final AtomicBoolean allPointsIn = new AtomicBoolean(false);
        private final BoundingSphere[] startingPoints;
        private final AtomicInteger counter = new AtomicInteger();

        private int bootstrapFrameCount = 0;
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
                bootstrapFrameCount++;
                while (l < fragments.length && j < startingPoints.length) {
                    if (insert(fragments[l], startingPoints[j])) {
                        l++;
                    }
                    j++;
                }
                if (l >= fragments.length) {
                    allPointsIn.set(true);
                }
            } else if (bootstrapFrameCount > 0) {
                bootstrapFrameCount -= 2;
                if (bootstrapFrameCount <= 0) {
                    queryInterface.stable.set(true);
                    calculateNormalizedPressure();
                }
            }
//            try {
//                Thread.sleep(90000L);
//            } catch (InterruptedException e) { }
            for (int i = 0; i < l; i++) {
                SpatialMap.Entry<FragmentImpl> entry = entries[i];
                FragmentImpl fragment = entry.get();
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

        private boolean insert(FragmentImpl fragment, BoundingSphere startingPoint) {
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

        public void found(SpatialMap.Entry<FragmentImpl> entry, double x, double y, double z) {
            counter.incrementAndGet();
        }
    }

    protected class QueryInterface implements FlowQueryInterface {

        private WorkManager workManager = new WorkManager("Query");
        private AtomicInteger seq = new AtomicInteger(1);
        private AtomicBoolean stable = new AtomicBoolean(false);
        private RecycleBin<QueryRunner> queryRunnerRecycleBin;
        private RecycleBin<WorkRunner> workRunnerRecycleBin;

        int frameCount = (int)Math.round(-100.0 / timeScale);
        WorkSourceKey<Void> lastKey = null;

        private QueryInterface() {
            {
                RecycleDeque<WeakReference<QueryRunner>> recycleDeque = new RecycleDeque<WeakReference<QueryRunner>>();
                Factory<QueryRunner> runnerFactory = new Factory<QueryRunner>() {
                    public QueryRunner createEmpty() {
                        return new QueryRunner(queryRunnerRecycleBin, nominalRadius, captureFragmentContributions);
                    }
                };
                queryRunnerRecycleBin = new RecycleBin<QueryRunner>(runnerFactory, recycleDeque);
            }
            {
                RecycleDeque<WeakReference<WorkRunner>> recycleDeque = new RecycleDeque<WeakReference<WorkRunner>>();
                Factory<WorkRunner> runnerFactory = new Factory<WorkRunner>() {
                    public WorkRunner createEmpty() {
                        return new WorkRunner(workRunnerRecycleBin);
                    }
                };
                workRunnerRecycleBin = new RecycleBin<WorkRunner>(runnerFactory, recycleDeque);
            }
        }

        private void ensureInsideQueryCall(final String methodName) {
            pendingWorkCount.incrementAndGet();
            if (!observerQueryLock.get()) {
                oneLess();
                throw new IllegalStateException("Cannot call " + methodName + "() outside a call to FlowObserver.frame()");
            }
        }

        public boolean isStable() {
            return stable.get();
        }

        void advanceFrame() {
            lastKey = null;
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
            if (pendingWorkCount.decrementAndGet() == 0) {
                synchronized (pendingWorkCount) {
                    pendingWorkCount.notifyAll();
                }
            }
        }

        public void query(final Iterable<? extends FlowQuery> queries) {
            ensureInsideQueryCall("query");
            try {
                WorkSourceKey<Void> key = WorkSourceKey.create("query-" + seq.get(), Void.class);
                if (lastKey == null) {
                    lastKey = key;
                }
                workManager.addWorkSource(new WorkSourceAdapter<Void>(key) {
                    public void addWork(WorkQueue workQueue, Results dependencies) {
                        for (final FlowQuery query : queries) {
                            pendingWorkCount.incrementAndGet();
                            workQueue.addWorkUnit(getQueryRunner(query));
                        }
                    }
                });
                workManager.getResults().getResult(key);
            } finally {
                pendingWorkCount.decrementAndGet();
            }
        }

        public void runNext(final Iterable<? extends Runnable> work) {
            ensureInsideQueryCall("runNext");
            if (lastKey == null) {
                throw new IllegalStateException("Cannot call runNext() before calling query()");
            }
            try {
                WorkSourceKey<Void> key = WorkSourceKey.create(lastKey.getName() + ".next", Void.class);
                workManager.addWorkSource(new WorkSourceAdapter<Void>(key, lastKey) {
                    public void addWork(WorkQueue workQueue, Results dependencies) {
                        for (final Runnable runnable : work) {
                            pendingWorkCount.incrementAndGet();
                            workQueue.addWorkUnit(getWorkRunner(runnable));
                        }
                    }
                });
                workManager.getResults().getResult(key);
            } finally {
                pendingWorkCount.decrementAndGet();
            }
        }

        protected QueryRunner getQueryRunner(FlowQuery query) {
            return queryRunnerRecycleBin.get().init(query);
        }

        protected WorkRunner getWorkRunner(Runnable runnable) {
            return workRunnerRecycleBin.get().init(runnable);
        }

        protected void destroy() {
            if (workManager != null) {
                workManager.destroy();
                workManager = null;
                seq = null;
                stable = null;
                queryRunnerRecycleBin = null;
                workRunnerRecycleBin = null;
            }
        }
    }

    protected class QueryRunner extends AbstractFlowPointData implements Runnable, SpatialMap.Consumer<FragmentImpl>, Recyclable<QueryRunner> {

        private final RecycleBin<QueryRunner> recycleBin;
        private final WeakReference<QueryRunner> me;
        private final MutableSimpleBounds bounds = new MutableSimpleBounds(true, nominalFragmentRadius * 3);

        private QueryRunner(RecycleBin<QueryRunner> recycleBin, double nominalRadius, boolean captureContributions) {
            super(nominalRadius, captureContributions);
            if (recycleBin == null) throw new NullPointerException("recycleBin");
            this.recycleBin = recycleBin;
            this.me = new WeakReference<QueryRunner>(this);
        }

        public QueryRunner init(FlowQuery query) {
            load(radius, query);
            return this;
        }

        public WeakReference<QueryRunner> getMyWeakReference() {
            return me;
        }

        public void run() {
            try {
                begin(radius);
                map.intersect(bounds.load(x, y, z), this);
                processData();
            } finally {
                queryInterface.oneLess();
                synchronized (recycleBin) {
                    recycleBin.put(this);
                }
            }
        }

        public void found(SpatialMap.Entry<FragmentImpl> entry, double x, double y, double z) {
            fragmentConsumer.consume(entry.get());
        }

    }

    protected class WorkRunner implements Runnable, Recyclable<WorkRunner> {

        private final RecycleBin<WorkRunner> recycleBin;
        private final WeakReference<WorkRunner> me;

        private Runnable runnable;

        private WorkRunner(RecycleBin<WorkRunner> recycleBin) {
            if (recycleBin == null) throw new NullPointerException("recycleBin");
            this.recycleBin = recycleBin;
            this.me = new WeakReference<WorkRunner>(this);
        }

        public WorkRunner init(Runnable runnable) {
            this.runnable = runnable;
            return this;
        }

        public WeakReference<WorkRunner> getMyWeakReference() {
            return me;
        }

        public void run() {
            try {
                runnable.run();
            } finally {
                queryInterface.oneLess();
                synchronized (recycleBin) {
                    recycleBin.put(this);
                }
            }
        }

    }

}
