package net.venaglia.realms.common.map.data;

import net.venaglia.common.util.Ref;
import net.venaglia.realms.common.map.UniqueIdSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: ed
 * Date: 3/23/14
 * Time: 10:15 PM
 */
public abstract class AbstractUniqueIdSource implements UniqueIdSource {

    private static final Collection<WeakReference<Runnable>> KNOWN_ID_SOURCE_UPDATERS =
            new LinkedList<WeakReference<Runnable>>();
    private static final int THRESHOLD = 24;

    static {
        Thread pipeline = new Thread(new UniqueIdSourceUpdater(), "Unique Id Source Updater");
        pipeline.setDaemon(true);
        pipeline.start();
    }

    private final List<IdRange> available = new ArrayList<IdRange>(2);
    private final Lock lock = new ReentrantLock();
    private final Sequence sequence;
    private final IdFetcher fetcher;
    private final Ref<UUID> instanceUUIDRef;

    private File local;

    protected IdRange active;
    protected long current;
    protected volatile int remaining = Integer.MIN_VALUE;

    protected AbstractUniqueIdSource(Sequence sequence, Ref<UUID> instanceUUIDRef) {
        if (sequence == null) throw new NullPointerException("sequence");
        if (instanceUUIDRef == null) throw new NullPointerException("instanceUUIDRef");
        this.sequence = sequence;
        this.instanceUUIDRef = instanceUUIDRef;
        this.fetcher = new IdFetcher();
    }

