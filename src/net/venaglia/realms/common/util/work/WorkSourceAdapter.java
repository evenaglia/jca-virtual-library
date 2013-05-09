package net.venaglia.realms.common.util.work;

import java.util.concurrent.atomic.AtomicReference;

/**
* User: ed
* Date: 1/23/13
* Time: 11:28 PM
*/
public abstract class WorkSourceAdapter<T> implements WorkSource<T> {

    protected final WorkSourceKey<T> key;
    protected final WorkSourceKey<?>[] dependencies;
    protected final AtomicReference<T> resultBuffer = new AtomicReference<T>();

    public WorkSourceAdapter(WorkSourceKey<T> key, WorkSourceKey<?>... dependencies) {
        this.key = key;
        this.dependencies = dependencies;
    }

    public String getName() {
        return key.getName();
    }

    public WorkSourceKey<T> getKey() {
        return key;
    }

    public WorkSourceKey<?>[] getDependencies() {
        return dependencies;
    }

    public T getResult() {
        return resultBuffer.get();
    }
}
