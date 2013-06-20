package net.venaglia.realms.common.util.work;

import java.util.NoSuchElementException;

/**
 * User: ed
 * Date: 1/23/13
 * Time: 5:59 PM
 */
public interface Results {

    <T> T getResult(WorkSourceKey<T> key) throws NoSuchElementException, CircularDependencyException;
}
