package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.util.Lock;
import net.venaglia.realms.common.util.LockedException;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 11:53 AM
 */
public final class ThreadSafeLock implements Lock {

    private final AtomicBoolean locked = new AtomicBoolean();
    private final AtomicReference<Runnable> runWhenLocked;

    public ThreadSafeLock() {
        this.runWhenLocked = null;
    }

    public ThreadSafeLock(Runnable runWhenLocked) {
        this.runWhenLocked = new AtomicReference<Runnable>(runWhenLocked);
    }

    public void assertUnlocked() throws LockedException {
        if (locked.get()) {
            throw new LockedException();
        }
    }

    public void lock() {
        if (locked.compareAndSet(false, true)) {
            Runnable runnable = runWhenLocked == null ? null : runWhenLocked.getAndSet(null);
            if (runnable != null) {
                runnable.run();
            }
        }
    }

    public boolean isLocked() {
        return locked.get();
    }
}
