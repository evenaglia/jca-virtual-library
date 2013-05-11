package net.venaglia.realms.common.util.impl;

import net.venaglia.realms.common.util.Ref;

/**
 * User: ed
 * Date: 5/11/13
 * Time: 10:03 AM
 */
public class SimpleRef<T> implements Ref<T> {

    private final T value;

    public SimpleRef(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    public static <T> SimpleRef<T> create(T value) {
        return new SimpleRef<T>(value);
    }
}
