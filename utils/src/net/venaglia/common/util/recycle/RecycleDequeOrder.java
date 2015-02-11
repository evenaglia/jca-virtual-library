package net.venaglia.common.util.recycle;

/**
* User: ed
* Date: 10/13/14
* Time: 8:10 AM
*/
public enum RecycleDequeOrder {
    LIFO{
        @Override
        <T> T next(RecycleDeque<T> deque) {
            return deque.pollLast();
        }
    }, FIFO {
        @Override
        <T> T next(RecycleDeque<T> deque) {
            return deque.pollFirst();
        }
    };

    abstract <T> T next(RecycleDeque<T> deque);
}
