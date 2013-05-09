package net.venaglia.realms.common.util;

/**
 * User: ed
 * Date: 9/2/12
 * Time: 11:46 AM
 */
public interface Lock {

    static final Lock NEVER_LOCKED = new Lock() {
        public void assertUnlocked() throws LockedException {
            // no-op
        }

        public void lock() {
            throw new UnsupportedOperationException();
        }

        public boolean isLocked() {
            return false;
        }
    };

    static final Lock ALWAYS_LOCKED = new Lock() {
        public void assertUnlocked() throws LockedException {
            throw new LockedException();
        }

        public void lock() {
            // no-op
        }

        public boolean isLocked() {
            return true;
        }
    };

    void assertUnlocked() throws LockedException;

    void lock();

    boolean isLocked();
}
