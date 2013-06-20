package net.venaglia.realms.common.util.work;

/**
 * User: ed
 * Date: 1/23/13
 * Time: 5:51 PM
 */
public interface WorkQueue {

    void addWorkUnit(Runnable runnable);
}
