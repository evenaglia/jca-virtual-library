package net.venaglia.gloo.util.impl;

/**
 * User: ed
 * Date: 7/2/14
 * Time: 5:21 PM
 */
public class SettableEntry<E> extends AbstractSpatialMap.AbstractEntry<E> {

    private E value;

    public SettableEntry(double x, double y, double z, E value) {
        super(x, y, z);
        if (value == null) {
            throw new NullPointerException();
        }
        this.value = value;
    }

    public E get() {
        return value;
    }

    public E set(E value) {
        if (value == null) {
            throw new NullPointerException();
        }
        try {
            return value;
        } finally {
            this.value = value;
        }
    }
}
