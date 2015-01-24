package net.venaglia.common.util;

/**
 * User: ed
 * Date: 1/16/15
 * Time: 10:50 AM
 */
public interface Lookup<K,V> {

    V get(K key);
}
