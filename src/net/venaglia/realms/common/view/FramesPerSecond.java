package net.venaglia.realms.common.view;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* User: ed
* Date: 2/19/13
* Time: 8:23 AM
*/
public class FramesPerSecond {
    private final int bufferSize;
    private final long[] nsAtFrame;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int index = 0;
    private long tickCount = 0;

    public FramesPerSecond(int bufferSize) {
        this.bufferSize = bufferSize;
        this.nsAtFrame = new long[bufferSize];
    }

    public void tick() {
        long now = System.nanoTime();
        lock.writeLock().lock();
        try {
            tickCount++;
            nsAtFrame[index] = now;
            index = (index + 1) % bufferSize;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private double calculate(long nsTarget) {
        lock.readLock().lock();
        try {
            if (tickCount == 0) {
                return Double.NaN; // not enough information
            }
            int index = (binarySearch(0, bufferSize, nsTarget) + this.index + 1) % bufferSize;
            if (tickCount < bufferSize && index <= bufferSize - tickCount || index == 0) {
                return Double.NaN; // not enough information
            }
            int count = index < this.index ? this.index - index : bufferSize - this.index + index;
            return count * 1000000000.0 / nsAtFrame[index];
        } finally {
            lock.readLock().unlock();
        }
    }

    private int binarySearch(int low, int high, long find) {
        int mid;
        while (low <= high) {
            mid = (low + high) / 2;
            long test = nsAtFrame[(mid + index + 1) % bufferSize];
            if (test < find) {
                low = mid + 1;
            } else if (test > find) {
                high = mid - 1;
            } else {
                if (high == mid) return mid;
                high = mid;
            }
        }
        return low;
    }

}
