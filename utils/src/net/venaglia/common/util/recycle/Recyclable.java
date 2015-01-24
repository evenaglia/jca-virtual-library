package net.venaglia.common.util.recycle;

import java.lang.ref.WeakReference;

/**
 * User: ed
 * Date: 10/9/14
 * Time: 8:28 AM
 */
public interface Recyclable<R extends Recyclable<R>> {

    WeakReference<R> getMyWeakReference();
}
