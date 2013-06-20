package com.jivesoftware.jcalibrary.scheduler;

import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: ed
 * Date: 5/9/13
 * Time: 10:57 PM
 */
public class WorkScheduler {

    private static final WorkScheduler INSTANCE = new WorkScheduler();

    private final ThreadGroup threadGroup = new ThreadGroup("Scheduler");
    private final Stack<Integer> recycledThreadIds = new Stack<Integer>();
    private final AtomicInteger threadIdSeq = new AtomicInteger(1);
    private final ThreadFactory threadFactory = new ThreadFactory() {

        private synchronized int getThreadId() {
            return recycledThreadIds.isEmpty() ? threadIdSeq.getAndIncrement() : recycledThreadIds.pop();
        }


        private synchronized void ungetThreadId(int threadId) {
            recycledThreadIds.push(threadId);
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Thread newThread(final Runnable runnable) {
            final int threadId = getThreadId();
            Runnable target = new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } finally {
                        ungetThreadId(threadId);
                    }
                }
            };
            String threadName = String.format("ScheduledWorker-%02d", threadId);
            Thread thread = new Thread(threadGroup, target, threadName);
            thread.setDaemon(true);
            return thread;
        }
    };
    private final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(8, threadFactory);

    private WorkScheduler() {
        // private constructor
    }

    public static ScheduledFuture<?> once(Runnable runnable, int interval, TimeUnit timeUnit) {
        return INSTANCE.executor.schedule(runnable, interval, timeUnit);
    }

    public static <T> ScheduledFuture<T> once(Callable<T> callable, int interval, TimeUnit timeUnit) {
        return INSTANCE.executor.schedule(callable, interval, timeUnit);
    }

    public static Future<?> now(Runnable runnable) {
        return once(runnable, 0, TimeUnit.SECONDS);
    }

    public static <T> Future<T> now(Callable<T> callable) {
        return once(callable, 0, TimeUnit.SECONDS);
    }

    public static Future<?> interval(Runnable runnable, int interval, TimeUnit timeUnit) {
        return INSTANCE.executor.scheduleAtFixedRate(runnable, 0, interval, timeUnit);
    }
}
