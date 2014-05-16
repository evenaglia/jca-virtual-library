package net.venaglia.realms.common.util.cache;

import net.venaglia.common.util.Series;
import net.venaglia.realms.common.util.Identifiable;

import java.util.Collection;

/**
 * User: ed
 * Date: 3/29/14
 * Time: 4:12 PM
 */
public interface Cache<E extends Identifiable> extends Series<E> {

    int getModCount();

    E get(Long id);

    boolean seed(E value);

    boolean seed(Collection<? extends E> values);

    boolean remove(E value);

    void clear();

    void evictOldest();
}
