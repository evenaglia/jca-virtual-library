package net.venaglia.realms.common.util.work;

import net.venaglia.common.util.StronglyTypedKey;

/**
 * User: ed
 * Date: 1/25/13
 * Time: 8:41 AM
 */
public final class WorkSourceKey<T> extends StronglyTypedKey<T> {

    public static final WorkSourceKey<?>[] NO_DEPENDENCIES = {};

    private WorkSourceKey(String name, Class<T> type) {
        super(name, type);
    }

    @SuppressWarnings("unchecked")
    public static <T> WorkSourceKey<T> create(String name, Class<?> type) {
        return new WorkSourceKey<T>(name, (Class<T>)type);
    }
}
