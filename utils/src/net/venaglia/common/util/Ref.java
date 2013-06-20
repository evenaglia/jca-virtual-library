package net.venaglia.common.util;

import java.io.Serializable;

/**
 * User: ed
 * Date: 1/28/13
 * Time: 8:45 AM
 *
 * An interface for abstract references to other objects. References are typically used as lightweight objects that lazily load more expensive objects.
 */
public interface Ref<T> extends Serializable {

    T get();
}
