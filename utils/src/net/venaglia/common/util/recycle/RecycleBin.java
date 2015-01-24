package net.venaglia.common.util.recycle;

import net.venaglia.common.util.Factory;

import java.lang.ref.WeakReference;

/**
 * User: ed
 * Date: 10/9/14
 * Time: 8:05 AM
 */
public class RecycleBin<E extends Recyclable<E>> {

    protected final Factory<E> factory;
    protected final RecycleDeque<WeakReference<E>> recycleDeque;

    protected RecyleDequeOrder order = RecyleDequeOrder.FIFO;
    protected int limit = 0;

    public RecycleBin(Factory<E> factory, RecycleDeque<WeakReference<E>> recycleDeque) {
        this.factory = factory;
        this.recycleDeque = recycleDeque;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOrder(RecyleDequeOrder order) {
        this.order = order;
    }

    public int size() {
        return recycleDeque.size();
    }

    public E get() {
        E result = null;
        do {
            WeakReference<E> ref = order.next(recycleDeque);
            if (ref == null) {
                break;
            }
            result = ref.get();
        } while (result == null);
        if (result == null) {
            result = factory.createEmpty();
        }
        return result;
    }

    public void put (E value) {
        WeakReference<E> ref = value.getMyWeakReference();
        if (limit > 0 && limit > size()) {
            recycleDeque.add(ref);
        }
    }
}
