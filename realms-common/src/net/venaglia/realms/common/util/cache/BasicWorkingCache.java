package net.venaglia.realms.common.util.cache;

import net.venaglia.realms.common.util.Identifiable;

/**
 * User: ed
 * Date: 3/21/14
 * Time: 10:37 PM
 */
public class BasicWorkingCache<E extends Identifiable> extends AbstractWorkingCache<E, AbstractWorkingCache.Node<E>> {

    protected Node<E> createEmptyNode() {
        return new Node<E>();
    }
}