    private void init() {
        this.local = new File(sequence + "." + instanceUUIDRef.get() + ".id-source.seq");
        synchronized (KNOWN_ID_SOURCE_UPDATERS) {
            KNOWN_ID_SOURCE_UPDATERS.add(new WeakReference<Runnable>(fetcher));
            KNOWN_ID_SOURCE_UPDATERS.notifyAll();
        }
        boolean wakeUpUpdaters = false;
        lock.lock();
        try {
            if (local.exists()) {
                BufferedReader in = null;
                try {
                    in = new BufferedReader(new FileReader(local));
                    for (String line = in.readLine(); line != null; line = in.readLine()) {
                        if (line.length() > 0) {
                            available.add(new IdRange(line));
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        if (in != null) {
                            in.close();
                        }
                    } catch (IOException e) {
                        // don't care
                    }
                }
                wakeUpUpdaters = recalculate(true);
            } else {
                wakeUpUpdaters = recalculate(false);
            }
        } finally {
            lock.unlock();
        }
        if (wakeUpUpdaters) {
            synchronized (KNOWN_ID_SOURCE_UPDATERS) {
                KNOWN_ID_SOURCE_UPDATERS.notifyAll(); // wake up the other threads
            }
        }
    }

    private boolean recalculate(boolean confirmNext) {
        active = available.isEmpty() ? null : available.get(0);
        current = active == null ? -1L : (confirmNext ? lookupNextAvailableInRange(active) : active.getStart());
        remaining = active == null ? 0 : active.sizeFrom(current);
        for (int i = 1; i < available.size(); i++) {
            remaining += available.get(i).size();
        }
        return remaining < THRESHOLD;
    }

    public String getName() {
        return sequence.name();
    }

    public Sequence getSequence() {
        return sequence;
    }

    public long next() {
        lock.lock();
        try {
            while (remaining < THRESHOLD) {
                boolean doInit = remaining == Integer.MIN_VALUE;
                if (doInit) {
                    remaining = 0;
                }
                lock.unlock(); // gonna let this one go
                try {
                    if (doInit) {
                        init();
                    }
                    synchronized (KNOWN_ID_SOURCE_UPDATERS) {
                        KNOWN_ID_SOURCE_UPDATERS.notifyAll(); // wake up the id source thread
                        KNOWN_ID_SOURCE_UPDATERS.wait(250L);
                    }
                } catch (InterruptedException e) {
                    // don't care
                } finally {
                    lock.lock(); // and we're back
                }
            }
            remaining--;
            return current++;
        } finally {
            String updateAvailable = null;
            try {
                if (active != null && current >= active.end) {
                    available.remove(0);
                    recalculate(false);
                    updateAvailable = toString(available);
                }
            } finally {
                lock.unlock();
            }
            if (updateAvailable != null) {
                fetcher.writeToDisk(updateAvailable);
            }
        }
    }

    private String toString(List<IdRange> available) {
        StringBuilder buffer = new StringBuilder(64);
        for (IdRange range : available) {
            buffer.append(range).append("\n");
        }
        return buffer.toString();
    }

    protected abstract IdRange getNextRange();

    protected abstract long lookupNextAvailableInRange(IdRange range);

    protected static class IdRange {
        private final long start; // inclusive
        private final long end;   // exclusive

        public IdRange(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public IdRange(String fromFile) {
            String[] pair = fromFile.split("\\.\\.");
            start = Long.parseLong(pair[0], 16);
            end = Long.parseLong(pair[1], 16);
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public int size() {
            return (int)(end - start);
        }

        private int sizeFrom(long mid) {
            return (int)(end - mid);
        }

        @Override
        public String toString() {
            return String.format("%x..%x", start, end);
        }
    }

    private class IdFetcher implements Runnable {

        public void run() {
            String updateAvailable = null;
            lock.lock();
            try {
                for (int i = 0; remaining < THRESHOLD; i++) {
                    IdRange range = getNextRange();
                    if (available.isEmpty()) {
                        current = range.getStart();
                        active = range;
                    }
                    available.add(range);
                    remaining += range.size();
                    if (i == 0) {
                        lock.unlock();
                        try {
                            synchronized (KNOWN_ID_SOURCE_UPDATERS) {
                                KNOWN_ID_SOURCE_UPDATERS.notifyAll();
                            }
                        } finally {
                            lock.lock();
                        }
                    }
                    if (remaining >= THRESHOLD) {
                        updateAvailable = AbstractUniqueIdSource.this.toString(available);
                    }
                }
            } finally {
                lock.unlock();
            }
            if (updateAvailable != null) {
                writeToDisk(updateAvailable);
            }
        }

        public synchronized void writeToDisk(String availableRanges) {
            FileWriter out = null;
            try {
                out = new FileWriter(local);
                out.write(availableRanges);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private static class UniqueIdSourceUpdater extends Thread implements Thread.UncaughtExceptionHandler {

        {
            setDefaultUncaughtExceptionHandler(this);
        }

        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            List<WeakReference<Runnable>> discard = new LinkedList<WeakReference<Runnable>>();
            List<WeakReference<Runnable>> updaters = null;
            while (true) {
                try {
                    synchronized (KNOWN_ID_SOURCE_UPDATERS) {
                        if (updaters == null || updaters.size() != KNOWN_ID_SOURCE_UPDATERS.size()) {
                            updaters = new ArrayList<WeakReference<Runnable>>(new ArrayList<WeakReference<Runnable>>(KNOWN_ID_SOURCE_UPDATERS));
                        }
                    }
                    for (WeakReference<Runnable> ref : updaters) {
                        Runnable updater = ref.get();
                        if (updater != null) {
                            try {
                                updater.run();
                            } catch (Throwable e) {
                                this.getUncaughtExceptionHandler().uncaughtException(this, e);
                            }
                        } else {
                            discard.add(ref);
                        }
                    }
                    if (!discard.isEmpty()) {
                        KNOWN_ID_SOURCE_UPDATERS.removeAll(discard);
                        discard.clear();
                        updaters = null; // force a refresh
                    }
                } catch (Exception e) {
                    this.getUncaughtExceptionHandler().uncaughtException(this, e);
                }
                synchronized (KNOWN_ID_SOURCE_UPDATERS) {
                    try {
                        KNOWN_ID_SOURCE_UPDATERS.wait(5000L);
                    } catch (InterruptedException e) {
                        // don't care
                    }
                }
            }
        }

        public void uncaughtException(Thread t, Throwable e) {
            e.printStackTrace();
        }
    }
}
