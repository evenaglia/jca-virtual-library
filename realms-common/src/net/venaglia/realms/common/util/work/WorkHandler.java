package net.venaglia.realms.common.util.work;

import net.venaglia.common.util.ProgressListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 12/3/12
 * Time: 8:56 AM
 */
public class WorkHandler {

    private final ThreadPoolExecutor executor;
    private final AtomicInteger processCounter = new AtomicInteger();
    private final Lock runWhenCompleteLock = new ReentrantLock(true);
    private final List<Runnable> runWhenComplete = new LinkedList<Runnable>();

    public WorkHandler(final String name) {
        int numThreads = Runtime.getRuntime().availableProcessors() * 2;
        final String threadNameFormat = "%sWorker-%0" + String.valueOf(numThreads).length() + "d";
        BlockingQueue<Runnable> queue = new LinkedBlockingDeque<Runnable>();
        ThreadFactory threadFactory = new ThreadFactory() {

            private final ThreadGroup tg = new ThreadGroup(name);
            private final AtomicInteger seq = new AtomicInteger();

            {
                tg.setDaemon(true);
            }

            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(tg, runnable, String.format(threadNameFormat, name, seq.incrementAndGet()));
                thread.setDaemon(true);
                return thread;
            }
        };
        executor = new ThreadPoolExecutor(numThreads, numThreads, 5, TimeUnit.SECONDS, queue, threadFactory);
    }

    public void addWorkUnit(Runnable runnable) {
        addWorkUnit(runnable, null);
    }

    public void addWorkUnit(final Runnable runnable, final ProgressListener progressListener) {
        processCounter.incrementAndGet();
        executor.execute(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    processCompeted();
                    if (progressListener != null) {
                        progressListener.nextStep();
                    }
                }
            }
        });
    }

    private void processCompeted() {
        if (processCounter.decrementAndGet() == 0) {
            List<Runnable> toRun;
            runWhenCompleteLock.lock();
            try {
                toRun = new ArrayList<Runnable>(runWhenComplete);
                runWhenComplete.clear();
            } finally {
                runWhenCompleteLock.unlock();
            }
            for (Runnable runnable : toRun) {
                addWorkUnit(runnable);
            }
        }
    }

    public void runWhenQueueIsCompleted(Runnable runnable) {
        if (processCounter.get() == 0) {
            addWorkUnit(runnable);
        } else {
            runWhenCompleteLock.lock();
            try {
                runWhenComplete.add(runnable);
            } finally {
                runWhenCompleteLock.unlock();
            }
        }
    }
}
