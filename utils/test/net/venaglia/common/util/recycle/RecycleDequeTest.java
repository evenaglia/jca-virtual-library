package net.venaglia.common.util.recycle;

import java.lang.reflect.Field;

/**
 * User: ed
 * Date: 10/11/14
 * Time: 8:14 AM
 */
public class RecycleDequeTest {

    public static void main(String[] args) throws Exception {
        Field buffer = RecycleDeque.class.getDeclaredField("buffer");
        buffer.setAccessible(true);
        RecycleDeque<Character> deque = new RecycleDeque<Character>();
        String set = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/+";
        for (int i = 0; i < 64; i++) {
            deque.add(set.charAt(i % set.length()));
        }
        System.out.printf("RecycleDeque has %d elements\n", deque.size());
        Object[] b = (Object[])buffer.get(deque);
        System.out.printf("RecycleDeque has %s capacity\n", b.length);
        System.out.print("Elements: ");
        for (Character c : deque) {
            System.out.print(c);
        }
        System.out.println();
    }
}
