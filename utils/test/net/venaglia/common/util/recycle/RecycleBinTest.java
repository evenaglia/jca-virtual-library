package net.venaglia.common.util.recycle;

import net.venaglia.common.util.Factory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 10/10/14
 * Time: 8:06 PM
 */
public class RecycleBinTest {

    private final List<Thread> activeThreads = new ArrayList<Thread>();
    private final RecycleBin<MyRecycleable> bin;
    private final long seed;
    private final long runUntil;
    private final AtomicInteger markCounter = new AtomicInteger();
    private final AtomicInteger objectsTaken = new AtomicInteger();
    private final AtomicInteger objectsRecycled = new AtomicInteger();

    public RecycleBinTest(RecycleBin<MyRecycleable> bin, long runUntil) {
        if (bin == null) {
            throw new NullPointerException("bin");
        }
        this.bin = bin;
        this.seed = System.currentTimeMillis();
        this.runUntil = runUntil;
    }

    public void runTest(Random rand, float chanceOfRecycle) {
        while (System.currentTimeMillis() < runUntil) {
            try {
                MyRecycleable obj = bin.get();
                objectsTaken.incrementAndGet();
                outputMark('.');
                try {
                    Thread.sleep(rand.nextInt(333) + 10);
                } catch (InterruptedException e) {
                    // don't care
                }
                if (rand.nextFloat() < chanceOfRecycle) {
                    bin.put(obj);
                    outputMark('x');
                    objectsRecycled.incrementAndGet();
                } else {
                    outputMark('o');
                }
            } catch (Throwable t) {
                Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), t);
            }
        }
    }

    private void outputMark(char c) {
        if ((markCounter.getAndIncrement() & 0x7F) == 0x7F) {
            System.out.println(c);
        } else {
            System.out.print(c);
        }
    }

    private static class MyRecycleable implements Recyclable<MyRecycleable> {

        private WeakReference<MyRecycleable> ref = null;

        public WeakReference<MyRecycleable> getMyWeakReference() {
            if (ref == null) {
                ref = new WeakReference<MyRecycleable>(this);
            }
            return ref;
        }
    }

    public void startThread(final float chanceOfRecycle) {
        final int seq = this.activeThreads.size() + 1;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                Random random = new Random(seed + seq);
                runTest(random, chanceOfRecycle);
            }
        }, "RecycleBinTest-" + seq);
        thread.setDaemon(true);
        thread.start();
        activeThreads.add(thread);
    }

    public void join() {
        for (Thread thread : activeThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // don't care
            }
        }
    }

    public static void main(String[] args) {
        final long runUntil = System.currentTimeMillis() + 60000; // 60 seconds
        final AtomicInteger objectsCreated = new AtomicInteger();
        final AtomicReference<RecycleDeque<WeakReference<MyRecycleable>>> parkingRef =
                new AtomicReference<RecycleDeque<WeakReference<MyRecycleable>>>();
        final Factory<MyRecycleable> factory = new Factory<MyRecycleable>() {
            public MyRecycleable createEmpty() {
                objectsCreated.incrementAndGet();
                return new MyRecycleable();
            }
        };
        final RecycleDeque<WeakReference<MyRecycleable>> recycleDeque = new RecycleDeque<WeakReference<MyRecycleable>>();
        final RecycleBin<MyRecycleable> recycleBin = new RecycleBin<MyRecycleable>(factory, recycleDeque);
        recycleBin.setLimit(64);
        RecycleBinTest test = new RecycleBinTest(recycleBin, runUntil);
        for (int i = 0; i < 64; i++) {
            test.startThread(0.99f);
        }
        test.join();
        System.out.println();
        System.out.printf("Objects taken: %d (%d reused)\n" +
                          "Objects created: %d\n" +
                          "Final buffer size: %d\n",
                          test.objectsTaken.get(),
                          test.objectsTaken.get() - objectsCreated.get(),
                          objectsCreated.get(),
                          parkingRef.get().size());
    }
}
