package net.venaglia.common.util.impl;

import net.venaglia.common.util.Lock;
import net.venaglia.common.util.LockedException;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 11:53 AM
 */
public final class SimpleLock implements Lock {

    private Runnable runWhenLocked = null;
    private boolean locked = false;

    public SimpleLock() {
    }

    public SimpleLock(Runnable runWhenLocked) {
        this.runWhenLocked = runWhenLocked;
    }

    public void assertUnlocked() throws LockedException {
        if (locked) {
            throw new LockedException();
        }
    }

    public void lock() {
        if (!locked) {
            locked = true;
            if (runWhenLocked != null) {
                runWhenLocked.run();
                runWhenLocked = null;
            }
        }
    }

    public boolean isLocked() {
        return locked;
    }
}
