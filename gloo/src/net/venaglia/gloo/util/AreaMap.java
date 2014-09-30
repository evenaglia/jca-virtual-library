package net.venaglia.gloo.util;

import net.venaglia.common.util.Ref;

/**
 * User: ed
 * Date: 9/10/12
 * Time: 9:57 PM
 */
public interface AreaMap<E> extends Iterable<AreaMap.Entry<E>> {

    int size();

    boolean isEmpty();

    boolean contains(double x, double y);

    E get(double x, double y);

    E get(double x, double y, double r);

    Bounds2D getBounds();

    void clear();

    boolean add(E obj, double x, double y) throws UnsupportedOperationException;

    int intersect(Bounds2D region, Consumer<E> consumer);

    public interface Entry<S> extends Ref<S> {

        public double getX();

        public double getY();

        public boolean move(double x, double y) throws IndexOutOfBoundsException, UnsupportedOperationException;

        public boolean remove() throws UnsupportedOperationException;
    }

    public interface Consumer<S> {
        void found(Entry<S> entry, double x, double y);
    }
}
