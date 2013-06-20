package net.venaglia.gloo.view;

import java.util.BitSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * User: ed
 * Date: 4/17/13
 * Time: 8:22 AM
 */
public class KeyboardManager implements KeyboardEventHandler {

    private final BitSet keysDown = new BitSet(256);
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void keyDown(int keyCode) {
        lock.writeLock().lock();
        try {
            keysDown.set(keyCode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void keyUp(int keyCode) {
        lock.writeLock().lock();
        try {
            keysDown.clear(keyCode);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean isDown(int keyCode) {
        return keysDown.get(keyCode);
    }
}
