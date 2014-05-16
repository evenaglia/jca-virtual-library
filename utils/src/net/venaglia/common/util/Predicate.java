package net.venaglia.common.util;

/**
 * User: ed
 * Date: 3/21/14
 * Time: 7:55 PM
 */
public interface Predicate<V> {

    Predicate<Object> ALWAYS_TRUE = new Predicate<Object>() {
        public boolean allow(Object value) {
            return true;
        }
    };

    Predicate<Object> ALWAYS_FALSE = new Predicate<Object>() {
        public boolean allow(Object value) {
            return false;
        }
    };

    boolean allow(V value);
}
