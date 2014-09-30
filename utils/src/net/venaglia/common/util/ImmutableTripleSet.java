package net.venaglia.common.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 2/14/13
 * Time: 10:17 AM
 */
public class ImmutableTripleSet<E> extends AbstractSet<E> implements Tuple3<E,E,E> {

    private final E a;
    private final E b;
    private final E c;

    public ImmutableTripleSet(E a, E b, E c) {
        if (a == null || b == null || c == null) {
            throw new NullPointerException();
        }
        if (a.equals(b) || b.equals(c) || a.equals(c)) {
            throw new IllegalArgumentException("Some passed values are equal and would produce a set containing less than 3 elements.");
        }
        this.a = a;
        this.b = b;
        this.c = c;
    }

    public E getA() {
        return a;
    }

    public E getB() {
        return b;
    }

    public E getC() {
        return c;
    }

    public ImmutableTripleSet<E> reverse() {
        return new ImmutableTripleSet<E>(c,b,a);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {

            private int i = 0;

            public boolean hasNext() {
                return i < 3;
            }

            public E next() {
                if (i >= 3) {
                    throw new NoSuchElementException();
                }
                switch (++i) {
                    case 1: return a;
                    case 2: return b;
                    case 3: return c;
                    default: throw new NoSuchElementException();
                }
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
