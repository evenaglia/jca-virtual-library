package net.venaglia.realms.common.util.work;

/**
 * User: ed
 * Date: 1/23/13
 * Time: 5:50 PM
 */
public interface WorkSource<RESULT> {

    WorkSourceKey<RESULT> getKey();

    WorkSourceKey<?>[] getDependencies();

    void addWork(WorkQueue workQueue, Results dependencies);

    RESULT getResult();
}
