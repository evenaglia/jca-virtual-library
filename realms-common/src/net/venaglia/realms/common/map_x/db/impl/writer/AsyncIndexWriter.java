package net.venaglia.realms.common.map_x.db.impl.writer;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 8/21/13
 * Time: 8:47 AM
 */
class AsyncIndexWriter extends Observable {

    static final AsyncIndexWriter INSTANCE = new AsyncIndexWriter();

    enum State {
        STOPPED,
        STARTING,
        IDLE,
        RUNNING,
        STOPPING,
        TERMINATING,
        DONE;

        private static final Map<State,State> NEXT_STATES = new EnumMap<State,State>(State.class);

        static {
            NEXT_STATES.put(STOPPED, STOPPED);
            NEXT_STATES.put(STARTING, IDLE);
            NEXT_STATES.put(IDLE, RUNNING);
            NEXT_STATES.put(RUNNING, IDLE);
            NEXT_STATES.put(STOPPING, STOPPED);
            NEXT_STATES.put(TERMINATING, DONE);
            NEXT_STATES.put(DONE, DONE);
        }

        boolean isStable() {
            return NEXT_STATES.get(this) == this;
        }

        State nextState() {
            return NEXT_STATES.get(this);
        }
    }

    private final AtomicReference<State> state = new AtomicReference<State>(State.STOPPED);
    private final Map<String,Runnable> queue = new LinkedHashMap<String,Runnable>();
    private final Lock lock = new ReentrantLock(true);
    private final BlockingDeque<String> completedQueue = new LinkedBlockingDeque<String>();

    private static final String QUEUE_IS_EMPTY_KEY = "__!!queue-is-empty!!__";

    private AsyncIndexWriter() {
    }

    private synchronized void start() {
        State now = state.get();
        if (now != State.STOPPED) {
            throw new IllegalStateException("Cannot start the worker when it is not stopped");
        }
        state.set(State.STARTING);
        new Thread("Async Index Writer") {

            private final Collection<Map.Entry<String,Runnable>> queue = AsyncIndexWriter.this.queue.entrySet();
            private boolean notifiedQueueIsEmpty = true;

            @Override
            public void run() {
                try {
                    if (state.compareAndSet(AsyncIndexWriter.State.STARTING, AsyncIndexWriter.State.RUNNING)) {
                        if (queue.size() < 25) sleep();
                        while (state.get() == AsyncIndexWriter.State.RUNNING) {
                            runSafely();
                        }
                    }
                } finally {
                    state.set(state.get().nextState());
                    completedQueue.add(null);
                }
            }

            private void runSafely() {
                try {
                    runOne();
                } catch (ThreadDeath td) {
                    throw td;
                } catch (Throwable t) {
                    try {
                        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(this, t);
                    } catch (ThreadDeath td) {
                        throw td;
                    } catch (Throwable th) {
                        // ignore it
                    }
                }
            }

            private void runOne() {
                Runnable work = null;
                lock.lock();
                String key = null;
                try {
                    if (queue.isEmpty() && state.compareAndSet(AsyncIndexWriter.State.RUNNING, AsyncIndexWriter.State.IDLE)) {
                        if (!notifiedQueueIsEmpty) {
                            notifiedQueueIsEmpty = true;
                            completedQueue.add(QUEUE_IS_EMPTY_KEY);
                        }
                        try {
                            sleep();
                        } finally {
                            state.compareAndSet(AsyncIndexWriter.State.IDLE, AsyncIndexWriter.State.RUNNING);
                        }
                    } else if (!queue.isEmpty()) {
                        notifiedQueueIsEmpty = false;
                        Iterator<Map.Entry<String,Runnable>> iterator = queue.iterator();
                        Map.Entry<String,Runnable> entry = iterator.next();
                        key = entry.getKey();
                        work = entry.getValue();
                        iterator.remove();
                    }
                } finally {
                    lock.unlock();
                }
                if (work == null && state.compareAndSet(AsyncIndexWriter.State.RUNNING, AsyncIndexWriter.State.IDLE)) {
                    try {
                        sleep();
                    } finally {
                        state.compareAndSet(AsyncIndexWriter.State.IDLE, AsyncIndexWriter.State.RUNNING);
                    }
                } else if (work != null) {
                    try {
                        work.run();
                    } finally {
                        completedQueue.add(key);
                    }
                }
            }

            private void sleep() {
                synchronized (AsyncIndexWriter.this) {
                    try {
                        AsyncIndexWriter.this.wait(2500);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            }
        }.start();
        new Thread("Async Index Write Completion Notifier") {
            @Override
            public void run() {
                while (true) {
                    try {
                        String key = completedQueue.take();
                        if (key == null) {
                            return; // all done
                        }
                        //noinspection StringEquality
                        AsyncIndexWriter.this.notifyObservers(key == QUEUE_IS_EMPTY_KEY ? null : key);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            }
        }.start();
    }

    void stop(boolean terminate) {
        State s = state.get();
        switch (s) {
            case STARTING:
            case RUNNING:
            case IDLE:
                if (!state.compareAndSet(s, terminate ? State.TERMINATING : State.STOPPING)) {
                    stop(terminate);
                }
                break;
            case STOPPED:
                if (!state.compareAndSet(s, terminate ? State.DONE : State.STOPPED)) {
                    stop(terminate);
                }
                break;
        }
    }

    private void ensureStarted() {
        State s = state.get();
        switch (s) {
            case STOPPED:
                start();
                break;
            case TERMINATING:
            case DONE:
                throw new IllegalStateException("AsyncIndexUpdater has been terminated");
            case STOPPING:
                while (state.get() == State.STOPPING) Thread.yield();
                ensureStarted();
                break;
        }
    }

    boolean cancel(String identifier) {
        lock.lock();
        try {
            return queue.remove(identifier) != null;
        } finally {
            lock.unlock();
        }
    }

    void queue(String identifier, Runnable work) {
        if (work == null) {
            return;
        }
        if (identifier == null) {
            throw new NullPointerException("identifier");
        }
        boolean big = false;
        lock.lock();
        try {
            queue.remove(identifier);
            queue.put(identifier, work);
            big = queue.size() > 25;
        } finally {
            lock.unlock();
        }
        ensureStarted();
        if (big && state.get() == State.IDLE) {
            synchronized (this) {
                // wake up the sleeping thread
                this.notify();
            }
        }
    }
}
