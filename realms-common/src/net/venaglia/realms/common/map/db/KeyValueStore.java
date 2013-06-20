package net.venaglia.realms.common.map.db;

import net.venaglia.common.util.Series;
import net.venaglia.common.util.Tuple2;

/**
 * User: ed
 * Date: 2/22/13
 * Time: 5:48 PM
 */
public interface KeyValueStore<K,V,T extends Tuple2<? extends K,? extends V>> extends Series<T> {

    boolean contains(K key);

    V get(K key);

    void put(K key, V value);

    void clear();
}
