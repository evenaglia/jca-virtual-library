package net.venaglia.realms.builder.terraform.impl;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * User: ed
 * Date: 11/28/14
 * Time: 10:43 AM
 */
public class AtomicDoubleArray {

    private final AtomicLongArray internal;

    public AtomicDoubleArray(int length) {
        assert length >= 0;
        internal = new AtomicLongArray(length);
    }

    public double get(int index) {
        return longBitsToDouble(internal.get(index));
    }

    public double getAndSet(int index, double value) {
        return longBitsToDouble(internal.getAndSet(index, doubleToLongBits(value)));
    }

    public boolean compareAndSet(int index, double expected, double value) {
        return internal.compareAndSet(index, doubleToLongBits(expected), doubleToLongBits(value));
    }

    public double addAndGet(int index, double add) {
        double result;
        boolean updated = false;
        do {
            long oldValue = internal.get(index);
            result = longBitsToDouble(oldValue) + add;
            updated = internal.compareAndSet(index, oldValue, doubleToLongBits(result));
        } while (!updated);
        return result;
    }

    public void fill(double value) {
        long v = doubleToLongBits(value);
        for (int i = 0, l = internal.length(); i < l; i++) {
            internal.set(i, v);
        }
    }
}
