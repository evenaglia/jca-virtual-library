package net.venaglia.realms.common.util.work;

import net.venaglia.common.util.ProgressListener;
import net.venaglia.common.util.ProgressMonitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 1/23/13
 * Time: 5:49 PM
 */
public class WorkManager {

    private static final ThreadLocal<WorkSourceWrapper<?>> ACTIVE_WORK_SOURCE = new ThreadLocal<WorkSourceWrapper<?>>();
    private static final int ONE_COMPLETE_WORK = 1048576;

    private final WorkHandler workHandler;
    private final Map<WorkSourceKey<?>,WorkSourceWrapper<?>> queue = new ConcurrentHashMap<WorkSourceKey<?>,WorkSourceWrapper<?>>();
    private final CompositeProgressMonitor progressMonitor = new CompositeProgressMonitor();

    private final Results results = new Results() {
        @SuppressWarnings("unchecked")
        public <T> T getResult(WorkSourceKey<T> key) throws NoSuchElementException, CircularDependencyException {
            blockForResult(key, new LinkedHashSet<WorkSourceKey<?>>());
            assert allAreDone(queue.get(key).dependsOn);
            return (T)queue.get(key).result;
        }
    };

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private void blockForResult(WorkSourceKey<?> key, Set<WorkSourceKey<?>> dependsOn) {
        final WorkSourceWrapper<?> workSourceWrapper = queue.get(key);
        if (workSourceWrapper == null) {
            throw new NoSuchElementException(key.toString());
        }
        if (dependsOn.contains(key)) {
            dependsOn.add(key);
            throw new CircularDependencyException(getDependenciesAsString(dependsOn));
        }
        if (workSourceWrapper.state.get().done()) {
            assert allAreDone(workSourceWrapper.dependsOn);
            return; // done
        }
        dependsOn.add(key);
        try {
            for (WorkSourceKey<?> dependent : workSourceWrapper.dependsOn) {
                blockForResult(dependent, dependsOn);
            }
        } finally {
            dependsOn.remove(key);
        }
        if (allAreDone(workSourceWrapper.dependsOn)) {
            synchronized (workSourceWrapper) {
                addToQueue(workSourceWrapper);
                while (!workSourceWrapper.state.get().done()) {
                    try {
                        workSourceWrapper.wait();
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            }
        }
    }

    private String getDependenciesAsString(Set<WorkSourceKey<?>> dependsOn) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (WorkSourceKey<?> dependent : dependsOn) {
            if (first) {
                first = false;
            } else {
                buffer.append(" -> ");
            }
            buffer.append(dependent);
        }
        return buffer.toString();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private <T> void addToQueue(final WorkSourceWrapper<T> workSourceWrapper) {
        if (!workSourceWrapper.state.compareAndSet(WorkSourceState.NEW, WorkSourceState.START)) {
            return;
        }
        synchronized (workSourceWrapper) {
            Results filteredResults = new Results() {
                public <T> T getResult(WorkSourceKey<T> key) throws NoSuchElementException, CircularDependencyException{
                    if (workSourceWrapper.dependsOn.contains(key)) {
                        @SuppressWarnings("unchecked")
                        WorkSourceWrapper<T> wsw = (WorkSourceWrapper<T>)queue.get(key);
                        return wsw.result;
                    }
                    throw new NoSuchElementException(key.toString());
                }
            };
            workSourceWrapper.workSource.addWork(workSourceWrapper, filteredResults);
            workSourceWrapper.state.set(WorkSourceState.PENDING);
            workSourceWrapper.decrement();
        }
    }

    private boolean allAreDone(Set<WorkSourceKey<?>> dependsOn) {
        for (WorkSourceKey<?> dependent : dependsOn) {
            WorkSourceWrapper<?> workSourceWrapper = queue.get(dependent);
            if (workSourceWrapper == null) {
                throw new NoSuchElementException(dependent.toString());
            }
            if (!workSourceWrapper.state.get().done()) {
                return false;
            }
        }
        return true;
    }

    public WorkManager(final String name) {
        workHandler = new WorkHandler(name);
    }

    public <T> void addWorkSource(WorkSource<T> workSource) {
        addWorkSource(workSource, 1.0);
    }

    public <T> void addWorkSource(WorkSource<T> workSource, double relativeWeight) {
        WorkSourceWrapper<T> workSourceWrapper = new WorkSourceWrapper<T>(workSource, relativeWeight);
        queue.put(workSource.getKey(), workSourceWrapper);
        if (allAreDone(workSourceWrapper.dependsOn)) {
            addToQueue(workSourceWrapper);
        }
    }

    public Results getResults() {
        return results;
    }

    public ProgressMonitor getProgressMonitor() {
        return progressMonitor;
    }

    private enum WorkSourceState {
        NEW, START, PENDING, COMPLETE, FAIL;

        public boolean running() {
            return this == PENDING;
        }

        public boolean done() {
            return this == COMPLETE || this == FAIL;
        }
    }

    private class CompositeProgressMonitor implements ProgressMonitor {

        public int getNumberOfSteps() {
            return queue.isEmpty() ? -1 : queue.size();
        }

        public int getCurrentStepNumber() {
            if (queue.isEmpty()) {
                return -1;
            }
            int done = 0;
            for (WorkSourceWrapper<?> w : queue.values().toArray(new WorkSourceWrapper[queue.size()])) {
                if (w.state.get().done()) {
                    done++;
                }
            }
            return done;
        }

        public String getCurrentStepName() {
            for (WorkSourceWrapper<?> w : queue.values().toArray(new WorkSourceWrapper[queue.size()])) {
                if (w.state.get().running()) {
                    return w.workSource.getKey().getName();
                }
            }
            return null;
        }

        public double getProgress() {
            if (queue.isEmpty()) {
                return Double.NaN;
            }
            double sum = 0.0;
            double sumWeights = 0.0;
            for (WorkSourceWrapper<?> w : queue.values().toArray(new WorkSourceWrapper[queue.size()])) {
                sumWeights += w.relativeWeight;
                WorkSourceState state = w.state.get();
                if (state.done()) {
                    sum += w.relativeWeight;
                } else if (state == WorkSourceState.PENDING) {
                    if (w.total.doubleValue() == 0) {
                        sum += w.relativeWeight;
                    } else {
                        double fraction = w.workInProgress.doubleValue() / ((double)ONE_COMPLETE_WORK);
                        double myPart = w.relativeWeight * (w.count.doubleValue() + fraction) / (w.total.doubleValue());
                        sum += myPart;
                    }
                }
            }
            return sum / sumWeights;
        }

        public ProgressListener getProgressListener() {
            throw new UnsupportedOperationException();
        }
    }

    private class WorkSourceWrapper<T> implements WorkQueue {

        private final WorkSource<T> workSource;
        private final double relativeWeight;
        private final Set<WorkSourceKey<?>> dependsOn;
        private final AtomicInteger count = new AtomicInteger(-1);
        private final AtomicLong workInProgress = new AtomicLong();
        private final AtomicInteger total = new AtomicInteger();
        private final AtomicBoolean fail = new AtomicBoolean();
        private final AtomicReference<WorkSourceState> state = new AtomicReference<WorkSourceState>(WorkSourceState.NEW);

        private T result;

        private WorkSourceWrapper(WorkSource<T> workSource, double relativeWeight) {
            this.workSource = workSource;
            this.relativeWeight = relativeWeight;
            WorkSourceKey<?>[] dependencies = workSource.getDependencies();
            if (dependencies == null) {
                dependencies = WorkSourceKey.NO_DEPENDENCIES;
            }
            this.dependsOn = new HashSet<WorkSourceKey<?>>(Arrays.asList(dependencies));
        }

        public void addWorkUnit(final Runnable runnable) {
            if (state.get() != WorkSourceState.START) {
                WorkSourceWrapper<?> active = ACTIVE_WORK_SOURCE.get();
                if (active == null) {
                    throw new IllegalStateException();
                }
                if (this != active) {
                    active.addWorkUnit(runnable);
                    return;
                }
            }
            total.incrementAndGet();
            workHandler.addWorkUnit(new Runnable() {
                public void run() {
                    boolean success = false;
                    ACTIVE_WORK_SOURCE.set(WorkSourceWrapper.this);
                    MyProgressExporter exporter = null;
                    try {
                        if (runnable instanceof ProgressExportingRunnable) {
                            exporter = new MyProgressExporter();
                            ((ProgressExportingRunnable)runnable).setProgressExporter(exporter);
                        }
                        runnable.run();
                        success = true;
                    } finally {
                        ACTIVE_WORK_SOURCE.remove();
                        if (!success) {
                            fail.set(true);
                        }
                        if (exporter != null) {
                            exporter.stop();
                        }
                        decrement();
                    }
                }
            });
        }

        public void decrement() {
            int newCount = count.incrementAndGet();
//            System.out.println("decrement() : count = " + (newCount-1) + " -> " + newCount + " -- total: " + total.get());
            if (newCount >= total.get()) {
                synchronized (this) {
                    if (fail.get()) {
                        state.set(WorkSourceState.FAIL);
                    } else {
                        result = workSource.getResult();
                        state.set(WorkSourceState.COMPLETE);
                    }
                    notifyAll();
                }
            }
        }

        @Override
        public String toString() {
            return String.format("WorkSource[%s](%d/%d items, state=%s)", workSource.getKey(), count.get(), total.get(), state.get());
        }

        private class MyProgressExporter implements ProgressExportingRunnable.ProgressExporter {

            private boolean active = true;
            private long lastSoFar = 0;
            private long lastTotal = 0;
            private long priorCount = 0;

            public void exportProgress(long soFar, long total) {
                exportProgress(((double)soFar)/((double)total));
                lastSoFar = soFar;
                lastTotal = total;
            }

            public void exportProgress(double percentage) {
                if (active) {
                    percentage = Math.max(Math.min(percentage, 1), 0);
                    set(percentage);
                }
            }

            public void oneMore() {
                if (lastTotal <= 0) {
                    throw new IllegalStateException();
                }
                exportProgress(lastSoFar + 1, lastTotal);
            }

            private synchronized void set(double percentage) {
                long count = Math.round(percentage * ONE_COMPLETE_WORK);
                workInProgress.addAndGet(count - priorCount);
                priorCount = count;
            }

            void stop() {
                active = false;
                set(0.0);
            }
        }
    }
}
