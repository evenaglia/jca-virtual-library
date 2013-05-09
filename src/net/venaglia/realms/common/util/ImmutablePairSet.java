package net.venaglia.realms.common.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 2/14/13
 * Time: 10:17 AM
 */
public class ImmutablePairSet<E> extends AbstractSet<E> implements Tuple2<E,E> {

    private final E a;
    private final E b;

    public ImmutablePairSet(E a, E b) {
        if (a == null || b == null) {
            throw new NullPointerException();
        }
        if (a.equals(b)) {
            throw new IllegalArgumentException("Both passed values are equal and would produce a set containing 1 element.");
        }
        this.a = a;
        this.b = b;
    }

    public E getA() {
        return a;
    }

    public E getB() {
        return b;
    }

    public ImmutablePairSet<E> reverse() {
        return new ImmutablePairSet<E>(b,a);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {

            private int i = 0;

            public boolean hasNext() {
                return i < 2;
            }

            public E next() {
                if (i >= 2) {
                    throw new NoSuchElementException();
                }
                i++;
                return i == 1 ? a : b;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
