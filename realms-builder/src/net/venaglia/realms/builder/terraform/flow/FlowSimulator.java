package net.venaglia.realms.builder.terraform.flow;

import net.venaglia.common.util.CumulativeDeviation;
import net.venaglia.common.util.Factory;
import net.venaglia.common.util.Ref;
import net.venaglia.common.util.impl.AbstractCachingRef;
import net.venaglia.common.util.recycle.Recyclable;
import net.venaglia.common.util.recycle.RecycleBin;
import net.venaglia.common.util.recycle.RecycleDeque;
import net.venaglia.gloo.physical.bounds.BoundingBox;
import net.venaglia.gloo.physical.bounds.BoundingSphere;
import net.venaglia.gloo.physical.bounds.MutableSimpleBounds;
import net.venaglia.gloo.physical.decorators.Color;
import net.venaglia.gloo.physical.decorators.Material;
import net.venaglia.gloo.physical.geom.Facet;
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
import java.util.List;
import java.util.Random;
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
    private final int count;
    private final int numTectonicPoints = 32;
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

    private final QueryInterface queryInterface = new QueryInterface();
    private final AtomicBoolean observerQueryLock = new AtomicBoolean(false);
    private final AtomicInteger pendingWorkCount = new AtomicInteger();

    public FlowSimulator(double radius, int count, double fps, double timeScale) {
        this(radius, count, fps, timeScale, System.currentTimeMillis(), false);
    }

    public FlowSimulator(double radius, int count, double fps, double timeScale, long seed, boolean captureContributions) {
        this.radius = radius;
        this.count = count;
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
            tectonicPoints[index++] = new TectonicPoint(p, randomTectonicVector(random, p),
                                                        TectonicPoint.PointClass.PLATONIC);
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
            tectonicPoints[index++] = new TectonicPoint(p, randomTectonicVector(random, p),
                                                        TectonicPoint.PointClass.MIDPOINT);
        }
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

    public int getCount() {
        return count;
    }

    public List<TectonicVectorArrow> getTectonicArrows(double radius) {
        Material green = Material.makeSelfIlluminating(Color.GREEN);
        List<TectonicVectorArrow> arrows = new ArrayList<>(tectonicPoints.length);
        for (TectonicPoint tectonicPoint : tectonicPoints) {
            arrows.add(TectonicVectorArrow.createArrow(tectonicPoint)
                               .scale(radius)
                               .setMaterial(green));
        }
        return Collections.unmodifiableList(arrows);
    }

    public void setObserver(FlowObserver observer) {
        this.observer = observer;
    }

    public synchronized void start() {
        switch (advance.runState.getAndSet(RunState.RUNNING)) {
            case STOPPED:
                queueIsEmpty();
                waitUntilAllIn();
        }
    }

    public void stop() {
        advance.runState.compareAndSet(RunState.RUNNING, RunState.STOPPING);
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

    public double getTimeScale() {
        return timeScale;
    }

    public Fragment[] getFragments() {
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
        while (!advance.allPointsIn.get()) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilRunning() {
        while (advance.runState.get() != RunState.RUNNING) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilDone() {
        while (advance.runState.get() != RunState.STOPPED) {
            try {
                wait(250L);
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public synchronized void waitUntilStable() {
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

            i = 0.0;
            j = 0.0;
            k = 0.0;
            double sumWeight = 0.0;
            double runOut = Math.sqrt(tectonicPointDistances[tectonicPointIndices[9]]);
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
            p = r - radius;
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
        STOPPED, RUNNING, STOPPING,
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

        private final WorkManager workManager = new WorkManager("Query");
        private final AtomicInteger seq = new AtomicInteger(1);
        private final AtomicBoolean stable = new AtomicBoolean(false);
        private final RecycleBin<QueryRunner> queryRunnerRecycleBin;
        private final RecycleBin<WorkRunner> workRunnerRecycleBin;

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
